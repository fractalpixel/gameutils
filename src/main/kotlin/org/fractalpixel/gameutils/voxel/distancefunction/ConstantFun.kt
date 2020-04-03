package org.fractalpixel.gameutils.voxel.distancefunction

class ConstantFun(var value: Double = 0.0): DistanceFun {
    override fun invoke(x: Double, y: Double, z: Double): Double {
        return value
    }
}