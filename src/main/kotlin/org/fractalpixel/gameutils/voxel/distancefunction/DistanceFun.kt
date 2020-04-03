package org.fractalpixel.gameutils.voxel.distancefunction

import org.fractalpixel.gameutils.utils.normalize
import org.fractalpixel.gameutils.voxel.distancefunction.MinMaxSampler.*
import org.kwrench.geometry.double3.Double3
import org.kwrench.geometry.double3.MutableDouble3
import org.kwrench.geometry.volume.Volume
import org.kwrench.math.clamp0To1
import org.kwrench.math.mix
import kotlin.math.max
import kotlin.math.min

/**
 * A distance function that defines a 3D object.  Render by finding and rendering the isosurface (surface where function returns zero).
 * The function returns the distance from the surface at a queried point, returning negative value if the point is inside the object.
 */
// TODO: Consider optimizing distance functions by having them generate code that is compiled with some on-the-fly compiler
//       (does e.g. Lua generate compiled and fast bytecode these days?).
//       This gets rid of a lot of object referencing in a critical path.
interface DistanceFun: (Double3) -> Double, (Double, Double, Double) -> Double {

    /**
     * Returns distance to the surface from the specified point, negative if the point is inside the object.
     */
    operator fun get(pos: Double3): Double = invoke(pos.x, pos.y, pos.z)

    /**
     * Returns distance to the surface from the specified point, negative if the point is inside the object.
     */
    override fun invoke(pos: Double3): Double  = invoke(pos.x, pos.y, pos.z)

    /**
     * Returns distance to the surface from the specified point, negative if the point is inside the object.
     */
    override fun invoke(x: Double, y: Double, z: Double): Double

    /**
     * Returns a minimum value for the specified volume.  No value in the volume retrieved with [get] should be smaller than
     * the returned value.  If a minimum can not be determined, return Double.NEGATIVE_INFINITY.
     */
    fun getMin(volume: Volume): Double

    /**
     * Returns a maximum value for the specified volume.  No value in the volume retrieved with [get] should be larger than
     * the returned value.  If a maximum can not be determined, return Double.POSITIVE_INFINITY.
     */
    fun getMax(volume: Volume): Double

    /**
     * Returns true if the specified volume may contain a surface (intersection of 0 level in the distance function).
     * If false, it will not contain a surface (it will be either completely inside or outside a surface).
     * If true, it may, but does not necessarily contain, a surface.
     * Uses min & max bounds for the volume for a quick check to speed up calculations or collision detection.
     *
     */
    fun mayContainSurface(volume: Volume): Boolean {
        return getMin(volume) <= 0.0 &&
               getMax(volume) >= 0.0
    }

    /**
     * Returns distance function that is this function added to the other function.
     */
    fun add(other: DistanceFun): DistanceFun =
        CombineFun(
            this,
            other
        ) { a, b -> a + b }

    /**
     * Returns distance function that is this function with the other subtracted from it.
     */
    fun subtract(other: DistanceFun): DistanceFun =
        CombineFun(
            this,
            other,
            MINIMUM, MAXIMUM,
            MAXIMUM, MINIMUM
        ) { a, b -> a - b }

    /**
     * Union of this object and the other.
     */
    fun union(other: DistanceFun): DistanceFun =
        CombineFun(
            this,
            other
        ) { a, b -> min(a, b) }

    /**
     * Remove the other object from this.
     */
    fun difference(other: DistanceFun): DistanceFun =
        CombineFun(
            this,
            other,
            MINIMUM, MAXIMUM,
            MAXIMUM, MINIMUM
        ) { a, b -> max(a, -b) }

    /**
     * Intersection of this and the other object,
     */
    fun intersection(other: DistanceFun): DistanceFun =
        CombineFun(
            this,
            other
        ) { a, b -> max(a, b) }

    /**
     * Union of this and the other, with the seam smoothed (outwards).
     * [smoothness] is given in distance units.
     */
    fun smoothUnion(other: DistanceFun, smoothness: Double): DistanceFun =
        CombineFun(this, other) { a, b ->
            val h = (0.5 + 0.5 * (b - a) / smoothness).clamp0To1()
            mix(h, b, a) - smoothness * h * (1.0 - h)
        }

    /**
     * Remove the other object from this, with the seam smoothed (inwards).
     * [smoothness] is given in distance units.
     */
    fun smoothDifference(other: DistanceFun, smoothness: Double): DistanceFun =
        CombineFun(this, other,
            MINIMUM, MAXIMUM,
            MAXIMUM, MINIMUM) { a, b ->
            val h = (0.5 - 0.5 * (b + a) / smoothness).clamp0To1()
            mix(h, a, -b) + smoothness * h * (1.0 - h)
        }

    /**
     * Intersect with the other object from this, with the seam smoothed (outwards).
     * [smoothness] is given in distance units.
     */
    fun smoothIntersection(other: DistanceFun, smoothness: Double): DistanceFun =
        CombineFun(this, other) { a, b ->
            val h = (0.5 - 0.5 * (b - a) / smoothness).clamp0To1()
            mix(h, b, a) + smoothness * h * (1.0 - h)
        }

    /**
     * Transform coordinate space that this noise uses with a 3D noise function.
     */
    fun perturb(scale: Double3 = Double3.ONES, amplitude: Double3 = Double3.ONES, offset: Double3 = Double3.ZEROES): DistanceFun = NoisePerturbFun(this, scale, amplitude, offset)


    /**
     * Calculate normal at the specified [pos].
     * Use the specified [samplingScale] for the normal calculation, normally the positions around the point are sampled
     * at the specified [samplingScale] in each direction to determine the normal, but implementations may also use
     * other methods.
     * The normal is stored in [normalOut], and it is returned.
     */
    fun getNormal(pos: Double3, samplingScale: Double, normalOut: MutableDouble3 = MutableDouble3()): MutableDouble3 {
        // Use normalOut as temporary sampling position, so that we do not need to create a new Double3 instance
        val mx = get(normalOut.set(pos).add(-samplingScale, 0.0, 0.0))
        val px = get(normalOut.set(pos).add(+samplingScale, 0.0, 0.0))
        val my = get(normalOut.set(pos).add(0.0, -samplingScale, 0.0))
        val py = get(normalOut.set(pos).add(0.0, +samplingScale, 0.0))
        val mz = get(normalOut.set(pos).add(0.0, 0.0, -samplingScale))
        val pz = get(normalOut.set(pos).add(0.0, 0.0, +samplingScale))

        // Calculate normal by comparing samples on both sides of the position along each axis, and normalizing the result
        normalOut.set(px-mx, py-my, pz-mz).normalize()

        return normalOut
    }
}