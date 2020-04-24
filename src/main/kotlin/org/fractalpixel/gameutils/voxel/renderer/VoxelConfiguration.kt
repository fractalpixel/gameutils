package org.fractalpixel.gameutils.voxel.renderer

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import org.fractalpixel.gameutils.libgdxutils.GdxColorType
import org.fractalpixel.gameutils.libgdxutils.set
import org.fractalpixel.gameutils.libgdxutils.setWithScaleAddAndFloor
import org.fractalpixel.gameutils.utils.iterate
import org.fractalpixel.gameutils.utils.sub
import org.fractalpixel.gameutils.voxel.distancefunction.utils.DepthBlockPool
import org.kwrench.checking.Check
import org.kwrench.color.GenColor
import org.kwrench.color.colorspace.HSLColorSpace
import org.kwrench.geometry.int3.ImmutableInt3
import org.kwrench.geometry.int3.Int3
import org.kwrench.geometry.int3.MutableInt3
import org.kwrench.geometry.volume.MutableVolume
import org.kwrench.math.map
import org.kwrench.math.mix
import java.lang.IllegalArgumentException
import kotlin.math.pow

/**
 * Configuration for technical rendering settings for a voxel based landscape.
 * The settings usually do not need editing, at most the number of detail levels and smallest detail level may be
 * tuned depending on performance or detail requirements respectively.
 *
 * Also contains functions for doing most of the tricky coordinate math for voxel blocks, chunks and detail levels.
 *
 * Also holds (some of) the object pools used when rendering a voxel landscape, so this is not a lightweight object.
 * // CHECK: is there need to move the pools to a separate object, that might be created on demand by the configuration?
 *
 * [chunkSize] number of blocks in a chunk along each axis.
 */
// FEATURE: More easily configurable resolution settings, from low res potato to high end machine,
//       in such a way that relevant terrain features are still visible close up, and rough outlines far away.
//       Probably level size and most detailed level are the ones to tune, as well as detail level count a bit.
// REFACTOR: Change detail level count to mostRoughDetailLevel or similar instead.
// FEATURE: Adaptively adjust chunk size based on performance?  To some degree at least?
// BUG: There are sometimes chunks that don't get released, more clearly e.g. with levelCount 3, mostDetailed 0, levelSize 8, chunk size 24
data class VoxelConfiguration(
    val detailLevelCount: Int = 10, //10,
    val mostDetailedDetailLevel: Int = 0, //-2,
    val chunkSize: Int = 20,//28, //24,
    val levelSize: Int = 9, // FIXME: Lot of overlapping layers rendered with low values..
    val baseDetailLevelBlockSizeMeters: Double = 1.0,
    val wireframeTerrain: Boolean = false,
    val debugLines: Boolean = false,
    val dashedDebugLines: Boolean = true,
    val debugLinesForEmptyBlocks: Boolean = false,
    val debugOutlines: Boolean = false,
    val colorizeTerrainByLevel: Boolean = false) {

    // The block corners in a chunk, so one more than blocks in each direction and one extra overlap covering/overlapping gaps.
    val overlap = 1 // Can be 0 (cracks), 1 (overlap in negative direction), or 2 (overlap in both directions).
    val chunkCornersSize = chunkSize + 1 + overlap
    val leadingSeam = 1
    val trailingSeam = overlap -1
    val blockCornerCountInChunk = chunkCornersSize * chunkCornersSize * chunkCornersSize

    init {
        // Vertex indexes are shorts, so we can't have too large chunks, or we'd crash/bug out on worst-case terrain
        Check.lessOrEqual(blockCornerCountInChunk, "maximum possible vertexes (from chunkSize $chunkSize)", Short.MAX_VALUE.toInt(), "Short.MAX_VALUE")
    }

    val blockCountInChunk: Int = chunkSize * chunkSize * chunkSize
    val chunkCountInLevel: Int = levelSize * levelSize * levelSize

    val levelExtent = ImmutableInt3(levelSize, levelSize, levelSize)
    val chunkExtent = ImmutableInt3(chunkSize, chunkSize, chunkSize)
    val chunkCornersExtent = ImmutableInt3(chunkCornersSize, chunkCornersSize, chunkCornersSize)

    // FEATURE: If / when we make chunk size modifiable on the fly, update it here too.
    val depthBlockPool =
        DepthBlockPool(chunkCornersExtent)


    fun blockWorldSize(level: Int): Double = baseDetailLevelBlockSizeMeters * 2.0.pow(level)
    fun chunkWorldSize(level: Int): Double = blockWorldSize(level) * chunkSize

    fun chunkWorldCornerPos(chunkPos: Int3, level: Int, posOut: Vector3 = Vector3()): Vector3 {
        val chunkWorldSize = chunkWorldSize(level).toFloat()
        posOut.x = chunkPos.x * chunkWorldSize
        posOut.y = chunkPos.y * chunkWorldSize
        posOut.z = chunkPos.z * chunkWorldSize
        return posOut
    }

    /**
     * Returns the volume for the specified chunk.
     */
    fun getChunkVolume(chunkPos: Int3, level: Int, volumeOut: MutableVolume = MutableVolume()): MutableVolume {
        val chunkWorldSize = chunkWorldSize(level)

        // First corner
        val x1 = chunkPos.x * chunkWorldSize
        val y1 = chunkPos.y * chunkWorldSize
        val z1 = chunkPos.z * chunkWorldSize

        // Extend from corner
        volumeOut.set(
            x1,
            y1,
            z1,
            x1 + chunkWorldSize,
            y1 + chunkWorldSize,
            z1 + chunkWorldSize)

        return volumeOut
    }

    /**
     * Returns the volume that the distance function is sampled for the specified chunk.
     * This is larger than the chunkVolume, as some area around the edges of the chunk needs to be sampled to create
     * a smooth, seamless surface.
     */
    fun getChunkSamplingVolume(chunkPos: Int3, level: Int, volumeOut: MutableVolume = MutableVolume()): MutableVolume {

        val blockWorldSize = blockWorldSize(level)
        val chunkWorldSize = blockWorldSize * chunkSize
        val chunkSamplingAreaSize = blockWorldSize * chunkCornersSize - blockWorldSize // TODO: This extra -blockWorldSize implies there's a one-off assumption somewhere, clear it out.

        // First corner
        val x1 = chunkPos.x * chunkWorldSize - blockWorldSize
        val y1 = chunkPos.y * chunkWorldSize - blockWorldSize
        val z1 = chunkPos.z * chunkWorldSize - blockWorldSize

        // Extend from corner
        volumeOut.set(
            x1,
            y1,
            z1,
            x1 + chunkSamplingAreaSize,
            y1 + chunkSamplingAreaSize,
            z1 + chunkSamplingAreaSize)

        return volumeOut
    }

    /**
     * Update provided axis-aligned bounding box with the volume of the chunk at the specified level and chunk pos.
     */
    fun calculateBounds(pos: Int3, level: Int, bounds: BoundingBox) {
        bounds.set(getChunkVolume(pos, level))
    }

    /**
     * Returns the radius, and sets the center
     */
    fun calculateBoundingSphere(pos: Int3, level: Int, boundingSphereCenter: Vector3): Float {
        val worldCornerPos = chunkWorldCornerPos(pos, level)
        val halfSize = chunkWorldSize(level).toFloat()
        boundingSphereCenter.set(
            worldCornerPos.x + halfSize,
            worldCornerPos.y + halfSize,
            worldCornerPos.z + halfSize)
        return halfSize * 3f // Overestimate, chunk might extend outside?  This didn't work: sqrt(halfSize*halfSize + halfSize*halfSize + halfSize*halfSize).toFloat()
    }

    inline fun iterateLevel(cornerChunkPos: Int3, iteratingInt3: MutableInt3 = MutableInt3(), code: (chunkPos: Int3, chunkIndex: Int) -> Unit) {
        var chunkIndex = 0
        levelExtent.iterate(cornerChunkPos, iteratingInt3) {
            code(it, chunkIndex)
            chunkIndex++
        }
    }

    inline fun iterateChunk(cornerBlockPos: Int3, iteratingInt3: MutableInt3 = MutableInt3(), code: (blockPos: Int3, blockIndex: Int) -> Unit) {
        var blockIndex = 0
        chunkExtent.iterate(cornerBlockPos, iteratingInt3) {
            code(it, blockIndex)
            blockIndex++
        }
    }

    inline fun chunkArrayIndex(localChunkPos: Int3): Int = localChunkPos.toIndex(levelExtent) ?: throw IllegalArgumentException("Chunk pos out of local range $localChunkPos")
    inline fun blockArrayIndex(localBlockPos: Int3): Int = localBlockPos.toIndex(chunkExtent) ?: throw IllegalArgumentException("Block pos out of local range $localBlockPos")

    fun getChunkForPosition(pos: Vector3, level: Int, chunkPosOut: MutableInt3 = MutableInt3()): MutableInt3 {
        return chunkPosOut.setWithScaleAddAndFloor(pos, 1f / chunkWorldSize(level).toFloat())
    }

    /**
     * Returns the corner chunk position (at the smallest x,y,z position) for the specified detail level,
     * when the specified focus position is being centered.
     */
    fun getLevelCornerChunk(focus: Vector3, level: Int, cornerChunkOut: MutableInt3): MutableInt3 {
        // Get camera chunk
        getChunkForPosition(focus, level, cornerChunkOut)

        // Get corner chunk
        cornerChunkOut.sub(levelSize / 2)

        return cornerChunkOut
    }

    /**
     * Range with the detail levels starting from the most detailed (smallest blocks)
     * and ending with the most coarse (largest blocks) detail level.
     */
    val detailLevelsRange get() = mostDetailedDetailLevel until (mostDetailedDetailLevel + detailLevelCount)

    fun relativeLevel(level: Int): Double = map(
        level.toDouble(),
        mostDetailedDetailLevel.toDouble(),
        (mostDetailedDetailLevel + detailLevelCount - 1).toDouble(),
        0.0, 1.0)

    val blockTypeDebugLineSpacing = 0.005f

    val blockEdgeDebugLineColor = Color(0.35f, 0.35f, 0.35f, 0.5f)

    fun calculateBlockLevelDebugColor(
        level: Int,
        mayContainSurface: Boolean,
        hasMesh: Boolean
    ): Color {
        var hue = mix(relativeLevel(level), 0.15, 0.7)
        return GenColor(hue, if (mayContainSurface) 0.9 else 0.25, if (hasMesh) 0.8 else if (mayContainSurface) 0.3 else 0.1, 1.0, HSLColorSpace).toColor(GdxColorType)
    }

    private fun getVisibleChunkManhattanRadius(level: Int): Float {
        return blockWorldSize(level).toFloat() * chunkSize * (levelSize - 1) * 0.5f
    }

    // TODO: Slight gap between fade in start and fade out end of previous layer, gives engine time to generate chunks

    /**
     * (Manhattan) distance from camera that the terrain should start to fade in at the specified [level].
     */
    fun getFadeInStart(level: Int): Float {
        return if (level <= mostDetailedDetailLevel) 0f
        else return getFadeOutStart(level - 1)
        /*
        val chunkWidth = blockWorldSize(level - 1).toFloat() * chunkSize
        return if (level <= mostDetailedDetailLevel) 0f
        else getVisibleChunkManhattanRadius(level - 1) - chunkWidth
        */
    }

    /**
     * (Manhattan) distance from camera that the terrain should have completely faded in at the specified [level].
     */
    fun getFadeInEnd(level: Int): Float {
        return if (level <= mostDetailedDetailLevel) 0f
        else return getFadeOutEnd(level - 1)
/*
        val chunkWidth = blockWorldSize(level - 1).toFloat() * chunkSize
        return if (level <= mostDetailedDetailLevel) 0f
        else getVisibleChunkManhattanRadius(level - 1)

 */
    }

    /**
     * (Manhattan) distance from camera that the terrain should start to fade out at the specified [level].
     */
    fun getFadeOutStart(level: Int): Float {
        val chunkWidth = blockWorldSize(level).toFloat() * chunkSize
        return getVisibleChunkManhattanRadius(level) - chunkWidth
    }

    /**
     * (Manhattan) distance from camera that the terrain should have completely faded out at the specified [level].
     */
    fun getFadeOutEnd(level: Int): Float {
        return getVisibleChunkManhattanRadius(level)
    }


}