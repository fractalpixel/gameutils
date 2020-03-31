package org.fractalpixel.gameutils.voxel.renderer

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import org.fractalpixel.gameutils.layer.Layer3D
import org.fractalpixel.gameutils.libgdxutils.ShapeBuilder
import org.fractalpixel.gameutils.rendering.RenderingContext3D
import org.fractalpixel.gameutils.utils.getCoordinate
import org.fractalpixel.gameutils.utils.setCoordinate
import org.fractalpixel.gameutils.voxel.VoxelTerrain
import org.kwrench.geometry.int3.ImmutableInt3
import org.kwrench.geometry.int3.Int3
import org.kwrench.geometry.int3.MutableInt3
import kotlin.math.abs


/**
 * Renders a voxel terrain using Naive Surface Nets.
 *
 * Based on Naive Surface Nets as described on 0fps: https://0fps.net/2012/07/12/smooth-voxel-terrain-part-2/
 * Referenced code:
 *   https://github.com/mikolalysenko/mikolalysenko.github.com/blob/master/Isosurface/js/surfacenets.js
 *   and https://github.com/TomaszFoster/NaiveSurfaceNets/blob/master/NaiveSurfaceNets.cs
 */
// TODO: Convert to chunking approach, restrict number of voxels in chunk to less than size of Short, so that it works
//       even in the worst case where a vertex is generated for each voxel (32*32*32 size should work with signed shorts,
//       if vertexes are 0-based (check)).
// TODO: Add level of detail
// TODO: Add materials & texturing (blend between 3 most present materials or similar - keep track of material amounts
//       in small integer values (grams / milliliters)? -> no risk of creating / destroying matter)
class VoxelRendererLayer(val terrain: VoxelTerrain,
                         val smallestDetailLevel: Int = -2,
                         val detailLevelCount: Int = 12): Layer3D() {

    private val detailLevels = ArrayList<VoxelDetailLevel>()

    init {
        for (level in smallestDetailLevel until smallestDetailLevel + detailLevelCount) {
            detailLevels.add(VoxelDetailLevel(terrain, level))
        }
    }

    override fun render(context: RenderingContext3D) {
        for (detailLevel in detailLevels) {
            detailLevel.render(context)
        }
    }
}