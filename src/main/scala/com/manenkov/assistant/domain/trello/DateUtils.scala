package com.manenkov.assistant.domain.trello

import java.time.{DayOfWeek, LocalDateTime, LocalTime}
import java.time.temporal.{ChronoUnit, TemporalAdjusters}

case class DateUtils(timeZoneCorrection: Integer) {

  private def plusTimezoneCorrection(dt: LocalDateTime): LocalDateTime =
    dt.plus(timeZoneCorrection.toLong, ChronoUnit.HOURS)

  private def minusTimezoneCorrection(dt: LocalDateTime): LocalDateTime =
    dt.minus(timeZoneCorrection.toLong, ChronoUnit.HOURS)

  def todayMin(): LocalDateTime = {
    minusTimezoneCorrection(LocalDateTime.now().`with`(LocalTime.of(0, 0)))
  }

  def todayMax(): LocalDateTime = {
    minusTimezoneCorrection(LocalDateTime.now().`with`(LocalTime.of(23, 59)))
  }

  def tomorrowMin(): LocalDateTime =
    minusTimezoneCorrection(LocalDateTime.now().plus(1, ChronoUnit.DAYS).`with`(LocalTime.of(0, 0)))

  def tomorrowMax(): LocalDateTime =
    minusTimezoneCorrection(LocalDateTime.now().plus(1, ChronoUnit.DAYS).`with`(LocalTime.of(23, 59)))

  def thisWeekMin(): LocalDateTime = {
    val now = LocalDateTime.now()
    if (now.getDayOfWeek == DayOfWeek.MONDAY)
      minusTimezoneCorrection(now.`with`(LocalTime.of(0, 0)))
    else
      minusTimezoneCorrection(now.`with`(TemporalAdjusters.previous(DayOfWeek.MONDAY)).`with`(LocalTime.of(0, 0)))
  }

  def thisWeekMax(): LocalDateTime = {
    val now = LocalDateTime.now()
    if (now.getDayOfWeek == DayOfWeek.SUNDAY)
      minusTimezoneCorrection(now.`with`(LocalTime.of(23, 59)))
    else
      minusTimezoneCorrection(now.`with`(TemporalAdjusters.next(DayOfWeek.SUNDAY)).`with`(LocalTime.of(23, 59)))
  }

  def thisMonthMin(): LocalDateTime = {
    val now = LocalDateTime.now()
    minusTimezoneCorrection(now.`with`(TemporalAdjusters.firstDayOfMonth()).`with`(LocalTime.of(0, 0)))
  }

  def thisMonthMax(): LocalDateTime = {
    val now = LocalDateTime.now()
    minusTimezoneCorrection(now.`with`(TemporalAdjusters.lastDayOfMonth()).`with`(LocalTime.of(23, 59)))
  }

  def isOverdue(date: LocalDateTime): Boolean = {
    val now = plusTimezoneCorrection(LocalDateTime.now()).`with`(LocalTime.of(0, 0))
    date.isBefore(now)
  }

  def isToday(date: LocalDateTime): Boolean = {
    val now = plusTimezoneCorrection(LocalDateTime.now())
    date.getYear == now.getYear &&
      date.getMonth == now.getMonth &&
      date.getDayOfMonth == now.getDayOfMonth
  }

  def isTomorrow(date: LocalDateTime): Boolean = {
    val tom = plusTimezoneCorrection(LocalDateTime.now().plus(1, ChronoUnit.DAYS))
    date.getYear == tom.getYear &&
      date.getMonth == tom.getMonth &&
      date.getDayOfMonth == tom.getDayOfMonth
  }

  def onThisWeek(date: LocalDateTime): Boolean = {
    val now = plusTimezoneCorrection(LocalDateTime.now())
    val mon = if (now.getDayOfWeek == DayOfWeek.MONDAY)
      now.`with`(LocalTime.of(0, 0)) else now.`with`(TemporalAdjusters.previous(DayOfWeek.MONDAY))
    val sun =
      if (now.getDayOfWeek == DayOfWeek.SUNDAY)
        now.`with`(LocalTime.of(23, 59))
      else
        now.`with`(TemporalAdjusters.next(DayOfWeek.SUNDAY)).`with`(LocalTime.of(23, 59))
    date.isAfter(mon) && date.isBefore(sun)
  }

  def onPrevMonths(date: LocalDateTime): Boolean = {
    val now = plusTimezoneCorrection(LocalDateTime.now())
    val firstDay = now.`with`(TemporalAdjusters.firstDayOfMonth()).`with`(LocalTime.of(0, 0))
    date.isBefore(firstDay)
  }

  def onThisMonth(date: LocalDateTime): Boolean = {
    val now = plusTimezoneCorrection(LocalDateTime.now())
    val firstDay = now.`with`(TemporalAdjusters.firstDayOfMonth()).`with`(LocalTime.of(0, 0))
    val lastDay = now.`with`(TemporalAdjusters.lastDayOfMonth()).`with`(LocalTime.of(23, 59))
    date.isAfter(firstDay) && date.isBefore(lastDay)
  }

  def onNextMonths(date: LocalDateTime): Boolean = {
    val now = plusTimezoneCorrection(LocalDateTime.now())
    val lastDay = now.`with`(TemporalAdjusters.lastDayOfMonth()).`with`(LocalTime.of(23, 59))
    date.isAfter(lastDay)
  }
}
