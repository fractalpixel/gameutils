package org.fractalpixel.gameutils.libgdxutils

import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3

/**
 * Utility class that can contain several transformation matrixes organized in a stack.
 * Supports pushing and popping matrixes, and getting the combined matrix.
 */
class MatrixStack {

    private val stack = com.badlogic.gdx.utils.Array<Matrix4>()

    /**
     * Push a new transformation on the stack.
     */
    fun push(transformation: Matrix4) {
        stack.add(transformation)
        this.transformation.mul(transformation)
    }

    /**
     * Remove the last added transformation from the stack.
     * Returns null if there was no transformation on the stack.
     */
    fun pop(): Matrix4? {
        if (stack.size == 0) return null
        else {
            val popped = stack.removeIndex(stack.size - 1)

            recalculateTransformation()

            return popped
        }
    }

    /**
     * The combined transformation of the stack
     */
    val transformation: Matrix4 = Matrix4()

    /**
     * Transforms the specified vector.
     * Returns the same vector that was provided for chaining.
     */
    fun apply(vector: Vector3): Vector3 {
        vector.mul(transformation)
        return vector
    }

    /**
     * Calculates the transformation from all the matrixes in the stack.
     * Use e.g. if the matrixes are modified.
     */
    fun recalculateTransformation() {
        transformation.idt()
        for (i in 0 until stack.size) {
            transformation.mul(stack[i])
        }
    }

}