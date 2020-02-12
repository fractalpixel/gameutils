package org.fractalpixel.gameutils.voxel.distancefunction

import org.fractalpixel.gameutils.utils.distanceTo
import org.kwrench.geometry.double3.Double3

class SphereFun(var radius: Double = 1.0,
                var center: Double3 = Double3.ZEROES): DistanceFun {

    override fun invoke(x: Double, y: Double, z: Double): Double {
        return center.distanceTo(x, y, z) - radius
    }
}