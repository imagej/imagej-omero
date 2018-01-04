FROM maven:3-jdk-8-alpine

COPY . /src
RUN adduser -S mvn
RUN chown -R mvn /src

USER mvn
WORKDIR /src
RUN mvn clean install -DskipTests
CMD mvn failsafe:integration-test failsafe:verify -DskipITs=false
