package org.fractalpixel.gameutils.voxel.renderer

import org.kwrench.geometry.int3.Int3

/**
 * Position of a voxel chunk at the specified detail level.
 * The [level] is the exponent of the scale, so size of the chunk is
 * 2^level meters.  E.g. level 0 is 1m blocks, level -1 is 0.5m blocks, and level 10 is 1024 m blocks.
 * [chunkSize] is the number of blocks in each direction that the chunk is.
 * Each chunk has the first corner located at the chunk position.
 * Implements Int3 for easy use of the position.
 */
data class VoxelChunkPos(override val x: Int,
                         override val y: Int,
                         override val z: Int,
                         val level: Int = 0,
                         val chunkSize: Int = 16): Int3 {
}