// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.math.tools

import kotlin.math.abs

/** Categories offered by the offline unit converter. */
enum class ConverterCategory { LENGTH, AREA, VOLUME, MASS, TEMPERATURE, SPEED, TIME, DATA }

/**
 * A linear unit: value_in_base = (value − offset) × factor.
 * Temperature units carry the offset (base is Celsius); everything else has offset 0.
 */
data class UnitDef(
    val id: String,
    val symbol: String,
    val factor: Double,
    val offset: Double = 0.0,
)

/**
 * Unit tables as Kotlin constants (no JSON assets). Factors use exact
 * international definitions where they exist (inch, mile, pound, US gallon…).
 */
object Units {
    val LENGTH = listOf(
        UnitDef("nm", "nm", 1e-9),
        UnitDef("um", "µm", 1e-6),
        UnitDef("mm", "mm", 1e-3),
        UnitDef("cm", "cm", 1e-2),
        UnitDef("m", "m", 1.0),
        UnitDef("km", "km", 1e3),
        UnitDef("in", "in", 0.0254),
        UnitDef("ft", "ft", 0.3048),
        UnitDef("yd", "yd", 0.9144),
        UnitDef("mi", "mi", 1609.344),
        UnitDef("nmi", "nmi", 1852.0),
    )

    val AREA = listOf(
        UnitDef("mm2", "mm²", 1e-6),
        UnitDef("cm2", "cm²", 1e-4),
        UnitDef("m2", "m²", 1.0),
        UnitDef("ha", "ha", 1e4),
        UnitDef("km2", "km²", 1e6),
        UnitDef("in2", "in²", 0.00064516),
        UnitDef("ft2", "ft²", 0.09290304),
        UnitDef("acre", "ac", 4046.8564224),
        UnitDef("mi2", "mi²", 2589988.110336),
    )

    val VOLUME = listOf(
        UnitDef("ml", "mL", 0.001),
        UnitDef("l", "L", 1.0),
        UnitDef("m3", "m³", 1000.0),
        UnitDef("tsp", "tsp", 0.00492892159375),
        UnitDef("tbsp", "tbsp", 0.01478676478125),
        UnitDef("floz", "fl oz", 0.0295735295625),
        UnitDef("cup", "cup", 0.2365882365),
        UnitDef("pt", "pt", 0.473176473),
        UnitDef("qt", "qt", 0.946352946),
        UnitDef("gal", "gal", 3.785411784),
    )

    val MASS = listOf(
        UnitDef("mg", "mg", 1e-6),
        UnitDef("g", "g", 1e-3),
        UnitDef("kg", "kg", 1.0),
        UnitDef("t", "t", 1000.0),
        UnitDef("oz", "oz", 0.028349523125),
        UnitDef("lb", "lb", 0.45359237),
        UnitDef("st", "st", 6.35029318),
    )

    val TEMPERATURE = listOf(
        UnitDef("c", "°C", 1.0, 0.0),
        UnitDef("f", "°F", 5.0 / 9.0, 32.0),
        UnitDef("k", "K", 1.0, 273.15),
    )

    val SPEED = listOf(
        UnitDef("mps", "m/s", 1.0),
        UnitDef("kmh", "km/h", 1.0 / 3.6),
        UnitDef("mph", "mph", 0.44704),
        UnitDef("kn", "kn", 1852.0 / 3600.0),
        UnitDef("fps", "ft/s", 0.3048),
    )

    val TIME = listOf(
        UnitDef("ms", "ms", 1e-3),
        UnitDef("s", "s", 1.0),
        UnitDef("min", "min", 60.0),
        UnitDef("h", "h", 3600.0),
        UnitDef("d", "d", 86400.0),
        UnitDef("wk", "wk", 604800.0),
        UnitDef("mo", "mo", 2629746.0), // average Gregorian month
        UnitDef("yr", "yr", 31556952.0), // average Gregorian year (365.2425 d)
    )

    val DATA = listOf(
        UnitDef("bit", "bit", 0.125),
        UnitDef("b", "B", 1.0),
        UnitDef("kb", "KB", 1e3),
        UnitDef("mb", "MB", 1e6),
        UnitDef("gb", "GB", 1e9),
        UnitDef("tb", "TB", 1e12),
        UnitDef("kib", "KiB", 1024.0),
        UnitDef("mib", "MiB", 1024.0 * 1024),
        UnitDef("gib", "GiB", 1024.0 * 1024 * 1024),
        UnitDef("tib", "TiB", 1024.0 * 1024 * 1024 * 1024),
    )

    val CATALOGUE: Map<ConverterCategory, List<UnitDef>> = mapOf(
        ConverterCategory.LENGTH to LENGTH,
        ConverterCategory.AREA to AREA,
        ConverterCategory.VOLUME to VOLUME,
        ConverterCategory.MASS to MASS,
        ConverterCategory.TEMPERATURE to TEMPERATURE,
        ConverterCategory.SPEED to SPEED,
        ConverterCategory.TIME to TIME,
        ConverterCategory.DATA to DATA,
    )
}

/**
 * Converts [value] from one unit to another within a category.
 * Pure function; temperature offset math is exercised by dedicated tests.
 */
fun convert(category: ConverterCategory, fromId: String, toId: String, value: Double): Double {
    val units = Units.CATALOGUE.getValue(category)
    val from = units.firstOrNull { it.id == fromId } ?: throw IllegalArgumentException("unknown unit: $fromId")
    val to = units.firstOrNull { it.id == toId } ?: throw IllegalArgumentException("unknown unit: $toId")
    val base = (value - from.offset) * from.factor
    return base / to.factor + to.offset
}

/** Tolerance helper for tests and UI rounding. */
fun nearlyEqual(a: Double, b: Double, epsilon: Double = 1e-9): Boolean = abs(a - b) <= epsilon
