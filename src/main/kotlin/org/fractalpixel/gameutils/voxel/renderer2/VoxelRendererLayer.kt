package org.fractalpixel.gameutils.voxel.renderer2

import org.fractalpixel.gameutils.layer.Layer3D
import org.fractalpixel.gameutils.layer.layers.ShaderLayer
import org.fractalpixel.gameutils.rendering.RenderingContext2D
import org.fractalpixel.gameutils.rendering.RenderingContext3D
import org.fractalpixel.gameutils.utils.RecyclingPool
import org.fractalpixel.gameutils.voxel.VoxelTerrain
import org.fractalpixel.gameutils.voxel.renderer2.impl.*
import org.fractalpixel.gameutils.voxel.renderer2.impl.shader.*


/**
 * Renders a voxel terrain using raymarching.
 */
// TODO: Pass in 3D camera in constructor, make possible to change mid-flight too.
class VoxelRendererLayer(val terrain: VoxelTerrain,
                         val voxelConfiguration: VoxelConfiguration = VoxelConfiguration()): ShaderLayer(VoxelRayMarchingShader()) {

    private val detailLevels = ArrayList<VoxelDetailLevel>()

    private val chunkPool = RecyclingPool(
        VoxelRenderChunk::class,
        createInstance = { VoxelRenderChunk(voxelConfiguration) })

    init {
        var previousLevel: VoxelDetailLevel? = null
        for (level in voxelConfiguration.detailLevelsRange) {
            val detailLevel = VoxelDetailLevel(terrain, level, voxelConfiguration, chunkPool, previousLevel)
            detailLevels.add(detailLevel)
            previousLevel = detailLevel
        }
    }



}
