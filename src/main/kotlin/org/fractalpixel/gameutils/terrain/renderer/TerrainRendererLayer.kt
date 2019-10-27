package org.fractalpixel.gameutils.terrain.renderer

import com.badlogic.gdx.graphics.Camera
import org.fractalpixel.gameutils.layer.Layer3D
import org.fractalpixel.gameutils.libgdxutils.ShapeBuilder
import org.fractalpixel.gameutils.rendering.RenderingContext3D
import org.fractalpixel.gameutils.terrain.Terrain
import org.kwrench.time.Time

/**
 * Renders a 3D terrain.
 */
class TerrainRendererLayer(var terrain: Terrain,
                           val levelCount: Int = 11,
                           val baseChunkSizeMeters: Double = 20.0,
                           val chunkSizeInCells: Int = 8,
                           val levelSizeChunks: Int = 12,
                           val moveThresholdChunks: Int = 2,
                           val skirtPercent: Double = 0.1): Layer3D() {

    val innerLevelSizeChunks: Int = levelSizeChunks/2
    val moveDistanceChunks: Int = 2

    val levels: Array<DetailLevel> = Array(levelCount) {
        DetailLevel(this, it)
    }

    val shapeBuilder = ShapeBuilder()



    fun calculateChunkSize(level: Int) = baseChunkSizeMeters * Math.pow(2.0, level.toDouble())

    fun update(camera: Camera, time: Time) {
        // Split chunks / merge chunks if needed
        for (level in levels) {
            level.update(camera, time)
        }

    }

    override fun render(context: RenderingContext3D) {
        // Get camera pos to determine chunks to split etc
        // Update
        val time = entity?.world?.time ?: throw IllegalStateException("Needs to be added to an entity before calling render")
        update(context.camera, time)

        // Recalculate needed areas, start with most rough ones
        for (i in levelCount - 1 downTo  0) {
            levels[i].recalculate()
        }

        // Render chunks
        for (level in levels) {
            level.render(context)
        }
    }

    override fun doDispose() {
        for (level in levels) {
            level.dispose()
        }
    }
}