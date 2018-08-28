ARG ALFRESCO_BASE_IMAGE
FROM $ALFRESCO_BASE_IMAGE

ARG TOMCAT_DIR=/usr/local/tomcat

RUN mkdir -p $TOMCAT_DIR/amps

COPY build/amps $TOMCAT_DIR/amps

RUN curl -k --create-dirs \
    https://nexus.alfresco.com/nexus/service/local/repositories/releases/content/org/alfresco/alfresco-mmt/5.2.g/alfresco-mmt-5.2.g.jar \
    -o $TOMCAT_DIR/alfresco-mmt/alfresco-mmt-5.2.g.jar

RUN java -jar $TOMCAT_DIR/alfresco-mmt/alfresco-mmt*.jar install \
            $TOMCAT_DIR/amps $TOMCAT_DIR/webapps/alfresco -directory -nobackup -force