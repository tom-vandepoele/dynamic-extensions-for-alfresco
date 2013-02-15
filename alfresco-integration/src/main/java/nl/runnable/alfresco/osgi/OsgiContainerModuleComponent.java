/*
Copyright (c) 2012, Runnable
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
 * Neither the name of Runnable nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package nl.runnable.alfresco.osgi;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.Assert;

/**
 * Module component that manages the OSGI child {@link ApplicationContext} and initializes the {@link FrameworkManager}
 * within it.
 * 
 * @author Laurens Fridael
 * 
 */
public class OsgiContainerModuleComponent extends AbstractModuleComponent implements ApplicationContextAware {

	/* Dependencies */

	/**
	 * The containing {@link ApplicationContext}. I.e. the context that this bean is defined in.
	 */
	private ApplicationContext applicationContext;

	/**
	 * The child {@link ApplicationContext} that contains the OSGi framework.
	 */
	private ConfigurableApplicationContext osgiContainerApplicationContext;

	/* Main operations */

	/**
	 * Initializes this module.
	 */
	@Override
	protected void executeInternal() {
		initializeFrameworkManager();
	}

	/**
	 * Called on bean destroy.
	 */
	public void destroy() {
		closeOsgiContainerApplicationContext();
	}

	/* Utility operations */

	/**
	 * Initializes the {@link FrameworkManager} in the child application context;
	 */
	protected void initializeFrameworkManager() {
		getFrameworkManager().initialize();
	}

	/**
	 * Closes the OSGi container {@link ApplicationContext}.
	 */
	protected void closeOsgiContainerApplicationContext() {
		getOsgiContainerApplicationContext().close();
	}

	/* State */

	/**
	 * Obtains the {@link FrameworkManager} from the OSGi container{@link ApplicationContext}.
	 */
	protected FrameworkManager getFrameworkManager() {
		return getOsgiContainerApplicationContext().getBean(BeanNames.CONTAINER_FRAMEWORK_MANAGER,
				FrameworkManager.class);
	}

	/* Dependencies */

	@Override
	public void setApplicationContext(final ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	protected ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	@Required
	public void setOsgiContainerApplicationContext(final ConfigurableApplicationContext osgiContainerApplicationContext) {
		Assert.notNull(osgiContainerApplicationContext);
		this.osgiContainerApplicationContext = osgiContainerApplicationContext;
	}

	protected ConfigurableApplicationContext getOsgiContainerApplicationContext() {
		return osgiContainerApplicationContext;
	}

}
