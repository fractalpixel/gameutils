package org.fractalpixel.gameutils.terrain.renderer

import com.badlogic.gdx.graphics.Camera
import org.fractalpixel.gameutils.rendering.RenderingContext3D
import org.kwrench.geometry.grid.RectangularGrid
import org.kwrench.math.modPositive
import org.kwrench.time.Time


/**
 * Stores a grid of chunks of a certain physical size / detail level.
 */
class DetailLevel(val terrainRenderer: TerrainRendererLayer,
                  val level: Int) {
    var cornerChunkX: Int = 0
        private set
    var cornerChunkZ: Int = 0
        private set
    val chunkSizeMeters: Double = terrainRenderer.calculateChunkSize(level)
    val levelSizeChunks = terrainRenderer.levelSizeChunks
    val levelSizeMeters: Double = levelSizeChunks * chunkSizeMeters

    val grid = RectangularGrid(chunkSizeMeters, chunkSizeMeters)
    private var anyNeedsRecalculation = true

    private var arrayOffsetX = 0
    private var arrayOffsetZ = 0

    private val chunks = Array(levelSizeChunks * levelSizeChunks) {
        TerrainChunk(terrainRenderer, this)
    }

    /**
     * The chunk at the specified global chunk coordinates for this detail level, or null if it is not contained in this detail level.
     */
    fun getChunk(chunkX: Int, chunkZ: Int): TerrainChunk? {
        // Position in this detail level
        var xOffs = chunkX - cornerChunkX
        var zOffs = chunkZ - cornerChunkZ

        // Check if outside current range of this detail level
        if (xOffs < 0 || xOffs >= levelSizeChunks ||
            zOffs < 0 || zOffs >= levelSizeChunks) return null

        // Position in array
        xOffs += arrayOffsetX
        zOffs += arrayOffsetZ
        val arrayX = xOffs.modPositive(levelSizeChunks)
        val arrayZ = zOffs.modPositive(levelSizeChunks)
        val chunk = chunks[arrayX + arrayZ * levelSizeChunks]
        assert(chunk.chunkX == chunkX &&
               chunk.chunkZ == chunkZ) {
            "The returned chunk should have the correct position " +
            "(requested chunk at $chunkX, $chunkZ, " +
            "but got chunk with coordinates ${chunk.chunkX}, ${chunk.chunkZ})"
        }
        return chunk
    }

    /**
     * Check if we need to relocate
     */
    fun update(camera: Camera, time: Time) {
        var newCornerChunkX = cornerChunkX
        var newCornerChunkZ = cornerChunkZ
        if (level <= 0) {
            // Update based on camera
            // Map from world coordinates to chunk coordinates at this detail level
            // Align to 2x2 grids, so that it lines up with rougher LOD
            //newCornerChunkX = worldToChunkPos(camera.position.x.toDouble() - levelSizeMeters * 0.5).floorDiv(2) * 2
            //newCornerChunkZ = worldToChunkPos(camera.position.z.toDouble() - levelSizeMeters * 0.5).floorDiv(2) * 2
            newCornerChunkX = grid.getCellXFromPos(camera.position.x.toDouble() - levelSizeMeters * 0.5).div(2) * 2
            newCornerChunkZ = grid.getCellYFromPos(camera.position.z.toDouble() - levelSizeMeters * 0.5).div(2) * 2
        }
        else {
            // Update based on more detailed levels
            val innerLevel = terrainRenderer.levels[level - 1]
            val innerLevelCornerChunkX = innerLevel.cornerChunkX / 2
            val innerLevelCornerChunkZ = innerLevel.cornerChunkZ / 2

            // Reposition if needed
            while(innerLevelCornerChunkX <= newCornerChunkX + terrainRenderer.moveThresholdChunks) newCornerChunkX -= terrainRenderer.moveDistanceChunks
            while(innerLevelCornerChunkZ <= newCornerChunkZ + terrainRenderer.moveThresholdChunks) newCornerChunkZ -= terrainRenderer.moveDistanceChunks
            while(innerLevelCornerChunkX >= newCornerChunkX + terrainRenderer.innerLevelSizeChunks - terrainRenderer.moveThresholdChunks) newCornerChunkX += terrainRenderer.moveDistanceChunks
            while(innerLevelCornerChunkZ >= newCornerChunkZ + terrainRenderer.innerLevelSizeChunks - terrainRenderer.moveThresholdChunks) newCornerChunkZ += terrainRenderer.moveDistanceChunks
        }

        val chunkDeltaX = newCornerChunkX - cornerChunkX
        val chunkDeltaZ = newCornerChunkZ - cornerChunkZ
        if (chunkDeltaX != 0) {
            // Scroll X
            arrayOffsetX -= chunkDeltaX
            arrayOffsetX = arrayOffsetX.modPositive(levelSizeChunks)
        }
        if (chunkDeltaZ != 0) {
            // Scroll Z
            arrayOffsetZ -= chunkDeltaZ
            arrayOffsetZ = arrayOffsetZ.modPositive(levelSizeChunks)
        }

        // Update corner pos
        cornerChunkX = newCornerChunkX
        cornerChunkZ = newCornerChunkZ

        // Check which chunks moved, recalculate the ones that need it
        anyNeedsRecalculation = false
        for (z in cornerChunkZ until cornerChunkZ + levelSizeChunks)
            for (x in cornerChunkX until cornerChunkX + levelSizeChunks) {
                val chunkNeedsRecalculation = getChunk(x, z)?.setPos(x, z)
                    ?: throw IllegalStateException("Expected to find chunk at pos x:$x, z:$z, but detail level area ranges from $cornerChunkX, $cornerChunkZ to ${cornerChunkX + levelSizeChunks - 1}, ${cornerChunkZ + levelSizeChunks - 1}")
                anyNeedsRecalculation = chunkNeedsRecalculation || anyNeedsRecalculation
            }
    }

    fun recalculate() {
        if (anyNeedsRecalculation) {
            for (chunk in chunks) {
                chunk.recalculate()
            }
        }
    }

    fun render(context: RenderingContext3D) {

        // TODO: When flying high (or maybe fast), do not render inner levels either..
        if (level <= 0) {
            // No inner levels
            for (chunk in chunks) {
                chunk.render(context)
            }
        } else {
            // Determine where the hole for the more detailed detail level is, do not render those chunks
            val innerLevel = terrainRenderer.levels[level - 1]
            val innerLevelCornerChunkX = (innerLevel.cornerChunkX) / 2
            val innerLevelCornerChunkZ = (innerLevel.cornerChunkZ) / 2

            for (z in cornerChunkZ until cornerChunkZ + levelSizeChunks)
                for (x in cornerChunkX until cornerChunkX + levelSizeChunks) {
                    val chunkOverInnerLevels =
                        x >= innerLevelCornerChunkX && x < innerLevelCornerChunkX + terrainRenderer.innerLevelSizeChunks &&
                        z >= innerLevelCornerChunkZ && z < innerLevelCornerChunkZ + terrainRenderer.innerLevelSizeChunks

                    if (!chunkOverInnerLevels) {
                        getChunk(x, z)?.render(context) ?: throw java.lang.IllegalStateException("Expected to have a chunk at $x, $z")
                    }
                }
        }


    }

    fun dispose() {
        for (chunk in chunks) {
            chunk.dispose()
        }

    }


}