package org.fractalpixel.gameutils.voxel.distancefunction.utils

import org.fractalpixel.gameutils.utils.checkForJobCancellation
import org.kwrench.geometry.int3.ImmutableInt3
import org.kwrench.geometry.volume.ImmutableVolume
import org.kwrench.geometry.volume.Volume
import kotlin.math.max
import kotlin.math.min

/**
 * A block of depth data, filled in by DistanceFunctions and used when constructing chunks.
 * [size] specifies the size of the block along each axis.
 *
 * The data is stored by z, then y, then x (so if looping, z loop is outermost and x loop is innermost).
 */
class DepthBlock(val size: ImmutableInt3) {

    val depths: DoubleArray = DoubleArray(size.multiplyAll())

    /**
     * Checks for 0-crossings in the data.
     * Loops the data, so cache the result if used often.
     */
    fun containsSurface(): Boolean {
        var min = Double.POSITIVE_INFINITY
        var max = Double.NEGATIVE_INFINITY
        for (d in depths) {
            min = min(d, min)
            max = max(d, max)
        }
        return min <= 0.0 && max >= 0.0
    }

    /**
     * Fills all depth values with the specified value
     */
    inline fun fill(value: Double) {
        depths.fill(value)
    }

    /**
     * Loops all values in order (z in outermost loop and x in innermost), calls the [depthCalculator] function for each.
     */
    inline fun fillUsingIndex(depthCalculator: (index: Int) -> Double) {
        for (i in depths.indices) {
            depths[i] = depthCalculator(i)
        }
    }

    /**
     * Interpolates the coordinate values of the corners of the specified [volume] and passes them as parameters to the
     * [depthCalculator] function, along with the index of the current position.
     */
    suspend inline fun fillUsingCoordinates(volume: Volume, depthCalculator: (index: Int, x: Double, y: Double, z: Double) -> Double) {
        // Loop variables
        val sizeX = size.x
        val sizeY = size.y
        val sizeZ = size.z
        var xp = 0.0
        var yp = 0.0
        var zp = 0.0
        val xpd = volume.sizeX / (sizeX-1)
        val ypd = volume.sizeY / (sizeY-1)
        val zpd = volume.sizeZ / (sizeZ-1)
        var index = 0

        // Fill with interpolation
        zp = volume.minZ
        for (z in 0 until sizeZ) {

            // Check if we are canceled
            checkForJobCancellation()

            yp = volume.minY
            for (y in 0 until sizeY) {

                xp = volume.minX
                for (x in 0 until sizeX) {
                    depths[index] = depthCalculator(index, xp, yp, zp)

                    index++

                    xp += xpd
                }

                yp += ypd
            }

            zp += zpd
        }
    }

    /**
     * Interpolates the specified corner values over this block and fills the depths with them.
     *
     * The specified seams may be used to specify the margin towards the low and high values, when the sampled
     * values are not actually from the edges but some margin inwards.
     */
    inline fun fillWithInterpolated(
        x1y1z1: Double,
        x2y1z1: Double,
        x1y2z1: Double,
        x2y2z1: Double,
        x1y1z2: Double,
        x2y1z2: Double,
        x1y2z2: Double,
        x2y2z2: Double,
        leadingSeam: Int = 0,
        trailingSeam: Int = 0
    ) {
        // Loop variables

        val sizeX = size.x
        val sizeY = size.y
        val sizeZ = size.z

        val zStepDivisor = (sizeZ - 1 - leadingSeam - trailingSeam)
        val yStepDivisor = (sizeY - 1 - leadingSeam - trailingSeam)
        val xStepDivisor = (sizeX - 1 - leadingSeam - trailingSeam)

        val x1y1StepZ = (x1y1z2 - x1y1z1) / zStepDivisor
        val x1y2StepZ = (x1y2z2 - x1y2z1) / zStepDivisor
        val x2y1StepZ = (x2y1z2 - x2y1z1) / zStepDivisor
        val x2y2StepZ = (x2y2z2 - x2y2z1) / zStepDivisor

        var x1y1Val = x1y1z1 - leadingSeam * x1y1StepZ
        var x1y2Val = x1y2z1 - leadingSeam * x1y2StepZ
        var x2y1Val = x2y1z1 - leadingSeam * x2y1StepZ
        var x2y2Val = x2y2z1 - leadingSeam * x2y2StepZ

        var index = 0

        // Fill with interpolated value
        for (z in 0 until sizeZ) {

            val x1Step = (x1y2Val - x1y1Val) / yStepDivisor
            val x2Step = (x2y2Val - x2y1Val) / yStepDivisor

            var x1 = x1y1Val - leadingSeam * x1Step
            var x2 = x2y1Val - leadingSeam * x2Step

            for (y in 0 until sizeY) {

                val xStep = (x2 - x1) / xStepDivisor
                var d = x1 - leadingSeam * xStep

                for (x in 0 until sizeX) {

                    depths[index++] = d

                    d += xStep
                }

                x1 += x1Step
                x2 += x2Step
            }

            x1y1Val += x1y1StepZ
            x1y2Val += x1y2StepZ
            x2y1Val += x2y1StepZ
            x2y2Val += x2y2StepZ
        }
    }


    /**
     * Interpolates x, y, and z between 0 and 1 and passes them as parameters to the
     * [depthCalculator] function, along with the index of the current position.
     */
    suspend inline fun fillWith0To1(depthCalculator: (index: Int, x: Double, y: Double, z: Double) -> Double) {
        fillUsingCoordinates(ZeroToOneVolume, depthCalculator)
    }

    /**
     * Returns the depth value at the specified local position within this block.
     * The coordinates must be in range (no range checking performed in this function).
     */
    inline fun getSample(x: Int, y: Int, z: Int): Double {
        return depths[x + y * size.x + z * size.x * size.y]
    }

    companion object {
        // TODO: Move to Volume class
        val ZeroToOneVolume = ImmutableVolume(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
    }
}