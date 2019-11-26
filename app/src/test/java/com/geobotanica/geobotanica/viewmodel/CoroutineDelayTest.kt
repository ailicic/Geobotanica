package com.geobotanica.geobotanica.viewmodel

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runBlockingTest
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import org.spekframework.spek2.style.specification.xdescribe

@ExperimentalCoroutinesApi
object CoroutineDelayTest : Spek({

    val dependency = mockk<SomeDependency>(relaxed = true)
    val foo by memoized { SomeClass(dependency) }

    beforeEachTest {
        clearMocks(dependency, answers = false)
    }

    xdescribe("SomeClass") {
        context("suspendFun without delay") {
            beforeEachTest {
                runBlockingTest { foo.suspendFun(false) }
            }

            it("Should call") {
                verify { foo.dependency.call() }
            }
        }

        context("suspendFun with delay") {
            beforeEachTest {
                runBlockingTest { foo.suspendFun(true) }
            }

            it("Should call") {
                verify { foo.dependency.call() }
            }
        }

        context("launchFun without delay") {
            beforeEachTest {
                runBlockingTest { foo.launchFun(false) }
            }

            it("Should call") {
                verify { foo.dependency.call() }
            }
        }

        context("launchFun with delay") {

            context("Test with advanceTimeBy()") {
                beforeEachTest {
                    println("beforeEachTest thread = ${Thread.currentThread().name}")

                    runBlockingTest {
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
                    foo.launchFun(true)
                    Thread.sleep(200)
                }

                it("Should call") {
                    verify { foo.dependency.call() }
                }
            }
        }
    }
})

class SomeClass(val dependency: SomeDependency) {

    suspend fun suspendFun(withDelay: Boolean) {
        if (withDelay)
            delay(100)
        dependency.call()
    }

    fun launchFun(withDelay: Boolean) = GlobalScope.launch {
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