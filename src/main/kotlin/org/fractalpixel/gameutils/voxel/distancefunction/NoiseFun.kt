package org.fractalpixel.gameutils.voxel.distancefunction

import org.kwrench.noise.OpenSimplexNoise
import org.kwrench.random.Rand

/**
 * Noise based distance function.
 */
class NoiseFun(var scale: Double = 1.0,
               var amplitude: Double = 1.0,
               var offset: Double = 1.0,
               val seed: Long = Rand.default.nextLong()) : DistanceFun {

    val noise = OpenSimplexNoise(seed)

    override fun invoke(x: Double, y: Double, z: Double): Double {
        return noise.noise(x * scale, y * scale, z * scale) * amplitude + offset
    }
}