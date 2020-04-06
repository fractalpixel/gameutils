package org.fractalpixel.gameutils.voxel.distancefunction

import org.kwrench.geometry.volume.Volume

class ConstantFun(var value: Double = 0.0): DistanceFun {
    override fun invoke(x: Double, y: Double, z: Double): Double {
        return value
    }

    override fun calculateBounds(volume: Volume, bounds: DistanceBounds) {
        bounds.setBothTo(value)
    }
}