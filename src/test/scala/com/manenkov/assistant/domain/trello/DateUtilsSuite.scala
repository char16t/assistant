package com.manenkov.assistant.domain.trello

import org.scalatest.funsuite.AnyFunSuite

import java.time.LocalDateTime

class DateUtilsSuite extends AnyFunSuite {
  test("DateUtils::isOverdue()") {
    val dateUtils = DateUtils(3)
    assert(dateUtils.isOverdue(LocalDateTime.now()) == false)
    assert(dateUtils.isOverdue(LocalDateTime.now().minusDays(5)) == true)
    assert(dateUtils.isOverdue(LocalDateTime.now().plusDays(5)) == false)
  }

  test("DateUtils::isToday()") {
    val dateUtils = DateUtils(3)
    assert(dateUtils.isToday(LocalDateTime.now()) == true)
    assert(dateUtils.isToday(LocalDateTime.now().plusDays(1)) == false)
    assert(dateUtils.isToday(LocalDateTime.now().minusDays(1)) == false)
  }

  test("DateUtils::isTomorrow()") {
    val dateUtils = DateUtils(3)
    assert(dateUtils.isTomorrow(LocalDateTime.now()) == false)
    assert(dateUtils.isTomorrow(LocalDateTime.now().plusDays(1)) == true)
    assert(dateUtils.isTomorrow(LocalDateTime.now().minusDays(1)) == false)
  }

  test("DateUtils::onThisWeek()") {
    val dateUtils = DateUtils(3)
    assert(dateUtils.onThisWeek(LocalDateTime.now()) == true)
    assert(dateUtils.onThisWeek(LocalDateTime.now().plusWeeks(1)) == false)
    assert(dateUtils.onThisWeek(LocalDateTime.now().minusWeeks(1)) == false)
  }

  test("DateUtils::onThisMonth()") {
    val dateUtils = DateUtils(3)
    assert(dateUtils.onThisMonth(LocalDateTime.now()) == true)
    assert(dateUtils.onThisMonth(LocalDateTime.now().plusMonths(1)) == false)
    assert(dateUtils.onThisMonth(LocalDateTime.now().minusMonths(1)) == false)
  }
}
