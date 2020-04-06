package org.fractalpixel.gameutils.voxel.renderer

import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider
import org.fractalpixel.gameutils.layer.Layer3D
import org.fractalpixel.gameutils.libgdxutils.loadShaderProvider
import org.fractalpixel.gameutils.rendering.RenderingContext3D
import org.fractalpixel.gameutils.utils.RecyclingPool
import org.fractalpixel.gameutils.voxel.VoxelTerrain


/**
 * Renders a voxel terrain using Naive Surface Nets.
 *
 * Based on Naive Surface Nets as described on 0fps: https://0fps.net/2012/07/12/smooth-voxel-terrain-part-2/
 * Referenced code:
 *   https://github.com/mikolalysenko/mikolalysenko.github.com/blob/master/Isosurface/js/surfacenets.js
 *   and https://github.com/TomaszFoster/NaiveSurfaceNets/blob/master/NaiveSurfaceNets.cs
 */
// TODO: Add level of detail
// TODO: Add materials & texturing (blend between 3 most present materials or similar - keep track of material amounts
//       in small integer values (grams / milliliters)? -> no risk of creating / destroying matter)
// TODO: Some kind of immediate mode (debugging) UI, that can show e.g. memory usage graphs and other stats.
class VoxelRendererLayer(val terrain: VoxelTerrain,
                         val voxelConfiguration: VoxelConfiguration = VoxelConfiguration()
): Layer3D(loadShaderProvider("shaders/default")) {

    private val detailLevels = ArrayList<VoxelDetailLevel>()

    private val chunkPool = RecyclingPool(
        VoxelRenderChunk::class,
        createInstance = {VoxelRenderChunk(voxelConfiguration)})

    private val meshCalculatorPool = RecyclingPool(ShapeCalculator::class, createInstance = {
        ShapeCalculator(voxelConfiguration)
    })

    init {
        var previousLevel: VoxelDetailLevel? = null
        for (level in voxelConfiguration.detailLevelsRange) {
            val detailLevel = VoxelDetailLevel(terrain, level, voxelConfiguration, chunkPool, meshCalculatorPool, previousLevel)
            detailLevels.add(detailLevel)
            previousLevel = detailLevel
        }
    }

    override fun render(context: RenderingContext3D) {
        for (detailLevel in detailLevels) {
            detailLevel.render(context)
        }
    }

}