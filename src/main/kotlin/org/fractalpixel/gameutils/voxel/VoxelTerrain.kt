package org.fractalpixel.gameutils.voxel

import org.fractalpixel.gameutils.lights.LightProvider
import org.fractalpixel.gameutils.voxel.distancefunction.DistanceFun
import org.kwrench.geometry.double3.Double3

/**
 * Holds data about a voxel terrain.
 */
// TODO: Listener interface for changes.  Changes should be pooled over some short time to avoid doing too much mesh-creation
// TODO: Add materials & texturing (blend between 3 most present materials or similar - keep track of material amounts
//       in small integer values (grams / milliliters)? -> no risk of creating / destroying matter)
class VoxelTerrain(val distanceFun: DistanceFun, val lightProvider: LightProvider) {


}