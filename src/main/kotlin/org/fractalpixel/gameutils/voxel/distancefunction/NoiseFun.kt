package org.fractalpixel.gameutils.voxel.distancefunction

import org.kwrench.geometry.volume.Volume
import org.kwrench.noise.OpenSimplexNoise
import org.kwrench.random.Rand
import kotlin.math.abs

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

    // TODO: If volume is smaller than half scale or so, use gradient decent/climb with a starting point in each corner
    //  and center for 10 iterations or so (with simulated annealing) to find the min and max values, adding a bit of
    //  extra margin (and clamping at -1..1 *amplitude+offset).
    //  This way long-wavelength noise functions can be used without adding a lot of chunks that need to be calculated.

    override fun getMin(volume: Volume): Double {
        // Assumes noise is in -1..1 range
        return -abs(amplitude) + offset
    }

    override fun getMax(volume: Volume): Double {
        // Assumes noise is in -1..1 range
        return abs(amplitude) + offset
    }
}