package com.geobotanica.geobotanica.viewmodel

import com.geobotanica.geobotanica.util.GbDispatchers
import com.geobotanica.geobotanica.util.SpekExt.beforeEachBlockingTest
import com.geobotanica.geobotanica.util.SpekExt.setupTestDispatchers
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

@ExperimentalCoroutinesApi
object CoroutineDelayTest : Spek({
    val testDispatchers = setupTestDispatchers()

    val dependency = mockk<SomeDependency> {
        every { call() } returns Unit
    }
    val foo by memoized { SomeClass(testDispatchers, dependency) }

    beforeEachTest { clearMocks(dependency, answers = false) }

    describe("suspendFun without delay") {
        beforeEachBlockingTest(testDispatchers) {
            foo.suspendFun(false)
        }

        it("Should call") { verify { foo.dependency.call() } }
    }

    describe("suspendFun with delay") {
        beforeEachBlockingTest(testDispatchers) {
            foo.suspendFun(true)
        }

        it("Should call") { verify { foo.dependency.call() } }
    }

    describe("launchFun without delay") {
        beforeEachBlockingTest(testDispatchers) {
            foo.launchFun(false)
        }

        it("Should call") { verify { foo.dependency.call() } }
    }

    describe("launchFun with delay") {
        beforeEachBlockingTest(testDispatchers) {
            foo.launchFun(true)
        }

        it("Should call") {
            println("it thread = ${Thread.currentThread().name}")
            verify { foo.dependency.call() }
        }
    }
})

class SomeClass(private val dispatchers: GbDispatchers, val dependency: SomeDependency) {

    suspend fun suspendFun(withDelay: Boolean) {
        if (withDelay)
            delay(100)
        dependency.call()
    }

    fun launchFun(withDelay: Boolean) = GlobalScope.launch(dispatchers.main) {
        if (withDelay)
            delay(100)
        dependency.call()
    }
}

class SomeDependency {
    fun call() {
        // Do something
    }
}