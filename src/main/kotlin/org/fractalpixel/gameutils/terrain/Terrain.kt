package org.fractalpixel.gameutils.terrain

/**
 * Defines the height and materials on a terrain.
 */
interface Terrain {

    fun getHeight(x: Double, z: Double): Double

    // TODO: Materials?

    /**
     * Returns a new terrain that adds this and the other specified terrains together, scaling each if desired.
     */
    fun add(other: Terrain, scaleOther: Double = 1.0, scaleThis: Double = 1.0, offset: Double = 0.0): AddTerrain {
        return AddTerrain(this, other, scaleThis, scaleOther, offset)
    }

}