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

RUN mvn package

FROM docker.io/library/jetty:9.4-jre17-alpine

COPY --from=buildimage /opt/oskari/webapp-map/target/oskari-map.war webapps/
COPY --from=buildimage /opt/oskari/webapp-map/target/oskari-map/WEB-INF/lib/postgresql-*.jar lib/ext/
COPY docker/oskari-map.xml webapps/
COPY docker/oskari.ini start.d/
COPY docker/jetty-oskari.xml etc/

COPY docker/oskari-ext.example.properties resources/
COPY docker/log4j2.xml resources/

# context path in oskari-map.xml
ENV OSKARI_CONTEXT_PATH="/oskari"
