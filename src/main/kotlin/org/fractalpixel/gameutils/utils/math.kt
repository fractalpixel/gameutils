package org.fractalpixel.gameutils.utils

import com.badlogic.gdx.math.Vector3
import org.kwrench.geometry.double3.Double3
import org.kwrench.geometry.double3.MutableDouble3
import org.kwrench.geometry.int2.Int2
import org.kwrench.geometry.int3.ImmutableInt3
import org.kwrench.geometry.int3.Int3
import org.kwrench.geometry.int3.MutableInt3
import org.kwrench.geometry.intvolume.IntVolume
import org.kwrench.geometry.intvolume.MutableIntVolume
import org.kwrench.geometry.volume.MutableVolume
import org.kwrench.geometry.volume.Volume
import org.kwrench.math.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

// TODO: Move all generic extension functions to utility library

fun Double3.distanceTo(x: Double, y: Double, z: Double): Double {
    val dx = x - this.x
    val dy = y - this.y
    val dz = z - this.z
    return sqrt(dx*dx + dy*dy + dz*dz)
}

fun MutableDouble3.normalize(): MutableDouble3 {
    val len = this.length()
    if (len <= 0.0) {
        this.y = 1.0
    }
    else {
        this.divide(len)
    }
    return this
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
 * True if all coordinates are zero.
 */
fun Int3.isAllZeroes(): Boolean = x == 0 && y == 0 && z == 0

/**
 * True if all coordinates are zero.
 */
fun Int2.isAllZeroes(): Boolean = x == 0 && y == 0

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
 * Wrap this value to the range 0,0,0 .. bounds by applying a mod function.
 * If the result would be negative, the bounds are added to it.
 */
fun MutableInt3.modPositive(bounds: Int3): MutableInt3 {
    this.x = this.x.modPositive(bounds.x)
    this.y = this.y.modPositive(bounds.y)
    this.z = this.z.modPositive(bounds.z)
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

/**
 * Moves all corners of the volume by the specified offset.
 */
fun MutableVolume.translate(offset: Double3): MutableVolume {
    this.set(
        this.minX + offset.x,
        this.minY + offset.y,
        this.minZ + offset.z,
        this.maxX + offset.x,
        this.maxY + offset.y,
        this.maxZ + offset.z,
        this.empty
    )
    return this
}

/**
 * @return true if the point is within the Volume or [radius] distance from the volume.
 * [radius] should be zero or positive, throws exception if it is negative or NaN.
 */
fun Volume.containsWithinRadius(p: Double3, radius: Double): Boolean {
    if (radius.isNaN()) throw IllegalArgumentException("Radius was NaN (not a number)")
    if (radius < 0.0) throw IllegalArgumentException("Negative radius is not supported (was $radius)")
    return distanceToPointSquared(p) <= radius * radius
}

/**
 * Returns the shortest distance from the volume to the point squared (this is a bit faster to calculate than non-squared).
 * If the point is inside the volume, 0 is returned.
 * The volume being empty is not checked for, the user should check for it if it has relevance.
 */
fun Volume.distanceToPointSquared(p: Double3): Double {
    val dx = max(minX - p.x, 0.0, p.x - maxX)
    val dy = max(minY - p.y, 0.0, p.y - maxY)
    val dz = max(minZ - p.z, 0.0, p.z - maxZ)
    return dx*dx + dy*dy + dz*dz
}

/**
 * Returns the shortest distance from the volume to the point.
 * If the point is inside the volume, 0 is returned.
 * The volume being empty is not checked for, the user should check for it if it has relevance.
 */
fun Volume.distanceToPoint(p: Double3): Double {
    return sqrt(distanceToPointSquared(p))
}

/**
 * Returns the longest distance from any point in the volume to the point.
 * (In practice maximum of the corner distances to the point)
 * The volume being empty is not checked for, the user should check for it if it has relevance.
 */
fun Volume.maximumDistanceToPoint(p: Double3): Double {
    val dx = max(abs(minX - p.x), abs(p.x - maxX))
    val dy = max(abs(minY - p.y), abs(p.y - maxY))
    val dz = max(abs(minZ - p.z), abs(p.z - maxZ))
    return sqrt(dx*dx + dy*dy + dz*dz)
}

/**
 * Multiply all coordinates with the specified value.
 * (Updates both position and size)
 */
fun MutableIntVolume.mul(value: Int): MutableIntVolume {
    set(minX*value, minY*value, minZ*value,
        maxX*value, maxY*value, maxZ*value,
        empty)
    return this
}

/**
 * Multiply coordinates with the specified values.
 * (Updates both position and size)
 */
fun MutableIntVolume.mul(values: Int3): MutableIntVolume {
    set(minX*values.x, minY*values.y, minZ*values.z,
        maxX*values.x, maxY*values.y, maxZ*values.z,
        empty)
    return this
}

/**
 * Move this volume with the specified amount.
 */
fun MutableIntVolume.move(delta: Int3): MutableIntVolume {
    move(delta.x, delta.y, delta.z)
    return this
}

// TODO: Change utility library to return this for operations on volumes too

// TODO: Change utility library to provide static mutable and immutalbe constructor functions in the interfaces for
//       things like Int3 etc. for easier finding and code completition


// TODO: Swap tempPos and visitor for normal foreach too..

/**
 * Calls the visitor for each coordinate that exists in this volume.
 * Visited in ascending order, z loops slowest, x fastest.
 *
 * Returns true if the visitor returns true for any elements, false otherwise.
 * If this volume is empty, returns false.
 */
inline fun IntVolume.any(tempPos: MutableInt3 = MutableInt3(),
                         visitor: (Int3) -> Boolean): Boolean {
    if (!empty) {
        for (z in minZ .. maxZ) {
            for (y in minY .. maxY) {
                for (x in minX .. maxX) {
                    tempPos.set(x, y, z)
                    if (visitor(tempPos)) return true
                }
            }
        }
    }

    return false
}

/**
 * Calls the visitor for each coordinate that exists in this volume.
 * Visited in ascending order, z loops slowest, x fastest.
 *
 * Returns true if the visitor returns true for all elements, false otherwise.
 * If this volume is empty, returns true.
 */
inline fun IntVolume.all(tempPos: MutableInt3 = MutableInt3(),
                         visitor: (Int3) -> Boolean): Boolean {
    if (!empty) {
        for (z in minZ .. maxZ) {
            for (y in minY .. maxY) {
                for (x in minX .. maxX) {
                    tempPos.set(x, y, z)
                    if (!visitor(tempPos)) return false
                }
            }
        }
    }

    return true
}

/**
 * Distance from the minimum point to the maximum point in this volume.
 */
fun Volume.diagonalLength(): Double {
    return if (empty) 0.0
    else {
        val dx = sizeX
        val dy = sizeY
        val dz = sizeZ
        sqrt(dx*dx + dy*dy + dz*dz)
    }
}



inline fun Volume.clampXToVolume(x: Double): Double {
    return when {
        x < minX -> minX
        x > maxX -> maxX
        else -> x
    }
}

inline fun Volume.clampYToVolume(y: Double): Double {
    return when {
        y < minY -> minY
        y > maxY -> maxY
        else -> y
    }
}

inline fun Volume.clampZToVolume(z: Double): Double {
    return when {
        z < minZ -> minZ
        z > maxZ -> maxZ
        else -> z
    }
}

inline fun Volume.getMaxSideSize(): Double {
    return max(sizeX, sizeY, sizeZ)
}

inline fun Volume.getMinSideSize(): Double {
    return min(sizeX, sizeY, sizeZ)
}

/**
 * Calls the visitor for each coordinate that exists in this volume.
 * Visited in ascending order, z loops slowest, x fastest.
 *
 * Returns true if the visitor returns true for no elements, false otherwise.
 * If this volume is empty, returns true.
 */
inline fun IntVolume.none(tempPos: MutableInt3 = MutableInt3(),
                          visitor: (Int3) -> Boolean): Boolean =
    this.all(tempPos) { !visitor(it) }

// TODO: IntVolumes should not be inclusive the end coordinate, better logic if it is exclusive, but it requires major refactoring..

// TODO: Add immutable to Int3 and similar, return new Immutable for mutables, and self for immutables
inline val Int3.immutable: ImmutableInt3 get() {
    return this as? ImmutableInt3 ?: ImmutableInt3(this)
}


// TODO: Inline mix in MathUtils too..

/**
 * Interpolate between the start and end values (and beyond).  Does not perform any clamping or smoothing.
 * @param t 0 corresponds to start, 1 to end.
 * @return interpolated value
 */
inline fun fastMix(t: Double, start: Double, end: Double): Double = start + t * (end - start)


/**
 * The largest of x, y or z.
 */
inline fun Int3.maxCoordinate(): Int = max(x, y, z)

/**
 * The smallest of x, y or z.
 */
inline fun Int3.minCoordinate(): Int = min(x, y, z)

/**
 * The largest of x, y or z.
 */
inline fun Double3.maxCoordinate(): Double = max(x, y, z)

/**
 * The smallest of x, y or z.
 */
inline fun Double3.minCoordinate(): Double = min(x, y, z)


/**
 * Represents a position on a cube map.
 * [side] is as following: 0 = positive X, 1 = negative x, 2 = positive y, 3 = negative y, 4 = positive z, 5 = negative z.
 * [u] and [v] range from 0 to 1 on each face.
 */
data class CubeMapPos(var side: Int = 0,
                      var u: Double = 0.0,
                      var v: Double = 0.0) {

    /**
     * Get a direction vector representing this position on a cube map.
     * The direction vector is not normalized by default.
     *
     * Reference: https://en.wikipedia.org/wiki/Cube_mapping
     */
    fun toDirection(out: MutableDouble3 = MutableDouble3()): MutableDouble3 {
        // convert range 0 to 1 to -1 to 1
        val uc = 2.0 * u - 1.0
        val vc = 2.0 * v - 1.0

        when (side)
        {
            0 -> out.set(1.0, vc, -uc)  // POSITIVE X
            1 -> out.set(-1.0, vc, uc)  // NEGATIVE X
            2 -> out.set(uc,1.0, -vc)   // POSITIVE Y
            3 -> out.set(uc,-1.0, vc)   // NEGATIVE Y
            4 -> out.set(uc, vc,1.0)    // POSITIVE Z
            5 -> out.set(-uc, vc,-1.0)  // NEGATIVE Z
        }

        return out
    }

    /**
     * Return closest index of this cube map pos, assuming the cube map texture is stored each side at a time,
     * with v-major order for the side.
     * [textureSize] is the number of samples along one edge of the cube texture.
     */
    fun toCubeTextureIndex(textureSize: Int): Int {
        val sideSize = textureSize * textureSize
        val x = (u * (textureSize - 1)).roundToInt().clampTo(0, textureSize - 1)
        val y = (v * (textureSize - 1)).roundToInt().clampTo(0, textureSize - 1)
        return sideSize * side + textureSize * y + x
    }

    // IDEA: Function that returns the four neighboring texture index locations for this point, and the relative overlap for each.
}

/**
 * Project a direction vector to a cube map side and texture position.
 *
 * Reference: https://en.wikipedia.org/wiki/Cube_mapping
 */
fun Double3.projectToCube(out: CubeMapPos = CubeMapPos()): CubeMapPos {
    val absX = x.abs()
    val absY = y.abs()
    val absZ = z.abs()

    val isXPositive = x > 0
    val isYPositive = y > 0
    val isZPositive = z > 0

    var maxAxis = 0.0
    var uc = 0.0
    var vc = 0.0

    // POSITIVE X
    if (isXPositive && absX >= absY && absX >= absZ) {
        // u (0 to 1) goes from +z to -z
        // v (0 to 1) goes from -y to +y
        maxAxis = absX
        uc = -z
        vc = y
        out.side = 0
    }

    // NEGATIVE X
    if (!isXPositive && absX >= absY && absX >= absZ) {
        // u (0 to 1) goes from -z to +z
        // v (0 to 1) goes from -y to +y
        maxAxis = absX
        uc = z
        vc = y
        out.side = 1
    }

    // POSITIVE Y
    if (isYPositive && absY >= absX && absY >= absZ) {
        // u (0 to 1) goes from -x to +x
        // v (0 to 1) goes from +z to -z
        maxAxis = absY
        uc = x
        vc = -z
        out.side = 2
    }

    // NEGATIVE Y
    if (!isYPositive && absY >= absX && absY >= absZ) {
        // u (0 to 1) goes from -x to +x
        // v (0 to 1) goes from -z to +z
        maxAxis = absY
        uc = x
        vc = z
        out.side = 3
    }

    // POSITIVE Z
    if (isZPositive && absZ >= absX && absZ >= absY) {
        // u (0 to 1) goes from -x to +x
        // v (0 to 1) goes from -y to +y
        maxAxis = absZ
        uc = x
        vc = y
        out.side = 4
    }

    // NEGATIVE Z
    if (!isZPositive && absZ >= absX && absZ >= absY) {
        // u (0 to 1) goes from +x to -x
        // v (0 to 1) goes from -y to +y
        maxAxis = absZ
        uc = -x
        vc = y
        out.side = 5
    }

    // Convert range from -1 to 1 to 0 to 1
    out.u = 0.5 * (uc / maxAxis + 1.0)
    out.v = 0.5 * (vc / maxAxis + 1.0)

    return out
}


fun Volume.containsExclusive(p: Vector3): Boolean = this.containsExclusive(p.x.toDouble(), p.y.toDouble(), p.z.toDouble())

fun Volume.containsExclusive(p: Double3): Boolean = this.containsExclusive(p.x, p.y, p.z)

fun Volume.containsExclusive(x: Double, y: Double, z: Double): Boolean {
    if (empty) return false
    return x >= minX && x < maxX &&
           y >= minY && y < maxY &&
           z >= minZ && z < maxZ
}
