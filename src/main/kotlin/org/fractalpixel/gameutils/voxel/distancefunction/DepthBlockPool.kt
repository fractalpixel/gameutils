package org.fractalpixel.gameutils.voxel.distancefunction

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withLock
import org.fractalpixel.gameutils.utils.RecyclingPool
import org.fractalpixel.gameutils.utils.immutable
import org.kwrench.checking.Check
import org.kwrench.geometry.int3.ImmutableInt3
import org.kwrench.geometry.int3.Int3

/**
 * Pools [DepthBlock] instances.  Also keeps track of the current [blockSize] to use for them.
 */
class DepthBlockPool(initialDataSize: Int3 = DefaultBlockSize): RecyclingPool<DepthBlock>(DepthBlock::class,1000) {

    /**
     * The size of data arrays to use.  If changed, it will clear out all currently pooled
     * DepthData objects and only produce ones with the new size after that.
     */
    var blockSize: Int3 = ImmutableInt3(initialDataSize)
        set(newSize:  Int3) = runBlocking {
            mutex.withLock() {
                if (newSize != field) {
                    // Check size.  Need to be at least 2 for any sensible application.
                    Check.greaterOrEqual(newSize.x, "blockSize.x", 2)
                    Check.greaterOrEqual(newSize.y, "blockSize.y", 2)
                    Check.greaterOrEqual(newSize.z, "blockSize.z", 2)

                    // The size of the data arrays changed, so we need to dispose all existing ones.
                    field = ImmutableInt3(newSize)
                    disposeUnlocked()
                }
            }
        }

    override fun createNewInstance(): DepthBlock {
        return DepthBlock(blockSize.immutable)
    }

    override fun releaseUnlocked(obj: DepthBlock) {
        if (obj.size != blockSize) {
            // Do not store objects that do not match the current size
            disposeObjectUnlocked(obj)
        }
        else {
            // Use normal logic
            super.releaseUnlocked(obj)
        }
    }

    companion object {
        val DefaultBlockSize = ImmutableInt3(12, 12, 12)
    }
}