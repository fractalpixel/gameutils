package org.fractalpixel.gameutils.voxel.renderer

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import kotlinx.coroutines.*
import org.fractalpixel.gameutils.libgdxutils.ShapeBuilder
import org.fractalpixel.gameutils.libgdxutils.buildWireframeBoxPart
import org.fractalpixel.gameutils.rendering.RenderingContext3D
import org.fractalpixel.gameutils.utils.MeshPool
import org.fractalpixel.gameutils.utils.Recyclable
import org.fractalpixel.gameutils.utils.RecyclingPool
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

    private var shapeFuture: Deferred<ShapeBuilder?>? = null
    private var mesh: Mesh? = null
    private var modelInstance: ModelInstance? = null

    /**
     * Initialize the position and detail level of this chunk and tell it the terrain it is located in.
     */
    fun init(terrain: VoxelTerrain, level: Int, pos: Int3) {
        this.terrain = terrain
        this.level = level
        this.pos.set(pos)
    }

    /**
     * Starts building the shape of this chunk in the background.
     * Returns immediately, sets the [meshCalculated] flag to false when ready.
     */
    fun calculateShapeInBackground(shapeCalculatorPool: RecyclingPool<ShapeCalculator>) {
        shapeFuture = GlobalScope.async {
            // Get a mesh calculator instance (has various memory structures used during calculation)
            val meshCalculator = shapeCalculatorPool.obtain()
            try {
                // Create shape
                meshCalculator.buildShape(terrain, pos, level)
            } finally {
                // Release calculator, mark calculation finished
                shapeCalculatorPool.release(meshCalculator)
            }
        }
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
        val job = shapeFuture
        if (job?.isCompleted == true) {
            // Set job to null so that we only enter this if-block once
            shapeFuture = null

            // Get calculated shape
            val shape: ShapeBuilder? = job.getCompleted()

            // Initialize OpenGL constructs in OpenGL thread
            createMesh(shape)
            createModelInstance()

            // Release the shape as it was already used to build the mesh and is no longer needed
            releaseShape(shape)
        }
    }

    private fun createMesh(shape: ShapeBuilder?) {
        mesh = if (shape != null) {
            // Get or create mesh instance
            val createdMesh = meshPool.obtain(shape.vertexCount, shape.indexCount)

            // Build shape from the surface points we found
            shape.updateMesh(createdMesh, false)
            createdMesh
        } else {
            // Empty shape, no mesh required
            null
        }
    }

    private fun createModelInstance() {
        modelInstance = if (mesh == null && !configuration.debugLines) {
            // In this case we have nothing to render, so keep model instance null
            null
        } else {
            // Build the model and create an instance of it
            val modelBuilder = ModelBuilder()
            modelBuilder.begin()

            // Add terrain shape to model
            val hasSurface = mesh != null
            if (hasSurface) {
                val material = Material()
                material.set(ColorAttribute.createDiffuse(0.8f, 0.8f, 0.8f, 1f))
                modelBuilder.part("mesh", mesh, GL20.GL_TRIANGLES, material)
            }

            // Add some wireframes showing where the chunks are if requested
            if (configuration.debugLines && (configuration.debugLinesForEmptyBlocks || hasSurface)) {
                val corner = configuration.chunkWorldCornerPos(pos, level)
                var sideLen = configuration.chunkWorldSize(level).toFloat()
                modelBuilder.buildWireframeBoxPart(corner, sideLen, color = configuration.blockEdgeDebugLineColor)

                corner.add(configuration.blockTypeDebugLineSpacing * sideLen)
                sideLen *= (1f - 2f * configuration.blockTypeDebugLineSpacing)

                // Debug visualize this:  TODO: (low priority) Add different debug visualization modes later if needed?  Maybe more generic system
                val mayContainSurface = terrain.distanceFun.mayContainSurface(configuration.getChunkVolume(pos, level))
                modelBuilder.buildWireframeBoxPart(
                    corner, sideLen, color = configuration.calculateBlockLevelDebugColor(
                        level, mayContainSurface, hasSurface
                    )
                )
            }

            // Create model from chunk shape and wireframe
            val model = modelBuilder.end()

            // Create instance of model
            ModelInstance(model)
        }
    }

    override fun dispose() {
        // Same procedure as for resetting, as all meshes are pooled so they are not disposed here.
        reset()
    }

    override fun reset() {
        cancelAnyPendingCalculation()
        releaseMesh()

        terrain = emptyTerrain
        level = 0
        pos.zero()
        modelInstance = null
    }

    private fun cancelAnyPendingCalculation() {
        runBlocking { shapeFuture?.cancelAndJoin() }
        shapeFuture = null
    }

    private fun releaseShape(shape: ShapeBuilder?) {
        // Release the shape (if it is not null)
        shape?.let { ShapeCalculator.shapeBuilderPool.release(it) }
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