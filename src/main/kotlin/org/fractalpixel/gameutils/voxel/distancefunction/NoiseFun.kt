package org.fractalpixel.gameutils.voxel.distancefunction

import org.fractalpixel.gameutils.utils.*
import org.fractalpixel.gameutils.voxel.distancefunction.DistanceFun.Companion.antiAliasFeaturesUsingScale
import org.fractalpixel.gameutils.voxel.distancefunction.DistanceFun.Companion.calculateSampleSize
import org.fractalpixel.gameutils.voxel.distancefunction.DistanceFun.Companion.featureBlendUsingScale
import org.fractalpixel.gameutils.voxel.distancefunction.utils.DepthBlock
import org.fractalpixel.gameutils.voxel.distancefunction.utils.DepthBlockPool
import org.fractalpixel.gameutils.voxel.distancefunction.utils.DistanceBounds
import org.kwrench.geometry.volume.Volume
import org.kwrench.math.abs
import org.kwrench.noise.OpenSimplexNoise
import org.kwrench.random.Rand

/**
 * Noise based distance function.
 */
// TODO: A custom 3D noise function that supports exact bounds calculation out of the box, as well as gradient and sample size would be nice and would
//       probably speed things up considerably.
// TODO: Switch scale to mean feature size instead of coordinate scaling, much more intuitive to use and code with
class NoiseFun(var scale: Double = 1.0,
               var amplitude: Double = 1.0,
               var offset: Double = 1.0,
               val seed: Long = Rand.default.nextLong(),
               var optimizationThreshold: Double = 0.001) : DistanceFun {

    val noise = OpenSimplexNoise(seed)

    override fun get(x: Double, y: Double, z: Double, sampleSize: Double): Double {
        // If the sample size is close to the feature size, blend towards average
        return antiAliasFeaturesUsingScale(sampleSize, scale, {offset}) {
            noise.noise(x * scale, y * scale, z * scale) * amplitude + offset
        }
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
            // Check if sampling volume is too large compared to feature size
            val sampleSize = calculateSampleSize(volume, block)
            val featureBlend = featureBlendUsingScale(sampleSize, scale)
            when {
                featureBlend <= 0.0 -> {
                    // Just fill with average
                    block.fill(offset)
                }
                featureBlend >= 1.0 -> {
                    // Fill block with the noise function
                    block.fillUsingCoordinates(volume) {_, x, y, z ->
                        noise.noise(x * scale, y * scale, z * scale) * amplitude + offset
                    }
                }
                else -> {
                    // Fill block with blend between the noise function and the average
                    block.fillUsingCoordinates(volume) {_, x, y, z ->
                        val feature = noise.noise(x * scale, y * scale, z * scale) * amplitude + offset
                        fastMix(featureBlend, offset, feature)
                    }

                }
            }
        }
    }

    override fun calculateBounds(volume: Volume, sampleSize: Double, bounds: DistanceBounds) {
        val featureBlend = featureBlendUsingScale(sampleSize, scale)
        if (featureBlend <= 0.0) {
            // If the sampling size is too large for the feature size, the value is just the average
            bounds.setBothTo(offset)
        }
        else if (featureBlend >= 1.0) {
            val volumeSize = volume.getMaxSideSize()
            if (1.0 / volumeSize > scale.abs() * 2.0) {
                // The sampling volume is small compared to the noise scale, so the volume is unlikely to span the whole range of the noise,
                // so use a gradient decent algorithm to find the local min and max values for the noise
                this.gradientDecentBoundsSearch(volume, sampleSize, bounds)
            } else {
                // The sampling volume spans such a large portion of the noise wave size that it is likely to have both the minimum
                // and maximum noise value

                // NOTE: Assumes noise function returns values is in the -1 .. 1 range before amplitude scaling.
                val amp = amplitude.abs()
                bounds.set(offset - amp, offset + amp)
            }
        }
        else {
            // Fade amplitude with feature blend if we are near the area where the features blend to average
            val amp = fastMix(featureBlend, 0.0, amplitude.abs())
            bounds.set(offset - amp, offset + amp)
        }


    }


}