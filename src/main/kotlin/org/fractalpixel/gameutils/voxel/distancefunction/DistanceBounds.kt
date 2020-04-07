package org.fractalpixel.gameutils.voxel.distancefunction

/**
 * Holds bounds for a distance function,
 * one estimate for the smallest distance and one for the largest.
 */
data class DistanceBounds(var min: Double = 0.0,
                          var max: Double = 0.0) {

    init {
        // Make sure the bounds stay in the correct order
        set(min, max)
    }

    fun set(a: Double, b: Double): DistanceBounds {
        if (a < b) {
            min = a
            max = b
        }
        else {
            min = b
            max = a
        }

        // For chaining
        return this
    }

    /**
     * Set min and max to the same value.
     */
    fun setBothTo(value: Double) {
        min = value
        max = value
    }

    /**
     * Set this range to -Inf .. + Inf
     */
    fun setUndetermined(): DistanceBounds {
        min = Double.NEGATIVE_INFINITY
        max = Double.POSITIVE_INFINITY
        return this
    }

    /**
     * True if these bounds at least partially overlap the other bounds.
     * If they just touch, it is not counted as intersecting though.
     */
    fun intersects(other: DistanceBounds): Boolean =
        min < other.max &&
        max > other.min

    /**
     * True if these bounds at least partially overlap the other bounds.
     * If they just touch it is also counted as intersecting.
     */
    fun intersectsInclusive(other: DistanceBounds): Boolean =
        min <= other.max &&
        max >= other.min


    /**
     * True if the value is in the bounds (exclusive max value)
     */
    fun contains(v: Double): Boolean = v >= min && v < max

    /**
     * True if the value is in the bounds (inclusive max value)
     */
    fun containsInclusive(v: Double): Boolean = v in min..max

}