package com.geobotanica.geobotanica

import com.geobotanica.geobotanica.util.Measurement
import com.geobotanica.geobotanica.util.Units
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object MeasurementTest : Spek ({

    describe("Unit conversions") {
        listOf(
                Measurement(1F, Units.CM) to Measurement(1F, Units.CM),
                Measurement(100F, Units.CM) to Measurement(1F, Units.M),
                Measurement(25.4F, Units.CM) to Measurement(10F, Units.IN),
                Measurement(25.4F * 12, Units.CM) to Measurement(10F, Units.FT),
                Measurement(1F, Units.M) to Measurement(100F, Units.CM),
                Measurement(10F, Units.IN) to Measurement(25.4F, Units.CM),
                Measurement(1F, Units.FT) to Measurement(12 * 2.54F, Units.CM),
                Measurement(25.4F, Units.M) to Measurement(1000F, Units.IN),
                Measurement(10F, Units.IN) to Measurement(25.4F, Units.CM)
        ).forEach { (fromMeasurement, toMeasurement) ->

            context("$fromMeasurement to ${toMeasurement.units}") {
                val result = fromMeasurement.convertTo(toMeasurement.units)

                it("Should be $toMeasurement") {
                    result shouldEqual toMeasurement
                }
            }
        }
    }

    describe("Convert to string") {
        listOf(
                Measurement(1.2F, Units.CM) to "1.2 cm",
                Measurement(10.5F, Units.M) to "10.5 m",
                Measurement(5.875F, Units.IN) to "5.9 in",
                Measurement(2.25F, Units.FT) to "2 ft 3.0 in",
                Measurement(2.75F, Units.FT) to "2 ft 9.0 in"
        ).forEach { (measurement, string) ->

            context("Print ${measurement.value} ${measurement.units}") {
                val result = "$measurement"

                it("Should be $string") {
                    result shouldEqual string
                }
            }
        }
    }
})