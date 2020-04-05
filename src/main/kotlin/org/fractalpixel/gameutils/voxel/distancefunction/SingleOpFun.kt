package org.fractalpixel.gameutils.voxel.distancefunction

import org.kwrench.geometry.volume.Volume

/**
 * Modifies a DistanceFun with a single operation.
 */
class SingleOpFun(var f: DistanceFun,
                  var invertMinMax: Boolean = false,
                  var calculateMin: (a: DistanceFun, volume: Volume) -> Double = {a, volume -> op(if (invertMinMax) a.getMax(volume) else a.getMin(volume)) },
                  var calculateMax: (a: DistanceFun, volume: Volume) -> Double = {a, volume -> op(if (invertMinMax) a.getMin(volume) else a.getMax(volume)) },
                  var op: (value: Double) -> Double): DistanceFun {

    override fun invoke(x: Double, y: Double, z: Double): Double {
        return op(f(x, y, z))
    }

    override fun getMin(volume: Volume): Double {
        return calculateMin(f, volume)
    }

    override fun getMax(volume: Volume): Double {
        return calculateMax(f, volume)
    }
}