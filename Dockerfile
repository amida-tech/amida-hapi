FROM openjdk:8-jdk-alpine as builder
WORKDIR /app
COPY . /app/
RUN ./gradlew clean build -x test
RUN cp /app/build/libs/*.jar /app/build/libs/app.jar

FROM openjdk:8-jdk-alpine
LABEL maintainer="mike.hiner@gmail.com"
VOLUME /tmp
EXPOSE 8080
COPY --from=builder /app/build/libs/app.jar /app.jar
COPY --from=builder /app/src/main/resources/samples/* /var/hapi/init/
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]