package org.fractalpixel.gameutils.voxel.renderer

import org.fractalpixel.gameutils.utils.isAllZeroes
import org.fractalpixel.gameutils.utils.iterate
import org.fractalpixel.gameutils.utils.modPositive
import org.kwrench.checking.Check
import org.kwrench.geometry.int3.ImmutableInt3
import org.kwrench.geometry.int3.Int3
import org.kwrench.geometry.int3.MutableInt3
import org.kwrench.geometry.intvolume.IntVolume
import org.kwrench.geometry.intvolume.MutableIntVolume
import org.kwrench.math.max
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException

/**
 * Buffer that holds values for some area of [initialSize] (not changeable) in 3D integer space, and that keeps the existing values
 * when it moves as long as they are still within it's range.
 * When new values scroll in, the [calculator] is used to calculate them, and when they scroll out, the [disposer] is called on them.
 * It is given the (global) position for the new value and should return the new value.
 * The area starts out at [initialPosition] and can be moved with [setPosition] or [offset].
 * [cornerPosition] contains the current position of the area (the area extends from the position towards positive
 * coordinate axis [size] amount (exclusive).
 *
 * Note that this does lazy recalculation, a missing value is only calculated if it is requested.
 *
 * Note that this class is not thread safe (e.g. iterators use same temporary variables, so are not recursion safe either..)
 */
// TODO: Make more thread and recursion safe?
// REFACTOR: Reduce creating new Int3:s by introducing functions that take xyz params.
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

    private val extentMutable = MutableIntVolume()

    /**
     * Current extent of the positions in this buffer.
     */
    val extent: IntVolume get() = extentMutable


    private val cornerPos = MutableInt3(initialPosition)

    private val origo = MutableInt3()

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

        updateExtent()

        for (i in 0 until dataSize) {
            data.add(null)
        }
    }

    /**
     * returns the value at the specified global position, or null if it is outside the current buffer.
     */
    fun get(pos: Int3): T {
        val p = MutableInt3(pos).sub(cornerPos)
        return if (!p.inRange(size)) throw IllegalArgumentException("Position $pos outside the range of this buffer $extent")
        else getLocal(p)
    }

    /**
     * returns the value at the specified global position, or null if it is outside the current buffer.
     */
    fun getOrNull(pos: Int3): T? {
        val p = MutableInt3(pos).sub(cornerPos)
        return if (!p.inRange(size)) null
        else getLocal(p)
    }

    /**
     * returns the value at the specified global position if it has been calculated,
     * or null if it is outside the current buffer or nor calculated.
     */
    fun getIfCalculatedOrNull(pos: Int3): T? {
        val p = MutableInt3(pos).sub(cornerPos)
        return if (!p.inRange(size)) null
        else getLocalIfCalculated(p)
    }

    fun set(pos: Int3, value: T?) {
        val p = MutableInt3(pos).sub(cornerPos)
        if (!p.inRange(size)) throw IllegalArgumentException("Position $pos is out of range")
        else setLocal(p, value)
    }

    /**
     * Get item
     */
    fun getLocal(localPos: Int3): T {
        val index = getBufferIndex(localPos)
        var value = data[index]
        if (value == null) {
            value = calculator(MutableInt3(localPos).add(cornerPos))
            data[index] = value
        }
        return value
    }

    /**
     * Value at the specified local coordinate, if it has been calculated, null otherwise
     */
    fun getLocalIfCalculated(localPos: Int3): T? {
        return data[getBufferIndex(localPos)]
    }

    fun setLocal(localPos: Int3, value: T?) {
        val index = getBufferIndex(localPos)
        data[index] = value
    }

    private fun getBufferIndex(localPos: Int3): Int {
        val p = MutableInt3(localPos).add(origo).modPositive(size)
        return p.toIndex(size) ?: throw IllegalStateException("Wrapped position $p outside RingBuffer size ($size)")
    }

    fun setPosition(newPosition: Int3) {
        offset(MutableInt3(newPosition).sub(cornerPos))
    }

    fun offset(offset: Int3) {
        // If we didn't move, there is nothing to do
        if (offset.isAllZeroes()) return

        // Update position
        val oldPos = MutableInt3(cornerPos)
        cornerPos.add(offset)
        updateExtent()

        // Roll the buffer with wrap
        origo.add(offset).modPositive(size)

        // Clear any data that scrolled out
        val oldEnd = MutableInt3(oldPos).add(size)
        iteratePositions() { globalPos, localPos ->
            if (!globalPos.inRange(oldPos, oldEnd)) {
                // Not in old range, so we do not have this data
                val value = getOrNull(globalPos)
                if (value != null) disposer(value)
                set(globalPos, null)
            }
        }
    }

    fun iterate(iteratingInt3: MutableInt3 = MutableInt3(), code: (globalPos: Int3, localPos: Int3, value: T) -> Unit) {
        val globalPos = MutableInt3()
        size.iterate(iteratingInt3 = iteratingInt3) {localPos ->
            val value = getLocal(localPos)
            globalPos.set(cornerPos).add(localPos)
            code(globalPos, localPos, value)
        }
    }

    fun iteratePositions(iteratingInt3: MutableInt3 = MutableInt3(), code: (globalPos: Int3, localPos: Int3) -> Unit) {
        val globalPos = MutableInt3()
        size.iterate(iteratingInt3 = iteratingInt3) {localPos ->
            globalPos.set(cornerPos).add(localPos)
            code(globalPos, localPos)
        }
    }

    private fun updateExtent() {
        extentMutable.setByCorner(
            cornerPos.x, cornerPos.y, cornerPos.z,
            size.x - 1, size.y - 1, size.z - 1)
        // TODO: IntVolume is currently broken, as it regards size zero to be one.  Need to fix range to be non inclusive of upper bound.
    }

}

