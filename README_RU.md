# Ассистент

Ассистент &mdash; это бот для Trello, который помогает поддерживать Канбан-доски в Trello в актуальном состоянии.

![Assistant Demo](/docs/assistant.gif)

Автоматизированные действия:

* Ежедневный перенос карточек по столбцам "Сегодня", "Завтра", "На этой неделе", "В этом месяце"
* Обновление крайнего срока на карточке при переносе между столбцами. Например, при переносе карточки в столбец "Сегодня" будет автоматически установлен крайний срок как "сегодня 23:59", а при переносе карточки в стобец "На этой неделе" будет установлен крайний срок "ближайшее воскресенье 23:59"
* Контроль лимитов карточек в столбцах: если лимит карточек в столбце превышен, то переместить туда новую карточку не получится, она вернется в исходный столбец с поясняющим комментарием внутри
* При создании карточки, она сразу же назначается на владельца доски и на ней устанавливается крайний срок в соответствии со столбцом, в котором она создана
* Все просроченные карточки переносятся в столбец "Сегодня"
* При простановке флажка "завершено" на крайнем сроке карточки, она автоматически переносится в столбец "Сделано" на самый верх
* При перемещении карточки в столбец "Сделано" проставляется флаг "завершено" на крайнем сроке и снимается,
  когда карточка перемещается из столбца "Сделано" в другой
* При удалении флажка "завершено" на крайнем сроке карточки, она автоматически переносится в столбец "Сегодня"
* Карточки со сроками в следующем месяце и далее, автоматически переносятся на основную доску, когда подходит их срок

Все действия выполняются от пользователя ассистента Trello, поэтому в истории действий карточек и доски видно, что было сделано вручную, а что автоматически. Так как Ассистент при активном использовании генерирует очень много действий и на каждое из них приходит уведомление, реализован механизм отписки от карточки и подписке заново до и после каждого действия.

## Содержание

* [Обзор](#обзор)
* [Установка](#установка)
    * [Подготовка сервера](#подготовка-сервера)
        * [Java Runtime Environment](#java-runtime-environment)
        * [PostgreSQL](#postgresql)
        * [CRON](#cron)
    * [Подготовка Trello](#подготовка-trello)
        * [Аккаунты](#аккаунты)
        * [Доски](#доски)
        * [Ключи вызова REST API](#ключи-вызова-rest-api)
    * [Конфигурация Ассистента](#конфигурация-ассистента)
    * [Сборка приложения Ассистента](#сборка-приложения-ассистента)
    * [Webhooks](#webhooks)

## Обзор

Ассистент написан на Scala, компилируется в исполняемый JAR-файл и должен быть развёрнут на сервере. Взаимодействие с Trello происходит через [REST API](https://developer.atlassian.com/cloud/trello/rest/) и [Webhooks](https://developer.atlassian.com/cloud/trello/guides/rest-api/webhooks/).

## Установка

### Подготовка сервера

#### Java Runtime Environment

Вы можете использовать сервер с Ubuntu 20.04 или выше. Нужно установить JRE и открыть порт.

```sh
# Install JRE
sudo apt install default-jre

# Check
java -version

# Open port
sudo ufw allow 8080
```

#### PostgreSQL

Вы можете использовать сервер с Ubuntu 20.04 или выше. Нужно установить сервер PostgreSQL, 
создать пароль для пользователя `postgres` и создать базу данных `assistant`.

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

Временная мера, автоматический ежедневный перенос карточек по столбцам. Добавьте это в cron (`crontab -e`):

```
0 0 * * * curl http://yourserver.com/api/trello/organize_cards
```

С учётом вашей временной зоны расписание cron будет выглядеть так: `0 (24-TZ) * * *`. Например для UTC+3 (Europe/Moscow)
это будет `0 21 * * *`.

### Подготовка Trello

#### Аккаунты

Для работы вам понадобится два аккаунта Trello: личный и для ассистента. Если у вас нет личного аккаунта Trello, [создайте его](https://trello.com/signup), затем создайте аккаунт для ассистента.

1. [Создайте личный аккаунт в Трелло](https://trello.com/signup) (если необходимо)
2. [Создайте аккаунт для ассистента в Трелло](https://trello.com/signup), загрузите аватарку и отключите в настройках уведомления на электронную почту

#### Доски

Из подвашего личого аккаунта Трелло потребуется создать две доски "Текущая" и "Следующая". Вы можете назвать их как угодно, но в этой инструкции будут использоваться такие названия.

1. Залогиньтесь в свой личный аккаунт Trello
2. Создайте доску "Текущая"
3. Для доски "Текущая" создайте столбцы "Сделать", "На этой неделе", "Завтра", "Сегодня", "В процессе", "Делегировано", "Сделано"
4. Добавьте пользователя Ассистента на доску "Текущая" с обычными (Normal) правами
5. Создайте доску "Следующая"
6. Для доски "Следующая" создайте столбцы "Сделать" и "Сделано"
7. Добавьте пользователя Ассистента на доску "Следующая" с обычными (Normal) правами

#### Ключи вызова REST API

Вызов API возможен из-под авторизованного пользователя. Для этого потребуется токен и ключ приложения.

Как было отмечено выше, при активном использовании ассистент генерирует очень много действий, каждое из которых генерирует уведомление. Чтобы избежать этого, приходится отписываться от уведомлений карточки до действия и подписываться на неё после каждого действия. Отписку и подписку приходится делать из-под личного пользователя, так что нужно получить токен и ключ приложения для личного пользователя и для пользователя Ассистента.

1. Залогиньтесь в свой личный аккаунт Trello
2. [Получите ключ приложения для личного аккаунта](https://trello.com/app-key)
3. Получите токен для личного аккаунта ([`https://trello.com/1/authorize?expiration=never&scope=read,write,account&response_type=token&name=Server%20Token&key=<YOUR_KEY>`](https://trello.com/1/authorize?expiration=never&scope=read,write,account&response_type=token&name=Server%20Token&key=<YOUR_KEY>))
4. Залогиньтесь в аккаунт Ассистента
5. [Получите ключ приложения для аккаунта Ассистента](https://trello.com/app-key)
6. Получите токен для аккаунта Ассистента  ([`https://trello.com/1/authorize?expiration=never&scope=read,write,account&response_type=token&name=Server%20Token&key=<YOUR_KEY>`](https://trello.com/1/authorize?expiration=never&scope=read,write,account&response_type=token&name=Server%20Token&key=<YOUR_KEY>))

### Конфигурация Ассистента

Ассистент полностью конфигурируется через единственный файл `reference.conf` (`src/main/resources/reference.conf`).

Заполните все поля:

* `assistant.trello.timeZoneCorrection`,целое число &mdash; коррекция времени относительно UTC. Для часового пояса UTC+3 (Europe/Moscow) это будет `3`
* `assistant.trello.users.assistant.id`, строка &mdash; идентификатор пользователя Ассистента
* `assistant.trello.users.assistant.token`, строка &mdash; токен для пользователя Ассистента
* `assistant.trello.users.assistant.appKey`, строка &mdash; ключ приложения для пользователя Ассистента
* `assistant.trello.users.owner.id`, строка &mdash; идентификатор личного пользователя
* `assistant.trello.users.owner.token`, строка &mdash; токен личного пользователя
* `assistant.trello.users.owner.appKey`, строка &mdash; ключ приложения личного пользователя
* `assistant.trello.boards.current.id`, строка &mdash; идентификатор доски "Текущая"
* `assistant.trello.boards.current.columns.todo.id`, строка &mdash; идентификатор столбца "Сделать" доски "Текущая"
* `assistant.trello.boards.current.columns.todo.name`, строка &mdash; название столбца "Сделать" доски "Текущая"
* `assistant.trello.boards.current.columns.todo.limit`, целое число &mdash; максимальное число карточек в столбце "Сделать" доски "Текущая"
* `assistant.trello.boards.current.columns.week.id`, строка &mdash; идентификатор столбца "На этой неделе" доски "Текущая"
* `assistant.trello.boards.current.columns.week.name`, строка &mdash; название столбца "На этой неделе" доски "Текущая"
* `assistant.trello.boards.current.columns.week.limit`, целое число &mdash; максимальное число карточек в столбце "На этой неделе" доски "Текущая"
* `assistant.trello.boards.current.columns.tomorrow.id`, строка &mdash; идентификатор столбца "Завтра" доски "Текущая"
* `assistant.trello.boards.current.columns.tomorrow.name`, строка &mdash; название столбца "Завтра" доски "Текущая"
* `assistant.trello.boards.current.columns.tomorrow.limit`, целое число &mdash; максимальное число карточек в столбце "Завтра" доски "Текущая"
* `assistant.trello.boards.current.columns.today.id`, строка &mdash; идентификатор столбца "Сегодня" доски "Текущая"
* `assistant.trello.boards.current.columns.today.name`, строка &mdash; название столбца "Сегодня" доски "Текущая"
* `assistant.trello.boards.current.columns.today.limit`, целое число &mdash; максимальное число карточек в столбце "Сегодня" доски "Текущая"
* `assistant.trello.boards.current.columns.inProgress.id`, строка &mdash; идентификатор столбца "В процессе" доски "Текущая"
* `assistant.trello.boards.current.columns.inProgress.name`, строка &mdash; название столбца "В процессе" доски "Текущая"
* `assistant.trello.boards.current.columns.inProgress.limit`, целое число &mdash; максимальное число карточек в столбце "В процессе" доски "Текущая"
* `assistant.trello.boards.current.columns.delegated.id`, строка &mdash; идентификатор столбца "Делегировано" доски "Текущая"
* `assistant.trello.boards.current.columns.delegated.name`, строка &mdash; название столбца "Делегировано" доски "Текущая"
* `assistant.trello.boards.current.columns.delegated.limit`, целое число &mdash; максимальное число карточек в столбце "Делегировано" доски "Текущая"
* `assistant.trello.boards.current.columns.done.id`, строка &mdash; идентификатор столбца "Сделано" доски "Текущая"
* `assistant.trello.boards.current.columns.done.name`, строка &mdash; название столбца "Сделано" доски "Текущая"
* `assistant.trello.boards.current.columns.done.limit`, целое число &mdash; максимальное число карточек в столбце "Сделано" доски "Текущая"
* `assistant.trello.boards.next.columns.todo.id`, строка &mdash; идентификатор столбца "Сделать" доски "Следующая"
* `assistant.trello.boards.next.columns.todo.name`, строка &mdash; название столбца "Сделать" доски "Следующая"
* `assistant.trello.boards.next.columns.todo.limit`, целое число &mdash; максимальное число карточек в столбце "Сделать" доски "Следующая"
* `assistant.trello.boards.next.columns.done.id`, строка &mdash; идентификатор столбца "Сделано" доски "Следующая"
* `assistant.trello.boards.next.columns.done.name`, строка &mdash; название столбца "Сделано" доски "Следующая"
* `assistant.trello.boards.next.columns.done.limit`, целое число &mdash; максимальное число карточек в столбце "Сделано" доски "Следующая"
* `assistant.server.host`, строка &mdash; хост сервера
* `assistant.server.port`, целое число &mdash; порт сервера
* `assistant.db.url`, строка &mdash; JDBC-урл для подключения к PostgreSQL
* `assistant.db.user`, строка &mdash; пользователь для подключения к PostgreSQL
* `assistant.db.password`, строка &mdash; пароль для подключения к PostgreSQL
* `assistant.db.driver`, строка &mdash; класс драйвера PostgreSQL
* `assistant.db.connections.poolSize`, строка &mdash; размер пула соединений PostgreSQL

Где взять? (везде используеются токен и ключ основного аккаунта Трелло)

* Идентификаторы досок: `https://api.trello.com/1/members/me/boards?key=<APP KEY>&token=<TOKEN>`
* Идентификаторы столбцов: `https://api.trello.com/1/boards/<BOARD ID>/lists?key=<APP KEY>&token=<TOKEN>`
* Создайте карточки для Ассистента и для основго пользователя на любой доске, назначьте одну на себя, другую на ассистента и достаньте идентификаторы пользователей из `https://api.trello.com/1/boards/<BOARD ID>/cards?key=<APP KEY>&token=<TOKEN>`

Скопируйте содержимое в файл `src/main/resources/reference.conf`:

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
            name="Сделать"
            limit=217
          }
          week {
            id="xxxxxxxxxxxxxxxxxxxxxxxx"
            name="На этой неделе"
            limit=49
          }
          tomorrow {
            id="xxxxxxxxxxxxxxxxxxxxxxxx"
            name="Завтра"
            limit=7
          }
          today {
            id="xxxxxxxxxxxxxxxxxxxxxxxx"
            name="Сегодня"
            limit=7
          }
          inProgress {
            id="xxxxxxxxxxxxxxxxxxxxxxxx"
            name="В процессе"
            limit=2
          }
          delegated {
            id="xxxxxxxxxxxxxxxxxxxxxxxx"
            name="Делегировано"
            limit=14
          }
          done {
            id="xxxxxxxxxxxxxxxxxxxxxxxx"
            name="Сделано"
            limit=217
          }
        }
      }
      next {
        id="xxxxxxxxxxxxxxxxxxxxxxxx"
        columns {
          todo {
            id="xxxxxxxxxxxxxxxxxxxxxxxx"
            name="Сделать"
            limit=2562
          }
          done {
            id="xxxxxxxxxxxxxxxxxxxxxxxx"
            name="Сделано"
            limit=2562
          }
        }
      }
    }
    messages {
      listLimitReached="Не удалось переместить карточку в столбец \"LIST_NAME\". Столбец \"LIST_NAME\" уже содержит LIMIT или больше карточек"
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

### Сборка приложения Ассистента

```sh
# Build
./sbtx assembly
```

Отправить на сервер приложение можно через scp:

```sh
# Deploy
scp target/scala-2.12/assistant-assembly-0.0.1-SNAPSHOT.jar user@yourserver.com:/root/assistant.jar
```

Запуск приложения:

```sh
nohup java -jar assistant.jar &> assistant.log &
```

Проверка, что приложение работает:

```sh
curl http://yourserver.com:8080/api/trello
```

Остановка приложения:

```sh
# Get PID of "java" process
top

# Kill it
kill -9 <pid>
```

### Webhooks

Через Webhooks Трелло сообщает о новых событиях, таких как создание и обновление карточек. Нужно создать веб-хуки для обеих досок "Текущая" и "Следующая". Вставьте токен, ключ приложения для вашего личного пользователя Трелло и адрес сервера, на котором уже работает ассистент и выполните:

Здесь также нужно вставить идентификатор доски "Текущая":

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

Придет ответ от Трелло:

```json
{
  "id": "xxxxxxxxxxxxxxxxxxxxxxxx",
  "description": "Assistant's Webhook for Daily board",
  "idModel": "xxxxxxxxxxxxxxxxxxxxxxxx",
  "callbackURL": "http://assistant.manenkov.com:8080/api/trello/receive_webhook",
  "active": true,
  "consecutiveFailures": 0,
  "firstConsecutiveFailDate": null
}
```

Аналогично для доски "Следующая":

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

Придет ответ от Трелло:

```json
{
  "id": "xxxxxxxxxxxxxxxxxxxxxxxx",
  "description": "Assistant's Webhook for Daily Next board",
  "idModel": "xxxxxxxxxxxxxxxxxxxxxxxx",
  "callbackURL": "http://assistant.manenkov.com:8080/api/trello/receive_webhook",
  "active": true,
  "consecutiveFailures": 0,
  "firstConsecutiveFailDate": null
}
```