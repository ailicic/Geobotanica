package com.geobotanica.geobotanica.util

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import org.spekframework.spek2.dsl.Root

object SpekLiveData {

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
}