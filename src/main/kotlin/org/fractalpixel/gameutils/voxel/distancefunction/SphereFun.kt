package org.fractalpixel.gameutils.voxel.distancefunction

import org.fractalpixel.gameutils.utils.distanceTo
import org.fractalpixel.gameutils.utils.distanceToPoint
import org.fractalpixel.gameutils.utils.maximumDistanceToPoint
import org.kwrench.geometry.double3.Double3
import org.kwrench.geometry.volume.Volume

class SphereFun(var radius: Double = 1.0,
                var center: Double3 = Double3.ZEROES): DistanceFun {

    override fun invoke(x: Double, y: Double, z: Double): Double {
        return center.distanceTo(x, y, z) - radius
    }

    override fun calculateBounds(volume: Volume, bounds: DistanceBounds) {
        // Get at shortest distance between sphere center and axis aligned volume,
        // subtract radius to get the  SphereFun value at that distance.
        val min = volume.distanceToPoint(center) - radius

        // Get the maximum distance from any point in the volume to the center,
        // subtract radius to get the SphereFun value at that distance.
        val max = volume.maximumDistanceToPoint(center) - radius

        bounds.set(min, max)
    }

}