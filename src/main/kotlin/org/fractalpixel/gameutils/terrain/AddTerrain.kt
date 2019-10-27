package org.fractalpixel.gameutils.terrain

/**
 * Adds two terrains, optionally multiplying them and adding a flat offset.
 */
class AddTerrain(var terrainA: Terrain,
                 var terrainB: Terrain,
                 var scaleA: Double = 1.0,
                 var scaleB: Double = 1.0,
                 var offset: Double = 0.0): Terrain {

    override fun getHeight(x: Double, z: Double): Double {
        val a = terrainA.getHeight(x, z)
        val b = terrainB.getHeight(x, z)
        return a * scaleA + b * scaleB + offset
    }
}