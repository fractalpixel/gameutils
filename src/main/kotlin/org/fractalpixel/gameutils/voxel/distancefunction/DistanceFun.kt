package org.fractalpixel.gameutils.voxel.distancefunction

import com.badlogic.gdx.math.Vector3
import org.fractalpixel.gameutils.utils.*
import org.kwrench.geometry.double3.Double3
import org.kwrench.geometry.double3.MutableDouble3
import org.kwrench.geometry.volume.Volume
import org.kwrench.math.abs
import org.kwrench.math.clamp0To1
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * A distance function that defines a 3D object.  Render by finding and rendering the isosurface (surface where function returns zero).
 * The function returns the distance from the surface at a queried point, returning negative value if the point is inside the object.
 */
// TODO: Consider optimizing distance functions by having them generate code that is compiled with some on-the-fly compiler
//       (does e.g. Lua generate compiled and fast bytecode these days?).
//       This gets rid of a lot of object referencing in a critical path.
// TODO: Add sampling scale as parameter, use to smooth noise flat which is too high frequency for sampling scale, and to skip other small features
// TODO: Calculate all depths in a volume to an array, use that to fetch distance data for a chunk -> faster!
interface DistanceFun: (Double3) -> Double, (Double, Double, Double) -> Double {

    /**
     * Returns distance to the surface from the specified point, negative if the point is inside the object.
     */
    operator fun get(pos: Double3): Double = invoke(pos.x, pos.y, pos.z)

    /**
     * Returns distance to the surface from the specified point, negative if the point is inside the object.
     */
    override fun invoke(pos: Double3): Double  = invoke(pos.x, pos.y, pos.z)

    /**
     * Returns distance to the surface from the specified point, negative if the point is inside the object.
     */
    override fun invoke(x: Double, y: Double, z: Double): Double

    /**
     * Calculates distances for all points in the [block] of the specified extent
     * (the [volume] includes the position in the world).
     *
     * The block pool is supplied as well, for acquiring temporary blocks when intermediate results need to be stored.
     *
     * This function is suspending, implementations should check if the current co-routine has been canceled if they
     * have long-running calculations using e.g. [checkForJobCancellation].
     *
     * Speed optimizations are allowed and recommended even if they slightly deviate from the directly calculated values.
     */
    suspend fun calculateBlock(volume: Volume, block: DepthBlock, blockPool: DepthBlockPool, leadingSeam: Int = 0, trailingSeam: Int = 0) /*{
        // TODO: Remove later?
        calculateBlockUsingSamples(volume, block, blockPool)

        // DEBUG
//        calculateBlockWithInterpolation(volume, block, blockPool, leadingSeam, trailingSeam)
    }*/

    /**
     * Calculate the block by sampling this function at each point.
     */
    suspend fun calculateBlockUsingSamples(volume: Volume, block: DepthBlock, blockPool: DepthBlockPool) {
        checkForJobCancellation()

        block.fillUsingCoordinates(volume) { index, x, y, z ->
            invoke(x, y, z)
        }
    }

    /**
     * Samples this function for the corner values of the specified [volume] and fill the [block] with interpolated values.
     * The seams specify the positions that the samples have been taken at, relative to the leading and trailing
     * edges of the block.
     */
    suspend fun calculateBlockWithInterpolation(volume: Volume, block: DepthBlock, blockPool: DepthBlockPool, leadingSeam: Int = 0, trailingSeam: Int = 0) {
        // Check if we are canceled
        checkForJobCancellation()

        val samplingStepX = volume.sizeX / (block.size.x - 1)
        val samplingStepY = volume.sizeY / (block.size.y - 1)
        val samplingStepZ = volume.sizeZ / (block.size.z - 1)

        val leadX = -samplingStepX * leadingSeam
        val leadY = -samplingStepY * leadingSeam
        val leadZ = -samplingStepZ * leadingSeam
        val trailX = -samplingStepX * trailingSeam
        val trailY = -samplingStepY * trailingSeam
        val trailZ = -samplingStepZ * trailingSeam

        // Sample corners, minus seams
        val x1y1z1 = invoke(volume.minX - leadX, volume.minY - leadY, volume.minZ - leadZ)
        val x2y1z1 = invoke(volume.maxX + trailX, volume.minY - leadY, volume.minZ - leadZ)
        val x1y2z1 = invoke(volume.minX - leadX, volume.maxY + trailY, volume.minZ - leadZ)
        val x2y2z1 = invoke(volume.maxX + trailX, volume.maxY + trailY, volume.minZ - leadZ)
        val x1y1z2 = invoke(volume.minX - leadX, volume.minY - leadY, volume.maxZ + trailZ)
        val x2y1z2 = invoke(volume.maxX + trailX, volume.minY - leadY, volume.maxZ + trailZ)
        val x1y2z2 = invoke(volume.minX - leadX, volume.maxY + trailY, volume.maxZ + trailZ)
        val x2y2z2 = invoke(volume.maxX + trailX, volume.maxY + trailY, volume.maxZ + trailZ)

        // Interpolate between the corners
        block.fillWithInterpolated(
            x1y1z1,
            x2y1z1,
            x1y2z1,
            x2y2z1,
            x1y1z2,
            x2y1z2,
            x1y2z2,
            x2y2z2,
            leadingSeam,
            trailingSeam
        )
    }

    /**
     * Get minimum and maximum values for the specified volume, store them in boundsOut, and return it.
     * If the range can't be determined, return Double.NEGATIVE_INFINITY to Double.POSITIVE_INFINITY
     *
     * It is recommended to implement [calculateBounds] instead of overriding [getBounds] in implementations.
     */
    fun getBounds(volume: Volume, boundsOut: DistanceBounds = DistanceBounds()): DistanceBounds {
        calculateBounds(volume, boundsOut)
        return boundsOut
    }

    /**
     * Determine minimum and maximum values for the specified volume and store them in bounds.
     * If the range can't be determined, use Double.NEGATIVE_INFINITY to Double.POSITIVE_INFINITY
     */
    fun calculateBounds(volume: Volume, bounds: DistanceBounds)

    /**
     * Returns true if the specified volume may contain a surface (intersection of 0 level in the distance function).
     * If false, it will not contain a surface (it will be either completely inside or outside a surface).
     * If true, it may, but does not necessarily contain, a surface.
     * Uses min & max bounds for the volume for a quick check to speed up calculations or collision detection.
     *
     */
    fun mayContainSurface(volume: Volume): Boolean {
        return getBounds(volume).containsInclusive(0.0)
    }

    /**
     * Returns distance function that is this function added to the other function.
     */
    fun add(other: DistanceFun): DistanceFun =
        DualOpFun(
            this,
            other
        ) { a, b -> a + b }

    /**
     * Returns distance function that is this function with the other subtracted from it.
     */
    fun subtract(other: DistanceFun): DistanceFun =
        DualOpFun(
            this,
            other,
            {volume, a, b, aMin, aMax, bMin, bMax, bounds ->
                bounds.set(aMin - bMax, aMax - bMin)
            }
        ) { a, b -> a - b }

    /**
     * Returns distance function that is this function multiplied with the other function.
     */
    fun mul(other: DistanceFun): DistanceFun =
        DualOpFun(
            this,
            other,
            {volume, a, b, aMin, aMax, bMin, bMax, bounds ->
                // Test all combinations for min and max value, as negative multiplicands complicate logic (maybe this could be optimized?)
                val v1 = aMin * bMin
                val v2 = aMax * bMin
                val v3 = aMin * bMax
                val v4 = aMax * bMax
                val min = min(min(v1, v2), min(v3, v4))
                val max = max(max(v1, v2), max(v3, v4))
                bounds.set(min, max)
            }
        ) { a, b -> a * b }


    /**
     * Union of this object and the other.
     */
    fun union(other: DistanceFun): DistanceFun =
        DualOpFun(
            this,
            other
        ) { a, b -> min(a, b) }

    /**
     * Remove the other object from this.
     */
    fun difference(other: DistanceFun): DistanceFun =
        DualOpFun(
            this,
            other
        ) { a, b -> max(a, -b) }

    /**
     * Intersection of this and the other object,
     */
    fun intersection(other: DistanceFun): DistanceFun =
        DualOpFun(
            this,
            other
        ) { a, b -> max(a, b) }

    /**
     * Union of this and the other, with the seam smoothed (outwards).
     * [smoothness] is given in distance units.
     */
    fun smoothUnion(other: DistanceFun, smoothness: Double): DistanceFun =
        DualOpFun(this, other) { a, b ->
            val h = (0.5 + 0.5 * (b - a) / smoothness).clamp0To1()
            fastMix(h, b, a) - smoothness * h * (1.0 - h)
        }

    /**
     * Remove the other object from this, with the seam smoothed (inwards).
     * [smoothness] is given in distance units.
     */
    fun smoothDifference(other: DistanceFun, smoothness: Double): DistanceFun =
        DualOpFun(this, other) { a, b ->
            val h = (0.5 - 0.5 * (b + a) / smoothness).clamp0To1()
            fastMix(h, a, -b) + smoothness * h * (1.0 - h)
        }

    /**
     * Intersect with the other object from this, with the seam smoothed (outwards).
     * [smoothness] is given in distance units.
     */
    fun smoothIntersection(other: DistanceFun, smoothness: Double): DistanceFun =
        DualOpFun(this, other) { a, b ->
            val h = (0.5 - 0.5 * (b - a) / smoothness).clamp0To1()
            fastMix(h, b, a) + smoothness * h * (1.0 - h)
        }

    /**
     * Transform coordinate space that this noise uses with a 3D noise function.
     */
    fun perturb(scale: Double3 = Double3.ONES, amplitude: Double3 = Double3.ONES, offset: Double3 = Double3.ZEROES): DistanceFun = NoisePerturbFun(this, scale, amplitude, offset)

    /**
     * Raises this function to the specified power if it is larger than zero, or returns 0 if it is smaller.
     *
     * Note that if this function is negative, and the exponent is a fractional value, the result would be imaginary (NaN),
     * for this reason this function is clamped to 0 if it is less than zero.
     */
    fun pow(exponent: DistanceFun): DistanceFun =
        DualOpFun(this, exponent) { a, b ->
            if (a <= 0.0) 0.0 else a.pow(b)
        }

    /**
     * Returns absolute value of this function.
     */
    fun abs(): DistanceFun =
        SingleOpFun(this,
            calculateBounds = {volume, a, aMin, aMax, bounds ->
                val min = if (aMin <= 0.0 && aMax >= 0.0) 0.0 // Value range crosses zero, so zero is smallest possible value
                                 else min(aMin.abs(), aMax.abs())
                val max = max(aMin.abs(), aMax.abs())
                bounds.set(min, max)
            }) { f ->
            f.abs()
        }

    // TODO: Add mix between two functions using a third to select.
    // TODO: More generally, use some criteria to select 'ecotype', and apply function for that ecotype,
    //       with smooth transitions.  Might include some data from outside, e.g. height that is used in all ecotypes.
    //       Ecotypes might require a higher abstraction layer that also addresses materials and entity instance generation.
    // TODO: Custom function that is used to create features at some random locations using a density function

    /**
     * Calculate normal at the specified [pos].
     * Use the specified [samplingScale] for the normal calculation, normally the positions around the point are sampled
     * at the specified [samplingScale] in each direction to determine the normal, but implementations may also use
     * other methods.
     * The normal is stored in [normalOut], and it is returned.
     */
    fun getNormal(pos: Double3, samplingScale: Double, normalOut: MutableDouble3 = MutableDouble3()): MutableDouble3 {
        // Use normalOut as temporary sampling position, so that we do not need to create a new Double3 instance
        val mx = get(normalOut.set(pos).add(-samplingScale, 0.0, 0.0))
        val px = get(normalOut.set(pos).add(+samplingScale, 0.0, 0.0))
        val my = get(normalOut.set(pos).add(0.0, -samplingScale, 0.0))
        val py = get(normalOut.set(pos).add(0.0, +samplingScale, 0.0))
        val mz = get(normalOut.set(pos).add(0.0, 0.0, -samplingScale))
        val pz = get(normalOut.set(pos).add(0.0, 0.0, +samplingScale))

        // Calculate normal by comparing samples on both sides of the position along each axis, and normalizing the result
        normalOut.set(px-mx, py-my, pz-mz).normalize()

        return normalOut
    }

    /**
     * Calculate normal at the specified [pos] using libGdx Vector3 classes.
     * Use the specified [samplingScale] for the normal calculation, normally the positions around the point are sampled
     * at the specified [samplingScale] in each direction to determine the normal, but implementations may also use
     * other methods.
     * The normal is stored in [normalOut], and it is returned.
     */
    fun getNormal(pos: Vector3, samplingScale: Double, normalOut: Vector3 = Vector3()): Vector3 {
        // Sample points around pos
        val mx = invoke(pos.x.toDouble() - samplingScale, pos.y.toDouble(), pos.z.toDouble())
        val px = invoke(pos.x.toDouble() + samplingScale, pos.y.toDouble(), pos.z.toDouble())
        val my = invoke(pos.x.toDouble(), pos.y.toDouble() - samplingScale, pos.z.toDouble())
        val py = invoke(pos.x.toDouble(), pos.y.toDouble() + samplingScale, pos.z.toDouble())
        val mz = invoke(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble() - samplingScale)
        val pz = invoke(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble() + samplingScale)

        // Calculate normal by comparing samples on both sides of the position along each axis, and normalizing the result
        normalOut.set(
            (px - mx).toFloat(),
            (py - my).toFloat(),
            (pz - mz).toFloat()).nor()

        return normalOut
    }

    /**
     * Uses gradient decent (and climb) on this function to find some minimum and maximum values within the [volume],
     * and writes them to the [bounds].  May be inexact, especially if the function is very wobbly, so [extraMargin] is added to the bounds.
     * Normally starts in each corner and the center, but only in the center if [centerOnly] is set to true.
     */
    fun gradientDecentBoundsSearch(volume: Volume, bounds: DistanceBounds = DistanceBounds(), maxSteps: Int = 3, centerOnly: Boolean = false, extraMargin: Double = 0.15, volumeMargin: Double = 0.1): DistanceBounds {
        var minValue = Double.POSITIVE_INFINITY
        var maxValue = Double.NEGATIVE_INFINITY

        fun search(findMax: Boolean, startX: Double, startY: Double, startZ: Double, stepSizeMultiplier: Double) {
            // Scale for adjusting step size with after each iteration.
            val stepScale = 0.75 // TODO: There's probably an optimal way to determine this..

            // Calculate initial step size
            var step = (volume.diagonalLength() / maxSteps) * stepSizeMultiplier

            // Flip sign of the step if we are searching for the minimum value instead
            if (!findMax) step = -step

            // Set starting pos
            var x = startX
            var y = startY
            var z = startZ

            // Keep track of previous position
            var prevX = x
            var prevY = y
            var prevZ = z

            // Sample starting point
            val startingValue = this(x, y, z)
            if (startingValue < minValue) minValue = startingValue
            if (startingValue > maxValue) maxValue = startingValue

            // Sample points, follow gradient in correct direction
            for (i in 1..maxSteps) {
                // Sample gradient
                val dx = this(x + step, y, z) - this(x - step, y, z)
                val dy = this(x, y + step, z) - this(x, y - step, z)
                val dz = this(x, y, z + step) - this(x, y, z - step)

                // Check if we are on a flat area, if so there is nothing more to sample.
                val len = sqrt(dx*dx + dy*dy + dz*dz)
                if (len <= 0.0) break

                // Move along gradient (normalize gradient, then multiply with step)
                x += step * dx / len
                y += step * dy / len
                z += step * dz / len

                // Clamp movement to the volume
                x = volume.clampXToVolume(x)
                y = volume.clampYToVolume(y)
                z = volume.clampZToVolume(z)

                // If we did not move (typically because the gradient points towards a corner) we can skip the rest of the steps.
                if (x == prevX && y == prevY && z == prevZ) break

                // Sample new point
                val value = this(x, y, z)
                if (value < minValue) minValue = value
                if (value > maxValue) maxValue = value

                // Reduce step
                step *= stepScale

                // Update previous position
                prevX = x
                prevY = y
                prevZ = z
            }
        }


        if (centerOnly) {
            // Search starting in center
            search(true, volume.centerX, volume.centerY, volume.centerZ, 1.0)
            search(false, volume.centerX, volume.centerY, volume.centerZ, 1.0)
        }
        else {
            val stepSizeMultiplier = 0.6

            // Also search from center in this case
            search(true, volume.centerX, volume.centerY, volume.centerZ, stepSizeMultiplier)
            search(false, volume.centerX, volume.centerY, volume.centerZ, stepSizeMultiplier)

            // Search starting from each corner
            search(true, volume.minX, volume.minY, volume.minZ, stepSizeMultiplier)
            search(false, volume.minX, volume.minY, volume.minZ, stepSizeMultiplier)
            search(true, volume.maxX, volume.minY, volume.minZ, stepSizeMultiplier)
            search(false, volume.maxX, volume.minY, volume.minZ, stepSizeMultiplier)
            search(true, volume.minX, volume.maxY, volume.minZ, stepSizeMultiplier)
            search(false, volume.minX, volume.maxY, volume.minZ, stepSizeMultiplier)
            search(true, volume.maxX, volume.maxY, volume.minZ, stepSizeMultiplier)
            search(false, volume.maxX, volume.maxY, volume.minZ, stepSizeMultiplier)
            search(true, volume.minX, volume.minY, volume.maxZ, stepSizeMultiplier)
            search(false, volume.minX, volume.minY, volume.maxZ, stepSizeMultiplier)
            search(true, volume.maxX, volume.minY, volume.maxZ, stepSizeMultiplier)
            search(false, volume.maxX, volume.minY, volume.maxZ, stepSizeMultiplier)
            search(true, volume.minX, volume.maxY, volume.maxZ, stepSizeMultiplier)
            search(false, volume.minX, volume.maxY, volume.maxZ, stepSizeMultiplier)
            search(true, volume.maxX, volume.maxY, volume.maxZ, stepSizeMultiplier)
            search(false, volume.maxX, volume.maxY, volume.maxZ, stepSizeMultiplier)
        }

        // Expand bounds a bit for error margin, as the real max or min was probably not found
        val boundsMargin = (maxValue - minValue).abs() * extraMargin
        val volumeMargin = volume.getMaxSideSize() * volumeMargin
        val margin = boundsMargin + volumeMargin
        minValue -= margin
        maxValue += margin

        // Set results
        bounds.set(minValue, maxValue)
        return bounds
    }

}

