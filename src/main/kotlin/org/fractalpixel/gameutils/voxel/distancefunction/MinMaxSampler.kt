package org.fractalpixel.gameutils.voxel.distancefunction

import org.kwrench.geometry.volume.Volume

/**
 * Used in [CombineFun] to specify the functions to use when calculating minimum or maximum of some combination
 * of two other functions.
 */
enum class MinMaxSampler(val calculate: (dist: DistanceFun, volume: Volume) -> Double) {
    /**
     * Use the maximum of the distance function argument.
     */
    MINIMUM({ dist, volume ->
        dist.getMin(volume)
    }),

    /**
     * Use the minimum of the distance function argument.
     */
    MAXIMUM({ dist, volume ->
        dist.getMax(volume)
    }),

    /** This can be used if the value of a function does not affect the minimum or maximum value in a volume. Rare case. */
    ZERO({dist, volume ->
        0.0
    }),

    /** This can be used if the value of a function does not affect the minimum or maximum value in a volume. Rare case. */
    ONE({dist, volume ->
        1.0
    }),
}