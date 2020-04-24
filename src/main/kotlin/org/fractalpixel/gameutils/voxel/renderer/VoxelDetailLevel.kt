package org.fractalpixel.gameutils.voxel.renderer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.math.Vector3
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.fractalpixel.gameutils.libgdxutils.ShapeBuilder
import org.fractalpixel.gameutils.rendering.RenderingContext3D
import org.fractalpixel.gameutils.utils.RecyclingPool
import org.fractalpixel.gameutils.utils.all
import org.fractalpixel.gameutils.voxel.VoxelTerrain
import org.kwrench.geometry.int3.Int3
import org.kwrench.geometry.int3.MutableInt3
import org.kwrench.geometry.intvolume.IntVolume
import org.kwrench.geometry.intvolume.MutableIntVolume

/**
 * Holds all voxel chunks at a specific detail level.
 */
// IDEA: Have extra non-visible layer in each direction, where the chunks are being asynchronously loaded,
//       so that when they get into view, most/all are already loaded.  -- this might be already handled with the
//       odd-overlap alignment - instead do not show furthest away chunk detail in a detail level,
//       so that chunks have time to get loaded before they need to be shown

// BUG: Chunks occasionally flicker away for one frame, appears to happen when they scroll out of view - should the chunk buffer perhaps be synchronized?
class VoxelDetailLevel(
    val terrain: VoxelTerrain,
    val level: Int,
    val configuration: VoxelConfiguration,
    val chunkPool: RecyclingPool<VoxelRenderChunk>,
    val shapeCalculatorPool: RecyclingPool<ShapeCalculator>,
    val moreDetailedLevel: VoxelDetailLevel?
) {

    private val visibleAreaCorner = MutableInt3()

    private val tempVolume = MutableIntVolume()
    private val tempPos = MutableInt3()
    private val tempchunkPos = MutableInt3()

    private val chunkBuffer = PanBuffer<Deferred<VoxelRenderChunk?>>(
        configuration.levelExtent,
        disposer = {
            // NOTE: When this was run asynchronously, it built up a huge backlog of calculators,
            //       as stopping calculations got backlogged by creating new calculations, so don't do that.
            //       Release chunks and call cancels immediately when a chunk scrolls out.
            val jobToDispose = it
            runBlocking {
                if (jobToDispose.isCompleted) {
                    // Release any completed chunk
                    val chunk = jobToDispose.getCompleted()
                    if (chunk != null) chunkPool.release(chunk)
                }
                else {
                    // Cancel any ongoing job
                    jobToDispose.cancel()
                }
            }
        }) { pos ->

        // Need to copy this, as it is used in async method
        val p = MutableInt3(pos)

        val job = VoxelCoroutineScope.async {

            // IDEA: If current terrain calculation takes too much of the frame time (configurable parameter),
            //       delay calculating terrain somewhat (delay high resolutions more), to wait for time and to allow it to
            //       scroll out of the view if we are moving fast. -- Is this still the sensible solution?  Maybe.
            // TODO: Ideally newly calculated terrain should fade in over time if it doesn't appear a the edges of the detail level.

            // Calculate shape
            val shape = calculateShape(terrain, level, p)

            // Create chunk if shape was not null
            if (shape != null) {

                // Reuse or create new chunk
                val chunk = chunkPool.obtain()

                // Initialize chunk
                chunk.init(terrain, level, p, shape)

                // Return configured chunk
                chunk
            }
            else {
                // No shape for chunk, leave it empty
                null
            }
        }
        //runBlocking { job.await() }
        job
    }

    /**
     * Starts building the shape of this chunk in the background.
     * Returns immediately, sets the [meshCalculated] flag to false when ready.
     */
    private suspend fun calculateShape(terrain: VoxelTerrain, level: Int, chunkPos: Int3): ShapeBuilder? {

        // Get a mesh calculator instance (has various memory structures used during calculation)
        val meshCalculator = shapeCalculatorPool.obtain()
        try {
            // Create shape
            return meshCalculator.buildShape(terrain, chunkPos, level)
        } finally {
            // Release calculator
            shapeCalculatorPool.release(meshCalculator)
        }
    }

    fun updateCameraPos(pos: Vector3) {

        // Determine new corner position for the visible chunks in this level
        configuration.getLevelCornerChunk(pos, level, visibleAreaCorner)

        // Update chunk buffer global position, rolling it as necessary and starting calculation of new chunks
        chunkBuffer.setPosition(visibleAreaCorner)
    }

    /**
     * This should be called for all detail levels before the render function
     */
    fun updateCameraAndScrollTerrain(context: RenderingContext3D) {
        updateCameraPos(context.camera.position)
    }

    fun render(context: RenderingContext3D) {

        chunkBuffer.iterate(tempchunkPos) { globalPos, localPos, chunkJob ->
            // Get the chunk if it has been calculated and is not null
            val chunk = getChunkForJob(chunkJob)

            // Render chunk if it is available and the area does not have higher resolution chunks rendering
            if (chunk != null && shouldRender(globalPos)) {
                chunk.render(context)
            }
        }
    }

    // TODO: If higher LOD chunks are missing, do not alpha fade over them.. how to do that logic?  Chunk-specific fade-over?  Or just skip whole layer if missing chunk?

    private inline fun getChunkForJob(chunkJob: Deferred<VoxelRenderChunk?>?): VoxelRenderChunk? {
        return if (chunkJob?.isCompleted == true) chunkJob.getCompleted() else null
    }


    /**
     * True if the chunk at the specified position on the specified level should be rendered
     */
    private fun shouldRender(chunkPos: Int3): Boolean {
        return if (moreDetailedLevel == null) {
            // If we are the most detailed level, we should render
            true
        } else {
            // Skip rendering if more detailed level covers the whole area of this chunk, and at least 1 chunk margin in all directions.
            /*
            tempVolume.setByCorner(chunkPos, Int3.ONES)
            tempVolume.mul(2) // More detailed level has all coordinates scaled by two
            */
            // Note that Int volumes are inclusive of min and max coordinates..  Should really not be...
            // TODO: Fix IntVolumes...
            tempVolume.set(
                chunkPos.x * 2 ,
                chunkPos.y * 2 ,
                chunkPos.z * 2 ,
                chunkPos.x * 2 + 1,
                chunkPos.y * 2 + 1,
                chunkPos.z * 2 + 1)

            tempVolume.expand(2) // Require at least 2 chunks for smooth transition

            !moreDetailedLevel.containsChunks(tempVolume)
        }
    }

    /**
     * True if all chunk positions in the given volume are currently overlapped by this level,
     * and the chunk positions have content calculated (or are determined to be empty).
     */
    fun containsChunks(volume: IntVolume): Boolean {
        // Check overlap
        if (!chunkBuffer.extent.contains(volume)) return false

        // Check that content is calculated and rendering
        return volume.all(tempPos) { pos ->
            // Consider sub-area calculated when all chunk calculation jobs in it have finished, or it is so remote
            // that no-one has needed to create it yet
            val chunkJob = chunkBuffer.getIfCalculatedOrNull(pos)
            chunkJob?.isCompleted != false
            /*
            if (chunkJob == null) true // No-one has tried to access this place yet, so it can't be in vision range
            else if (!chunkJob.isCompleted) false // Still calculating
            else {
                // Get the calculated chunk
                val chunk = chunkJob.getCompleted()

                if (chunk == null) true // Empty chunk (e.g. air or solid)
                else true // chunk.initialized // Chunk should have the model created and in use
                // BUG: For some reason the above doesn't work.. and there seems to be occasional frames where one chunk is missing (could maybe be gradient descent screening too? - but why only 1 frame in that case?)
            }
            */

        }
    }



}