package org.fractalpixel.gameutils.voxel.distancefunction

import org.fractalpixel.gameutils.utils.checkForJobCancellation
import org.fractalpixel.gameutils.utils.fastMix
import org.fractalpixel.gameutils.utils.getMaxSideSize
import org.fractalpixel.gameutils.voxel.distancefunction.DistanceFun.Companion.antiAliasFeaturesUsingScale
import org.fractalpixel.gameutils.voxel.distancefunction.DistanceFun.Companion.calculateSampleSize
import org.fractalpixel.gameutils.voxel.distancefunction.DistanceFun.Companion.featureBlendUsingScale
import org.fractalpixel.gameutils.voxel.distancefunction.utils.DepthBlock
import org.fractalpixel.gameutils.voxel.distancefunction.utils.DepthBlockPool
import org.fractalpixel.gameutils.voxel.distancefunction.utils.DistanceBounds
import org.kwrench.geometry.volume.Volume
import org.kwrench.noise.OpenSimplexNoise
import org.kwrench.random.Rand

/**
 * Noise based distance function, where the scale and amplitude may be functions.
 */
// TODO: Switch scale to mean feature size instead of coordinate scaling, much more intuitive to use and code with
class ModulatedNoiseFun(var scale: DistanceFun = ConstantFun(1.0),
                        var amplitude: DistanceFun = ConstantFun(1.0),
                        var offset: Double = 1.0,
                        val seed: Long = Rand.default.nextLong(),
                        var optimizationThreshold: Double = 0.001) : DistanceFun {

    val noise = OpenSimplexNoise(seed)

    override fun get(x: Double, y: Double, z: Double, sampleSize: Double): Double {
        // If the sample size is close to the feature size, blend towards average
        val s = scale.get(x, y, z, sampleSize)
        return antiAliasFeaturesUsingScale(sampleSize, s, { offset }) {
            noise.noise(x * s, y * s, z * s) * amplitude.get(x, y, z, sampleSize) + offset
        }
    }

    override suspend fun calculateBlock(
        volume: Volume,
        block: DepthBlock,
        blockPool: DepthBlockPool,
        leadingSeam: Int,
        trailingSeam: Int
    ) {
        val sampleSize = calculateSampleSize(volume, block)

        // Check if the sampling volume is small compared to the feature size
        val volumeSize = volume.getMaxSideSize()
        val scaleBounds = scale.getBounds(volume, sampleSize)
        if (volumeSize * scaleBounds.maxAbsoluteValue() < optimizationThreshold) {
            checkForJobCancellation()

            // Linearly interpolate values sampled at corners
            calculateBlockWithInterpolation(volume, block, blockPool, leadingSeam, trailingSeam)
        }
        else {
            checkForJobCancellation()

            // Check if sampling volume is too large compared to feature size
            val sampleSize = calculateSampleSize(volume, block)
            val featureBlend = featureBlendUsingScale(sampleSize, scaleBounds.minAbsoluteValue())
            if (featureBlend <= 0.0) {
                // Scale is too small detail everywhere, just fill with average
                block.fill(offset)
            }
            else {
                // Reserve temporary blocks
                val scaleBlock = blockPool.obtain()
                val ampBlock = blockPool.obtain()
                try {
                    // Calculate scale
                    scale.calculateBlock(volume, scaleBlock, blockPool, leadingSeam, trailingSeam)

                    checkForJobCancellation()

                    // Calculate amplitude
                    amplitude.calculateBlock(volume, ampBlock, blockPool, leadingSeam, trailingSeam)

                    checkForJobCancellation()

                    // Fill block with the noise function
                    val scaleDepths = scaleBlock.depths
                    val ampDepths = ampBlock.depths
                    block.fillUsingCoordinates(volume) {index, x, y, z ->
                        // Calculate scaling to use for voxel
                        val s = scaleDepths[index]

                        // Determine anti-alias blend between average and noise
                        antiAliasFeaturesUsingScale(sampleSize, s, {offset}) {
                            // Noise needed, calculate it
                            noise.noise(x * s, y * s, z * s) * ampDepths[index] + offset
                        }
                    }
                }
                finally {
                    // Free blocks
                    blockPool.release(ampBlock)
                    blockPool.release(scaleBlock)
                }
            }
        }
    }

    override fun calculateBounds(volume: Volume, sampleSize: Double, bounds: DistanceBounds) {

        // Determine scale of smallest features
        scale.calculateBounds(volume, sampleSize, bounds)
        val scaleOfLargestFeatures = bounds.minAbsoluteValue()
        val scaleOfSmallestFeatures = bounds.maxAbsoluteValue()

        val featureBlend = featureBlendUsingScale(sampleSize, scaleOfLargestFeatures)
        if (featureBlend <= 0.0) {
            // If the sampling size is too large for the feature size, the value is just the average
            bounds.setBothTo(offset)
        }
        else if (featureBlend >= 1.0) {
            if (1.0 / volume.getMaxSideSize() > scaleOfSmallestFeatures) {
                // The sampling volume is small compared to the noise scale, so the volume is unlikely to span the whole range of the noise,
                // so use a gradient decent algorithm to find the local min and max values for the noise
                this.gradientDecentBoundsSearch(volume, sampleSize, bounds)
            } else {
                // The sampling volume spans such a large portion of the noise wave size that it is likely to have both the minimum
                // and maximum noise value, so use the amplitude to calculate the bounds

                // Get maximum amplitude extent
                amplitude.calculateBounds(volume, sampleSize, bounds)
                val maxExtent = bounds.maxAbsoluteValue()

                // Set bounds to average +/- max amplitude extent
                bounds.set(offset - maxExtent, offset + maxExtent)
            }
        }
        else {
            // Get maximum amplitude extent
            amplitude.calculateBounds(volume, sampleSize, bounds)
            val maxExtent = bounds.maxAbsoluteValue()

            // Fade amplitude with feature blend if we are near the area where the features blend to average
            val fadedAmplitudeExtent = fastMix(featureBlend, 0.0, maxExtent)
            bounds.set(offset - fadedAmplitudeExtent, offset + fadedAmplitudeExtent)
        }
    }


}