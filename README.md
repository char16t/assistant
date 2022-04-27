# Assistant

_Этот документ доступен также [на русском языке](README_RU.md)._ 

Assistant is a bot for Trello, that helps keep Kanban-boards at Trello up to date.

![Assistant Demo](/docs/assistant.gif)

Automated actions:

* Daily transfer of cards by columns "Today", "Tomorrow", "This week", "This month"
* Updating the due date on the card when transferring between columns. For example, when transferring the card to the "Today" column, the due date will be automatically set as "today 23:59", and when transferring the card to the "This week" column, the due date "next Sunday 23:59" will be set
* Control of card limits in columns: if the card limit in a column is exceeded, it will not be possible to move a new card there, it will return to the original column with an explanatory comment inside
* When creating a card, it is immediately assigned to the owner of the board and a due date is set on it in accordance with the column in which it was created
* All expired cards are transferred to the "Today" column
* When the "completed" checkbox is placed on the due date of the card, it is automatically transferred to the "Done" column at the top
* When moving a card to the "Done" column, the "completed" flag is set to due date and removed when the card is moved from the "Done" column to another
* When you delete the "completed" checkbox on the due date of the card, it is automatically transferred to the "Today" column
* Cards with due dates in the next month and beyond are automatically transferred to the main board when their due dates are comes

All actions are performed by the assistant user, so in the history of the actions on cards and on the board you can see what was done manually and what was done automatically. Since the Assistant generates a lot of actions during active use and a notification arrives for each of them, a mechanism for unsubscribing from the card and subscribing again before and after each action is implemented.

## Table of Contents

* [Overview](#overview)
* [Installation](#installation)
    * [Server preparation](#server-preparation)
        * [Java Runtime Environment](#java-runtime-environment)
        * [PostgreSQL](#postgresql)
        * [CRON](#cron)
    * [Trello preparation](#trello-preparation)
        * [Accounts](#accounts)
        * [Boards](#boards)
        * [REST API keys](#rest-api-keys)
    * [Assistant configuration](#assistant-configuration)
    * [Building Assistant](#building-assistant)
    * [Webhooks](#webhooks)

## Overview

The assistant is written in Scala, compiled into an executable JAR file and must be deployed on the server. Interaction with Trello takes place through the [REST API](https://developer.atlassian.com/cloud/trello/rest/) and [Webhooks](https://developer.atlassian.com/cloud/trello/guides/rest-api/webhooks/).

## Installation

### Server preparation

#### Java Runtime Environment

You can use a server with Ubuntu 20.04 or higher. You need to install the JRE and open the port.

```sh
# Install JRE
sudo apt install default-jre

# Check
java -version

# Open port
sudo ufw allow 8080
```

#### PostgreSQL

You can use a server with Ubuntu 20.04 or higher. You need to install a PostgreSQL server, create a password for the user `postgres` and create a database `assistant'.

```sh
# Update indexes
sudo apt update

# Install psql server
sudo apt install postgresql postgresql-contrib

# Make sure psql works & add to autostart
sudo systemctl start postgresql.service
systemctl enable postgresql

# Login as postgres user
sudo -i -u postgres

# psql shell
psql

# Execute in psql shell, change password
ALTER USER postgres PASSWORD 'xxxxxxxxxxxxxxxx';
\q

# Create database
createdb assistant
```

#### CRON

Temporary ad-hoc, automatic daily transfer of cards by columns. Add this to cron (`crontab -e`):

```
0 0 * * * curl http://yourserver.com/api/trello/organize_cards
```

Based on your time zone the cron schedule will look like this: `0 (24-TZ) * * *`. Example: for Europe/Moscow UTC+3 
it will be `0 21 * * *`.

### Trello preparation

#### Accounts

To work you will need two Trello accounts: personal and for an assistant. If you don't have a personal Trello account, [create one](https://trello.com/signup), then create an account for the assistant.

1. [Create a personal account in Trello](https://trello.com/signup) (if necessary)
2. [Create an account for assistant in Trello](https://trello.com/signup), upload avatar and disable email notifications in the settings

#### Boards

In your personal Trello account you will need to create two boards "Current" and "Next". You can call them whatever you want, but in this manual such names will be used.

1. Log in to your personal Trello account
2. Create a "Current" board
3. In the "Current" board create the columns "To Do", "This week", "Tomorrow", "Today", "In progress", "Delegated", "Done"
4. Add an Assistant user to the "Current" board with normal rights
5. Create a "Next" board
6. In the "Next" board create the "To Do" and "Done" columns
7. Add an Assistant user to the "Next" board with normal rights

#### REST API keys

The API call is possible by an authorized user. To do this you will need a token and an application key.

As noted above, when actively used the assistant generates a lot of actions. Each of which generates a notification. To avoid this, you have to unsubscribe from the card notifications before the action and subscribe to it after each action. Unsubscribing and subscribing have to be done from under a personal user. So you need to get a token and an application key for a personal user and for an Assistant user.

1. Log in to your personal Trello account
2. [Get the application key for your personal account](https://trello.com/app-key)
3. Get a token for your personal account ([`https://trello.com/1/authorize?expiration=never&scope=read,write,account&response_type=token&name=Server%20Token&key=<YOUR_KEY>`](https://trello.com/1/authorize?expiration=never&scope=read,write,account&response_type=token&name=Server%20Token&key=<YOUR_KEY>))
4. Log in to the Assistant's account
5. [Get the application key for the Assistant account](https://trello.com/app-key)
6. Get a token for the Assistant account  ([`https://trello.com/1/authorize?expiration=never&scope=read,write,account&response_type=token&name=Server%20Token&key=<YOUR_KEY>`](https://trello.com/1/authorize?expiration=never&scope=read,write,account&response_type=token&name=Server%20Token&key=<YOUR_KEY>))

### Assistant configuration

The assistant is fully configured via a single file `reference.conf` (`src/main/resources/reference.conf`).

Fill in all the fields:

* `assistant.trello.timeZoneCorrection`,integer &mdash; correction of time relative to UTC. For UTC+3 time zone (Europe/Moscow) it will be `3`
* `assistant.trello.users.assistant.id`, string &mdash; Assistant user ID
* `assistant.trello.users.assistant.token`, string &mdash; token for the Assistant user
* `assistant.trello.users.assistant.appKey`, string &mdash; application key for the Assistant user
* `assistant.trello.users.owner.id`, string &mdash; personal user ID
* `assistant.trello.users.owner.token`, string &mdash; personal user token
* `assistant.trello.users.owner.appKey`, string &mdash; personal user's application key
* `assistant.trello.boards.current.id`, string &mdash; ID of the "Current" board
* `assistant.trello.boards.current.columns.todo.id`, string &mdash; ID of the "To Do" column of the "Current" board
* `assistant.trello.boards.current.columns.todo.name`, string &mdash; name of the "To Do" column of the "Current" board
* `assistant.trello.boards.current.columns.todo.limit`, integer &mdash; the maximum number of cards in the "To Do" column of the "Current" board
* `assistant.trello.boards.current.columns.week.id`, string &mdash; ID of the "This week" column of the "Current" board
* `assistant.trello.boards.current.columns.week.name`, string &mdash; name of the "This week" column of the "Current" board
* `assistant.trello.boards.current.columns.week.limit`, integer &mdash; maximum number of cards in the "This week" column of the "Current" board
* `assistant.trello.boards.current.columns.tomorrow.id`, string &mdash; ID of the "Tomorrow" column of the "Current" board
* `assistant.trello.boards.current.columns.tomorrow.name`, string &mdash; name of the "Tomorrow" column of the "Current" board
* `assistant.trello.boards.current.columns.tomorrow.limit`, integer &mdash; maximum number of cards in the "Tomorrow" column of the "Current" board
* `assistant.trello.boards.current.columns.today.id`, string &mdash; ID of the "Today" column of the "Current" board
* `assistant.trello.boards.current.columns.today.name`, string &mdash; name of the "Today" column of the "Current" board
* `assistant.trello.boards.current.columns.today.limit`, integer &mdash; maximum number of cards in the "Today" column of the "Current" board
* `assistant.trello.boards.current.columns.inProgress.id`, string &mdash; ID of the "In progress" column of the "Current" board
* `assistant.trello.boards.current.columns.inProgress.name`, string &mdash; name of the column "In progress" of the board "Current"
* `assistant.trello.boards.current.columns.inProgress.limit`, integer &mdash; maximum number of cards in the "In progress" column of the "Current" board
* `assistant.trello.boards.current.columns.delegated.id`, string &mdash; ID of the "Delegated" column of the "Current" board
* `assistant.trello.boards.current.columns.delegated.name`, string &mdash; name of the "Delegated" column of the "Current" board
* `assistant.trello.boards.current.columns.delegated.limit`, integer &mdash; maximum number of cards in the "Delegated" column of the "Current" board
* `assistant.trello.boards.current.columns.done.id`, string &mdash; ID of the "Done" column of the "Current" board
* `assistant.trello.boards.current.columns.done.name`, string &mdash; name of the "Done" column of the "Current" board
* `assistant.trello.boards.current.columns.done.limit`, integer &mdash; maximum number of cards in the "Done" column of the "Current" board
* `assistant.trello.boards.next.columns.todo.id`, string &mdash; ID of the "To Do" column of the "Next" board
* `assistant.trello.boards.next.columns.todo.name`, string &mdash; name of the "To Do" column of the board is "Next"
* `assistant.trello.boards.next.columns.todo.limit`, integer &mdash; maximum number of cards in the "To Do" column of the "Next" board
* `assistant.trello.boards.next.columns.done.id`, string &mdash; ID of the "Done" column of the "Next" board
* `assistant.trello.boards.next.columns.done.name`, string &mdash; name of the column "Done" of the board is "Next"
* `assistant.trello.boards.next.columns.done.limit`, integer &mdash; maximum number of cards in the "Done" column of the "Next" board
* `assistant.server.host`, string &mdash; server host
* `assistant.server.port`, integer &mdash; server port
* `assistant.db.url`, string &mdash; JDBC-URL for connecting to PostgreSQL
* `assistant.db.user`, string &mdash; user to connect to PostgreSQL
* `assistant.db.password`, string &mdash; password for connecting to PostgreSQL
* `assistant.db.driver`, string &mdash; PostgreSQL driver class
* `assistant.db.connections.poolSize`, string &mdash; connection pool size

Where to get it? (the token and key of personal Trello account are used everywhere)

* Board IDs: `https://api.trello.com/1/members/me/boards?key=<APP KEY>&token=<TOKEN>`
* Column IDs: `https://api.trello.com/1/boards/<BOARD ID>/lists?key=<APP KEY>&token=<TOKEN>`
* Create cards for the Assistant and for the personal user on any board, assign one to yourself, the other to the assistant and get the user IDs from `https://api.trello.com/1/boards/<BOARD ID>/cards?key=<APP KEY>&token=<TOKEN>`

Copy the contents to the file `src/main/resources/reference.conf`:

```
assistant {
  trello {
    timeZoneCorrection=3
    users {
        assistant {
            id="xxxxxxxxxxxxxxxxxxxxxxxx"
            appKey="xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
            token="xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
        }
        owner {
            id="xxxxxxxxxxxxxxxxxxxxxxxx"
            appKey="xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
            token="xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
        }
    }
    boards {
      current {
        id="xxxxxxxxxxxxxxxxxxxxxxxx"
        columns {
          todo {
            id="xxxxxxxxxxxxxxxxxxxxxxxx"
            name="To Do"
            limit=217
          }
          week {
            id="xxxxxxxxxxxxxxxxxxxxxxxx"
            name="This week"
            limit=49
          }
          tomorrow {
            id="xxxxxxxxxxxxxxxxxxxxxxxx"
            name="Tomorrow"
            limit=7
          }
          today {
            id="xxxxxxxxxxxxxxxxxxxxxxxx"
            name="Today"
            limit=7
          }
          inProgress {
            id="xxxxxxxxxxxxxxxxxxxxxxxx"
            name="In progress"
            limit=2
          }
          delegated {
            id="xxxxxxxxxxxxxxxxxxxxxxxx"
            name="Delegated"
            limit=14
          }
          done {
            id="xxxxxxxxxxxxxxxxxxxxxxxx"
            name="Done"
            limit=217
          }
        }
      }
      next {
        id="xxxxxxxxxxxxxxxxxxxxxxxx"
        columns {
          todo {
            id="xxxxxxxxxxxxxxxxxxxxxxxx"
            name="To Do"
            limit=2562
          }
          done {
            id="xxxxxxxxxxxxxxxxxxxxxxxx"
            name="Done"
            limit=2562
          }
        }
      }
    }
    messages {
      listLimitReached="Unable to move this card to \"LIST_NAME\" list. List \"LIST_NAME\" already contains LIMIT or more cards"
    }
  }
  server {
    host="0.0.0.0"
    port=8080
  }
  db {
    url="jdbc:postgresql://postgres/web"
    user="postgres"
    password="postgres"
    driver="org.postgresql.Driver"
    connections = {
      poolSize = 10
    }
  }
}

```

### Building Assistant

```sh
# Build
./sbtx assembly
```

You can send the application to the server via scp:

```sh
# Deploy
scp target/scala-2.12/assistant-assembly-0.0.1-SNAPSHOT.jar user@yourserver.com:/root/assistant.jar
```

Launching the application:

```sh
nohup java -jar assistant.jar &> assistant.log &
```

Checking that the app is working:

```sh
curl http://yourserver.com:8080/api/trello
```

Stopping the application:

```sh
# Get PID of "java" process
top

# Kill it
kill -9 <pid>
```

### Webhooks

Through Webhooks Trello reports on new events, such as the creation and updating of cards. You need to create web hooks for both the "Current" and "Next" boards. Insert the token, the application key for your personal user Trello and the address of the server where the assistant is already working and run:

Here you also need to insert the "Current" board ID:

```sh
curl -X POST -H "Content-Type: application/json" \
https://api.trello.com/1/tokens/<TOKEN>/webhooks/ \
-d '{
  "key": "<APP KEY>",
  "callbackURL": "http://yourserver.com:8080/api/trello/receive_webhook",
  "idModel":"<CURRENT BOARD ID>",
  "description": "Assistant's Webhook for Daily board"
}'
```

A reply will come from Trello:

```json
{
  "id": "xxxxxxxxxxxxxxxxxxxxxxxx",
  "description": "Assistant's Webhook for Daily board",
  "idModel": "xxxxxxxxxxxxxxxxxxxxxxxx",
  "callbackURL": "http://yourserver.com/api/trello/receive_webhook",
  "active": true,
  "consecutiveFailures": 0,
  "firstConsecutiveFailDate": null
}
```

Similarly for the "Next" board:

```sh
curl -X POST -H "Content-Type: application/json" \
https://api.trello.com/1/tokens/<TOKEN>/webhooks/ \
-d '{
  "key": "<APP KEY>",
  "callbackURL": "http://yourserver.com:8080/api/trello/receive_webhook",
  "idModel":"<NEXT BOARD ID>",
  "description": "Assistant's Webhook for Daily Next board"
}'
```

A reply will come from Trello:

```json
{
  "id": "xxxxxxxxxxxxxxxxxxxxxxxx",
  "description": "Assistant's Webhook for Daily Next board",
  "idModel": "xxxxxxxxxxxxxxxxxxxxxxxx",
  "callbackURL": "http://yourserver.com/api/trello/receive_webhook",
  "active": true,
  "consecutiveFailures": 0,
  "firstConsecutiveFailDate": null
}
```
