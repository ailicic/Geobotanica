package com.geobotanica.geobotanica.util

import java.io.Serializable

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

data class Measurement(val value: Float, val units: Units = Units.CM) : Serializable {

    override fun toString(): String {
        return if (units == Units.FT)
            "%.0f ft %.1f in".format(getFeetWithoutInches(), getInchesWithoutFeet())
        else
            "%.1f $units".format(value)
    }

    fun convertTo(toUnits: Units): Measurement {
        val cm = toCm()
        return Measurement( when (toUnits) {
            Units.CM -> cm
            Units.M -> cm * 0.01F
            Units.IN -> cm / 2.54F
            Units.FT -> cm / 2.54F / 12
        }, toUnits)
    }

    fun toCm(): Float {
        return when (units) {
            Units.CM -> value
            Units.M -> value * 100
            Units.IN -> value * 2.54F
            Units.FT -> value * 12 * 2.54F
        }
    }

    fun toFtIn(): Pair<Float, Float> {
        if (units != Units.FT)
            throw ArithmeticException()
        return Pair( getFeetWithoutInches(), getInchesWithoutFeet() )
    }

    private fun getFeetWithoutInches() = value - (getInchesWithoutFeet() / 12)

    private fun getInchesWithoutFeet() = value * 12 % 12
}