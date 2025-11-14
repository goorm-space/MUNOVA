# Amazon Corretto 21 JRE 사용
FROM amazoncorretto:21-alpine

LABEL maintainer="namoo36"
LABEL version="1.0"

# 빌드된 Spring Boot JAR 복사
COPY build/libs/*.jar /app/MUNOVA.jar

WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=docker

# JVM 시간대
ENV TZ=Asia/Seoul

# 애플리케이션 포트
EXPOSE 8080

# 실행 명령
CMD ["java", "-jar", "/app/MUNOVA.jar", "--spring.profiles.active=docker"]