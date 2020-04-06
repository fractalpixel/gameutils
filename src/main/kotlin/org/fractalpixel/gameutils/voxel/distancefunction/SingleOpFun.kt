package org.fractalpixel.gameutils.voxel.distancefunction

import org.kwrench.geometry.volume.Volume

/**
 * Modifies a DistanceFun with a single operation.
 */
class SingleOpFun(var f: DistanceFun,
                  var calculateBounds: (volume: Volume, a: DistanceFun, aMin: Double, aMax: Double, bounds: DistanceBounds) -> Unit = {
                          volume, a, aMin, aMax, bounds ->
                      bounds.set(op(aMin), op(aMax))
                  },
                  var op: (value: Double) -> Double): DistanceFun {

    override fun invoke(x: Double, y: Double, z: Double): Double {
        return op(f(x, y, z))
    }

    override fun calculateBounds(volume: Volume, bounds: DistanceBounds) {
        // Use same bounds instance to get bounds for the function
        f.getBounds(volume, bounds)
        val aMin = bounds.min
        val aMax = bounds.max

        calculateBounds(volume, f, aMin, aMax, bounds)
    }

}