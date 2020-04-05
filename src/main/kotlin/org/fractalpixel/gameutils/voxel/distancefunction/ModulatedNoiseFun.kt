package org.fractalpixel.gameutils.voxel.distancefunction

import org.kwrench.geometry.volume.Volume
import org.kwrench.noise.OpenSimplexNoise
import org.kwrench.random.Rand
import kotlin.math.abs
import kotlin.math.max

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

    // TODO: Use same gradient decent approach as in regular noise (perhaps merge the two?)

    override fun getMin(volume: Volume): Double {
        // Assumes noise is in -1..1 range
        return -max(abs(amplitude.getMin(volume)), abs(amplitude.getMax(volume))) + offset
    }

    override fun getMax(volume: Volume): Double {
        // Assumes noise is in -1..1 range
        return max(abs(amplitude.getMin(volume)), abs(amplitude.getMax(volume))) + offset
    }
}