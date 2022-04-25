package com.manenkov.assistant.domain.trello

import java.time.{DayOfWeek, LocalDateTime, LocalTime}
import java.time.temporal.{ChronoUnit, TemporalAdjusters}

case class DateUtils(timeZoneCorrection: Integer) {

  private def withTimezone(dt: LocalDateTime): LocalDateTime =
    dt.plus(timeZoneCorrection.toLong, ChronoUnit.HOURS)

  def todayMin(): LocalDateTime = {
    withTimezone(LocalDateTime.now().`with`(LocalTime.MIN))
  }

  def todayMax(): LocalDateTime = {
    withTimezone(LocalDateTime.now().`with`(LocalTime.MAX))
  }

  def tomorrowMin(): LocalDateTime =
    withTimezone(LocalDateTime.now().plus(1, ChronoUnit.DAYS).`with`(LocalTime.MIN))

  def tomorrowMax(): LocalDateTime =
    withTimezone(LocalDateTime.now().plus(1, ChronoUnit.DAYS).`with`(LocalTime.MAX))

  def thisWeekMin(): LocalDateTime = {
    val now = LocalDateTime.now()
    if (now.getDayOfWeek == DayOfWeek.MONDAY)
      withTimezone(now.`with`(LocalTime.MIN))
    else
      withTimezone(now.`with`(TemporalAdjusters.previous(DayOfWeek.MONDAY)).`with`(LocalTime.MIN))
  }

  def thisWeekMax(): LocalDateTime = {
    val now = LocalDateTime.now()
    if (now.getDayOfWeek == DayOfWeek.SUNDAY)
      withTimezone(now.`with`(LocalTime.MAX))
    else
      withTimezone(now.`with`(TemporalAdjusters.next(DayOfWeek.SUNDAY)).`with`(LocalTime.MAX))
  }

  def thisMonthMin(): LocalDateTime = {
    val now = LocalDateTime.now()
    withTimezone(now.`with`(TemporalAdjusters.firstDayOfMonth()).`with`(LocalTime.MIN))
  }

  def thisMonthMax(): LocalDateTime = {
    val now = LocalDateTime.now()
    withTimezone(now.`with`(TemporalAdjusters.lastDayOfMonth()).`with`(LocalTime.MAX))
  }

  def isOverdue(date: LocalDateTime): Boolean = {
    val now = LocalDateTime.now().`with`(LocalTime.MIN)
    date.isBefore(now)
  }

  def isToday(date: LocalDateTime): Boolean = {
    val now = LocalDateTime.now()
    date.getYear == now.getYear &&
      date.getMonth == now.getMonth &&
      date.getDayOfMonth == now.getDayOfMonth
  }

  def isTomorrow(date: LocalDateTime): Boolean = {
    val tom = LocalDateTime.now().plus(1, ChronoUnit.DAYS)
    date.getYear == tom.getYear &&
      date.getMonth == tom.getMonth &&
      date.getDayOfMonth == tom.getDayOfMonth
  }

  def onThisWeek(date: LocalDateTime): Boolean = {
    val now = LocalDateTime.now()
    val mon = if (now.getDayOfWeek == DayOfWeek.MONDAY)
      now.`with`(LocalTime.MIN) else now.`with`(TemporalAdjusters.previous(DayOfWeek.MONDAY))
    val sun =
      if (now.getDayOfWeek == DayOfWeek.SUNDAY)
        now.`with`(LocalTime.MAX)
      else
        now.`with`(TemporalAdjusters.next(DayOfWeek.SUNDAY)).`with`(LocalTime.MAX)
    date.isAfter(mon) && date.isBefore(sun)
  }

  def onPrevMonths(date: LocalDateTime): Boolean = {
    val now = LocalDateTime.now()
    val firstDay = now.`with`(TemporalAdjusters.firstDayOfMonth()).`with`(LocalTime.MIN)
    date.isBefore(firstDay)
  }

  def onThisMonth(date: LocalDateTime): Boolean = {
    val now = LocalDateTime.now()
    val firstDay = now.`with`(TemporalAdjusters.firstDayOfMonth()).`with`(LocalTime.MIN)
    val lastDay = now.`with`(TemporalAdjusters.lastDayOfMonth()).`with`(LocalTime.MAX)
    date.isAfter(firstDay) && date.isBefore(lastDay)
  }

  def onNextMonths(date: LocalDateTime): Boolean = {
    val now = LocalDateTime.now()
    val lastDay = now.`with`(TemporalAdjusters.lastDayOfMonth()).`with`(LocalTime.MAX)
    date.isAfter(lastDay)
  }
}
