package org.fractalpixel.gameutils.voxel.renderer

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import org.fractalpixel.gameutils.libgdxutils.ShapeBuilder
import org.fractalpixel.gameutils.libgdxutils.buildWireframeBoxPart
import org.fractalpixel.gameutils.rendering.RenderingContext3D
import org.fractalpixel.gameutils.utils.MeshPool
import org.fractalpixel.gameutils.utils.Recyclable
import org.fractalpixel.gameutils.voxel.VoxelTerrain
import org.fractalpixel.gameutils.voxel.distancefunction.ConstantFun
import org.kwrench.geometry.int3.Int3
import org.kwrench.geometry.int3.MutableInt3

/**
 * Holds rendering data for a voxel chunk.
 */
class VoxelRenderChunk(val configuration: VoxelConfiguration): Recyclable {

    private val pos = MutableInt3()
    val position: Int3 get() = pos // Do not allow editing

    var terrain: VoxelTerrain = emptyTerrain
        private set

    var level: Int = 0
        private set


    private var initialShape: ShapeBuilder? = null
    private var mesh: Mesh? = null
    private var modelInstance: ModelInstance? = null

    /**
     * Initialize the position and detail level of this chunk and tell it the terrain it is located in.
     */
    fun init(
        terrain: VoxelTerrain,
        level: Int,
        pos: Int3,
        shape: ShapeBuilder
    ) {
        this.terrain = terrain
        this.level = level
        this.pos.set(pos)
        this.initialShape = shape
    }

    /**
     * Update mesh based on latest terrain distance function.
     */
    fun update() {
        // TODO: Update mesh if world edited, run in background co-routine...
    }

    /**
     * Called to render this chunk.
     * Also creates the 3D model from the shape whenever the calculation is ready.
     */
    fun render(context: RenderingContext3D) {
        initializeModelIfCalculated()

        // Render model instance if available
        val currentModelInstance = modelInstance
        if (currentModelInstance != null) {
            context.modelBatch.render(currentModelInstance, context.environment)
        }
    }

    private fun initializeModelIfCalculated() {
        // Create OpenGL mesh when a calculation is ready
        val shape = initialShape
        if (shape != null) {
            // Initialize OpenGL constructs in OpenGL thread
            createModelInstance( createMesh(shape) )

            // Release the shape as it was already used to build the mesh and is no longer needed
            releaseShape()
        }
    }

    private fun createMesh(shape: ShapeBuilder): Mesh {
        // Get or create mesh instance
        val createdMesh = meshPool.obtain(shape.vertexCount, shape.indexCount)

        // Build shape from the surface points we found
        shape.updateMesh(createdMesh, false)

        mesh = createdMesh
        return createdMesh
    }

    private fun createModelInstance(createdMesh: Mesh) {
        // Build the model and create an instance of it
        val modelBuilder = ModelBuilder()
        modelBuilder.begin()

        // Add some wireframes showing where the chunks are if requested
        var debugColor = Color.WHITE

        // TODO: Avoid calculating this if no debug visualization is on..
        val mayContainSurface = terrain.distanceFun.mayContainSurface(configuration.getChunkVolume(pos, level))
        val wireframeColor = configuration.calculateBlockLevelDebugColor(level, mayContainSurface, true)

        // TODO: (low priority) Add different debug visualization modes later if needed?  Maybe more generic system

        if (configuration.debugLines && (configuration.debugLinesForEmptyBlocks)) {
            val corner = configuration.chunkWorldCornerPos(pos, level)
            var sideLen = configuration.chunkWorldSize(level).toFloat()
            if (configuration.debugOutlines) {
                modelBuilder.buildWireframeBoxPart(corner, sideLen, color = configuration.blockEdgeDebugLineColor)
            }

            corner.add(configuration.blockTypeDebugLineSpacing * sideLen)
            sideLen *= (1f - 2f * configuration.blockTypeDebugLineSpacing)

            modelBuilder.buildWireframeBoxPart(
                corner, sideLen, color = wireframeColor
            )
        }

        // Debug visualize this:
        if (configuration.colorizeTerrainByLevel) debugColor = wireframeColor

        // Add terrain shape to model
        val material = Material()
        material.set(ColorAttribute.createDiffuse(0.8f * debugColor.r, 0.8f * debugColor.g, 0.8f * debugColor.b, 1f))
        modelBuilder.part("mesh", createdMesh, GL20.GL_TRIANGLES, material)

        // Create model from chunk shape and wireframe
        val model = modelBuilder.end()

        // Create instance of model
        modelInstance = ModelInstance(model)
    }

    override fun dispose() {
        // Same procedure as for resetting, as all meshes are pooled so they are not disposed here.
        reset()
    }

    override fun reset() {
        releaseShape()
        releaseMesh()

        terrain = emptyTerrain
        level = 0
        pos.zero()
        modelInstance = null
    }

    private fun releaseShape() {
        // Release the shape (if it is not null)
        initialShape?.let { ShapeCalculator.shapeBuilderPool.release(it) }
        initialShape = null
    }

    private fun releaseMesh() {
        // Free mesh for later re-use
        mesh?.let{ meshPool.release(it) }

        // Set it to null so that it doesn't get released again
        mesh = null
    }


    companion object {
        private val emptyTerrain = VoxelTerrain(ConstantFun(1.0))

        private val meshPool = MeshPool() // This needs to be accessed from the OpenGL thread anyway, so keep it here.



    }

}