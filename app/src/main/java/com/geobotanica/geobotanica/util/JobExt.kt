@file:Suppress("unused")

package com.geobotanica.geobotanica.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.Job

fun Job.invokeOnSuccess(handler: CompletionHandler) {
    invokeOnCompletion { throwable ->
        if (throwable != null)
            return@invokeOnCompletion
        handler.invoke(throwable) //
    }
}

fun Job.invokeOnCancellation(handler: CompletionHandler) {
    invokeOnCompletion { throwable ->
        if (throwable is CancellationException)
            handler.invoke(throwable)
    }
}

fun Job.invokeOnCancelOrError(handler: CompletionHandler) {
    invokeOnCompletion { throwable ->
        if (throwable != null)
            handler.invoke(throwable)
    }
}