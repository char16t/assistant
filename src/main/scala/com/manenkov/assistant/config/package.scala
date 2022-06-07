package com.manenkov.assistant

import io.circe.Decoder
import io.circe.generic.semiauto._

package object config {
  implicit val todoColumnConfigDecoder: Decoder[TodoColumnConfig] = deriveDecoder
  implicit val WeekColumnConfigDecoder: Decoder[WeekColumnConfig] = deriveDecoder
  implicit val tomorrowColumnConfigDecoder: Decoder[TomorrowColumnConfig] = deriveDecoder
  implicit val todayColumnConfigDecoder: Decoder[TodayColumnConfig] = deriveDecoder
  implicit val inProgressColumnConfigDecoder: Decoder[InProgressColumnConfig] = deriveDecoder
  implicit val delegatedColumnConfigDecoder: Decoder[DelegatedColumnConfig] = deriveDecoder
  implicit val doneColumnConfig: Decoder[DoneColumnConfig] = deriveDecoder
  implicit val nextTodoColumnConfigDecoder: Decoder[NextTodoColumnConfig] = deriveDecoder
  implicit val nextDoneColumnConfigDecoder: Decoder[NextDoneColumnConfig] = deriveDecoder
  implicit val assistantConfigDecoder: Decoder[AssistantConfig] = deriveDecoder
  implicit val assistantUserConfigDecoder: Decoder[AssistantUserConfig] = deriveDecoder
  implicit val ownerUserConfigDecoder: Decoder[OwnerUserConfig] = deriveDecoder
  implicit val trelloUsersConfigDecoder: Decoder[TrelloUsersConfig] = deriveDecoder
  implicit val trelloConfigDecoder: Decoder[TrelloConfig] = deriveDecoder
  implicit val serverConfigDecoder: Decoder[ServerConfig] = deriveDecoder
  implicit val boardsConfigDecoder: Decoder[BoardsConfig] = deriveDecoder
  implicit val nextBoardConfigDecoder: Decoder[NextBoardConfig] = deriveDecoder
  implicit val nextBoardColumnsConfigDecoder: Decoder[NextBoardColumnsConfig] = deriveDecoder
  implicit val currentBoardConfigDecoder: Decoder[CurrentBoardConfig] = deriveDecoder
  implicit val currentBoardColumnsConfigDecoder: Decoder[CurrentBoardColumnsConfig] = deriveDecoder
  implicit val databaseConnectionsConfigDecoder: Decoder[DatabaseConnectionsConfig] = deriveDecoder
  implicit val databaseConfigDecoder: Decoder[DatabaseConfig] = deriveDecoder
}
