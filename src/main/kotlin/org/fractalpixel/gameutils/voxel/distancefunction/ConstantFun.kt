package org.fractalpixel.gameutils.voxel.distancefunction

import org.kwrench.geometry.volume.Volume

class ConstantFun(var value: Double = 0.0): DistanceFun {
    override fun get(x: Double, y: Double, z: Double, sampleSize: Double): Double {
        return value
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