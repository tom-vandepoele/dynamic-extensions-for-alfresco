ARG ALFRESCO_BASE_IMAGE

FROM $ALFRESCO_BASE_IMAGE

ARG TOMCAT_DIR=/usr/local/tomcat

RUN mkdir -p $TOMCAT_DIR/amps

COPY build/amps $TOMCAT_DIR/amps

RUN java -jar $TOMCAT_DIR/alfresco-mmt/alfresco-mmt*.jar install \
            $TOMCAT_DIR/amps $TOMCAT_DIR/webapps/alfresco -directory -nobackup -force