package com.geobotanica.geobotanica.util

import org.threeten.bp.Instant
import org.threeten.bp.format.DateTimeFormatter


object GbTime {
    private var frozenTime: Instant = Instant.EPOCH
    private val mockTimeProvider: () -> Instant = { frozenTime }

    private val defaultTimeProvider: () -> Instant = { Instant.now() }
    private var timeProvider: () -> Instant = defaultTimeProvider

    fun now() = timeProvider.invoke()

    fun freeze() {
        frozenTime = Instant.now()
        timeProvider = mockTimeProvider
    }

    fun unfreeze() {
        timeProvider = defaultTimeProvider
    }
}

fun Instant.toDateString(): String = toString().substringBefore('T') // TODO: Account for timezone
fun Instant.asFilename(): String = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS").format(this)
