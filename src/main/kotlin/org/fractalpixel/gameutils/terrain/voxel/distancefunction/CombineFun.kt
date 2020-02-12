package org.fractalpixel.gameutils.terrain.voxel.distancefunction

/**
 * Combines the two specified distance functions using the specified operator.
 */
class CombineFun(var a: DistanceFun,
                 var b: DistanceFun,
                 var op :(a: Double, b: Double) -> Double):
    DistanceFun {

    override fun invoke(x: Double, y: Double, z: Double): Double {
        return op(a(x, y, z), b(x, y, z))
    }
}