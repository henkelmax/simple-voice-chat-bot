# Simple Voice Chat Discord Support Bot

The Simple Voice Chat Discord Bot.

## Functionality

The bot initially sends a message in the support channel (This will get deleted and re-sent after the bot restarted).

![](https://user-images.githubusercontent.com/13237524/184628361-32bb9b74-1ffd-4605-abbb-60df6121b31e.png)

Users can then press the `Get Support` button and the bot will create a thread and notifies the user.

![](https://user-images.githubusercontent.com/13237524/184628557-c643f63d-f8b8-4ccf-8566-f208e7d331d1.png)

In the meantime users get prompted to enter a support key in their support thread.

![](https://user-images.githubusercontent.com/13237524/184628755-89b4fb1b-35ab-4326-90de-83854a7216da.png)

After providing the support key, the user gets prompted to choose their issue.

![](https://user-images.githubusercontent.com/13237524/184632963-f7430ea7-e730-4161-84a5-02671471074f.png)

![](https://user-images.githubusercontent.com/13237524/184629309-882451b8-5d1f-4562-bd64-a12fbd2a2807.png)

After selecting an issue from the dropdown, the user gets additional information about the selected issue,
as well as a set of questions, specifically tailored for the chosen issue.

![](https://user-images.githubusercontent.com/13237524/184633154-5d3d3dfd-41c9-4033-9664-317989c5105a.png)

Until now the user wasn't able to write any messages in the thread.

After answering the questions, the user needs to press the `Confirm` button.

This will trigger a message in the **notification channel**.

![](https://user-images.githubusercontent.com/13237524/184633681-82e4f921-196c-4dbe-a929-7d12ddee1822.png)

Now support staff can join the thread and answer the questions.

In the case, the user selected the wrong issue,
support can change the questions by typing the command `/issue <issue-type>`.
This will trigger the same messages, that would have been sent when a user selects an issue from the dropdown.

If you want the user to be able to write messages before completing the process, you can enter the `/unlock` command.

You can lock threads by entering the `/close` command
(This does the same thing as the user pressing the `I don't need support anymore` button).
This will create a message in the thread, letting the user know who locked the thread.

![](https://user-images.githubusercontent.com/13237524/184634478-3a3c4585-3651-474a-a5c3-eda65d0b2e3f.png)

There will also be an update of the original message in the **notification channel**,
notifying staff that the thread doesn't need looking into again.

![](https://user-images.githubusercontent.com/13237524/184634574-decdad13-0021-45fb-b28a-571d781c24c9.png)

Admins can also clean up the thread database by locking all threads that are older than a week
by entering the `/cleanup` command.

## Additional Notes

- The bot doesn't keep track of locked threads anymore, so please don't re-open them.
- Normal users shouldn't have permission to write messages in the support channel.

## Environment Variables

| Variable                       | Default value | Description                     |
|--------------------------------|---------------|---------------------------------|
| `TOKEN`                        | *Required*    | The discord bot token           |
| `DB_PATH`                      | `database.db` | The path to the sqlite database |
| `SUPPORT_CHANNEL_ID`           | *Required*    | The support channel ID          |
| `SUPPORT_THREAD_CHANNEL_ID`    | *Required*    | The support thread channel ID   |
| `COMMON_ISSUES_CHANNEL_ID`     | *Required*    | The common issues channel ID    |
| `SERVER_HOSTING_CHANNEL_ID`    | *Required*    | The server hosting channel ID   |
| `SUPPORT_NOTIFICATION_CHANNEL` | *Required*    | The notification channel ID     |
| `SUPPORT_ROLE`                 | *Required*    | The support role                |
