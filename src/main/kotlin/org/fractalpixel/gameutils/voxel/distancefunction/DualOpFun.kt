package org.fractalpixel.gameutils.voxel.distancefunction

import org.fractalpixel.gameutils.utils.checkForJobCancellation
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
class DualOpFun(var a: DistanceFun,
                var b: DistanceFun,
                inline var calculateBounds: (volume: Volume, a: DistanceFun, b: DistanceFun, aMin: Double, aMax: Double, bMin: Double, bMax: Double,  bounds: DistanceBounds) -> Unit = {
                     volume, a, b, aMin, aMax, bMin, bMax, bounds ->
                     bounds.set(op(aMin, bMin), op(aMax, bMax))
                 },
                inline var op :(a: Double, b: Double) -> Double): DistanceFun {

    override fun invoke(x: Double, y: Double, z: Double): Double {
        return op(a(x, y, z), b(x, y, z))
    }

    override suspend fun calculateBlock(
        volume: Volume,
        block: DepthBlock,
        blockPool: DepthBlockPool,
        leadingSeam: Int,
        trailingSeam: Int
    ) {
        checkForJobCancellation()

        // Calculate input a, use the given block
        a.calculateBlock(volume, block, blockPool, leadingSeam, trailingSeam)

        checkForJobCancellation()

        // Reserve separate block for results of b and release it when done
        blockPool.withObtained { bBlock ->

            // Calculate input b
            b.calculateBlock(volume, bBlock, blockPool, leadingSeam, trailingSeam)

            checkForJobCancellation()

            // Apply operation to each value
            val depths = block.depths
            val bDepths = bBlock.depths
            for (i in depths.indices) {
                depths[i] = op(depths[i], bDepths[i])
            }
        }
    }

    override fun calculateBounds(volume: Volume, bounds: DistanceBounds) {
        // Use same bounds instance to get bounds for both a and b
        a.getBounds(volume, bounds)
        val aMin = bounds.min
        val aMax = bounds.max

        b.getBounds(volume, bounds)
        val bMin = bounds.min
        val bMax = bounds.max

        calculateBounds(volume, a, b, aMin, aMax, bMin, bMax, bounds)
    }

}