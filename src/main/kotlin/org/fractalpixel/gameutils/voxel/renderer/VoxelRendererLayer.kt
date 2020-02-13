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
// TODO: Add materials & texturing
class VoxelRendererLayer(val terrain: VoxelTerrain): Layer3D() {


    val shapeBuilder = ShapeBuilder()
    var mesh: Mesh? = null
    var modelInstance: ModelInstance? = null
    private val voxelVertexIndexes = ShortArray(terrain.totalSize) {-1}

    fun build() {
        // Create mesh
        mesh = createMesh(shapeBuilder)

        // Create modelInstance
        val modelBuilder = ModelBuilder()
        val material = Material()
        material.set(ColorAttribute.createDiffuse(1f, 1f, 1f, 1f))

        modelBuilder.begin()
        modelBuilder.part("mesh", mesh, GL20.GL_TRIANGLES, material)

        val model = modelBuilder.end()

        modelInstance = ModelInstance(model)
    }

    private fun createMesh(shapeBuilder: ShapeBuilder): Mesh {

        val indexStepDelta = ImmutableInt3(1, terrain.voxelSize-1, (terrain.voxelSize-1) * (terrain.voxelSize-1))

        // Iterate voxel space
        val worldStep = (terrain.worldSize / (terrain.voxelSize - 1)).toFloat()
        var xp: Float
        var yp: Float
        var zp: Float = (terrain.worldCenter.z - terrain.worldSize * 0.5).toFloat()
        var index = 0
        val voxelPos = MutableInt3()
        for (z in 0 until terrain.voxelSize-1) {
            yp = (terrain.worldCenter.y - terrain.worldSize * 0.5).toFloat()
            for (y in 0 until terrain.voxelSize-1) {
                xp = (terrain.worldCenter.y - terrain.worldSize * 0.5).toFloat()
                for (x in 0 until terrain.voxelSize-1) {
                    val d = terrain.distance[index++]

                    voxelPos.set(x, y, z)
                    calculateVertexPosition(terrain, index, voxelPos, xp, yp, zp, worldStep, indexStepDelta)

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
        terrain: VoxelTerrain,
        index: Int,
        voxelPos: Int3,
        xp: Float,
        yp: Float,
        zp: Float,
        step: Float,
        indexStepDelta: Int3
    ) {
        // Read depth field information at voxel corners, construct a mask on whether the corner is inside or outside the shape
        var cornerMask = 0
        var g = 0
        for (cz in 0 .. 1) {
            for (cy in 0 .. 1) {
                for (cx in 0 .. 1) {
                    val sample = terrain.getSample(voxelPos.x + cx, voxelPos.y + cy, voxelPos.z + cz)
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
        tempV.set(0f, 0f, 0f)
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
                    tempV.setCoordinate(j, tempV.getCoordinate(j) + if (a != 0) 1f - t else t)
                } else {
                    tempV.setCoordinate(j, tempV.getCoordinate(j) + if (a != 0) 1f else 0f)
                }
                k *= 2
            }
        }

        //Now we just average the edge intersections and add them to coordinate
        val s = 1f / edgeCrossings
        tempV.x = xp + s * step * tempV.x
        tempV.y = yp + s * step * tempV.y
        tempV.z = zp + s * step * tempV.z
        tempVertexPos.set(tempV)

        // Create vertex for mesh
        val vertexIndex = shapeBuilder.addVertex(tempVertexPos)

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



    override fun render(context: RenderingContext3D) {
        if (modelInstance == null) build()

        val currentModelInstance = modelInstance
        if (currentModelInstance != null) {
            context.modelBatch.render(currentModelInstance, context.environment)
        }
    }

    companion object {
        private val cubeEdges = IntArray(24)
        private val edgeTable = IntArray(256)
        private val voxelCornerDepths = DoubleArray(8)
        private val voxelCornerPositions = Array<Vector3>(8) { Vector3() }
        private val tempVertexPos = Vector3()
        private val tempV = Vector3()

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