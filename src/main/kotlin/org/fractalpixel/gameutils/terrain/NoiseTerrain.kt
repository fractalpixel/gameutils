package org.fractalpixel.gameutils.terrain

import org.kwrench.noise.OpenSimplexNoise


class NoiseTerrain(var altitude: Double = 20.0,
                   var featureSize: Double = 50.0,
                   val seed: Long = 42) : Terrain {
    val noise = OpenSimplexNoise(seed)

    override fun getHeight(x: Double, z: Double): Double {
        return noise.noise(x / featureSize, z / featureSize) * altitude

    }

}