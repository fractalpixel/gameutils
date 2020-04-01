package org.fractalpixel.gameutils.utils

import com.badlogic.gdx.math.Vector3
import org.kwrench.geometry.double3.Double3
import org.kwrench.geometry.int3.Int3
import org.kwrench.geometry.int3.MutableInt3
import java.lang.IllegalArgumentException
import kotlin.math.sqrt


fun Double3.distanceTo(x: Double, y: Double, z: Double): Double {
    val dx = x - this.x
    val dy = y - this.y
    val dz = z - this.z
    return sqrt(dx*dx + dy*dy + dz*dz)
}


/**
 * Return x if [coordinateIndex] is 0, y if 1 and z if 2.  Throws exception otherwise.
 */
fun Int3.getCoordinate(coordinateIndex: Int): Int {
    return when (coordinateIndex) {
        0 -> x
        1 -> y
        2 -> z
        else -> throw IllegalArgumentException("Coordinate index $coordinateIndex is out of range, expected 0 (x), 1 (y) or 2 (z)")
    }
}

/**
 * Add the specified values for each coordinate axis.
 */
fun MutableInt3.add(x: Int, y: Int, z: Int): MutableInt3 {
    this.x += x
    this.y += y
    this.z += z
    return this
}

/**
 * Add the specified value to each coordinate axis.
 */
fun MutableInt3.add(value: Int): MutableInt3 {
    this.x += value
    this.y += value
    this.z += value
    return this
}


/**
 * Subtract the specified values from each coordinate axis.
 */
fun MutableInt3.sub(x: Int, y: Int, z: Int): MutableInt3 {
    this.x -= x
    this.y -= y
    this.z -= z
    return this
}

/**
 * Subtract the specified value from each coordinate axis.
 */
fun MutableInt3.sub(value: Int): MutableInt3 {
    this.x -= value
    this.y -= value
    this.z -= value
    return this
}

/**
 * Set x if [coordinateIndex] is 0, y if 1 and z if 2.  Throws exception otherwise.
 */
fun MutableInt3.setCoordinate(coordinateIndex: Int, value: Int) {
    when (coordinateIndex) {
        0 -> x = value
        1 -> y = value
        2 -> z = value
        else -> throw IllegalArgumentException("Coordinate index $coordinateIndex is out of range, expected 0 (x), 1 (y) or 2 (z)")
    }
}

/**
 * Set x if [coordinateIndex] is 0, y if 1 and z if 2.  Throws exception otherwise.
 */
fun Vector3.setCoordinate(coordinateIndex: Int, value: Float) {
    when (coordinateIndex) {
        0 -> x = value
        1 -> y = value
        2 -> z = value
        else -> throw IllegalArgumentException("Coordinate index $coordinateIndex is out of range, expected 0 (x), 1 (y) or 2 (z)")
    }
}

/**
 * Return x if [coordinateIndex] is 0, y if 1 and z if 2.  Throws exception otherwise.
 */
fun Vector3.getCoordinate(coordinateIndex: Int): Float {
    return when (coordinateIndex) {
        0 -> x
        1 -> y
        2 -> z
        else -> throw IllegalArgumentException("Coordinate index $coordinateIndex is out of range, expected 0 (x), 1 (y) or 2 (z)")
    }
}

/**
 * Iterate from zero until (but not including) the values of this Int3 along each axis, with z in the outermost loop and x in the innermost loop.
 * Calls the code with a Int3 instance having the current value for each step.  The Int3 instance is reused, so if
 * a specific value will be stored somewhere it should be copied before storing.
 *
 * Note that if any coordinate in this Int3 is zero or negative, the loop is not run and the code is not called.
 *
 * [offset] is optionally applied to the value before passing to the code.
 * [iteratingInt3] is the Int3 instance that is reused for each step, if not specified a new temporary one will be created before the loop.
 */
inline fun Int3.iterate(offset: Int3 = Int3.ZEROES, iteratingInt3: MutableInt3 = MutableInt3(), code: (value: Int3) -> Unit) {
    for (z in offset.z until offset.z + this.z) {
        for (y in offset.y until offset.y + this.y) {
            for (x in offset.x until offset.x + this.x) {
                iteratingInt3.x = x
                iteratingInt3.y = y
                iteratingInt3.z = z

                code(iteratingInt3)
            }
        }
    }
}


