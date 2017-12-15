FROM maven:3-jdk-8-alpine

COPY . /src
RUN adduser -S mvn
RUN chown -R mvn /src

USER mvn
WORKDIR /src
CMD mvn install failsafe:integration-test -DskipITs=false 