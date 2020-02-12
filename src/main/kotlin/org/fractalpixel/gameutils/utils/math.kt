package org.fractalpixel.gameutils.utils

import org.kwrench.geometry.double3.Double3
import kotlin.math.sqrt


fun Double3.distanceTo(x: Double, y: Double, z: Double): Double {
    val dx = x - this.x
    val dy = y - this.y
    val dz = z - this.z
    return sqrt(dx*dx + dy*dy + dz*dz)
}
