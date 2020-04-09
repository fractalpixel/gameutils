package org.fractalpixel.gameutils.voxel.distancefunction.utils

import org.codehaus.janino.ScriptEvaluator
import org.fractalpixel.gameutils.voxel.distancefunction.CompilingDistanceFun
import org.fractalpixel.gameutils.voxel.distancefunction.DistanceFun
import java.lang.reflect.Method
import java.util.*

/**
 * Helps coordinate unique variables and such when compiling code for a [CompilingDistanceFunction]
 */
class CompilationContext() {

    private var nextFreeId: Int = 1

    fun peekNextId(): Int = nextFreeId
    fun peekNextPrefix(): String = "t" + peekNextId() + "_"

    fun nextId(): Int = nextFreeId++
    fun nextPrefix(): String = "t" + nextId() + "_"

  //  private var code: StringBuilder = StringBuilder()
    private val prefixStack: Deque<String> = ArrayDeque()

    val currentPrefix: String get() = prefixStack.peekFirst() ?: outerMostPrefix
    val callerPrefix: String get() = prefixStack.elementAt(1)

    private val extraParameters = ArrayList<Any>()

    fun clear() {
        extraParameters.clear()
        prefixStack.clear()
        nextFreeId = 1
    }

    /**
     * Returns complete name of the parameter (is name with local prefix attached).
     */
    fun <T: Any>parameter(codeOut: StringBuilder, name: String, value: T): String {
        val completeName = currentPrefix + name
        val index = extraParameters.size
        extraParameters.add(value)

        codeOut.appendln(value.javaClass.canonicalName + " " + completeName + " = ("+value.javaClass.canonicalName+") context.getParameter($index);")

        return completeName
    }

    fun getParameter(index: Int): Any = extraParameters[index]

    /*
    fun replaceIds(codeOut: StringBuilder, newCode: String) {
        codeOut.append(introduceUniqueIds(currentPrefix, newCode))
    }
     */

    /**
     * Creates code that calls the specified distance function.
     * The result will be saved in a variable with the name # followed by <resultVariableName>,
     * where # will be automatically expanded to an unique id for your script.
     */
    fun createCall(
        codeOut: StringBuilder,
        distanceFun: DistanceFun,
        resultVariableName: String,
        returnResult: Boolean = false
    ) {
        val outerPrefix = currentPrefix

        // Add function name as comment for easier debugging
        codeOut.appendln("// " + distanceFun.name)

        if (distanceFun is CompilingDistanceFun) {
            // Create new unique prefix for the called function
            val calledPrefix = nextPrefix()
            prefixStack.push(calledPrefix)

            // Generate code for the function
            val funCode = StringBuilder()
            distanceFun.constructCode(funCode,this)
            codeOut.appendln(introduceUniqueIds(funCode.toString(), calledPrefix))

            // Use result variable name demanded by calling code instead of generic #out name
            val currentCode = codeOut.toString()
            val fixedReturnValueCode = currentCode.replace(calledPrefix + "out", outerPrefix + resultVariableName)
            codeOut.clear()
            codeOut.append(fixedReturnValueCode)

            // Return to previous prefix
            prefixStack.pop()
        }
        else {
            // Store distance function in context and have the generated code get it from there
            val paramName = parameter(codeOut, resultVariableName + "_calculator", distanceFun)

            // Create call
            codeOut.appendln("double $outerPrefix$resultVariableName = $paramName.get(x, y, z, sampleSize);")
        }

        // Return result if desired
        if (returnResult) {
            codeOut.appendln("return $outerPrefix$resultVariableName;")
        }
    }

    fun compileFunction(mainFunc: CompilingDistanceFun): Method {
        // Clear any earlier context content
        clear()

        // Generate code
        val code = StringBuilder()
        createCall(code, mainFunc, "result", returnResult = true)

        // Compile it
        val evaluator = ScriptEvaluator()
        val doubleType = java.lang.Double.TYPE
        val p = outerMostPrefix
        evaluator.setParameters(
            arrayOf("x", "y", "z", "sampleSize", "context"),
            arrayOf(doubleType, doubleType, doubleType, doubleType, CompilationContext::class.java)
        )
        evaluator.setReturnType(doubleType)
        val codeToCompile = code.toString()

        evaluator.cook(codeToCompile)
        return evaluator.method
    }

    fun introduceUniqueIds(code: String, prefix: String = currentPrefix): String {
        return code.replace("#", prefix)
    }

    companion object {
        val outerMostPrefix = "_"
    }
}