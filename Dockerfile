FROM openjdk:8-jdk-alpine as builder
WORKDIR /app
COPY . /app/
RUN chmod +x gradlew
RUN ./gradlew clean build
RUN cp /app/build/libs/*.jar /app/build/libs/app.jar

FROM openjdk:8-jdk-alpine
LABEL maintainer="mike.hiner@gmail.com"
VOLUME /tmp
COPY --from=builder /app/build/libs/app.jar /app.jar
COPY --from=builder /app/src/main/resources/samples/* /var/hapi/init/

EXPOSE 8080
# USER 50000:50000 # Investigate later, I think this user needs permissions to do things. Alpine sucks.
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]