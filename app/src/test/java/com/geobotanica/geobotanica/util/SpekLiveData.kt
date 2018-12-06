package com.geobotanica.geobotanica.util

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import org.spekframework.spek2.dsl.GroupBody
import org.spekframework.spek2.style.specification.Suite
import org.spekframework.spek2.style.specification.describe

object SpekLiveData {
    /**
     * A wrapper method for Spek's describe() which manages the ArchTaskExecutor delegation when
     * testing with LiveData. This avoids the "getMainLooper in android.os.Looper not mocked" error.
     */
    fun GroupBody.describeWithLiveData(text: String, block: Suite.() -> Unit) {
        describe(text) {
            setLiveDataDelegate()
            block()
            unsetLiveDataDelegate()
        }
    }
    fun Suite.describeWithLiveData(text: String, block: Suite.() -> Unit) {
        describe(text) {
            setLiveDataDelegate()
            block()
            unsetLiveDataDelegate()
        }
    }

    private fun setLiveDataDelegate() {
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) = runnable.run()
            override fun isMainThread(): Boolean = true
            override fun postToMainThread(runnable: Runnable) = runnable.run()
        })
    }

    private fun unsetLiveDataDelegate() = ArchTaskExecutor.getInstance().setDelegate(null)
}