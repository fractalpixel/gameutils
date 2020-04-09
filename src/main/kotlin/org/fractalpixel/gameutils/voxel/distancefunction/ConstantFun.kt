package org.fractalpixel.gameutils.voxel.distancefunction

import org.fractalpixel.gameutils.voxel.distancefunction.utils.CompilationContext
import org.fractalpixel.gameutils.voxel.distancefunction.utils.DepthBlock
import org.fractalpixel.gameutils.voxel.distancefunction.utils.DepthBlockPool
import org.fractalpixel.gameutils.voxel.distancefunction.utils.DistanceBounds
import org.kwrench.geometry.volume.Volume

class ConstantFun(var value: Double = 0.0): CompilingDistanceFun() {

    override val name: String get() = "Constant ($value)"

    override fun constructCode(codeOut: StringBuilder, context: CompilationContext) {
        codeOut.append("double #out = $value;")
    }

    override suspend fun calculateBlock(
        volume: Volume,
        block: DepthBlock,
        blockPool: DepthBlockPool,
        leadingSeam: Int,
        trailingSeam: Int
    ) {
        block.fill(value)
    }

    override fun calculateBounds(volume: Volume, sampleSize: Double, bounds: DistanceBounds) {
        bounds.setBothTo(value)
    }
}