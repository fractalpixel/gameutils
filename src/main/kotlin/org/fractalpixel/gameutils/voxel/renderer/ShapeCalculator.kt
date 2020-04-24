package org.fractalpixel.gameutils.voxel.renderer

import com.badlogic.gdx.math.Vector3
import kotlinx.coroutines.*
import org.fractalpixel.gameutils.libgdxutils.ShapeBuilder
import org.fractalpixel.gameutils.libgdxutils.setMax
import org.fractalpixel.gameutils.utils.*
import org.fractalpixel.gameutils.voxel.VoxelTerrain
import org.fractalpixel.gameutils.voxel.distancefunction.DistanceFun
import org.fractalpixel.gameutils.voxel.distancefunction.utils.DepthBlock
import org.kwrench.geometry.int3.ImmutableInt3
import org.kwrench.geometry.int3.Int3
import org.kwrench.geometry.int3.MutableInt3
import org.kwrench.geometry.volume.MutableVolume
import org.kwrench.geometry.volume.Volume
import kotlin.math.abs

/**
 * Calculates the geometry of a chunk, but does not actually create OpenGL objects, to allow this
 * to be calculated asynchronously in multiple threads.
 *
 * This class holds various temporary data structures that can be re-used for further calculations,
 * avoiding frequent memory allocation and de-allocation.
 */
class ShapeCalculator(private val configuration: VoxelConfiguration) {

    private val voxelVertexIndexes = ShortArray(configuration.blockCornerCountInChunk) {-1}
    private val voxelCornerDepths = DoubleArray(8)
    private val voxelCornerPositions = Array<Vector3>(8) { Vector3() }

    /**
     * Create shape data for the specified chunk position and level, given the specified terrain function.
     * Returns null if the chunk does not have any surfaces (completely solid or air).
     */
    suspend fun buildShape(terrain: VoxelTerrain, pos: Int3, level: Int): ShapeBuilder? {
        val worldStep = configuration.blockWorldSize(level).toFloat()
        val distanceFun = terrain.distanceFun

        // Determine if the chunk is all empty or solid using min and max bounds for the volume,
        // for quick skipping of chunks without content.
        val chunkVolume = MutableVolume()
        configuration.getChunkVolume(pos, level, chunkVolume)
        if (!distanceFun.mayContainSurface(chunkVolume, worldStep.toDouble())) return null

        // Obtain a DepthBlock
        configuration.depthBlockPool.withObtained {
            // The depth block will be automatically released when this block exits
            val depthBlock = it

            // Calculate distance values over the chunk
            distanceFun.calculateBlock(
                configuration.getChunkSamplingVolume(pos, level),
                depthBlock,
                configuration.depthBlockPool,
                configuration.leadingSeam,
                configuration.trailingSeam
            )

            // No need to calculate a mesh if all points are inside or outside the terrain
            // Exception for if we want to see those empty blocks
            if (!depthBlock.containsSurface() && !configuration.debugLinesForEmptyBlocks) return null

            // Obtain shape builder
            val shapeBuilder = shapeBuilderPool.obtain()

            // Use wireframe for shape if debug wireframe view is on (not possible to toggle quickly without rebuilding shapes)
            shapeBuilder.wireframe = configuration.wireframeTerrain

            // Iterate voxel space
            val sideCellCount = configuration.chunkCornersSize - 1
            val indexStepDelta = ImmutableInt3(1, sideCellCount, (sideCellCount) * (sideCellCount))
            val chunkWorldCorner = configuration.chunkWorldCornerPos(pos, level)
            var xp: Float
            var yp: Float
            var index = 0
            val voxelPos = MutableInt3()
            val tempVec = Vector3() // Create these temporary values outside the loop to avoid memory trashing
            val tempPos = Vector3()
            val tempNormal = Vector3()
            var zp: Float = chunkWorldCorner.z - worldStep
            for (z in 0 until sideCellCount) {

                // Check for job cancellation here (not in innermost loop)
                if (isCurrentJobCanceled()) {
                    shapeBuilderPool.release(shapeBuilder)
                    throw CancellationException("Mesh calculation cancelled")
                }

                yp = chunkWorldCorner.y - worldStep
                for (y in 0 until sideCellCount) {

                    xp = chunkWorldCorner.x - worldStep
                    for (x in 0 until sideCellCount) {

                        voxelPos.set(x, y, z)
                        calculateVertexPosition(
                            shapeBuilder,
                            distanceFun,
                            depthBlock,
                            index,
                            voxelPos,
                            xp,
                            yp,
                            zp,
                            worldStep,
                            indexStepDelta,
                            tempVec,
                            tempPos,
                            tempNormal,
                            chunkVolume
                        )

                        index++

                        xp += worldStep
                    }
                    yp += worldStep
                }
                zp += worldStep
            }

            return shapeBuilder
        }
    }

    private suspend inline fun calculateVertexPosition(
        shapeBuilder: ShapeBuilder,
        terrainFun: DistanceFun,
        depthBlock: DepthBlock,
        index: Int,
        voxelPos: Int3,
        xp: Float,
        yp: Float,
        zp: Float,
        worldStep: Float,
        indexStepDelta: Int3,
        tempVec: Vector3,
        tempPos: Vector3,
        tempNormal: Vector3,
        chunkVolume: Volume
    ) {
        // Read depth field information at voxel corners, construct a mask on whether the corner is inside or outside the shape
        var cornerMask = 0
        var g = 0

        for (cz in 0 .. 1) {
            for (cy in 0 .. 1) {
                for (cx in 0 .. 1) {

                    val sample = depthBlock.getSample(voxelPos.x + cx, voxelPos.y + cy, voxelPos.z + cz)
                    voxelCornerDepths[g] = sample
                    voxelCornerPositions[g].set(
                        xp + cx * worldStep,
                        yp + cy * worldStep,
                        zp + cz * worldStep
                    )
                    cornerMask = cornerMask or (if (sample > 0) (1 shl g) else 0 )
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

        // Now we just average the edge intersections and add them to coordinate
        val s = 1f / edgeCrossings
        tempVec.x = xp + s * worldStep * tempVec.x
        tempVec.y = yp + s * worldStep * tempVec.y
        tempVec.z = zp + s * worldStep * tempVec.z
        tempPos.set(tempVec)

        // Determine normal
        terrainFun.getNormal(tempPos, worldStep.toDouble(), tempNormal)

        // Create vertex for mesh
        val vertexIndex = shapeBuilder.addVertex(tempPos, tempNormal)

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

            // Determine face corners
            val vertex1 = voxelVertexIndexes[index]
            val vertex2 = voxelVertexIndexes[index - du]
            val vertex3 = voxelVertexIndexes[index - du - dv]
            val vertex4 = voxelVertexIndexes[index - dv]

            // Only include faces that have the vertex with highest x,y and z coordinate in this chunk.
            // That way seam faces are only included once (note that due to the order the vertexes are created(?), this
            // doesn't work if checking for the minimum coordinate).
            tempVec.set(shapeBuilder.getPos(vertex1, tempPos))
            tempVec.setMax(shapeBuilder.getPos(vertex2, tempPos))
            tempVec.setMax(shapeBuilder.getPos(vertex3, tempPos))
            tempVec.setMax(shapeBuilder.getPos(vertex4, tempPos))
            if (!chunkVolume.containsExclusive(tempVec)) continue

            // Add face
            // Remember to flip orientation depending on the sign of the corner.
            val invertFace = (cornerMask and 1) == 0
            shapeBuilder.addQuad(vertex1, vertex2, vertex3, vertex4, invertFace = invertFace, updateNormals = false)
        }
    }


    companion object {

        val shapeBuilderPool = RecyclingPool<ShapeBuilder>(ShapeBuilder::class)

        private val cubeEdges = IntArray(24)
        private val edgeTable = IntArray(256)

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