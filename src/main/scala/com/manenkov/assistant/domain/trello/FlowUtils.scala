package com.manenkov.assistant.domain.trello

import com.manenkov.assistant.config.AssistantConfig
import com.manenkov.flow.{Change, Event, Flow, PerDay, PerMonth, PerWeek, PerYear}

case class FlowUtils(conf: AssistantConfig) {
  def cardToEvent(card: CardInternal): Event =
    Event(
      id = card.id,
      name = card.name,
      isPin = card.labels.map(_.name).contains(conf.trello.labels.pin.name),
      due = card.due.getOrElse(DateUtils(conf.trello.timeZoneCorrection).todayMax()) // TODO: Fix me
    )

  def cardsToEvents(cards: Seq[CardInternal]): Seq[Event] =
    cards.map(cardToEvent).zipWithIndex.map(eventWithIndex => eventWithIndex._1.copy(order = eventWithIndex._2))

  def flow(events: Seq[Event]): Seq[Event] = {
    val perDay = Flow.flow(events)(PerDay(conf.trello.limits.cardsPerDay))
    val perWeek = Flow.flow(perDay)(PerWeek(conf.trello.limits.cardsPerWeek))
    val perMonth = Flow.flow(perWeek)(PerMonth(conf.trello.limits.cardsPerMonth))
    Flow.flow(perMonth)(PerYear(conf.trello.limits.cardsPerYear))
  }

  def diff(from: Seq[Event], to: Seq[Event]): Seq[Change] = Flow.diff(from, to)
}
