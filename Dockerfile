FROM docker.io/library/maven:3.9-eclipse-temurin-17-alpine as buildimage
#FROM maven:3-openjdk-8-slim as buildimage
WORKDIR /opt/oskari/

COPY docker/maven-settings /root/.m2/

COPY pom.xml pom.xml
COPY app-resources/pom.xml app-resources/pom.xml
COPY app-specific-code/pom.xml app-specific-code/pom.xml
COPY webapp-map/pom.xml webapp-map/pom.xml

# Cache maven dependencies, so we don't need to fetch them every time any file changes
RUN mvn test verify package clean dependency:resolve dependency:resolve-plugins --fail-never && rm -Rf ./target ./*/target


COPY app-resources app-resources
COPY app-specific-code app-specific-code
COPY webapp-map webapp-map

ARG FRONTEND_VERSION=2.14.2-ubigu3
# add the frontend to oskari-override, which is loaded in webapp-map/src/main/java/org.oskari/ClientResourceConfiguration
RUN echo "oskari.client.version=dist/${FRONTEND_VERSION}" > webapp-map/src/main/resources/oskari-docker.properties
RUN mkdir -p ./webapp-map/src/main/webapp/Oskari/
RUN curl -s -L "https://github.com/ubigu/oskari-front-shared/releases/download/${FRONTEND_VERSION}/oskari-frontend-${FRONTEND_VERSION}.tar.gz" | tar xz -C ./webapp-map/src/main/webapp/Oskari/

RUN mvn package

FROM docker.io/library/jetty:9.4-jre17-alpine

COPY --from=buildimage /opt/oskari/webapp-map/target/oskari-map.war webapps/
COPY --from=buildimage /opt/oskari/webapp-map/target/oskari-map/WEB-INF/lib/postgresql-*.jar lib/ext/
COPY docker/oskari-map.xml webapps/
COPY docker/oskari.ini start.d/
COPY docker/jetty-oskari.xml etc/
# Trust X-Forwarded headers
COPY docker/forwarded-customizer.ini start.d/
COPY docker/forwarded-customizer.xml etc/


COPY docker/oskari-ext.properties resources/
COPY docker/log4j2.xml resources/

# context path in oskari-map.xml
ENV OSKARI_CONTEXT_PATH="/"
