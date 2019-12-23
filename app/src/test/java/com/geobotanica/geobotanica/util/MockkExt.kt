package com.geobotanica.geobotanica.util

import io.mockk.MockKVerificationScope
import io.mockk.coVerify
import io.mockk.verify

object MockkExt {
    fun verifyOne(block: MockKVerificationScope.() -> Unit) = verify(exactly = 1, verifyBlock = block)
    fun coVerifyOne(block: suspend MockKVerificationScope.() -> Unit) = coVerify(exactly = 1, verifyBlock = block)
}