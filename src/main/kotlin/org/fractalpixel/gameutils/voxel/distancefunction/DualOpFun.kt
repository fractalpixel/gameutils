package org.fractalpixel.gameutils.voxel.distancefunction

import org.fractalpixel.gameutils.utils.checkForJobCancellation
import org.fractalpixel.gameutils.voxel.distancefunction.utils.CompilationContext
import org.fractalpixel.gameutils.voxel.distancefunction.utils.DepthBlock
import org.fractalpixel.gameutils.voxel.distancefunction.utils.DepthBlockPool
import org.fractalpixel.gameutils.voxel.distancefunction.utils.DistanceBounds
import org.kwrench.geometry.volume.Volume

/**
 * Combines the two specified distance functions using the specified operator.
 *
 * To enable calculating the minimum and maximum values in a volume, you need to
 * define whether the minimum or the maximum should be sampled for each distance function when calculating
 * the minim or maximum of their combination. (Normally it uses minimum of each distance
 * function when calculating the minimum of the combination, and maximum of both when calculating the maximum,
 * but for cases like subtraction they are different (minimum of combination a - b happens when a is sampled with minimum
 * and b is sampled with maximum).
 */
class DualOpFun(override val name: String,
                var a: DistanceFun,
                var b: DistanceFun,
                inline var calculateBounds: (volume: Volume, sampleSize: Double, a: DistanceFun, b: DistanceFun, aMin: Double, aMax: Double, bMin: Double, bMax: Double,  bounds: DistanceBounds) -> Unit = {
                     volume, sampleSize, a, b, aMin, aMax, bMin, bMax, bounds ->
                     bounds.set(op(aMin, bMin), op(aMax, bMax))
                 },
                var codeExpression: String,
                var code: String = "double #out = $codeExpression;",
                inline var op :(a: Double, b: Double) -> Double): CompilingDistanceFun() {

    /*
    override fun get(x: Double, y: Double, z: Double, sampleSize: Double): Double {
        return op(a.get(x, y, z, sampleSize), b.get(x, y, z, sampleSize))
    }
    */

    override fun constructCode(codeOut: StringBuilder, context: CompilationContext) {
        context.createCall(codeOut, a, "a")
        context.createCall(codeOut, b, "b")
        codeOut.append(code)
    }

    override suspend fun calculateBlock(
        volume: Volume,
        block: DepthBlock,
        blockPool: DepthBlockPool,
        leadingSeam: Int,
        trailingSeam: Int
    ) {
        checkForJobCancellation()

        // Calculate input a, use the given block
        a.calculateBlock(volume, block, blockPool, leadingSeam, trailingSeam)

        checkForJobCancellation()

        // Reserve separate block for results of b and release it when done
        blockPool.withObtained { bBlock ->

            // Calculate input b
            b.calculateBlock(volume, bBlock, blockPool, leadingSeam, trailingSeam)

            checkForJobCancellation()

            // Apply operation to each value
            val depths = block.depths
            val bDepths = bBlock.depths
            for (i in depths.indices) {
                depths[i] = op(depths[i], bDepths[i])
            }
        }
    }

    override fun calculateBounds(volume: Volume, sampleSize: Double, bounds: DistanceBounds) {
        // Use same bounds instance to get bounds for both a and b
        a.getBounds(volume, sampleSize, bounds)
        val aMin = bounds.min
        val aMax = bounds.max

        b.getBounds(volume, sampleSize, bounds)
        val bMin = bounds.min
        val bMax = bounds.max

        calculateBounds(volume, sampleSize, a, b, aMin, aMax, bMin, bMax, bounds)
    }

}