package com.geobotanica.geobotanica.util

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.spekframework.spek2.dsl.LifecycleAware
import org.spekframework.spek2.dsl.Root

object SpekExt {

    /**
     * This avoids the "getMainLooper in android.os.Looper not mocked" error when using LiveData in Spek.
     */
    fun Root.allowLiveData() {
        setExecutorDelegate()
//        afterGroup { unsetExecutorDelegate() }
    }

    private fun setExecutorDelegate() {
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) = runnable.run()
            override fun isMainThread(): Boolean = true
            override fun postToMainThread(runnable: Runnable) = runnable.run()
        })
    }

    private fun unsetExecutorDelegate() = ArchTaskExecutor.getInstance().setDelegate(null)

    @Suppress("EXPERIMENTAL_API_USAGE")
    fun Root.setupTestDispatchers(): TestDispatchers {
        val testDispatchers = TestDispatchers() // IMPORTANT: Pass this into beforeEachBlockingTest() if test launches a coroutine with delay() (to auto-advance)

        beforeGroup {
            Dispatchers.setMain(testDispatchers.main)
        }

        afterGroup {
            Dispatchers.resetMain()
            testDispatchers.main.cleanupTestCoroutines()
        }
        return testDispatchers
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    fun LifecycleAware.beforeEachBlockingTest(
            testDispatchers: TestDispatchers = TestDispatchers(),
            block: suspend TestCoroutineScope.() -> Unit
    ) {
        beforeEachTest {
            testDispatchers.main.runBlockingTest {
                block()
            }
        }
    }

    fun LifecycleAware.mockTime() {
        beforeGroup { GbTime.freeze() }
        afterGroup { GbTime.unfreeze() }
    }

    class TestDispatchers : GbDispatchers {
        @ExperimentalCoroutinesApi private val testCoroutineDispatcher = TestCoroutineDispatcher()

        @ExperimentalCoroutinesApi override val main = testCoroutineDispatcher
        @ExperimentalCoroutinesApi override val default = testCoroutineDispatcher
        @ExperimentalCoroutinesApi override val io = testCoroutineDispatcher
        @ExperimentalCoroutinesApi override val unconfined = testCoroutineDispatcher
    }
}