package org.fractalpixel.gameutils.voxel.distancefunction

import org.kwrench.geometry.double3.Double3
import org.kwrench.noise.OpenSimplexNoise
import org.kwrench.random.Rand

/**
 * A distance function that perturbs the input position with noise to another distance function.
 */
class NoisePerturbFun(var distanceFun: DistanceFun,
                      var noiseScale: Double3 = Double3.ONES,
                      var noiseOffset: Double3 = Double3.ZEROES,
                      val seed: Long = Rand.default.nextLong()): DistanceFun {

    val noiseX = OpenSimplexNoise(seed + 13)
    val noiseY = OpenSimplexNoise(seed + 41)
    val noiseZ = OpenSimplexNoise(seed + 97)

    override fun invoke(x: Double, y: Double, z: Double): Double {
        val xp = noiseX.noise(x, y, z) * noiseScale.x + noiseOffset.x
        val yp = noiseY.noise(x, y, z) * noiseScale.y + noiseOffset.y
        val zp = noiseZ.noise(x, y, z) * noiseScale.z + noiseOffset.z
        return distanceFun(xp, yp, zp)
    }
}