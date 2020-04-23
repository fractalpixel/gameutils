package org.fractalpixel.gameutils.space

import org.entityflakes.Entity
import org.kwrench.geometry.double3.Double3
import org.kwrench.geometry.volume.Volume

// TODO: Implement sparse Octree EntitySpace to store entities for fast retrieval
// TODO: Alternatively hash space

/**
 * Brute force implementation of EntitySpace, simply loops through all entities when an entity in some place is requested.
 */
class BruteForceEntitySpace(): EntitySpace {

    val entityLocations: MutableList<Location> = ArrayList()

    override fun addLocatedEntity(location: Location) {
        entityLocations.add(location)
    }

    override fun removeLocatedEntity(location: Location) {
        entityLocations.remove(location)
    }

    override fun updateLocatedEntityPosition(location: Location, oldX: Double, oldY: Double, oldZ: Double) {
        // Ignored
    }

    override fun updateLocatedEntityDiameter(location: Location, oldDiameter: Double) {
        // Ignored
    }

    override fun forEachEntity(volume: Volume,
                               entityVisitor: (entity: Entity, entityLocation: Location) -> Unit) {
        for (entityLocation in entityLocations) {
            // TODO: Include entity radius when checking whether the volume contains the entity
            if (volume.contains(entityLocation)) {
                val entity = entityLocation.entity
                if (entity != null) {
                    entityVisitor(entity, entityLocation)
                }
            }
        }
    }

    override fun getClosestEntity(pos: Double3,
                                  maxDistance: Double,
                                  filter: (Location) -> Boolean): Location? {
        var closestDistance = Double.POSITIVE_INFINITY
        var closestEntity: Location? = null
        for (entityLocation in entityLocations) {
            val distance = pos.distanceTo(entityLocation)
            if (distance < closestDistance && filter(entityLocation)) {
                closestDistance = distance
                closestEntity = entityLocation
            }
        }

        return closestEntity
    }
}