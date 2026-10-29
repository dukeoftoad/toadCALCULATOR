// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.math.tools

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UnitConverterTest {

    private fun conv(cat: ConverterCategory, from: String, to: String, v: Double) =
        convert(cat, from, to, v)

    private fun assertNear(actual: Double, expected: Double, eps: Double = 1e-9) {
        assertThat(actual).isWithin(eps).of(expected)
    }

    // ---- length ----

    @Test
    fun `mile to meters uses the exact definition`() = assertNear(conv(ConverterCategory.LENGTH, "mi", "m", 1.0), 1609.344)

    @Test
    fun `inch to cm`() = assertNear(conv(ConverterCategory.LENGTH, "in", "cm", 1.0), 2.54)

    @Test
    fun `kilometer to miles`() = assertNear(conv(ConverterCategory.LENGTH, "km", "mi", 1.0), 0.621371192237334)

    @Test
    fun `identity conversion is exact`() = assertNear(conv(ConverterCategory.LENGTH, "ft", "ft", 123.456), 123.456)

    // ---- mass ----

    @Test
    fun `pound to kg exact`() = assertNear(conv(ConverterCategory.MASS, "lb", "kg", 1.0), 0.45359237)

    @Test
    fun `stone to pounds`() = assertNear(conv(ConverterCategory.MASS, "st", "lb", 1.0), 14.0)

    // ---- volume ----

    @Test
    fun `us gallon to liters exact`() = assertNear(conv(ConverterCategory.VOLUME, "gal", "l", 1.0), 3.785411784)

    @Test
    fun `cubic meter to liters`() = assertNear(conv(ConverterCategory.VOLUME, "m3", "l", 1.0), 1000.0)

    // ---- area ----

    @Test
    fun `hectare to square meters`() = assertNear(conv(ConverterCategory.AREA, "ha", "m2", 1.0), 10000.0)

    @Test
    fun `acre to square feet`() = assertNear(conv(ConverterCategory.AREA, "acre", "ft2", 1.0), 43560.0, 1e-6)

    // ---- speed ----

    @Test
    fun `meters per second to km per hour`() = assertNear(conv(ConverterCategory.SPEED, "mps", "kmh", 1.0), 3.6)

    @Test
    fun `knot to mph`() = assertNear(conv(ConverterCategory.SPEED, "kn", "mph", 1.0), 1.1507794480235388)

    // ---- time ----

    @Test
    fun `day to seconds`() = assertNear(conv(ConverterCategory.TIME, "d", "s", 1.0), 86400.0)

    @Test
    fun `week to days`() = assertNear(conv(ConverterCategory.TIME, "wk", "d", 1.0), 7.0)

    // ---- data ----

    @Test
    fun `gibibyte to mebibyte is binary`() = assertNear(conv(ConverterCategory.DATA, "gib", "mib", 1.0), 1024.0)

    @Test
    fun `gigabyte to megabyte is decimal`() = assertNear(conv(ConverterCategory.DATA, "gb", "mb", 1.0), 1000.0)

    @Test
    fun `byte to bits`() = assertNear(conv(ConverterCategory.DATA, "b", "bit", 1.0), 8.0)

    // ---- temperature: offset math ----

    @Test
    fun `fahrenheit freezing point is zero celsius`() =
        assertNear(conv(ConverterCategory.TEMPERATURE, "f", "c", 32.0), 0.0)

    @Test
    fun `fahrenheit boiling point is one hundred celsius`() =
        assertNear(conv(ConverterCategory.TEMPERATURE, "f", "c", 212.0), 100.0)

    @Test
    fun `minus forty is the crossover point`() {
        assertNear(conv(ConverterCategory.TEMPERATURE, "f", "c", -40.0), -40.0)
        assertNear(conv(ConverterCategory.TEMPERATURE, "c", "f", -40.0), -40.0)
    }

    @Test
    fun `zero kelvin is absolute zero celsius`() =
        assertNear(conv(ConverterCategory.TEMPERATURE, "k", "c", 0.0), -273.15)

    @Test
    fun `water freezes at 273 point 15 kelvin`() =
        assertNear(conv(ConverterCategory.TEMPERATURE, "k", "c", 273.15), 0.0)

    @Test
    fun `celsius to kelvin round trip`() {
        val k = conv(ConverterCategory.TEMPERATURE, "c", "k", 37.0)
        assertNear(k, 310.15)
        assertNear(conv(ConverterCategory.TEMPERATURE, "k", "c", k), 37.0)
    }

    @Test
    fun `fahrenheit to kelvin`() =
        assertNear(conv(ConverterCategory.TEMPERATURE, "f", "k", 32.0), 273.15)

    // ---- structural ----

    @Test
    fun `round trip through base preserves value`() {
        for (cat in ConverterCategory.entries) {
            for (unit in Units.CATALOGUE.getValue(cat)) {
                val there = convert(cat, unit.id, Units.CATALOGUE.getValue(cat).first().id, 5.0)
                val back = convert(cat, Units.CATALOGUE.getValue(cat).first().id, unit.id, there)
                assertThat(nearlyEqual(back, 5.0, epsilon = 1e-6)).isTrue()
            }
        }
    }

    @Test
    fun `every category has at least two units`() {
        for (cat in ConverterCategory.entries) {
            assertThat(Units.CATALOGUE.getValue(cat).size).isAtLeast(2)
        }
    }

    @Test
    fun `unknown unit throws`() {
        try {
            convert(ConverterCategory.LENGTH, "bogus", "m", 1.0)
            throw AssertionError("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // ok
        }
    }
}
