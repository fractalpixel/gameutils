package org.fractalpixel.gameutils.utils

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlin.coroutines.coroutineContext


/**
 * Returns true if the currently running coroutine has been canceled.
 */
suspend inline fun isCurrentJobCanceled(): Boolean {
    return coroutineContext[Job]?.isActive == false
}

/**
 * Check if the currently running coroutine has been canceled, if so throws a cancellation exception
 * with the optional [cancelMessage] and [cancelAction] to run if canceled before throwing the CancellationException.
 * @throws CancellationException
 */
suspend inline fun checkForJobCancellation(cancelMessage: String = "Job cancelled", cancelAction: () -> Unit = {}) {
    if (isCurrentJobCanceled())  {
        cancelAction()
        throw CancellationException(cancelMessage)
    }
}
