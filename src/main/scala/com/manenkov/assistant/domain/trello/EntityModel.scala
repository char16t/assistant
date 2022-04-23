package com.manenkov.assistant.domain.trello

import java.time.LocalDateTime

case class CardTechInfo(pin: Boolean = true, testCreatedAt: String = LocalDateTime.now().toString)

case class EntityModel(name: String)
case class Card(id: String, name: String, desc: String, pos: Double, due: Option[String], idList: String, idBoard: String, dueComplete: Boolean, idMembers: List[String])
case class CardInternal(
                         id: String,
                         desc: String,
                         name: String,
                         due: Option[LocalDateTime],
                         idList: String,
                         idBoard: String,
                         pos: Double,
                         dueComplete: Boolean,
                         idMembers: List[String],
                         techInfo: CardTechInfo
                       )
case class CardChanges(
                        boardId: Option[String] = None,
                        listId: Option[String] = None,
                        pos: Option[String] = None,
                        due: Option[LocalDateTime] = None,
                        dueComplete: Option[Boolean] = None,
                        idMembers: Option[List[String]]= None,
                        techInfo: Option[CardTechInfo] = None,
                      )

case class Old(due: Option[String], idList: Option[String])
case class CardW(id: String, name: String, idShort: Long, shortLink: String, due: Option[String], idList: Option[String])
case class Data(card: Option[CardW], old: Option[Old])
case class Action(idMemberCreator: String, `type`: String, data: Data)
case class Webhook(action: Action)

