FROM maven:3.9.9-eclipse-temurin-21-alpine as builder

COPY . .

RUN mvn install

FROM openjdk:21-jdk-slim

RUN mkdir /bot

COPY --from=builder target/simple-voice-chat-bot.jar /bot/simple-voice-chat-bot.jar

ENV TOKEN=""
ENV DB_PATH="database.db"
ENV SUPPORT_CHANNEL_ID="0"
ENV SUPPORT_NOTIFICATION_CHANNEL="0"
ENV SUPPORT_LOG_CHANNEL="0"
ENV SUPPORT_ROLE="0"

WORKDIR /bot

ENTRYPOINT ["java", "-Xms2G", "-Xmx2G", "-jar", "simple-voice-chat-bot.jar"]
