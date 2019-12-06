package com.geobotanica.geobotanica.util

import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.Job

fun Job.invokeOnSuccess(handler: CompletionHandler) {
    invokeOnCompletion { throwable ->
        if (throwable != null)
            return@invokeOnCompletion
        handler.invoke(throwable)
    }
}