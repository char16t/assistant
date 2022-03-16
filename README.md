Server setup:

1. Install JRE
```
sudo apt install default-jre
```
2. Check
```
java -version
```

How to:

1. Create new account for your bot at Trello: https://trello.com/signup
2. Login to your personal account (not as bot, your own account)
3. Create board "Current"
4. Create columns for "Current" board: "To Do", "On this week", "Tomorrow", "Today", "In progress", "In progress (delegated)", "Done"
5. Add your bot to members at "Current" board with "Normal" rights
6. Create board "Next"
7. Create columns for "Next" board: "To Do", "Done"
8. Add your bot to members at "Next" board with "Normal" rights
9. Login to your bot account
10. Get Developer API Keys for your bot: https://trello.com/app-key
11. Fill app key and put to your browser to get Token for your bot: 
```
https://trello.com/1/authorize?expiration=never&scope=read,write,account&response_type=token&name=Server%20Token&key=<PUT_YOUR_APP_KEY_HERE>
```
12. Fill app key and token and get ids for your "Current" and "Next" boards:
```
https://api.trello.com/1/members/me/boards?key=<PUT_APP_KEY>&token=<PUT_TOKEN>
```
13. Fill app key, token and "Current" board id and get columns ids for "Current" board:
```
https://api.trello.com/1/boards/<PUT_CURRENT_BOARD_ID>/lists?key=<PUT_APP_KEY>&token=<PUT_TOKEN>
```
14. Fill app key, token and "Next" board id and get columns ids for "Next" board:
```
https://api.trello.com/1/boards/<PUT_NEXT_BOARD_ID>/lists?key=<PUT_APP_KEY>&token=<PUT_TOKEN>
```
15. Fill configuration
```
assistant {
  trello {
    appKey="PUT_APP_KEY"
    token="PUT_TOKEN"
    boards {
      current {
        id="PUT_CURRENT_BOARD_ID"
        columns {
          todo="PUT_TODO_COLUMN_ID_AT_CURRENT_BOARD"
          week="PUT_WEEK_COLUMN_ID_AT_CURRENT_BOARD"
          tomorrow="PUT_TOMORROW_COLUMN_ID_AT_CURRENT_BOARD"
          today="PUT_TODAY_COLUMN_ID_AT_CURRENT_BOARD"
          inProgress="PUT_IN_PROGRESS_COLUMN_ID_AT_CURRENT_BOARD"
          delegated="PUT_DELEGATED_COLUMN_ID_AT_CURRENT_BOARD"
          done="PUT_DONE_COLUMN_ID_AT_CURRENT_BOARD"
        }
      }
      next {
        id="PUT_NEXT_BOARD_ID"
        columns {
          todo="PUT_TODO_COLUMN_ID_AT_NEXT_BOARD"
          done="PUT_DONE_COLUMN_ID_AT_NEXT_BOARD"
        }
      }
    }
  }
  server {
    host="0.0.0.0"
    port=8080
  }
}
```
