package org.fractalpixel.gameutils.voxel.distancefunction

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.fractalpixel.gameutils.voxel.distancefunction.utils.CompilationContext
import java.lang.reflect.Method

/**
 * Handles compiling functions as needed
 */
abstract class CompilingDistanceFun: DistanceFun {

    private var compiledDistanceFun: Method? = null

    private val compileMutex = Mutex()

    override fun get(x: Double, y: Double, z: Double, sampleSize: Double): Double {
        // Get evaluator method, or compile it if it is null
        var distanceFun = compiledDistanceFun
        if (distanceFun == null) {
            distanceFun = runBlocking {
                compileMutex.withLock {
                    // If another coroutine already compiled the evaluator we can use it
                    val anotherTry = compiledDistanceFun
                    if (anotherTry != null) {
                        // Someone else just compiled it, return it
                        anotherTry
                    } else {
                        // No one else compiled it yet, so we do it
                        val compiledFun = compileFunction()

                        // Cache it with this DistanceFun, so that we don't have to compile it every time!
                        compiledDistanceFun = compiledFun

                        // Return it
                        compiledFun
                    }
                }
            }
        }

        // Invoke evaluator method
        return distanceFun.invoke(null, x, y, z, sampleSize) as Double
    }

    fun compileFunction(): Method {
        // Build code and compile it
        val context = CompilationContext()
        return context.compileFunction(this)
    }

    /**
     * Return code template for this function.
     *
     * Parameters #x, #y, #z, #sampleSize will be replaced with the parameter variable names.
     * #out will be replaced with the output variable name.
     * # will be replaced with a unique id for this code, it can be attached to variable names to make them unique.
     *
     * The [context] can be used to create code to invoke another [DistanceFun].
     */
    abstract fun constructCode(context: CompilationContext): String

    /**
     * Clears previously compiled scripts.
     */
    // TODO: Call this if the function is changed anywhere..
    fun functionChanged() {
        compiledDistanceFun = null
    }

}