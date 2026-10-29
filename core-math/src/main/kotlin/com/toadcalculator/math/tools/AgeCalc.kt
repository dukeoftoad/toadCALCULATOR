// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.math.tools

import java.time.LocalDate
import java.time.MonthDay
import java.time.Period
import java.time.temporal.ChronoUnit

data class AgeResult(
    val years: Long,
    val months: Long,
    val days: Long,
    val totalDays: Long,
    val nextBirthdayDate: LocalDate,
    val daysUntilNextBirthday: Long,
)

/**
 * Age computation on java.time. Pure function; deterministic given [asOf].
 *
 * Convention for Feb-29 birthdays: in a non-leap year the birthday is
 * celebrated on Feb 28 (MonthDay.atYear throws there, so we clamp explicitly).
 */
object AgeCalc {

    fun age(birth: LocalDate, asOf: LocalDate): AgeResult {
        require(!asOf.isBefore(birth)) { "asOf must not be before birth" }

        val period = Period.between(birth, asOf)
        val totalDays = ChronoUnit.DAYS.between(birth, asOf)

        var next = birthdayInYear(birth, asOf.year)
        if (!next.isAfter(asOf)) {
            next = birthdayInYear(birth, asOf.year + 1)
        }
        return AgeResult(
            years = period.years.toLong(),
            months = period.months.toLong(),
            days = period.days.toLong(),
            totalDays = totalDays,
            nextBirthdayDate = next,
            daysUntilNextBirthday = ChronoUnit.DAYS.between(asOf, next),
        )
    }

    private fun birthdayInYear(birth: LocalDate, year: Int): LocalDate = try {
        MonthDay.from(birth).atYear(year)
    } catch (_: Exception) {
        // Feb 29 in a non-leap year → celebrate on Feb 28.
        LocalDate.of(year, 2, 28)
    }
}
