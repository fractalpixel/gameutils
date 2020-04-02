package org.fractalpixel.gameutils.voxel.renderer

import com.badlogic.gdx.math.Vector3
import org.fractalpixel.gameutils.rendering.RenderingContext3D
import org.fractalpixel.gameutils.utils.iterate
import org.fractalpixel.gameutils.voxel.VoxelTerrain
import org.kwrench.geometry.int3.ImmutableInt3
import org.kwrench.geometry.int3.MutableInt3

/**
 * Holds all voxel chunks at a specific detail level.
 */
class VoxelDetailLevel(val terrain: VoxelTerrain,
                       val level: Int,
                       val configuration: VoxelConfiguration) {


    private val chunks = ArrayList<VoxelRenderChunk?>(configuration.chunkCountInLevel)
    private val visibleAreaCorner = MutableInt3()
    private val newVisibleAreaCorner = MutableInt3()
    private val newCornerPos = Vector3()

    init {
        for (i in 0 until configuration.chunkCountInLevel) {
            chunks.add(null)
        }
    }


    fun updateCameraPos(pos: Vector3) {
        // TODO: Create / dispose chunks as needed.  Ideally in background thread...

        // Determine chunks that are visible
        configuration.getLevelCornerChunk(pos, level, newVisibleAreaCorner)

        // TODO: Translate chunk array / copy moved chunks and null empty chunks

        // Create chunks for missing places
        configuration.iterateLevel(newVisibleAreaCorner) {chunkPos, chunkIndex ->
            if (chunks[chunkIndex] == null) {
                chunks[chunkIndex] = VoxelRenderChunk(terrain, level, chunkPos, configuration)
            }
        }
    }

    fun render(context: RenderingContext3D) {
        // TODO: Uncomment later to focus on camera
        //updateCameraPos(context.camera.position)
        // Focus center:
        updateCameraPos(Vector3.Zero)

        for (chunk in chunks) {
            chunk?.render(context)
        }
    }

}