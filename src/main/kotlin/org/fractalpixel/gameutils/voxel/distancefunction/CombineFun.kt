package org.fractalpixel.gameutils.voxel.distancefunction

import org.kwrench.geometry.volume.Volume

/**
 * Combines the two specified distance functions using the specified operator.
 *
 * To enable calculating the minimum and maximum values in a volume, you need to
 * define whether the minimum or the maximum should be sampled for each distance function when calculating
 * the minim or maximum of their combination. (Normally it uses minimum of each distance
 * function when calculating the minimum of the combination, and maximum of both when calculating the maximum,
 * but for cases like subtraction they are different (minimum of combination a - b happens when a is sampled with minimum
 * and b is sampled with maximum).
 */
class CombineFun(var a: DistanceFun,
                 var b: DistanceFun,
                 var aMinSampler: MinMaxSampler = MinMaxSampler.MINIMUM,
                 var bMinSampler: MinMaxSampler = MinMaxSampler.MINIMUM,
                 var aMaxSampler: MinMaxSampler = MinMaxSampler.MAXIMUM,
                 var bMaxSampler: MinMaxSampler = MinMaxSampler.MAXIMUM,
                 var op :(a: Double, b: Double) -> Double): DistanceFun {

    override fun invoke(x: Double, y: Double, z: Double): Double {
        return op(a(x, y, z), b(x, y, z))
    }

    override fun getMin(volume: Volume): Double {
        return op(
            aMinSampler.calculate(a, volume),
            bMinSampler.calculate(b, volume)
        )
    }

    override fun getMax(volume: Volume): Double {
        return op(
            aMaxSampler.calculate(a, volume),
            bMaxSampler.calculate(b, volume)
        )
    }
}