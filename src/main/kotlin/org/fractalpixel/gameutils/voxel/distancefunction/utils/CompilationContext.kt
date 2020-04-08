package org.fractalpixel.gameutils.voxel.distancefunction.utils

import org.codehaus.janino.ScriptEvaluator
import org.fractalpixel.gameutils.voxel.distancefunction.CompilingDistanceFun
import org.fractalpixel.gameutils.voxel.distancefunction.DistanceFun
import java.lang.reflect.Method

/**
 * Helps coordinate unique variables and such when compiling code for a [CompilingDistanceFunction]
 */
class CompilationContext() {

    private var nextFreeId: Int = 1

    fun peekNextId(): Int = nextFreeId
    fun peekNextPrefix(): String = "t" + peekNextId() + "_"

    fun getNextId(): Int = nextFreeId++
    fun getNextPrefix(): String = "t" + getNextId() + "_"

    var code: String = ""

    /**
     * Returns the prefix used.
     */
    fun createMainFunction(mainFunc: CompilingDistanceFun): String {
        val prefix = getNextPrefix()
        val templateCode = mainFunc.constructCode(this)
        code = introduceUniqueIds(prefix, templateCode)
        code += ";\nreturn ${prefix}out;"
        return prefix
    }

    /**
     * Creates code that calls the specified distance function.
     * The result will be saved in a variable with the name # followed by <resultVariableName>,
     * where # will be automatically expanded to an unique id for your script.
     */
    fun createCall(resultVariableName: String, distanceFun: CompilingDistanceFun, x: String = "#x", y: String = "#y", z: String = "#z", sampleSize: String = "#sampleSize"): String {
        TODO("Implement")

    }

    fun compileFunction(mainFunc: CompilingDistanceFun): Method {
        // Generate code
        val prefix = createMainFunction(mainFunc)

        // Compile it
        val se = ScriptEvaluator()
        se.setParameters(
            arrayOf(prefix + "x", prefix + "y", prefix + "z", prefix + "sampleSize"),
            arrayOf(java.lang.Double.TYPE, java.lang.Double.TYPE, java.lang.Double.TYPE, java.lang.Double.TYPE))
        se.setReturnType(java.lang.Double.TYPE)
        se.cook(code)
        return se.method
    }

    fun introduceUniqueIds(prefix: String, code: String): String {
        return code.replace("#", prefix)
    }

}