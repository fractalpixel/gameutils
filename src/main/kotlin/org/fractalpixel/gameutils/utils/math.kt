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

