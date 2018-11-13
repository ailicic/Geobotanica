package com.geobotanica.geobotanica.util

enum class Units {
    CM, M, IN, FT;

    override fun toString(): String {
        return when (this) {
            CM -> "cm"
            M -> "m"
            IN -> "in"
            FT -> "ft"
        }
    }
}

data class Measurement(val value: Float, val units: Units = Units.CM) {

    constructor(value: Float, units: Int) : this(value, Units.values()[units])

    override fun toString() = when (units) {
        Units.FT -> {
            val inches = value * 12 % 12
            val feet = value - (inches / 12)
            "%.0f ft %.1f in".format(feet, inches)
        }
        else -> "%.1f $units".format(value)
    }

    fun convert(toUnits: Units): Measurement {
        val cm = toCm()
        return Measurement( when (toUnits) {
            Units.CM -> cm
            Units.M -> cm * 0.01F
            Units.IN -> cm / 2.54F
            Units.FT -> cm / 2.54F / 12
        }, toUnits)
    }

    private fun toCm(): Float {
        return when (units) {
            Units.CM -> value
            Units.M -> value * 100
            Units.IN -> value * 2.54F
            Units.FT -> value * 12 * 2.54F
        }
    }
}