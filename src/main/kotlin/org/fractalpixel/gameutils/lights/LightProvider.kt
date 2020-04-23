package org.fractalpixel.gameutils.lights

import org.entityflakes.Entity
import org.fractalpixel.gameutils.space.Location
import org.kwrench.geometry.double3.Double3
import org.kwrench.geometry.volume.Volume

/**
 * Returns lights relevant to a scene.
 */
// TODO: Add ambient lights
interface LightProvider {

    fun forEachPointLight(
        volume: Volume,
        minimumSize: Double,
        visitor: (entity: Entity, location: Location, light: SphericalLight) -> Unit
    )

    /**
     * Loop point lights where the pos falls inside the effect radius of the point light.
     */
    fun forEachPointLightInRange(
        pos: Double3,
        visitor: (entity: Entity, location: Location, light: SphericalLight, squaredDistance: Double) -> Unit
    )

    fun forEachInfiniteLight(visitor: (entity: Entity, light: InfiniteLight) -> Unit)

}
