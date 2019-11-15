package com.geobotanica.geobotanica.util

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.spekframework.spek2.dsl.LifecycleAware
import org.spekframework.spek2.dsl.Root
import org.spekframework.spek2.style.specification.Suite

object SpekExt {

    /**
     * This avoids the "getMainLooper in android.os.Looper not mocked" error when using LiveData in Spek.
     */
    fun Root.allowLiveData() {
        setLiveDataDelegate()
        afterGroup { unsetLiveDataDelegate() }
    }

    private fun setLiveDataDelegate() {
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) = runnable.run()
            override fun isMainThread(): Boolean = true
            override fun postToMainThread(runnable: Runnable) = runnable.run()
        })
    }

    private fun unsetLiveDataDelegate() = ArchTaskExecutor.getInstance().setDelegate(null)


    @ExperimentalCoroutinesApi
    @ObsoleteCoroutinesApi
    fun Suite.allowCoroutines() {
        val mainThreadSurrogate = newSingleThreadContext("UI thread")
        Dispatchers.setMain(newSingleThreadContext("UI"))

        afterGroup {
            Dispatchers.resetMain()
            mainThreadSurrogate.close()
        }
    }

    @ExperimentalCoroutinesApi
    fun LifecycleAware.beforeEachBlockingTest(block: suspend TestCoroutineScope.() -> Unit) {
        beforeEachTest {
            runBlockingTest {
                block()
            }
        }
    }
}