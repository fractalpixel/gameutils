package org.fractalpixel.gameutils.voxel

import org.fractalpixel.gameutils.voxel.distancefunction.DistanceFun
import org.kwrench.geometry.double3.Double3

/**
 * Holds data about a voxel terrain.
 */
class VoxelTerrain(var distanceFun: DistanceFun,
                   val voxelSize: Int = 64,
                   val worldSize: Double = 30.0,
                   val worldCenter: Double3 = Double3.ZEROES) {

    val totalSize: Int = voxelSize * voxelSize * voxelSize
    val distance = DoubleArray(totalSize) {1.0}

    init {
        calculate()
    }

    inline fun getSample(x: Int, y: Int, z: Int): Double {
        return distance[x + y * voxelSize + z * voxelSize * voxelSize]
    }

    inline fun getSample(index: Int): Double {
        return distance[index]
    }

    /**
     * Fills distance array based on distance function.
     */
    fun calculate() {
        val worldStep = worldSize / (voxelSize - 1)
        var xp: Double
        var yp: Double
        var zp: Double = worldCenter.z - worldSize * 0.5
        var index = 0
        for (z in 0 until voxelSize) {
            yp = worldCenter.y - worldSize * 0.5
            for (y in 0 until voxelSize) {
                xp = worldCenter.x - worldSize * 0.5
                for (x in 0 until voxelSize) {
                    distance[index++] = distanceFun(xp, yp, zp)
                    xp += worldStep
                }
                yp += worldStep
            }
            zp += worldStep
        }
    }


}