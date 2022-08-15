FROM maven:3.8-eclipse-temurin-17-alpine as builder

COPY . .

RUN mvn install

FROM openjdk:17-jdk-slim

RUN mkdir /bot

COPY --from=builder target/simple-voice-chat-bot.jar /bot/simple-voice-chat-bot.jar

ENV TOKEN=""
ENV DB_URL="localhost:27017"
ENV DB_NAME="discordbot"
ENV SUPPORT_CHANNEL_ID="0"
ENV COMMON_ISSUES_CHANNEL_ID="0"
ENV SERVER_HOSTING_CHANNEL_ID="0"
ENV SUPPORT_NOTIFICATION_CHANNEL="0"
ENV SUPPORT_LOG_CHANNEL="0"
ENV SUPPORT_ROLE="0"

WORKDIR /bot

ENTRYPOINT ["java", "-Xms2G", "-Xmx2G", "-jar", "simple-voice-chat-bot.jar"]
