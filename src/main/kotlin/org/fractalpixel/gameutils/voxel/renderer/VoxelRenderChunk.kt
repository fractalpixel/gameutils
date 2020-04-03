package org.fractalpixel.gameutils.voxel.renderer

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import org.fractalpixel.gameutils.libgdxutils.ShapeBuilder
import org.fractalpixel.gameutils.libgdxutils.buildWireframeBoxPart
import org.fractalpixel.gameutils.rendering.RenderingContext3D
import org.fractalpixel.gameutils.utils.Recyclable
import org.fractalpixel.gameutils.utils.getCoordinate
import org.fractalpixel.gameutils.utils.setCoordinate
import org.fractalpixel.gameutils.voxel.VoxelTerrain
import org.fractalpixel.gameutils.voxel.distancefunction.ConstantFun
import org.kwrench.geometry.int3.ImmutableInt3
import org.kwrench.geometry.int3.Int3
import org.kwrench.geometry.int3.MutableInt3
import org.kwrench.properties.threadLocal
import kotlin.math.abs

/**
 * Holds rendering data for a voxel chunk.
 */
// TODO: Reuse models, allocate some extra vertexes, if they are not enough re-allocate it.
// TODO: If chunk is just air or solid, do not update/create a model for it and do not render it?
// TODO: Meshcalculator classes, with data arrays used during calculation but not later, put in pool and give each thread/worker one
class VoxelRenderChunk(val configuration: VoxelConfiguration): Recyclable {

    private val pos = MutableInt3()
    val position: Int3 get() = pos // Do not allow editing

    var terrain: VoxelTerrain = emptyTerrain
        private set

    var level: Int = 0
        private set

    private var modelInstance: ModelInstance? = null

    fun init(terrain: VoxelTerrain,
             level: Int,
             pos: Int3) {
        this.terrain = terrain
        this.level = level
        this.pos.set(pos)
    }

    fun buildChunk(meshCalculator: MeshCalculator) {
        // Create mesh
        val mesh = meshCalculator.createMesh(terrain, pos, level)

        if (mesh == null && !configuration.debugLines) {
            // In this case we have nothing to render, so keep fields null
            modelInstance = null
            return
        }

        // Create modelInstance
        val modelBuilder = ModelBuilder()

        modelBuilder.begin()

        // Include mesh only if it has some content
        if (mesh != null) {
            val material = Material()
            material.set(ColorAttribute.createDiffuse(1f, 1f, 1f, 1f))
            modelBuilder.part("mesh", mesh, GL20.GL_TRIANGLES, material)
        }

        // Add debug wireframe if requested
        if (configuration.debugLines) {
            val corner = configuration.chunkWorldCornerPos(pos, level)
            var sideLen = configuration.chunkWorldSize(level).toFloat()
            modelBuilder.buildWireframeBoxPart(corner, sideLen, color = configuration.blockEdgeDebugLineColor)

            corner.add(configuration.blockTypeDebugLineSpacing * sideLen)
            sideLen *= (1f - 2f * configuration.blockTypeDebugLineSpacing)
            modelBuilder.buildWireframeBoxPart(corner, sideLen, color = configuration.calculateBlockLevelDebugColor(level))
        }

        val model = modelBuilder.end()

        modelInstance = ModelInstance(model)
    }

    fun render(context: RenderingContext3D) {
        val currentModelInstance = modelInstance
        if (currentModelInstance != null) {
            context.modelBatch.render(currentModelInstance, context.environment)
        }
    }


    /**
     * Update mesh based on latest terrain distance function.
     */
    fun update() {
        // TODO: Update mesh of world edited
    }

    override fun reset() {
        terrain = emptyTerrain
        level = 0
        pos.zero()

        // TODO: Re-use model mesh too if it would fit the structure..
        modelInstance?.model?.dispose()
        modelInstance = null
    }

    override fun dispose() {
        reset()
    }




    companion object {
        private val emptyTerrain = VoxelTerrain(ConstantFun(1.0))
    }

}