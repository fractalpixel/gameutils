package org.fractalpixel.gameutils.voxel.distancefunction

import org.fractalpixel.gameutils.utils.getMaxSideSize
import org.kwrench.geometry.volume.Volume
import org.kwrench.math.abs
import org.kwrench.noise.OpenSimplexNoise
import org.kwrench.random.Rand
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Noise based distance function, where the scale and amplitude may be functions.
 */
class ModulatedNoiseFun(var scale: DistanceFun = ConstantFun(1.0),
                        var amplitude: DistanceFun = ConstantFun(1.0),
                        var offset: Double = 1.0,
                        val seed: Long = Rand.default.nextLong()) : DistanceFun {

    val noise = OpenSimplexNoise(seed)

    override fun invoke(x: Double, y: Double, z: Double): Double {
        val s = scale(x, y, z)
        return noise.noise(x * s, y * s, z * s) * amplitude(x, y, z) + offset
    }

    override fun calculateBounds(volume: Volume, bounds: DistanceBounds) {
        // Determine scale of smallest features
        scale.calculateBounds(volume, bounds)
        val scale = max(bounds.min.abs(), bounds.max.abs())

        if (1.0 / volume.getMaxSideSize() > scale) {
            // The sampling volume is small compared to the noise scale, so the volume is unlikely to span the whole range of the noise,
            // so use a gradient decent algorithm to find the local min and max values for the noise
            this.gradientDecentBoundsSearch(volume, bounds)
        } else {
            // The sampling volume spans such a large portion of the noise wave size that it is likely to have both the minimum
            // and maximum noise value, so use the amplitude to calculate the bounds

            amplitude.calculateBounds(volume, bounds)
            val minAmp = bounds.min
            val maxAmp = bounds.max

            // NOTE: Assumes noise function returns values is in the -1 .. 1 range before amplitude scaling.
            val maxExtent = max(minAmp.abs(), maxAmp.abs())

            bounds.set(-maxExtent + offset, maxExtent + offset)
        }
    }


}