package com.geobotanica.geobotanica.viewmodel

import com.geobotanica.geobotanica.util.GbDispatchers
import com.geobotanica.geobotanica.util.SpekExt.setupTestDispatchers
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

@ExperimentalCoroutinesApi
object CoroutineDelayTest : Spek({
    val testDispatchers = setupTestDispatchers()

    val dependency = mockk<SomeDependency>(relaxed = true)
    val foo by memoized { SomeClass(testDispatchers, dependency) }

    beforeEachTest {
        clearMocks(dependency, answers = false)
    }

    describe("SomeClass") {

        context("suspendFun without delay") {
            beforeEachTest {
                testDispatchers.main.runBlockingTest { foo.suspendFun(false) }
            }

            it("Should call") {
                verify { foo.dependency.call() }
            }
        }

        context("suspendFun with delay") {
            beforeEachTest {
                testDispatchers.main.runBlockingTest { foo.suspendFun(true) }
            }

            it("Should call") {
                verify { foo.dependency.call() }
            }
        }

        context("launchFun without delay") {
            beforeEachTest {
                testDispatchers.main.runBlockingTest { foo.launchFun(false) }
            }

            it("Should call") {
                verify { foo.dependency.call() }
            }
        }

        context("launchFun with delay") {

            context("Test with advanceTimeBy()") {
                beforeEachTest {
                    println("beforeEachTest thread = ${Thread.currentThread().name}")

                    testDispatchers.main.runBlockingTest {
                        println("runBlockingTest thread = ${Thread.currentThread().name}")
                        foo.launchFun(true)
                        advanceTimeBy(200)
                    }
                }

                it("Should call") {
                    println("it thread = ${Thread.currentThread().name}")
                    verify { foo.dependency.call() }
                }
            }

            context("Test with Thread.sleep()") {
                beforeEachTest {
                    testDispatchers.main.runBlockingTest {
                        foo.launchFun(true)
                        Thread.sleep(200)
                    }
                }

                it("Should call") {
                    verify { foo.dependency.call() }
                }
            }
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