package org.fractalpixel.gameutils.terrain

/**
 * Simple flat terrain
 */
class FlatTerrain(var height: Double) : Terrain {
    override fun getHeight(x: Double, z: Double): Double = height
}