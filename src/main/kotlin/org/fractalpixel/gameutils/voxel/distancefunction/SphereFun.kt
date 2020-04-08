package org.fractalpixel.gameutils.voxel.distancefunction

import org.fractalpixel.gameutils.utils.*
import org.fractalpixel.gameutils.voxel.distancefunction.utils.DepthBlock
import org.fractalpixel.gameutils.voxel.distancefunction.utils.DepthBlockPool
import org.fractalpixel.gameutils.voxel.distancefunction.utils.DistanceBounds
import org.kwrench.geometry.double3.Double3
import org.kwrench.geometry.volume.Volume

/**
 * Set [optimizationThreshold] to 0.0 if no optimization of the shape should be done.
 */
class SphereFun(var radius: Double = 1.0,
                var center: Double3 = Double3.ZEROES,
                var optimizationThreshold: Double = 0.001): DistanceFun {

    override fun get(x: Double, y: Double, z: Double, sampleSize: Double): Double {
        return center.distanceTo(x, y, z) - radius
    }

    override suspend fun calculateBlock(
        volume: Volume,
        block: DepthBlock,
        blockPool: DepthBlockPool,
        leadingSeam: Int,
        trailingSeam: Int
    ) {
        checkForJobCancellation()

        // If we are far enough from the sphere center, and small enough volume, we can interpolate corners
        // instead of calculating the distance to the center.  Probably not very notable difference in this case thou
        val volumeSize = volume.getMaxSideSize()
        val distanceToCenter = volume.distanceToPoint(center)
        if (distanceToCenter > 0.0 && volumeSize / distanceToCenter < optimizationThreshold) {
            // Linearly interpolate values sampled at corners
            calculateBlockWithInterpolation(volume, block, blockPool, leadingSeam, trailingSeam)
        }
        else {
            // Fill block with the sphere function
            block.fillUsingCoordinates(volume) {_, x, y, z ->
                center.distanceTo(x, y, z) - radius
            }
        }
    }

    override fun calculateBounds(volume: Volume, sampleSize: Double, bounds: DistanceBounds) {
        // Get at shortest distance between sphere center and axis aligned volume,
        // subtract radius to get the  SphereFun value at that distance.
        val min = volume.distanceToPoint(center) - radius

        // Get the maximum distance from any point in the volume to the center,
        // subtract radius to get the SphereFun value at that distance.
        val max = volume.maximumDistanceToPoint(center) - radius

        bounds.set(min, max)
    }

}