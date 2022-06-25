v0.2.0 (in development)

 - New algorithm to automatically reschedule events in the flow depending on the constraints at different time intervals

v0.0.1 (22.05.2022)

 - Daily transfer of cards by columns "Today", "Tomorrow", "This week", "This month"
 - Updating the due date on the card when transferring between columns. For example, when transferring the card to the "Today" column, the due date will be automatically set as "today 23:59", and when transferring the card to the "This week" column, the due date "next Sunday 23:59" will be set
 - Control of card limits in columns: if the card limit in a column is exceeded, it will not be possible to move a new card there, it will return to the original column with an explanatory comment inside
 - When creating a card, it is immediately assigned to the owner of the board and a due date is set on it in accordance with the column in which it was created
 - All expired cards are transferred to the "Today" column
 - When the "completed" checkbox is placed on the due date of the card, it is automatically transferred to the "Done" column at the top
 - When moving a card to the "Done" column, the "completed" flag is set to due date and removed when the card is moved from the "Done" column to another
 - When you delete the "completed" checkbox on the due date of the card, it is automatically transferred to the "Today" column
 - Cards with due dates in the next month and beyond are automatically transferred to the main board when their due dates are comes

