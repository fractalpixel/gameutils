package org.fractalpixel.gameutils.voxel.distancefunction

import org.fractalpixel.gameutils.utils.*
import org.kwrench.geometry.volume.Volume
import org.kwrench.math.abs
import org.kwrench.noise.OpenSimplexNoise
import org.kwrench.random.Rand

/**
 * Noise based distance function.
 */
// TODO: A custom 3D noise function that supports exact bounds calculation out of the box, as well as gradient and sample size would be nice and would
//       probably speed things up considerably.
class NoiseFun(var scale: Double = 1.0,
               var amplitude: Double = 1.0,
               var offset: Double = 1.0,
               val seed: Long = Rand.default.nextLong(),
               var optimizationThreshold: Double = 0.001) : DistanceFun {

    val noise = OpenSimplexNoise(seed)

    override fun invoke(x: Double, y: Double, z: Double): Double {
        return noise.noise(x * scale, y * scale, z * scale) * amplitude + offset
    }

    override suspend fun calculateBlock(
        volume: Volume,
        block: DepthBlock,
        blockPool: DepthBlockPool,
        leadingSeam: Int,
        trailingSeam: Int
    ) {
        checkForJobCancellation()

        // Check if the sampling volume is small compared to the feature size
        val volumeSize = volume.getMaxSideSize()
        if (volumeSize * scale < optimizationThreshold) {
            // Linearly interpolate values sampled at corners
            calculateBlockWithInterpolation(volume, block, blockPool, leadingSeam, trailingSeam)
        }
        else {
            // Fill block with the noise function
            block.fillUsingCoordinates(volume) {_, x, y, z ->
                noise.noise(x * scale, y * scale, z * scale) * amplitude + offset
            }
        }
    }

    override fun calculateBounds(volume: Volume, bounds: DistanceBounds) {
        if (1.0 / volume.getMaxSideSize() > scale.abs() * 2.0) {
            // The sampling volume is small compared to the noise scale, so the volume is unlikely to span the whole range of the noise,
            // so use a gradient decent algorithm to find the local min and max values for the noise
            this.gradientDecentBoundsSearch(volume, bounds)
        } else {
            // The sampling volume spans such a large portion of the noise wave size that it is likely to have both the minimum
            // and maximum noise value

            // NOTE: Assumes noise function returns values is in the -1 .. 1 range before amplitude scaling.
            bounds.set(-amplitude.abs() + offset, amplitude.abs() + offset)
        }
    }


}