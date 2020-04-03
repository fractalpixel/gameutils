package org.fractalpixel.gameutils.voxel.renderer

import com.badlogic.gdx.math.Vector3
import org.fractalpixel.gameutils.rendering.RenderingContext3D
import org.fractalpixel.gameutils.utils.RecyclingPool
import org.fractalpixel.gameutils.voxel.VoxelTerrain
import org.kwrench.geometry.int3.MutableInt3

/**
 * Holds all voxel chunks at a specific detail level.
 */
// TODO: Have extra non-visible layer in each direction, where the chunks are being asynchronously loaded,
//       so that when they get into view, most/all are already loaded.
class VoxelDetailLevel(
    val terrain: VoxelTerrain,
    val level: Int,
    val configuration: VoxelConfiguration,
    val chunkPool: RecyclingPool<VoxelRenderChunk>
) {

    private val visibleAreaCorner = MutableInt3()

    private val chunkBuffer = PanBuffer(
        configuration.levelExtent,
        disposer = chunkPool::release) { pos ->
        // Calculate new chunk for position
        // TODO: This should be done asynchronously
        val chunk = chunkPool.obtain()
        chunk.init(terrain, level, pos)
        chunk
    }

    fun updateCameraPos(pos: Vector3) {

        // Determine new corner position for the visible chunks in this level
        configuration.getLevelCornerChunk(pos, level, visibleAreaCorner)

        // Update chunk buffer global position, rolling it as necessary and starting calculation of new chunks
        chunkBuffer.setPosition(visibleAreaCorner)
    }

    fun render(context: RenderingContext3D) {
        updateCameraPos(context.camera.position)
        // DEBUG: Focus center:
        //updateCameraPos(Vector3.Zero)

        chunkBuffer.iterate() { pos, chunk ->
            chunk.render(context)
        }
    }

}