package org.fractalpixel.gameutils.voxel.renderer

import com.badlogic.gdx.math.Vector3
import org.fractalpixel.gameutils.libgdxutils.set
import org.fractalpixel.gameutils.voxel.distancefunction.DistanceFun
import org.kwrench.geometry.double3.MutableDouble3
import org.kwrench.geometry.int3.Int3

/**
 * Used to cache distance field values for one chunk.
 * Reusable.  Not thread safe (use thread local).
 */
class CachedDistances(val configuration: VoxelConfiguration) {

    val actualSize = configuration.chunkCornersSize

    val distance = DoubleArray(actualSize * actualSize * actualSize) {1.0}

    var isAir: Boolean = false
        private set

    var isSolid: Boolean = false
        private set

    private val tempPos = MutableDouble3()
    private val tempNormal = MutableDouble3()

    private var level: Int = 0
    private var worldStep: Double = 1.0

    inline fun getSample(x: Int, y: Int, z: Int): Double {
        return distance[x + y * actualSize + z * actualSize * actualSize]
    }

    /**
     * Fills distance array based on distance function.
     * Overlaps one step over each neighbouring chunk.
     */
    fun calculate(distanceFun: DistanceFun, chunkPos: Int3, level: Int) {
        this.level = level

        this.worldStep = configuration.blockWorldSize(level)
        val worldCornerPos = configuration.chunkWorldCornerPos(chunkPos, level)

        isAir = true
        isSolid = true

        var index = 0

        var xp: Double
        var yp: Double
        var zp: Double = worldCornerPos.z.toDouble() - worldStep
        for (z in 0 until configuration.chunkCornersSize) {
            yp = worldCornerPos.y.toDouble() - worldStep
            for (y in 0 until configuration.chunkCornersSize) {
                xp = worldCornerPos.x.toDouble() - worldStep
                for (x in 0 until configuration.chunkCornersSize) {
                    val d = distanceFun(xp, yp, zp)
                    distance[index++] = d

                    if (d > 0) isSolid = false;
                    if (d <= 0) isAir = false;

                    xp += worldStep
                }
                yp += worldStep
            }
            zp += worldStep
        }
    }

    /**
     * Get normal for the specified position.
     * Samples the distance field to get an accurate normal
     */
    fun getNormal(distanceFun: DistanceFun, position: Vector3, normalOut: Vector3) {
        tempPos.set(position)
        distanceFun.getNormal(tempPos, worldStep * 0.5, tempNormal)
        normalOut.set(tempNormal)
    }


}