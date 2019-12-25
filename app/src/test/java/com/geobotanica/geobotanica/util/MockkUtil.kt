package com.geobotanica.geobotanica.util

import androidx.lifecycle.Observer
import io.mockk.*
import org.spekframework.spek2.dsl.Root

object MockkUtil {

    private var isRoomMocked = false

    @Suppress("unused")
    fun Root.mockkRoomStatic() {
        if (! isRoomMocked) {
            mockkStatic("androidx.room.RoomDatabaseKt") // db.withTransaction{} test hangs indefinitely if omitted!
            isRoomMocked = true
        }
    }

    @Suppress("unused")
    inline fun <reified T: Any> Root.mockkObserver(): Observer<T> {
        return mockk {
            every { onChanged(any()) } returns Unit
        }
    }

    @Suppress("unused")
    inline fun <reified T: Any> Root.mockkObserverNullable(): Observer<T?> {
        return mockk {
            every { onChanged(any()) } returns Unit
        }
    }

    inline fun <reified T: Any> Root.mockkBeforeGroup(crossinline block: T.() -> Unit): T {
        return mockk {
            beforeGroup {
                block()
            }
        }
    }

    fun verifyZero(block: MockKVerificationScope.() -> Unit) = verify(exactly = 0, verifyBlock = block)
    fun verifyOne(block: MockKVerificationScope.() -> Unit) = verify(exactly = 1, verifyBlock = block)
    fun coVerifyZero(block: suspend MockKVerificationScope.() -> Unit) = coVerify(exactly = 0, verifyBlock = block)
    fun coVerifyOne(block: suspend MockKVerificationScope.() -> Unit) = coVerify(exactly = 1, verifyBlock = block)
}