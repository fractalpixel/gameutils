package org.fractalpixel.gameutils.voxel.renderer2.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.*
import kotlin.coroutines.CoroutineContext

object VoxelCoroutineScope: CoroutineScope {

    /**
     * Use one calculator thread less than the number of available cores, and hope that the JVM allocates new
     * threads to other cores than the main program / OpenGL thread first...
     */
    private val calculatorThreadCount = Runtime.getRuntime().availableProcessors() - 1

    private val calculatorExecutor: Executor =  ThreadPoolExecutor(calculatorThreadCount, calculatorThreadCount, 1, TimeUnit.SECONDS, LinkedTransferQueue(),
        ThreadFactory {
            val thread = Thread(it)

            // Give the calculator threads low priority, so that they don't slow down the main OpenGL and UI thread too much.
            thread.priority = Thread.MIN_PRIORITY

            thread.isDaemon = true
            thread
        })


    private val calculatorDispatcher = calculatorExecutor.asCoroutineDispatcher()

    override val coroutineContext: CoroutineContext get() = calculatorDispatcher
}