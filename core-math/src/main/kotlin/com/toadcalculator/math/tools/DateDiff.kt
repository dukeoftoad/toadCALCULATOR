// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.math.tools

import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

data class DateDiffResult(
    val totalDays: Long,
    val weeks: Long,
    val remainderDays: Long,
    val years: Long,
    val months: Long,
    val days: Long,
)

/**
 * Difference between two dates. With [inclusive] the end date counts as a
 * full day (end + 1 day before differencing). Pure function.
 */
object DateDiff {

    fun difference(start: LocalDate, end: LocalDate, inclusive: Boolean): DateDiffResult {
        val effectiveEnd = if (inclusive) end.plusDays(1) else end
        require(!effectiveEnd.isBefore(start)) { "end must not be before start" }

        val totalDays = ChronoUnit.DAYS.between(start, effectiveEnd)
        val period = Period.between(start, effectiveEnd)
        return DateDiffResult(
            totalDays = totalDays,
            weeks = totalDays / 7,
            remainderDays = totalDays % 7,
            years = period.years.toLong(),
            months = period.months.toLong(),
            days = period.days.toLong(),
        )
    }
}
