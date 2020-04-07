package org.fractalpixel.gameutils.voxel.distancefunction

import org.fractalpixel.gameutils.utils.checkForJobCancellation
import org.kwrench.geometry.volume.Volume

/**
 * Modifies a DistanceFun with a single operation.
 */
class SingleOpFun(var f: DistanceFun,
                  inline var calculateBounds: (volume: Volume, a: DistanceFun, aMin: Double, aMax: Double, bounds: DistanceBounds) -> Unit = {
                          volume, a, aMin, aMax, bounds ->
                      bounds.set(op(aMin), op(aMax))
                  },
                  inline var op: (value: Double) -> Double): DistanceFun {

    override fun invoke(x: Double, y: Double, z: Double): Double {
        return op(f(x, y, z))
    }

    override suspend fun calculateBlock(
        volume: Volume,
        block: DepthBlock,
        blockPool: DepthBlockPool,
        leadingSeam: Int,
        trailingSeam: Int
    ) {
        checkForJobCancellation()

        // Calculate the original function
        f.calculateBlock(volume, block, blockPool, leadingSeam, trailingSeam)

        checkForJobCancellation()

        // Apply the function to all values
        val depths = block.depths
        for (i in depths.indices) {
            depths[i] = op(depths[i])
        }
    }

    override fun calculateBounds(volume: Volume, bounds: DistanceBounds) {
        // Use same bounds instance to get bounds for the function
        f.getBounds(volume, bounds)
        val aMin = bounds.min
        val aMax = bounds.max

        calculateBounds(volume, f, aMin, aMax, bounds)
    }

}