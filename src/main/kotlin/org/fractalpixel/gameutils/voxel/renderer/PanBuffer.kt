package org.fractalpixel.gameutils.voxel.renderer

import org.fractalpixel.gameutils.utils.isAllZeroes
import org.fractalpixel.gameutils.utils.iterate
import org.fractalpixel.gameutils.utils.modPositive
import org.kwrench.checking.Check
import org.kwrench.geometry.int3.ImmutableInt3
import org.kwrench.geometry.int3.Int3
import org.kwrench.geometry.int3.MutableInt3
import org.kwrench.math.max
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException

/**
 * Buffer that holds values for some area of [initialSize] (not changeable) in 3D integer space, and that keeps the existing values
 * when it moves as long as they are still within it's range.
 * When new values scroll in, the [calculator] is used to calculate them.
 * It is given the (global) position for the new value and should return the new value.
 * The area starts out at [initialPosition] and can be moved with [setPosition] or [offset].
 * [cornerPosition] contains the current position of the area (the area extends from the position towards positive
 * coordinate axis [size] amount (exclusive).
 */
class PanBuffer<T: Any>(
    initialSize: Int3,
    initialPosition: Int3 = Int3.ZEROES,
    val disposer: (T) -> Unit,
    val calculator: (pos: Int3) -> T
) {

    /**
     * Size of the buffer along each axis.
     */
    val size = ImmutableInt3(initialSize)

    private val cornerPos = MutableInt3(initialPosition)

    private val origo = MutableInt3()
    private val tempBufferPos = MutableInt3()
    private val oldEnd = MutableInt3()
    private val tempOffset = MutableInt3()
    private val tempLocalPos = MutableInt3()
    private val tempGlobalPos = MutableInt3()
    private val tempGlobalPos2 = MutableInt3()
    private val oldPos = MutableInt3()
    private val ti = MutableInt3()

    private val dataSize = size.multiplyAll()
    private val data = ArrayList<T?>(dataSize.max(1))

    /**
     * Position of the corner for this buffer (towards the lowest coordinate values)
     * The buffer extends up to, but not including [size] from the corner (the corner is inclusive).
     */
    val cornerPosition: Int3 get() = cornerPos

    init {
        Check.greater(initialSize.x, "initialSize.x", 0)
        Check.greater(initialSize.y, "initialSize.y", 0)
        Check.greater(initialSize.z, "initialSize.z", 0)

        for (i in 0 until dataSize) {
            data.add(null)
        }
    }

    /**
     * returns the value at the specified global position, or null if it is outside the current buffer.
     */
    fun get(pos: Int3): T? {
        tempLocalPos.set(pos).sub(cornerPos)
        return if (!tempLocalPos.inRange(size)) null
        else getLocal(tempLocalPos)
    }

    fun set(pos: Int3, value: T?) {
        tempLocalPos.set(pos).sub(cornerPos)
        if (!tempLocalPos.inRange(size)) throw IllegalArgumentException("Position $pos is out of range")
        else setLocal(tempLocalPos, value)
    }

    fun getLocal(localPos: Int3): T {
        val index = getBufferIndex(localPos)
        var value = data[index]
        if (value == null) {
            tempGlobalPos2.set(localPos).add(cornerPos)
            value = calculator(tempGlobalPos2)
            data[index] = value
        }
        return value
    }

    fun setLocal(localPos: Int3, value: T?) {
        val index = getBufferIndex(localPos)
        data[index] = value
    }

    private fun getBufferIndex(localPos: Int3): Int {
        tempBufferPos.set(localPos).add(origo).modPositive(size)
        return tempBufferPos.toIndex(size) ?: throw IllegalStateException("Wrapped position $tempBufferPos outside RingBuffer size ($size)")
    }

    fun setPosition(newPosition: Int3) {
        tempOffset.set(newPosition).sub(cornerPos)
        offset(tempOffset)
    }

    fun offset(offset: Int3) {
        // If we didn't move, there is nothing to do
        if (offset.isAllZeroes()) return

        // Update position
        oldPos.set(cornerPos)
        cornerPos.add(offset)

        // Roll the buffer with wrap
        origo.add(offset).modPositive(size)

        // Clear any data that scrolled out
        oldEnd.set(oldPos).add(size)
        iteratePositions(ti) { pos ->
            if (!pos.inRange(oldPos, oldEnd)) {
                // Not in old range, so we do not have this data
                val value = get(pos)
                if (value != null) disposer(value)
                set(pos, null)

                // TODO: Start recalculating any data that is null
            }
        }
    }

    fun iterate(iteratingInt3: MutableInt3 = MutableInt3(), code: (pos: Int3, value: T) -> Unit) {
        size.iterate(iteratingInt3 = iteratingInt3) {localPos ->
            val value = getLocal(localPos)
            tempGlobalPos.set(cornerPos).add(localPos)
            code(tempGlobalPos, value)
        }
    }

    fun iteratePositions(iteratingInt3: MutableInt3 = MutableInt3(), code: (pos: Int3) -> Unit) {
        size.iterate(iteratingInt3 = iteratingInt3) {localPos ->
            tempGlobalPos.set(cornerPos).add(localPos)
            code(tempGlobalPos)
        }
    }
}

