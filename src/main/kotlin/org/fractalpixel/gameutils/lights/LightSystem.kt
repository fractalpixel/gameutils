package org.fractalpixel.gameutils.lights

import org.entityflakes.Entity
import org.entityflakes.World
import org.entityflakes.entityfilters.RequiredComponentsFilter
import org.entityflakes.entitygroup.EntityGroup
import org.entityflakes.entitymanager.ComponentRef
import org.entityflakes.system.SystemBase
import org.fractalpixel.gameutils.space.Location
import org.fractalpixel.gameutils.utils.SpatialEntityGroup
import org.kwrench.geometry.double3.Double3
import org.kwrench.geometry.volume.Volume

/**
 * Keeps track of the lights
 */
class LightSystem(): SystemBase(), LightProvider {

    private val sphericalLightRef = ComponentRef(SphericalLight::class)
    private val directedLightRef = ComponentRef(InfiniteLight::class)
    private val locationRef = ComponentRef(Location::class)
    private lateinit var pointLights: SpatialEntityGroup
    private lateinit var directionalLights: EntityGroup

    override fun doInit(world: World) {
        // Get groups that are kept up to date and contain all pointlights / directional lights
        pointLights = SpatialEntityGroup(RequiredComponentsFilter(world,  locationRef, sphericalLightRef), world)
        directionalLights = world.getEntityGroup(directedLightRef)
    }

    override fun doDispose() {
    }

    override fun forEachInfiniteLight(visitor: (entity: Entity, light: InfiniteLight) -> Unit) {
        directionalLights.forEachEntity { entity ->
            val light = directedLightRef[entity]
            visitor(entity, light)
        }
    }

    override fun forEachPointLight(volume: Volume, minimumSize: Double, visitor: (entity: Entity, location: Location, sphericalLight: SphericalLight) -> Unit) {
        pointLights.forEachEntityInVolume(volume, minimumSize) {entity ->
            val location = locationRef[entity]
            val sphericalLight = sphericalLightRef[entity]
            visitor(entity, location, sphericalLight)
        }
    }

    override fun forEachPointLightInRange(
        pos: Double3,
        visitor: (entity: Entity, location: Location, light: SphericalLight, squaredDistance: Double) -> Unit
    ) {
        pointLights.forEachEntityOverlapping(pos) {entity, squaredDistance ->
            val location = locationRef[entity]
            val sphericalLight = sphericalLightRef[entity]
            visitor(entity, location, sphericalLight, squaredDistance)
        }
    }

}