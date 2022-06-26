package com.manenkov.assistant.config

final case class TodoColumnConfig(id: String, name: String)
final case class WeekColumnConfig(id: String, name: String)
final case class TomorrowColumnConfig(id: String, name: String)
final case class TodayColumnConfig(id: String, name: String)
final case class InProgressColumnConfig(id: String, name: String)
final case class DelegatedColumnConfig(id: String, name: String)
final case class DoneColumnConfig(id: String, name: String)
final case class CurrentBoardColumnsConfig(
                                            todo: TodoColumnConfig,
                                            week: WeekColumnConfig,
                                            tomorrow: TomorrowColumnConfig,
                                            today: TodayColumnConfig,
                                            inProgress: InProgressColumnConfig,
                                            delegated: DelegatedColumnConfig,
                                            done: DoneColumnConfig,
                                          )
final case class CurrentBoardConfig(id: String, columns: CurrentBoardColumnsConfig)
final case class NextTodoColumnConfig(id: String, name: String)
final case class NextDoneColumnConfig(id: String, name: String)
final case class NextBoardColumnsConfig(todo: NextTodoColumnConfig, done: NextDoneColumnConfig)
final case class NextBoardConfig(id: String, columns: NextBoardColumnsConfig)
final case class BoardsConfig(current: CurrentBoardConfig, next: NextBoardConfig)
final case class TrelloUsersConfig(assistant: AssistantUserConfig, owner: OwnerUserConfig)
final case class LimitsConfig(cardsPerDay: Int, cardsPerWeek: Int, cardsPerMonth: Int, cardsPerYear: Int)
final case class PinLabelConfig(name: String)
final case class LabelsConfig(pin: PinLabelConfig)
final case class AssistantUserConfig(id: String, appKey: String, token: String)
final case class OwnerUserConfig(id: String, appKey: String, token: String)
final case class TrelloConfig(
                               timeZoneCorrection: Integer,
                               users: TrelloUsersConfig,
                               limits: LimitsConfig,
                               labels: LabelsConfig,
                               boards: BoardsConfig,
                             )
final case class ServerConfig(host: String, port: Int)
final case class AssistantConfig(db: DatabaseConfig, trello: TrelloConfig, server: ServerConfig)
