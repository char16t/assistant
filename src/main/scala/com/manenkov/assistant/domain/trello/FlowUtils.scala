package com.manenkov.assistant.domain.trello

import com.manenkov.flow.{Change, Event, Flow, PerDay, PerMonth, PerWeek, PerYear}

case class FlowUtils(
                      timeZoneCorrection: Integer,
                      limitPerDay: Int,
                      limitPerWeek: Int,
                      limitPerMonth: Int,
                      limitPerYear: Int,
                    ) {
  def cardToEvent(card: CardInternal): Event =
    Event(
      id = card.id,
      name = card.name,
      isPin = card.labels.map(_.name).contains("pin"),
      due = card.due.getOrElse(DateUtils(timeZoneCorrection).todayMax()) // TODO: Fix me
    )

  def cardsToEvents(cards: Seq[CardInternal]): Seq[Event] =
    cards.map(cardToEvent).zipWithIndex.map(eventWithIndex => eventWithIndex._1.copy(order = eventWithIndex._2))

  def flow(events: Seq[Event]): Seq[Event] = {
    val perDay = Flow.flow(events)(PerDay(limitPerDay))
    val perWeek = Flow.flow(perDay)(PerWeek(limitPerWeek))
    val perMonth = Flow.flow(perWeek)(PerMonth(limitPerMonth))
    Flow.flow(perMonth)(PerYear(limitPerYear))
  }

  def diff(from: Seq[Event], to: Seq[Event]): Seq[Change] = Flow.diff(from, to)
}
