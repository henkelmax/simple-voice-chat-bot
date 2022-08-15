# Simple Voice Chat Discord Support Bot

The source code of the Simple Voice Chat Discord Bot.

## Environment Variables

| Variable                       | Default value     | Description                                          |
|--------------------------------|-------------------|------------------------------------------------------|
| `TOKEN`                        | *Required*        | The discord bot token                                |
| `DB_URL`                       | `localhost:27017` | The MongoDB connection URL                           |
| `DB_NAME`                      | `discordbot`      | The name of the MongoDB database                     |
| `SUPPORT_CHANNEL_ID`           | *Required*        | The support channel ID                               |
| `COMMON_ISSUES_CHANNEL_ID`     | *Required*        | The common issues channel ID                         |
| `SERVER_HOSTING_CHANNEL_ID`    | *Required*        | The server hosting channel ID                        |
| `SUPPORT_NOTIFICATION_CHANNEL` | *Required*        | The notification channel ID                          |
| `SUPPORT_LOG_CHANNEL`          | *Required*        | The support log channel ID                           |
| `SUPPORT_ROLE`                 | *Required*        | The support role                                     |
