package org.fractalpixel.gameutils.lights

import org.entityflakes.Entity
import org.fractalpixel.gameutils.space.Location
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

    fun forEachInfiniteLight(visitor: (entity: Entity, light: InfiniteLight) -> Unit)

}
