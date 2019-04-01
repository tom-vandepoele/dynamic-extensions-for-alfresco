package com.github.dynamicextensionsalfresco.webscripts;

import com.github.dynamicextensionsalfresco.webscripts.annotations.Attribute;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Before;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Cache;
import com.github.dynamicextensionsalfresco.webscripts.annotations.ExceptionHandler;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Transaction;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;
import com.github.dynamicextensionsalfresco.webscripts.arguments.HandlerMethodArgumentsResolver;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.extensions.webscripts.Description;
import org.springframework.extensions.webscripts.Description.FormatStyle;
import org.springframework.extensions.webscripts.Description.Lifecycle;
import org.springframework.extensions.webscripts.Description.RequiredAuthentication;
import org.springframework.extensions.webscripts.Description.RequiredTransaction;
import org.springframework.extensions.webscripts.Description.TransactionCapability;
import org.springframework.extensions.webscripts.DescriptionImpl;
import org.springframework.extensions.webscripts.TransactionParameters;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Creates {@link AnnotationWebScript} instances from beans defined in a {@link BeanFactory}.
 *
 * @author Laurens Fridael
 * @author Laurent Van der Linden
 */
public final class AnnotationWebScriptBuilder implements BeanFactoryAware {

    @Autowired
    private HandlerMethodArgumentsResolver handlerMethodArgumentsResolver;
    @Autowired
    private MessageConverterRegistry messageConverterRegistry;

    public final void setHandlerMethodArgumentsResolver(@NotNull HandlerMethodArgumentsResolver value) {
        Intrinsics.checkParameterIsNotNull(value, "value");
        this.handlerMethodArgumentsResolver = value;
    }

    public final void setMessageConverterRegistry(@NotNull MessageConverterRegistry value) {
        Intrinsics.checkParameterIsNotNull(value, "value");
        this.messageConverterRegistry = value;
    }

    /* Dependencies */

    @Nullable
    private ConfigurableListableBeanFactory beanFactory;
    private final String trailingSlashExpression = "/$";
    private final String leadingSlashExpression = "^/";

    @Nullable
    protected final ConfigurableListableBeanFactory getBeanFactory() {
        return this.beanFactory;
    }

    /* Main operations */

    /**
     * Creates {@link AnnotationWebScript}s from a given named bean by scanning methods annotated with {@link Uri}.
     *
     * @return The {@link AnnotationWebScript} or null if the implementation does not consider the bean to be a handler
     * *         for an {@link AnnotationWebScript}.
     */
    @NotNull
    public final List<org.springframework.extensions.webscripts.WebScript> createWebScripts(
            @NotNull final String beanName) {
        Assert.hasText(beanName, "Bean name cannot be empty.");

        if (beanFactory == null) {
            throw new IllegalStateException("beanFactory");
        }
        Class<?> beanType = beanFactory.getType(beanName);
        if (beanType == null) {
            return Collections.emptyList();
        }
        final WebScript webScriptAnnotation =
                beanFactory.findAnnotationOnBean(beanName, WebScript.class) != null
                        ? beanFactory.findAnnotationOnBean(beanName, WebScript.class)
                        : getDefaultWebScriptAnnotation();
        String baseUri = webScriptAnnotation.baseUri();
        if (StringUtils.hasText(baseUri) && !baseUri.startsWith("/")) {
            throw new RuntimeException("@WebScript baseUri for class '" + beanType + "' does not start with a slash: '"
                    + baseUri + "'");
        }

        final HandlerMethods handlerMethods = new HandlerMethods();
        ReflectionUtils.doWithMethods(beanType, (method) -> {
                    Intrinsics.checkParameterIsNotNull(method, "method");
                    Before before = AnnotationUtils.findAnnotation(method, Before.class);
                    if (before != null) {
                        if (AnnotationUtils.findAnnotation(method, Attribute.class) != null
                                || AnnotationUtils.findAnnotation(method, Uri.class) != null) {
                            throw new RuntimeException(
                                    "Cannot combine @Before, @Attribute and @Uri on a single method. Method: "
                                            + ClassUtils.getQualifiedMethodName(method));
                        }
                        handlerMethods.getBeforeMethods().add(method);
                    }
                }
        );
        ReflectionUtils.doWithMethods(beanType, (method) -> {
                    Intrinsics.checkParameterIsNotNull(method, "method");
                    Attribute attribute = AnnotationUtils.findAnnotation(method, Attribute.class);
                    if (attribute != null) {
                        if (AnnotationUtils.findAnnotation(method, Before.class) != null
                                || AnnotationUtils.findAnnotation(method, Uri.class) != null) {
                            throw new RuntimeException(
                                    "Cannot combine @Before, @Attribute and @Uri on a single method. Method: "
                                            + ClassUtils.getQualifiedMethodName(method));
                        }
                        if (Void.TYPE.equals(method.getReturnType())) {
                            throw new RuntimeException("@Attribute methods cannot have a void return type.");
                        }
                        handlerMethods.getAttributeMethods().add(method);
                    }
                }
        );
        ReflectionUtils.doWithMethods(beanType, (method) -> {
                    Intrinsics.checkParameterIsNotNull(method, "method");
                    ExceptionHandler exceptionHandler = AnnotationUtils.findAnnotation(method, ExceptionHandler.class);
                    if (exceptionHandler != null) {
                        if (AnnotationUtils.findAnnotation(method, Attribute.class) != null
                                || AnnotationUtils.findAnnotation(method, Before.class) != null
                                || AnnotationUtils.findAnnotation(method, Uri.class) != null) {
                            throw new RuntimeException(
                                    "Cannot combine @Before, @Attribute @ExceptionHandler or @Uri on a single method. Method: "
                                            + ClassUtils.getQualifiedMethodName(method));
                        }
                        handlerMethods.getExceptionHandlerMethods().add(new ExceptionHandlerMethod(exceptionHandler, method));
                    }
                }
        );

        final ArrayList<org.springframework.extensions.webscripts.WebScript> webScripts = new ArrayList<>();
        ReflectionUtils.doWithMethods(beanType, (method) -> {
                    Intrinsics.checkParameterIsNotNull(method, "method");
                    Uri uri = AnnotationUtils.findAnnotation(method, Uri.class);
                    if (uri != null) {
                        org.springframework.extensions.webscripts.WebScript webScript =
                                createWebScript(beanName, webScriptAnnotation, uri, handlerMethods.createForUriMethod(method));
                        webScripts.add(webScript);
                    }

                }
        );

        HashSet<String> ids = new HashSet<>();
        for (org.springframework.extensions.webscripts.WebScript webScript : webScripts) {
            final String webscriptId = webScript.getDescription().getId();
            final boolean notContained = ids.add(webscriptId);
            if (!notContained) {
                throw new IllegalStateException("Duplicate Web Script ID \"" + webscriptId
                        + "\" Make sure handler methods of annotation-based Web Scripts have unique names.");
            }
        }

        return webScripts;
    }

    /* Utility operations */

    @NotNull
    protected final AnnotationWebScript createWebScript(@NotNull String beanName, @NotNull WebScript webScript,
            @NotNull Uri uri, @NotNull HandlerMethods handlerMethods) {
        Intrinsics.checkParameterIsNotNull(beanName, "beanName");
        Intrinsics.checkParameterIsNotNull(webScript, "webScript");
        Intrinsics.checkParameterIsNotNull(uri, "uri");
        Intrinsics.checkParameterIsNotNull(handlerMethods, "handlerMethods");
        DescriptionImpl description = new DescriptionImpl();
        if (StringUtils.hasText(webScript.defaultFormat())) {
            description.setDefaultFormat(webScript.defaultFormat());
        }
        final String baseUri = webScript.baseUri();
        this.handleHandlerMethodAnnotation(uri, handlerMethods.getUriMethod(), description, baseUri);
        this.handleTypeAnnotations(beanName, webScript, description);
        String id = String.format("%s.%s.%s", generateId(beanName), handlerMethods.getUriMethod().getName(),
                description.getMethod().toLowerCase());
        description.setId(id);

        if (beanFactory == null) {
            throw new IllegalStateException("beanFactory");
        }
        Object handler = beanFactory.getBean(beanName);
        description.setStore(new DummyStore());
        return createWebScript(description, handler, handlerMethods);
    }

    @NotNull
    protected final AnnotationWebScript createWebScript(@NotNull Description description, @NotNull Object handler,
            @NotNull HandlerMethods handlerMethods) {
        Intrinsics.checkParameterIsNotNull(description, "description");
        Intrinsics.checkParameterIsNotNull(handler, "handler");
        Intrinsics.checkParameterIsNotNull(handlerMethods, "handlerMethods");
        return new AnnotationWebScript(description, handler, handlerMethods, this.handlerMethodArgumentsResolver,
                this.messageConverterRegistry);
    }

    protected final void handleHandlerMethodAnnotation(@NotNull Uri uri, @NotNull Method method,
            @NotNull DescriptionImpl description, @NotNull String baseUri) {
        Assert.notNull(uri, "Uri cannot be null.");
        Assert.notNull(method, "HttpMethod cannot be null.");
        Assert.notNull(description, "Description cannot be null.");
        Intrinsics.checkParameterIsNotNull(baseUri, "baseUri");

        String[] uris;
        if (uri.value().length > 0) {
            uris = Arrays.stream(uri.value())
                    .map(it -> baseUri.replaceAll(trailingSlashExpression, "") + "/" +
                            it.replaceAll(leadingSlashExpression, ""))
                    .toArray(String[]::new);
        } else if (StringUtils.hasText(baseUri)) {
            uris = new String[]{baseUri.replaceAll(trailingSlashExpression, "")};
        } else {
            throw new RuntimeException(
                    String.format(
                            "No value specified for @Uri on method '%s' and no base URI found for @WebScript on class."
                            , ClassUtils.getQualifiedMethodName(method))
            );
        }
        description.setUris(uris);
        /*
         * For the sake of consistency we translate the HTTP method from the HttpMethod enum. This also shields us from
         * changes in the HttpMethod enum names.
         */
        description.setMethod(uri.method().name());
        /*
         * Idem dito for FormatStyle.
         */
        Description.FormatStyle springFormatStyle;
        switch (uri.formatStyle()) {
            case ANY:
                springFormatStyle = FormatStyle.any;
                break;
            case ARGUMENT:
                springFormatStyle = FormatStyle.argument;
                break;
            case EXTENSION:
                springFormatStyle = FormatStyle.extension;
                break;
            default:
                throw new IllegalArgumentException("Unknown format style: " + uri.formatStyle());
        }
        description.setFormatStyle(springFormatStyle);
        if (StringUtils.hasText(uri.defaultFormat())) {
            description.setDefaultFormat(uri.defaultFormat());
        }
        description.setMultipartProcessing(uri.multipartProcessing());

        Authentication methodAuthentication = method.getAnnotation(Authentication.class);
        if (methodAuthentication != null) {
            this.handleAuthenticationAnnotation(methodAuthentication, description);
        }

        Transaction methodTransaction = method.getAnnotation(Transaction.class);
        if (methodTransaction != null) {
            this.handleTransactionAnnotation(methodTransaction, description);
        }
    }

    protected final void handleTypeAnnotations(@NotNull String beanName, @NotNull WebScript webScript,
            @NotNull DescriptionImpl description) {
        Intrinsics.checkParameterIsNotNull(beanName, "beanName");
        Intrinsics.checkParameterIsNotNull(webScript, "webScript");
        Intrinsics.checkParameterIsNotNull(description, "description");

        this.handleWebScriptAnnotation(webScript, beanName, description);

        if (this.beanFactory == null) {
            throw new IllegalStateException("beanFactory");
        }

        if (description.getRequiredAuthentication() == null) {
            Authentication authentication = beanFactory.findAnnotationOnBean(beanName, Authentication.class);
            if (authentication == null) {
                authentication = this.getDefaultAuthenticationAnnotation();
            }
            this.handleAuthenticationAnnotation(authentication, description);
        }

        if (description.getRequiredTransactionParameters() == null) {
            Transaction transaction = beanFactory.findAnnotationOnBean(beanName, Transaction.class);
            if (transaction == null) {
                if (description.getMethod().equals("GET")) {
                    transaction = this.getDefaultReadonlyTransactionAnnotation();
                } else {
                    transaction = this.getDefaultReadWriteTransactionAnnotation();
                }
            }

            this.handleTransactionAnnotation(transaction, description);
        }

        Cache cache = beanFactory.findAnnotationOnBean(beanName, Cache.class);
        if (cache == null) {
            cache = this.getDefaultCacheAnnotation();
        }
        this.handleCacheAnnotation(cache, beanName, description);

        description.setDescPath("");
    }

    protected final void handleWebScriptAnnotation(@NotNull WebScript webScript, @NotNull String beanName,
            @NotNull DescriptionImpl description) {
        Assert.notNull(webScript, "Annotation cannot be null.");
        Assert.hasText(beanName, "Bean name cannot be empty.");
        Assert.notNull(description, "Description cannot be null.");
        Assert.hasText(description.getMethod(), "Description method is not specified.");

        if (StringUtils.hasText(webScript.value())) {
            description.setShortName(webScript.value());
        } else {
            description.setShortName(this.generateShortName(beanName));
        }
        if (StringUtils.hasText(webScript.description())) {
            description.setDescription(webScript.description());
        } else {
            if (beanFactory == null) {
                throw new IllegalStateException("beanFactory");
            }
            description.setDescription(
                    String.format("Annotation-based WebScript for class %s", beanFactory.getType(beanName).getName()));
        }
        if (webScript.families().length > 0) {
            description.setFamilys(new LinkedHashSet<>(Arrays.asList(webScript.families())));
        }
        Lifecycle springLifeCycle;
        switch (webScript.lifecycle()) {
            case NONE:
                springLifeCycle = Lifecycle.none;
                break;
            case DRAFT:
                springLifeCycle = Lifecycle.draft;
                break;
            case DRAFT_PUBLIC_API:
                springLifeCycle = Lifecycle.draft_public_api;
                break;
            case DEPRECATED:
                springLifeCycle = Lifecycle.deprecated;
                break;
            case INTERNAL:
                springLifeCycle = Lifecycle.internal;
                break;
            case PUBLIC_API:
                springLifeCycle = Lifecycle.public_api;
                break;
            case SAMPLE:
                springLifeCycle = Lifecycle.sample;
                break;
            default:
                throw new IllegalArgumentException("Unknown lifeCycle '" + webScript.lifecycle() + "'");
        }
        description.setLifecycle(springLifeCycle);
    }

    protected final void handleAuthenticationAnnotation(@NotNull Authentication authentication,
            @NotNull DescriptionImpl description) {
        Assert.notNull(authentication, "Annotation cannot be null.");
        Assert.notNull(description, "Description cannot be null.");

        if (StringUtils.hasText(authentication.runAs())) {
            description.setRunAs(authentication.runAs());
        }
        RequiredAuthentication requiredAuthentication;
        switch (authentication.value()) {
            case NONE:
                requiredAuthentication = RequiredAuthentication.none;
                break;
            case GUEST:
                requiredAuthentication = RequiredAuthentication.guest;
                break;
            case USER:
                requiredAuthentication = RequiredAuthentication.user;
                break;
            case ADMIN:
                requiredAuthentication = RequiredAuthentication.admin;
                break;
            default:
                throw new IllegalArgumentException("Unknown authentication value '" + authentication.value() + "'");
        }
        description.setRequiredAuthentication(requiredAuthentication);
    }

    protected final void handleTransactionAnnotation(@NotNull Transaction transaction,
            @NotNull DescriptionImpl description) {
        Assert.notNull(transaction, "Annotation cannot be null.");
        Assert.notNull(description, "Description cannot be null.");

        TransactionParameters transactionParameters = new TransactionParameters();
        RequiredTransaction requiredTransaction;
        switch (transaction.value()) {
            case NONE:
                requiredTransaction = RequiredTransaction.none;
                break;
            case REQUIRED:
                requiredTransaction = RequiredTransaction.required;
                break;
            case REQUIRES_NEW:
                requiredTransaction = RequiredTransaction.requiresnew;
                break;
            default:
                throw new IllegalArgumentException("Unknown transaction value '" + transaction.value() + "'");
        }
        transactionParameters.setRequired(requiredTransaction);
        if (transaction.readOnly()) {
            transactionParameters.setCapability(TransactionCapability.readonly);
        } else {
            transactionParameters.setCapability(TransactionCapability.readwrite);
        }
        transactionParameters.setBufferSize(transaction.bufferSize());
        description.setRequiredTransactionParameters(transactionParameters);
    }

    protected final void handleCacheAnnotation(@NotNull Cache cache, @NotNull String beanName,
            @NotNull DescriptionImpl description) {
        Assert.notNull(cache, "Annotation cannot be null.");
        Assert.hasText(beanName, "Bean name cannot be empty.");
        Assert.notNull(description, "Description cannot be null.");

        org.springframework.extensions.webscripts.Cache requiredCache = new org.springframework.extensions.webscripts.Cache();
        requiredCache.setNeverCache(cache.neverCache());
        requiredCache.setIsPublic(cache.isPublic());
        requiredCache.setMustRevalidate(cache.mustRevalidate());
        description.setRequiredCache(requiredCache);
    }

    @NotNull
    protected final String generateId(@NotNull String beanName) {
        Assert.hasText(beanName, "Bean name cannot be empty");

        if (beanFactory == null) {
            throw new IllegalStateException("beanFactory");
        }
        Class<?> clazz = beanFactory.getType(beanName);
        Intrinsics.checkExpressionValueIsNotNull(clazz, "clazz");
        String id = clazz.getName();
        Intrinsics.checkExpressionValueIsNotNull(id, "clazz.name");
        return id;
    }

    @NotNull
    protected final String generateShortName(@NotNull String beanName) {
        Assert.hasText(beanName, "Bean name cannot be empty");

        if (beanFactory == null) {
            throw new IllegalStateException("beanFactory");
        }
        final Class<?> clazz = beanFactory.getType(beanName);
        Intrinsics.checkExpressionValueIsNotNull(clazz, "clazz");
        final String shortName = ClassUtils.getShortName(clazz);
        Intrinsics.checkExpressionValueIsNotNull(shortName, "ClassUtils.getShortName(clazz)");
        return shortName;
    }

    /*
     * These methods use local classes to obtain annotations with default settings.
     */

    private Authentication getDefaultAuthenticationAnnotation() {
        @Authentication
        final class Default {

        }
        return Default.class.getAnnotation(Authentication.class);
    }

    private final Transaction getDefaultReadWriteTransactionAnnotation() {
        @Transaction
        final class Default {

        }
        return Default.class.getAnnotation(Transaction.class);
    }

    private Transaction getDefaultReadonlyTransactionAnnotation() {
        @Transaction(readOnly = true)
        final class Default {

        }
        return Default.class.getAnnotation(Transaction.class);
    }

    private Cache getDefaultCacheAnnotation() {
        @Cache
        final class Default {

        }
        return Default.class.getAnnotation(Cache.class);
    }

    private WebScript getDefaultWebScriptAnnotation() {
        @WebScript
        final class Default {

        }
        return Default.class.getAnnotation(WebScript.class);
    }

    /* Dependencies */

    @Override
    public void setBeanFactory(@NotNull BeanFactory beanFactory) {
        Intrinsics.checkParameterIsNotNull(beanFactory, "beanFactory");
        Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory,
                "BeanFactory is not of type ConfigurableListableBeanFactory.");
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
    }
}
