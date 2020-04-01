package org.fractalpixel.gameutils.voxel.renderer

import org.fractalpixel.gameutils.voxel.distancefunction.DistanceFun
import org.kwrench.geometry.int3.Int3

/**
 * Used to cache distance field values for one chunk.
 * Reusable.
 */
class CachedDistances(var distanceFun: DistanceFun,
                      val configuration: VoxelConfiguration) {

    val distance = DoubleArray(configuration.blockCornerCountInChunk) {1.0}

    inline fun getSample(x: Int, y: Int, z: Int): Double {
        return distance[x + y * configuration.chunkCornersSize + z * configuration.chunkCornersSize * configuration.chunkCornersSize]
    }

    inline fun getSample(index: Int): Double {
        return distance[index]
    }

    /**
     * Fills distance array based on distance function.
     */
    fun calculate(chunkPos: Int3, level: Int) {
        val worldStep = configuration.blockWorldSize(level)
        var xp: Double
        var yp: Double
        var index = 0
        val worldCornerPos = configuration.chunkWorldCornerPos(chunkPos, level)
        var zp: Double = worldCornerPos.z.toDouble()
        for (z in 0 until configuration.chunkCornersSize) {
            yp = worldCornerPos.y.toDouble()
            for (y in 0 until configuration.chunkCornersSize) {
                xp = worldCornerPos.x.toDouble()
                for (x in 0 until configuration.chunkCornersSize) {
                    distance[index++] = distanceFun(xp, yp, zp)
                    xp += worldStep
                }
                yp += worldStep
            }
            zp += worldStep
        }
    }

}