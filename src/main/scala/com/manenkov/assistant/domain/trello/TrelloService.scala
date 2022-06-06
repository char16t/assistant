package com.manenkov.assistant.domain.trello

import cats.effect.Sync
import com.manenkov.assistant.config.AssistantConfig
import io.circe.generic.extras.Configuration
import sttp.client3.quick.backend

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TrelloService[F[_]](trelloRepo: TrelloRepositoryAlgebra[F], conf: AssistantConfig) {

  implicit val customConfig: Configuration = Configuration.default.withDefaults

  def hello(): F[EntityModel] = trelloRepo.hello()

  val dateUtils = DateUtils(conf.trello.timeZoneCorrection)

  def receiveWebhook(webhook: Webhook)(implicit S: Sync[F]): F[Seq[Card]] = S.delay[Seq[Card]] {
    import sttp.client3._
    import sttp.client3.circe._
    import io.circe.generic.auto._

    val isProcessAllowed = webhook.action.idMemberCreator == conf.trello.users.owner.id

    val cards = webhook.action.`type` match {

      // skip "update due date" update type
      case "updateCard" if isProcessAllowed
        && webhook.action.data.old.isDefined
        && webhook.action.data.old.get.due.isDefined
        && webhook.action.data.card.get.due.isDefined => Seq()

      // update due date depends on column
      case "updateCard" if isProcessAllowed
        && webhook.action.data.old.isDefined
        && webhook.action.data.old.get.idList.isDefined
        && webhook.action.data.card.get.idList.isDefined =>

        // get all cards
        import sttp.client3._
        val getCurrentCardsUri = uri"https://api.trello.com/1/boards/${conf.trello.boards.current.id}/cards?key=${conf.trello.users.assistant.appKey}&token=${conf.trello.users.assistant.token}"
        val currentBoardCardsRequest = basicRequest.get(getCurrentCardsUri).response(asJson[List[Card]])
        val currentBoardResponse = currentBoardCardsRequest.send(backend)
        val getNextCardsUri = uri"https://api.trello.com/1/boards/${conf.trello.boards.next.id}/cards?key=${conf.trello.users.assistant.appKey}&token=${conf.trello.users.assistant.token}"
        val nextBoardCardsRequest = basicRequest.get(getNextCardsUri).response(asJson[List[Card]])
        val nextBoardCardsResponse = nextBoardCardsRequest.send(backend)
        val allCards = Seq(currentBoardResponse.body.toOption, nextBoardCardsResponse.body.toOption).flatten.flatten

        val actualSize = cardToCardInternal(allCards).count(_.idList == webhook.action.data.card.get.idList.get)
        val limit = webhook.action.data.card.get.idList.get match {
          case conf.trello.boards.current.columns.todo.id => conf.trello.boards.current.columns.todo.limit
          case conf.trello.boards.current.columns.week.id => conf.trello.boards.current.columns.week.limit
          case conf.trello.boards.current.columns.tomorrow.id => conf.trello.boards.current.columns.tomorrow.limit
          case conf.trello.boards.current.columns.today.id => conf.trello.boards.current.columns.today.limit
          case conf.trello.boards.current.columns.inProgress.id => conf.trello.boards.current.columns.inProgress.limit
          case conf.trello.boards.current.columns.delegated.id => conf.trello.boards.current.columns.delegated.limit
          case conf.trello.boards.current.columns.done.id => conf.trello.boards.current.columns.done.limit
          case _ => Int.MaxValue
        }

        val maybeCurrentCard = cardToCardInternal(allCards).find(_.id == webhook.action.data.card.get.id)

        if (actualSize > limit) {
          val card = maybeCurrentCard.get
          updateCard(card, CardChanges(boardId = Option(card.idBoard), listId = webhook.action.data.old.get.idList), silent = true, asOwner = false)

          val listName = webhook.action.data.card.get.idList.get match {
            case conf.trello.boards.current.columns.todo.id => conf.trello.boards.current.columns.todo.name
            case conf.trello.boards.current.columns.week.id => conf.trello.boards.current.columns.week.name
            case conf.trello.boards.current.columns.tomorrow.id => conf.trello.boards.current.columns.tomorrow.name
            case conf.trello.boards.current.columns.today.id => conf.trello.boards.current.columns.today.name
            case conf.trello.boards.current.columns.inProgress.id => conf.trello.boards.current.columns.inProgress.name
            case conf.trello.boards.current.columns.delegated.id => conf.trello.boards.current.columns.delegated.name
            case conf.trello.boards.current.columns.done.id => conf.trello.boards.current.columns.done.name
            case id => id
          }
          val msg = conf.trello.messages.listLimitReached
            .replaceAll("LIMIT", limit.toString)
            .replaceAll("LIST_NAME", listName)
          commentCard(card, msg)
          Seq()
        } else {

          // When card moved from Done column to other column
          // uncheck "complete" flag
          if ((webhook.action.data.old.get.idList.get == conf.trello.boards.current.columns.done.id
            || webhook.action.data.old.get.idList.get == conf.trello.boards.next.columns.done.id)
            && webhook.action.data.card.get.idList.get != conf.trello.boards.current.columns.done.id
            && webhook.action.data.card.get.idList.get != conf.trello.boards.next.columns.done.id
          ) {
            maybeCurrentCard match {
              case Some(card) =>
                updateCard(card, CardChanges(dueComplete = Option(false)), silent = true, asOwner = false)
              case None => ()
            }
          }

          // When card moved from other column column to Done
          // check "complete" flag
          if ((webhook.action.data.card.get.idList.get == conf.trello.boards.current.columns.done.id
            || webhook.action.data.card.get.idList.get == conf.trello.boards.next.columns.done.id)
            && webhook.action.data.old.get.idList.get != conf.trello.boards.current.columns.done.id
            && webhook.action.data.old.get.idList.get != conf.trello.boards.next.columns.done.id
          ) {
            maybeCurrentCard match {
              case Some(card) =>
                updateCard(card, CardChanges(dueComplete = Option(true)), silent = true, asOwner = false)
              case None => ()
            }
          }

          maybeCurrentCard match {
            case Some(card) => webhook.action.data.card.get.idList.get match {
              case conf.trello.boards.current.columns.todo.id =>
                updateCard(card, CardChanges(due = Option(dateUtils.thisMonthMax())), silent = true, asOwner = false)
                Seq()
              case conf.trello.boards.current.columns.week.id =>
                updateCard(card, CardChanges(due = Option(dateUtils.thisWeekMax())), silent = true, asOwner = false)
                Seq()
              case conf.trello.boards.current.columns.tomorrow.id =>
                updateCard(card, CardChanges(due = Option(dateUtils.tomorrowMax())), silent = true, asOwner = false)
                Seq()
              case conf.trello.boards.current.columns.today.id =>
                updateCard(card, CardChanges(due = Option(dateUtils.todayMax())), silent = true, asOwner = false)
                Seq()
              case _ =>
                // do nothing
                Seq()
            }
            case None => Seq()
          }
        }

      // update or create card
      case "createCard" | "updateCard" if isProcessAllowed =>
        val uri = uri"https://api.trello.com/1/cards/${webhook.action.data.card.get.id}?key=${conf.trello.users.assistant.appKey}&token=${conf.trello.users.assistant.token}"
        val request = basicRequest.get(uri).response(asJson[Card])
        request.send(backend).body.toOption match {
          case Some(card) => Seq(card)
          case None => Seq()
        }

      case _ => Seq()
    }
    processCards(cards)
    cards
  }

  def organizeCards()(implicit S: Sync[F]): F[Seq[Card]] =
    S.delay[Seq[Card]] {
      import sttp.client3._
      import sttp.client3.circe._
      import io.circe.generic.auto._

      val getCurrentCardsUri = uri"https://api.trello.com/1/boards/${conf.trello.boards.current.id}/cards?key=${conf.trello.users.assistant.appKey}&token=${conf.trello.users.assistant.token}"
      val request = basicRequest.get(getCurrentCardsUri).response(asJson[List[Card]])
      val response = request.send(backend)

      val getNextCardsUri = uri"https://api.trello.com/1/boards/${conf.trello.boards.next.id}/cards?key=${conf.trello.users.assistant.appKey}&token=${conf.trello.users.assistant.token}"
      val request2 = basicRequest.get(getNextCardsUri).response(asJson[List[Card]])
      val response2 = request2.send(backend)

      val allCards = Seq(response.body.toOption, response2.body.toOption).flatten.flatten

      processCards(allCards)

      allCards
    }

  private def processCards(cardsToProcess: Seq[Card]): Unit = {

    val flowUtils = FlowUtils(
      timeZoneCorrection = conf.trello.timeZoneCorrection,
      limitPerDay = conf.trello.boards.current.columns.today.limit,
      limitPerWeek = conf.trello.boards.current.columns.today.limit * 7,
      limitPerMonth = conf.trello.boards.current.columns.today.limit * 7 * 4,
      limitPerYear = conf.trello.boards.current.columns.today.limit * 7 * 4 * 12
    )
    val cardsAsEvents = flowUtils.cardsToEvents(cardToCardInternal(cardsToProcess))
    val updatedCardsAsEvents = flowUtils.flow(cardsAsEvents)
    val diff = flowUtils.diff(cardsAsEvents, updatedCardsAsEvents)
    println(
      s"""
        |=================
        ||     BEFORE    |
        |=================
        | $cardsAsEvents
        |
        |=================
        |      AFTER     |
        |=================
        | $updatedCardsAsEvents
        |
        |=================
        ||    CHANGES    |
        |=================
        | $diff
        |=================
        |""".stripMargin)

    cardToCardInternal(cardsToProcess).foreach({
      case card if card.due.isEmpty && card.idList == conf.trello.boards.current.columns.todo.id =>
        updateCard(card, CardChanges (due = Option(dateUtils.thisMonthMax())), silent = true, asOwner = false)
      case card if card.due.isEmpty && card.idList == conf.trello.boards.current.columns.week.id =>
        updateCard(card, CardChanges(due = Option(dateUtils.thisWeekMax())), silent = true, asOwner = false)
      case card if card.due.isEmpty && card.idList == conf.trello.boards.current.columns.tomorrow.id =>
        updateCard(card, CardChanges(due = Option(dateUtils.tomorrowMax())), silent = true, asOwner = false)
      case card if card.due.isEmpty && card.idList == conf.trello.boards.current.columns.today.id =>
        updateCard(card, CardChanges(due = Option(dateUtils.todayMax())), silent = true, asOwner = false)
      case card if card.due.isEmpty && card.idList == conf.trello.boards.current.columns.inProgress.id =>
        updateCard(card, CardChanges(due = Option(dateUtils.todayMax())), silent = true, asOwner = false)
      case card if card.due.isEmpty && card.idList == conf.trello.boards.current.columns.delegated.id =>
        updateCard(card, CardChanges(due = Option(dateUtils.todayMax())), silent = true, asOwner = false)
      case card if card.due.isEmpty && card.idList == conf.trello.boards.current.columns.done.id =>
        updateCard(card, CardChanges(due = Option(dateUtils.todayMax())), silent = true, asOwner = false)

      // not matched cards
      case _ =>
    })

    cardToCardInternal(cardsToProcess).foreach({
      // Not assigned cards
      case card if card.idMembers.isEmpty =>
        updateCard(card, CardChanges(idMembers = Option(List(conf.trello.users.owner.id))), silent = true, asOwner = true)

      // not matched cards
      case _ =>
    })

    cardToCardInternal(cardsToProcess).foreach {

      // Move done cards to Done column
      case card if card.dueComplete && card.idList != conf.trello.boards.current.columns.done.id =>
        updateCard(card, CardChanges(boardId = Option(conf.trello.boards.current.id), listId = Option(conf.trello.boards.current.columns.done.id), pos = Option("top")), silent = true, asOwner = false)

      // Move undone cards from Done to Today column
      case card if !card.dueComplete && card.idList == conf.trello.boards.current.columns.done.id =>
        updateCard(card, CardChanges(boardId = Option(conf.trello.boards.current.id), listId = Option(conf.trello.boards.current.columns.today.id)), silent = true, asOwner = false)

      // skip cards in inProgress, delegated and done columns
      case card if card.idList == conf.trello.boards.current.columns.inProgress.id =>
      case card if card.idList == conf.trello.boards.current.columns.delegated.id =>
      case card if card.idList == conf.trello.boards.current.columns.done.id =>
      case card if card.idList == conf.trello.boards.next.columns.done.id =>

      // Move overdue cards to Today column
      case card if card.due.exists(d => dateUtils.isOverdue(d)) && card.idList == conf.trello.boards.current.columns.today.id =>
      case card if card.due.exists(d => dateUtils.isOverdue(d)) =>
        updateCard(card, CardChanges(boardId = Option(conf.trello.boards.current.id), listId = Option(conf.trello.boards.current.columns.today.id)), silent = true, asOwner = false)

      // Move cards with due dates on previous months to Today column
      case card if card.due.exists(d => dateUtils.onPrevMonths(d)) && card.idList == conf.trello.boards.current.columns.today.id =>
      case card if card.due.exists(d => dateUtils.onPrevMonths(d)) =>
        updateCard(card, CardChanges(boardId = Option(conf.trello.boards.current.id), listId = Option(conf.trello.boards.current.columns.today.id)), silent = true, asOwner = false)

      // Move cards with due date today to Today column
      case card if card.due.exists(d => dateUtils.isToday(d)) && card.idList == conf.trello.boards.current.columns.today.id =>
      case card if card.due.exists(d => dateUtils.isToday(d)) =>
        updateCard(card, CardChanges(boardId = Option(conf.trello.boards.current.id), listId = Option(conf.trello.boards.current.columns.today.id)), silent = true, asOwner = false)

      // Move cards with due date tomorrow to Tomorrow column
      case card if card.due.exists(d => dateUtils.isTomorrow(d)) && card.idList == conf.trello.boards.current.columns.tomorrow.id =>
      case card if card.due.exists(d => dateUtils.isTomorrow(d)) =>
        updateCard(card, CardChanges(boardId = Option(conf.trello.boards.current.id), listId = Option(conf.trello.boards.current.columns.tomorrow.id)), silent = true, asOwner = false)

      // Move cards with due date on this week to On this week column
      case card if card.due.exists(d => dateUtils.onThisWeek(d)) && card.idList == conf.trello.boards.current.columns.week.id =>
      case card if card.due.exists(d => dateUtils.onThisWeek(d)) =>
        updateCard(card, CardChanges(boardId = Option(conf.trello.boards.current.id), listId = Option(conf.trello.boards.current.columns.week.id)), silent = true, asOwner = false)

      // Move cards with due date on this month to On this month column
      case card if card.due.exists(d => dateUtils.onThisMonth(d)) && card.idList == conf.trello.boards.current.columns.todo.id =>
      case card if card.due.exists(d => dateUtils.onThisMonth(d)) =>
        updateCard(card, CardChanges(boardId = Option(conf.trello.boards.current.id), listId = Option(conf.trello.boards.current.columns.todo.id)), silent = true, asOwner = false)

      // Move cards with due date on next months to Next board into To Do column
      case card if card.due.exists(d => dateUtils.onNextMonths(d)) && card.idList == conf.trello.boards.next.columns.todo.id =>
      case card if card.due.exists(d => dateUtils.onNextMonths(d)) =>
        updateCard(card, CardChanges(boardId = Option(conf.trello.boards.next.id), listId = Option(conf.trello.boards.next.columns.todo.id)), silent = true, asOwner = false)

      // Log not matched cards
      case card =>
        println(s"Card '${card.name}' (${card.id}) not matched")
    }
  }

  private def commentCard(card: CardInternal, message: String): Unit = {
    import sttp.client3._

    val pathParams = Map[String, String](
      "key" -> conf.trello.users.assistant.appKey,
      "token" -> conf.trello.users.assistant.token,
      "text" -> message,
    )

    val uri = pathParams
      .filter(pair => !pair._2.isBlank)
      .foldLeft(uri"https://api.trello.com/1/cards/${card.id}/actions/comments")((acc, part) => acc.addParam(part._1, part._2))

    println(s"STTP Request: PUT $uri")
    basicRequest.post(uri).send(backend)
    ()
  }

  private def updateCard(card: CardInternal, changes: CardChanges, silent: Boolean, asOwner: Boolean): Unit = {
    import sttp.client3._
    import sttp.client3.circe._
    import io.circe.syntax._

    val silentPathParams = Map[String, String](
      "key" -> conf.trello.users.owner.appKey,
      "token" -> conf.trello.users.owner.token,
    )
    val silentInParams = silentPathParams + (("subscribed", "false"))
    val silentOutParams = silentPathParams + (("subscribed", "true"))
    val uriSilentIn = silentInParams
      .filter(pair => !pair._2.isBlank)
      .foldLeft(uri"https://api.trello.com/1/cards/${card.id}")((acc, part) => acc.addParam(part._1, part._2))
    val uriSilentOut = silentOutParams
      .filter(pair => !pair._2.isBlank)
      .foldLeft(uri"https://api.trello.com/1/cards/${card.id}")((acc, part) => acc.addParam(part._1, part._2))

    val pathParams = Map[String, String](
      "key" -> (if (asOwner) conf.trello.users.owner.appKey else conf.trello.users.assistant.appKey),
      "token" -> (if (asOwner) conf.trello.users.owner.token else conf.trello.users.assistant.token),
    )
    val bodyParams = Map[String, String](
      "idBoard" -> changes.boardId.getOrElse(""),
      "idList" -> changes.listId.getOrElse(""),
      "pos" -> (changes.pos match {
        case Some(pos) => pos
        case None => ""
      }),
      "due" -> changes.due.map(dd => dd.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))).getOrElse(""),
      "dueComplete" -> (changes.dueComplete match {
        case Some(dueComplete) => dueComplete.toString
        case None => ""
      }),
      "idMembers" -> changes.idMembers.getOrElse(List()).mkString(","),
      "desc"-> (changes.techInfo match {
        case Some(info) => makeCardDescription(card.desc, info)
        case None => makeCardDescription(card.desc, getCardTechInfo(card.desc))
      })
    ).filter(!_._2.isBlank)

    val uri = pathParams
      .filter(pair => !pair._2.isBlank)
      .foldLeft(uri"https://api.trello.com/1/cards/${card.id}")((acc, part) => acc.addParam(part._1, part._2))

    if (silent) {
      println(s"STTP Request: PUT $uriSilentIn")
      basicRequest.put(uriSilentIn).send(backend)
    }
    println(s"STTP Request: PUT $uri\n${bodyParams.asJson}\n")
    basicRequest.put(uri)
      .contentType("application/json")
      .body(bodyParams.asJson)
      .send(backend)
    if (silent) {
      println(s"STTP Request: PUT $uriSilentOut")
      basicRequest.put(uriSilentOut).send(backend)
    }
    ()
  }

  private def getCardTechInfo(cardDescription: String): CardTechInfo = {
    val lastIdx = cardDescription.lastIndexOf("```")
    if (lastIdx == -1) {
      return CardTechInfo()
    }
    val substr1 = cardDescription.substring(0, lastIdx)
    val firstIdx = substr1.lastIndexOf("```")
    if (firstIdx == -1) {
      return CardTechInfo()
    }
    val json = cardDescription.substring(firstIdx + 3, lastIdx)

    import io.circe.parser._
    import io.circe.generic.extras.auto._
    val decodedFoo = decode[CardTechInfo](json)
    decodedFoo match {
      case Right(info) =>
        info
      case Left(error) =>
        println(error)
        CardTechInfo()
    }
  }

  private def makeCardDescription(cardDescription: String, techInfo: CardTechInfo): String = {
    import io.circe.syntax._
    import io.circe.generic.extras.auto._

    val json = techInfo.asJson.noSpaces

    val lastIdx = cardDescription.lastIndexOf("```")
    if (lastIdx == -1) {
      return cardDescription ++
        s"""
          |
          |---
          |_Tech info for assistant (don't touch):_
          |
          |```
          |$json
          |```
          |""".stripMargin
    }
    val substr1 = cardDescription.substring(0, lastIdx)
    val firstIdx = substr1.lastIndexOf("```")
    if (firstIdx == -1) {
      return cardDescription ++
        s"""
           |
           |---
           |_Tech info for assistant (don't touch):_
           |
           |```
           |$json
           |```
           |""".stripMargin
    }

    cardDescription.substring(0, firstIdx) ++
      s"""```
        |$json
        |```""".stripMargin
  }

  private def cardToCardInternal(cards: Seq[Card]): Seq[CardInternal] = {
    cards.map {
      case card if card.due.nonEmpty =>
        CardInternal(
          id = card.id,
          name = card.name,
          desc = card.desc,
          due = card.due.map(dd => LocalDateTime.parse(dd, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))),
          idList = card.idList,
          idBoard = card.idBoard,
          pos = card.pos,
          dueComplete = card.dueComplete,
          idMembers = card.idMembers,
          labels = card.labels,
          idLabels = card.idLabels,
          techInfo = getCardTechInfo(card.desc),
        )
      case card if card.due.isEmpty =>
        CardInternal(
          id = card.id,
          name = card.name,
          desc = card.desc,
          due = None,
          idList = card.idList,
          idBoard = card.idBoard,
          pos = card.pos,
          dueComplete = card.dueComplete,
          idMembers = card.idMembers,
          labels = card.labels,
          idLabels = card.idLabels,
          techInfo = getCardTechInfo(card.desc),
        )
    }
  }
}

object TrelloService {
  def apply[F[_]](
                   repository: TrelloRepositoryAlgebra[F],
                   conf: AssistantConfig
                 ): TrelloService[F] =
    new TrelloService[F](repository, conf)
}
