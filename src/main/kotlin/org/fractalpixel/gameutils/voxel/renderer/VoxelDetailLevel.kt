package org.fractalpixel.gameutils.voxel.renderer

import com.badlogic.gdx.math.Vector3
import org.fractalpixel.gameutils.rendering.RenderingContext3D
import org.fractalpixel.gameutils.voxel.VoxelTerrain

/**
 * Holds all voxel chunks at a specific detail level.
 */
class VoxelDetailLevel(val terrain: VoxelTerrain,
                       val level: Int) {

    private val chunks = ArrayList<VoxelRenderChunk>()

    fun updateCameraPos(pos: Vector3) {
        // TODO: Create / dispose chunks as needed.  Ideally in background thread...

        // DEBUG:
        if (chunks.isEmpty()) chunks.add(VoxelRenderChunk(terrain, VoxelChunkPos(0,0,0, level)))
    }

    fun render(context: RenderingContext3D) {
        updateCameraPos(context.camera.position)

        for (chunk in chunks) {
            chunk.render(context)
        }
    }

}