package com.geobotanica.geobotanica.util

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import kotlinx.coroutines.*
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.spekframework.spek2.dsl.LifecycleAware
import org.spekframework.spek2.dsl.Root
import org.spekframework.spek2.style.specification.Suite
import java.lang.Runnable
import java.util.concurrent.Executors

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


    @Suppress("EXPERIMENTAL_API_USAGE")
    fun Suite.allowCoroutines() {
//        val mainThreadSurrogate = newSingleThreadContext("UI thread") // DON'T USE THIS -> OBSOLETE
        val mainThreadSurrogate =  Executors.newSingleThreadExecutor().asCoroutineDispatcher() // Alternate
        Dispatchers.setMain(mainThreadSurrogate)

        afterGroup {
            Dispatchers.resetMain()
            mainThreadSurrogate.close()
        }
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    fun LifecycleAware.beforeEachBlockingTest(block: suspend TestCoroutineScope.() -> Unit) {
        beforeEachTest {
            runBlockingTest {
                block()
            }
        }
    }
}