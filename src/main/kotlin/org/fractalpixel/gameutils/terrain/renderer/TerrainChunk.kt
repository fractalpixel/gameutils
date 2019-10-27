package org.fractalpixel.gameutils.terrain.renderer

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import org.fractalpixel.gameutils.rendering.RenderingContext3D
import org.kwrench.math.clampTo
import org.kwrench.math.map


/**
 * Square area of terrain at some level of detail.  Used in renderer.
 */
// TODO: Samples are more to the positive x and z in larger levels..  Some kind of sampling bias issue.  Fix.
class TerrainChunk(val terrainRenderer: TerrainRendererLayer,
                   val detailLevel: DetailLevel
) {

    /**
     * Global chunk x position of this chunk at the detail level it exists at.
     */
    var chunkX: Int = 0
        private set

    /**
     * Global chunk z position of this chunk at the detail level it exists at.
     */
    var chunkZ: Int = 0
        private set

    /**
     * If true, the chunk content and model will be recalculated at the next update.
     */
    var requiresRecalculation = true

    val sizeCells = terrainRenderer.chunkSizeInCells
    val cellSizeMeter = detailLevel.chunkSizeMeters / sizeCells

    val worldMinX get() = detailLevel.grid.getCellXPosFromCell(chunkX)// + detailLevel.chunkSizeMeters
    val worldMaxX get() = detailLevel.grid.getCellXPosFromCell(chunkX + 1)// + detailLevel.chunkSizeMeters
    val worldMinZ get() = detailLevel.grid.getCellYPosFromCell(chunkZ)// + detailLevel.chunkSizeMeters
    val worldMaxZ get() = detailLevel.grid.getCellYPosFromCell(chunkZ + 1)// + detailLevel.chunkSizeMeters

    private val meshSize = sizeCells + 3
    private val heightSize = sizeCells + 3
    private val heightData = Array(heightSize * heightSize) {0f}

    private var mesh: Mesh? = null
    private var modelInstance: ModelInstance? = null

    /**
     * Returns true if needs recalculation
     */
    fun setPos(newChunkX: Int, newChunkZ: Int): Boolean {
        if (newChunkX != chunkX ||
            newChunkZ != chunkZ) {

            // Update position
            this.chunkX = newChunkX
            this.chunkZ = newChunkZ

            // We need recalculation
            requiresRecalculation = true
        }

        return requiresRecalculation
    }

    fun render(context: RenderingContext3D) {
        // Render chunk surface
        val currentModelInstance = modelInstance
        if (currentModelInstance != null) {
            context.modelBatch.render(currentModelInstance, context.environment)
        }
    }

    fun recalculate() {
        // Recalculate chunk from terrain function if it needs it
        if (requiresRecalculation) {
            requiresRecalculation = false

            // Get local heightfield
            retrieveHeightData()

            // Create mesh if we do not have it
            ensureMeshCreated()

            // Update vertexes and normals
            updateMesh()
        }
    }

    /**
     * Update vertexes and normals of mesh using current data
     */
    private fun updateMesh() {
        val builder = detailLevel.terrainRenderer.shapeBuilder
        builder.clear()

        // Add vertexes
        val skirtY = (terrainRenderer.skirtPercent * detailLevel.chunkSizeMeters).toFloat()
        val pos = Vector3()
        val normal = Vector3(0f, 1f, 0f)
        val normalHeight = 2f * cellSizeMeter.toFloat()
        val minX = worldMinX.toFloat()
        val maxX = worldMaxX.toFloat()
        val minZ = worldMinZ.toFloat()
        val maxZ = worldMaxZ.toFloat()
        for (z in 0 until meshSize) {
            for (x in 0 until meshSize) {
                // Determine position
                pos.x = map(x.toFloat(), 1f, meshSize - 2f, minX, maxX, clamp = true)
                pos.z = map(z.toFloat(), 1f, meshSize - 2f, minZ, maxZ, clamp = true)
                pos.y = heightAt(x, z)

                // Apply skirt at edges
                if (x == 0 || x == meshSize - 1 ||
                    z == 0 || z == meshSize - 1) {
                    pos.y = heightAt(x.clampTo(1, meshSize - 2),
                                     z.clampTo(1, meshSize - 2))
                    pos.y -= skirtY

                    // Skirt normal is just up
                    normal.set(0f, 1f, 0f)
                }
                else {
                    // Determine normal for non-skirt nodes
                    val hx1 = heightAt(x - 1, z)
                    val hx2 = heightAt(x + 1, z)
                    val hz1 = heightAt(x, z - 1)
                    val hz2 = heightAt(x, z + 1)
                    normal.x = hx1 - hx2
                    normal.z = hz1 - hz2
                    normal.y = normalHeight
                    normal.nor()
                }

                // Copy inside normal for skirt normals
                // TODO

                builder.addVertex(pos, normal)
            }
        }

        // Update mesh
        builder.updateMeshVertices(mesh!!)
    }

    /**
     * Get height data from terrain function
     */
    private fun retrieveHeightData() {
        var index = 0
        val terrain = detailLevel.terrainRenderer.terrain
        val stepMeters = detailLevel.chunkSizeMeters / sizeCells
        val worldX1 = worldMinX - stepMeters
        var worldX : Double
        var worldZ = worldMinZ - stepMeters
        for (cellZ in 0 until heightSize) {
            worldZ += stepMeters
            worldX = worldX1
            for (cellX in 0 until heightSize) {
                worldX += stepMeters
                heightData[index++] = terrain.getHeight(worldX, worldZ).toFloat()
            }
        }
    }

    private fun ensureMeshCreated() {
        if (mesh == null) {

            val builder = detailLevel.terrainRenderer.shapeBuilder
            builder.clear()

            // Add vertexes
            val pos = Vector3()
            val normal = Vector3(0f, 1f, 0f)
            for (z in 0 until meshSize) {
                for (x in 0 until meshSize) {
                    builder.addVertex(pos, normal)
                }
            }

            // Add faces
            for (z in 0 until meshSize-1) {
                for (x in 0 until meshSize-1) {
                    val a = vertexIndex(x, z)
                    val b = vertexIndex(x + 1, z)
                    val c = vertexIndex(x + 1, z + 1)
                    val d = vertexIndex(x, z + 1)
                    builder.addTriangle(a, d, c, updateNormals = false)
                    builder.addTriangle(b, a, c, updateNormals = false)
                }
            }

            // Create mesh
            mesh = builder.createMesh(false)

            // Create modelInstance
            val modelBuilder = ModelBuilder()
            val material = Material()
            material.set(ColorAttribute.createDiffuse(0.5f, 0.6f, 0.55f, 1f))
            modelBuilder.begin()
            modelBuilder.part("mesh", mesh, GL20.GL_TRIANGLES, material)
            val model = modelBuilder.end()
            modelInstance = ModelInstance(model)
        }
    }

    private inline fun heightAt(x: Int, z: Int): Float = heightData[x + z * heightSize]

    private inline fun vertexIndex(x: Int, z: Int): Short = (x + z * meshSize).toShort()

    fun dispose() {
        // Reset the state, free mesh
        modelInstance?.model?.dispose() // Will dispose the mesh also
        mesh = null
        modelInstance = null
        requiresRecalculation = true
    }

}