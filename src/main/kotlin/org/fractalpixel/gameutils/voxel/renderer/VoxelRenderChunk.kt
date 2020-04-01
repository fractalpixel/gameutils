package org.fractalpixel.gameutils.voxel.renderer

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import org.fractalpixel.gameutils.libgdxutils.ShapeBuilder
import org.fractalpixel.gameutils.rendering.RenderingContext3D
import org.fractalpixel.gameutils.utils.getCoordinate
import org.fractalpixel.gameutils.utils.setCoordinate
import org.fractalpixel.gameutils.voxel.VoxelTerrain
import org.fractalpixel.gameutils.voxel.distancefunction.SphereFun
import org.kwrench.geometry.double3.Double3
import org.kwrench.geometry.double3.MutableDouble3
import org.kwrench.geometry.int3.ImmutableInt3
import org.kwrench.geometry.int3.Int3
import org.kwrench.geometry.int3.MutableInt3
import org.kwrench.properties.threadLocal
import kotlin.concurrent.thread
import kotlin.math.abs

/**
 * Holds rendering data for a voxel chunk.
 */
class VoxelRenderChunk(val terrain: VoxelTerrain,
                       val level: Int,
                       initialPos: Int3,
                       val configuration: VoxelConfiguration) {

    val pos = ImmutableInt3(initialPos) // Take a copy of the position, as the one passed as a parameter may change

    private var mesh: Mesh? = null
    private var modelInstance: ModelInstance? = null
    private val voxelVertexIndexes = ShortArray(configuration.blockCornerCountInChunk) {-1}


    fun render(context: RenderingContext3D) {
        if (modelInstance == null) build()

        val currentModelInstance = modelInstance
        if (currentModelInstance != null) {
            context.modelBatch.render(currentModelInstance, context.environment)
        }
    }


    /**
     * Create the chunk model based on the terrain distance function and the location of this chunk.
     */
    fun build() {
        println("Started building chunk $pos at level $level")
        // Create mesh
        mesh = createMesh()

        // Create modelInstance
        val modelBuilder = ModelBuilder()
        val material = Material()
        material.set(ColorAttribute.createDiffuse(1f, 1f, 1f, 1f))

        modelBuilder.begin()
        modelBuilder.part("mesh", mesh, GL20.GL_TRIANGLES, material)

        val model = modelBuilder.end()

        modelInstance = ModelInstance(model)
        println("Completed chunk $pos at level $level")
    }

    /**
     * Update mesh based on latest terrain distance function.
     */
    fun update() {
        // TODO
    }


    private fun createMesh(): Mesh {
        val shapeBuilder = shapeBuilder.get()

        val indexStepDelta = ImmutableInt3(1, configuration.chunkSize-1, (configuration.chunkSize-1) * (configuration.chunkSize-1))

        // Calculate distance values over the chunk
        val distanceCache = CachedDistances(terrain.distanceFun, configuration) // TODO: Reuse the cache in this local thread!
        distanceCache.calculate(pos, level)

        // Iterate voxel space
        val chunkWorldCorner = configuration.chunkWorldCornerPos(pos, level)
        val chunkWorldSize = configuration.chunkWorldSize(level)
        val worldStep = (chunkWorldSize / (configuration.chunkSize - 1)).toFloat()
        var xp: Float
        var yp: Float
        var index = 0
        val voxelPos = MutableInt3()
        val tempVec = Vector3() // Create these temporary values outside the loop to avoid memory trashing
        val tempPos = Vector3()

        var zp: Float = chunkWorldCorner.z
        for (z in 0 until configuration.chunkSize-1) {
            yp = chunkWorldCorner.y
            for (y in 0 until configuration.chunkSize-1) {
                xp = chunkWorldCorner.x
                for (x in 0 until configuration.chunkSize-1) {
                    val d = distanceCache.getSample(index++)

                    voxelPos.set(x, y, z)
                    calculateVertexPosition(distanceCache, index, voxelPos, xp, yp, zp, worldStep, indexStepDelta, shapeBuilder, tempVec, tempPos)

                    xp += worldStep
                }
                yp += worldStep
            }
            zp += worldStep
        }

        // Build shape from the surface points we found
        return shapeBuilder.createMesh()
    }

    private inline fun calculateVertexPosition(
        distanceCache: CachedDistances,
        index: Int,
        voxelPos: Int3,
        xp: Float,
        yp: Float,
        zp: Float,
        step: Float,
        indexStepDelta: Int3,
        shapeBuilder: ShapeBuilder,
        tempVec: Vector3,
        tempPos: Vector3
    ) {
        // Read depth field information at voxel corners, construct a mask on whether the corner is inside or outside the shape
        // TODO: This could be cached to reduce terrain depth calculations by 80-85%
        var cornerMask = 0
        var g = 0

        val voxelOffsetX = pos.x * configuration.chunkSize
        for (cz in 0 .. 1) {
            for (cy in 0 .. 1) {
                for (cx in 0 .. 1) {

                    val sample = distanceCache.getSample(voxelPos.x + cx, voxelPos.y + cy, voxelPos.z + cz)
                    voxelCornerDepths[g] = sample
                    voxelCornerPositions[g].set(
                        xp + cx * step,
                        yp + cy * step,
                        zp + cz * step
                    )
                    cornerMask = cornerMask or (if (sample > 0) 1 shl g else 0 )
                    g++
                }
            }
        }


        // Check for early termination if cell does not intersect boundary
        if (cornerMask == 0 || cornerMask == 0xff) {
            return
        }

        // get edgemask from table using our corner mask
        val edgeMask = edgeTable[cornerMask]

        var edgeCrossings = 0

        // For every edge on the cube
        tempVec.set(0f, 0f, 0f)
        for (i in 0 .. 11) {
            // Use edge mask to check if it is crossed
            if (edgeMask and (1 shl i) == 0) {
                continue
            }

            // If it did, increment number of edge crossings
            edgeCrossings++

            // Now find the point of intersection
            val e0 = cubeEdges[i shl 1]
            val e1 = cubeEdges[(i shl 1) + 1]
            val g0: Float = voxelCornerDepths[e0].toFloat()
            val g1: Float = voxelCornerDepths[e1].toFloat()
            var t: Float = g0 - g1
            if (abs(t) > 1e-6) t = g0 / t

            //Interpolate vertices and add up intersections (this can be done without multiplying)
            var k = 1
            for(j in 0..2) {
                val a = e0 and k
                val b = e1 and k
                if(a != b) {
                    tempVec.setCoordinate(j, tempVec.getCoordinate(j) + if (a != 0) 1f - t else t)
                } else {
                    tempVec.setCoordinate(j, tempVec.getCoordinate(j) + if (a != 0) 1f else 0f)
                }
                k *= 2
            }
        }

        //Now we just average the edge intersections and add them to coordinate
        val s = 1f / edgeCrossings
        tempVec.x = xp + s * step * tempVec.x
        tempVec.y = yp + s * step * tempVec.y
        tempVec.z = zp + s * step * tempVec.z
        tempPos.set(tempVec)

        // Create vertex for mesh
        val vertexIndex = shapeBuilder.addVertex(tempPos)

        // Store vertex index
        voxelVertexIndexes[index] = vertexIndex

        // Add faces
        // Now we need to add faces together, to do this we just loop over 3 basis components
        for(i in 0..2) {
            // The first three entries of the edge_mask count the crossings along the edge
            if((edgeMask and (1 shl i)) == 0) continue

            // i = axis we are pointing along.  iu, iv = orthogonal axes
            val iu = (i + 1) % 3
            val iv = (i + 2) % 3

            // If we are on a boundary, skip it
            if(voxelPos.getCoordinate(iu) == 0 || voxelPos.getCoordinate(iv) == 0) continue

            // Otherwise, look up adjacent edges in buffer
            val du = indexStepDelta.getCoordinate(iu)
            val dv = indexStepDelta.getCoordinate(iv);

            // Remember to flip orientation depending on the sign of the corner.
            if ((cornerMask and 1) != 0) {
                shapeBuilder.addQuad(
                    voxelVertexIndexes[index],
                    voxelVertexIndexes[index -du],
                    voxelVertexIndexes[index -du -dv],
                    voxelVertexIndexes[index -dv]
                )
            } else {
                shapeBuilder.addQuad(
                    voxelVertexIndexes[index],
                    voxelVertexIndexes[index -dv],
                    voxelVertexIndexes[index -du -dv],
                    voxelVertexIndexes[index -du]
                )
            }
        }
    }



    companion object {
        private val shapeBuilder = threadLocal { ShapeBuilder() }

        private val cubeEdges = IntArray(24)
        private val edgeTable = IntArray(256)
        private val voxelCornerDepths = DoubleArray(8)
        private val voxelCornerPositions = Array<Vector3>(8) { Vector3() }



        init {
            initializeCubeEdgesTable()
            initializeIntersectionTable()
        }


        private fun initializeCubeEdgesTable() {
            /**
             * Utility function to build a table of possible edges for a cube with each
             * pair of points representing one edge i.e. [0,1,0,2,0,4,...] would be the
             * edges from points 0 to 1, 0 to 2, and 0 to 4 respectively:
             *
             *  y         z
             *  ^        /
             *  |
             *    6----7
             *   /|   /|
             *  4----5 |
             *  | 2--|-3
             *  |/   |/
             *  0----1   --> x
             */
            var k = 0
            for (i in 0..7) {
                var j = 1
                while (j <= 4) {
                    val p = i xor j
                    if (i <= p) {
                        cubeEdges[k++] = i
                        cubeEdges[k++] = p
                    }
                    j = j shl 1
                }
            }
        }


        /**
         * Build an intersection table. This is a 2^(cube config) -> 2^(edge config) map
         * There is only one entry for each possible cube configuration
         * and the output is a 12-bit vector enumerating all edges
         * crossing the 0-level
         */
        private fun initializeIntersectionTable() {
            for (i in 0..255) {
                var edgeMask: Int = 0
                var j = 0
                while (j < 24) {
                    val a = (i and (1 shl cubeEdges[j])) != 0
                    val b = (i and (1 shl cubeEdges[j + 1])) != 0
                    edgeMask = edgeMask or if (a != b) 1 shl (j shr 1) else 0
                    j += 2
                }
                edgeTable[i] = edgeMask
            }
        }
    }

}