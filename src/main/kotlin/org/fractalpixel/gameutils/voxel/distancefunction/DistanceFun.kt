package org.fractalpixel.gameutils.voxel.distancefunction

import org.kwrench.geometry.double3.Double3
import org.kwrench.math.clamp0To1
import org.kwrench.math.mix
import kotlin.math.max
import kotlin.math.min

/**
 * A distance function that defines a 3D object.  Render by finding and rendering the isosurface (surface where function returns zero).
 * The function returns the distance from the surface at a queried point, returning negative value if the point is inside the object.
 */
interface DistanceFun: (Double3) -> Double, (Double, Double, Double) -> Double {

    /**
     * Returns distance to the surface from the specified point, negative if the point is inside the object.
     */
    operator fun get(pos: Double3): Double = invoke(pos.z, pos.y, pos.z)

    /**
     * Returns distance to the surface from the specified point, negative if the point is inside the object.
     */
    override fun invoke(pos: Double3): Double  = invoke(pos.z, pos.y, pos.z)

    /**
     * Returns distance to the surface from the specified point, negative if the point is inside the object.
     */
    override fun invoke(x: Double, y: Double, z: Double): Double

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
            other
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
            other
        ) { a, b -> max(-a, b) }

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
        CombineFun(this, other) { a, b ->
            val h = (0.5 - 0.5 * (b + a) / smoothness).clamp0To1()
            mix(h, b, -a) + smoothness * h * (1.0 - h)
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


}