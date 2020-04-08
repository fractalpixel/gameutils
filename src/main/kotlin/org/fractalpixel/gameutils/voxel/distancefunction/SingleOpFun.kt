package org.fractalpixel.gameutils.voxel.distancefunction

import org.fractalpixel.gameutils.utils.checkForJobCancellation
import org.fractalpixel.gameutils.voxel.distancefunction.utils.DepthBlock
import org.fractalpixel.gameutils.voxel.distancefunction.utils.DepthBlockPool
import org.fractalpixel.gameutils.voxel.distancefunction.utils.DistanceBounds
import org.kwrench.geometry.volume.Volume

/**
 * Modifies a DistanceFun with a single operation.
 */
class SingleOpFun(var f: DistanceFun,
                  inline var calculateBounds: (volume: Volume, sampleSize: Double, a: DistanceFun, aMin: Double, aMax: Double, bounds: DistanceBounds) -> Unit = {
                          volume, sampleSize, a, aMin, aMax, bounds ->
                      bounds.set(op(aMin), op(aMax))
                  },
                  inline var op: (value: Double) -> Double): DistanceFun {

    override fun get(x: Double, y: Double, z: Double, sampleSize: Double): Double {
        return op(f.get(x, y, z, sampleSize))
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

    override fun calculateBounds(volume: Volume, sampleSize: Double, bounds: DistanceBounds) {
        // Use same bounds instance to get bounds for the function
        f.getBounds(volume, sampleSize, bounds)
        val aMin = bounds.min
        val aMax = bounds.max

        calculateBounds(volume, sampleSize, f, aMin, aMax, bounds)
    }

}