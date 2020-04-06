package org.fractalpixel.gameutils.voxel.renderer

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector3
import org.fractalpixel.gameutils.libgdxutils.GdxColorType
import org.fractalpixel.gameutils.libgdxutils.setWithScaleAddAndFloor
import org.fractalpixel.gameutils.utils.iterate
import org.fractalpixel.gameutils.utils.sub
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
 * [chunkSize] number of blocks in a chunk along each axis.
 */
// TODO: More easily configurable resolution settings, from low res potato to high end machine,
//       in such a way that relevant terrain features are still visible close up, and rough outlines far away.
//       Probably level size and most detailed level are the ones to tune, as well as detail level count a bit.
// TODO: Change detail level count to mostRoughDetailLevel or similar instead.
// TODO: Adaptively adjust chunk size based on performance?  To some degree at least?
data class VoxelConfiguration(
    val detailLevelCount: Int = 15,
    val mostDetailedDetailLevel: Int = -3,
    val chunkSize: Int = 14,
    val levelSize: Int = 10,
    val baseDetailLevelBlockSizeMeters: Double = 1.0,
    val debugLines: Boolean = false,
    val debugLinesForEmptyBlocks: Boolean = false,
    val debugOutlines: Boolean = false,
    val colorizeTerrainByLevel: Boolean = true) {

    // The block corners in a chunk, so one more than blocks in each direction and one extra overlap covering/overlapping gaps.
    val overlap = 1 // Can be 0 (cracks), 1 (overlap in negative direction), or 2 (overlap in both directions).
    val chunkCornersSize = chunkSize + 1 + overlap
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

        // Align to even chunk coordinates.
        // TODO: Align
        cornerChunkOut.divide(2).scale(2).add(Int3.ONES)
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

}