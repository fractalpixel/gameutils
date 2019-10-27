package org.fractalpixel.gameutils.space

import org.entityflakes.Entity
import org.kwrench.geometry.double3.Double3
import org.kwrench.geometry.volume.Volume


/**
 * A space that keeps track of the location of entities in it, and allows fetching them using their locations.
 * Could also be used for broad-phase collision detection.
 */
// TODO: Consider a separate space to world transformation for each entityspace?
interface EntitySpace: Space {

    /**
     * Call the specified function for each located entity in the specified volume.
     */
    fun forEachEntity(volume: Volume,
                      entityVisitor: (entity: Entity, entityLocation: Location) -> Unit)

    /**
     * Get all entities in the specified volume.
     */
    fun getEntities(volume: Volume, outputList: MutableList<Location>): MutableList<Location> {
        outputList.clear()
        forEachEntity(volume) { _, location ->
            outputList.add(location)
        }
        return outputList
    }

    /**
     * @param pos target pos to get entities close to
     * @param maxDistance maximum distance to look for entities at
     * @param filter filter to check whether to return the specified entity
     * @return the closest entity to the specified position that is accepted by the filter,
     * or null if no entity at or closer than maxDistance found.
     */
    fun getClosestEntity(pos: Double3,
                         maxDistance: Double = Double.MAX_VALUE,
                         filter: (Location) -> Boolean = {true}): Location?

    /**
     * @param entity located entity to get the closest other entity to
     * @param maxDistance maximum distance to look for entities at
     * @param filter filter to check whether to return the specified entity
     * @return the closest entity to the specified entity, or null if no entity at or closer than maxDistance found.
     */
    fun getClosestEntityToEntity(entity: Location,
                                 maxDistance: Double = Double.MAX_VALUE,
                                 filter: (Location) -> Boolean = {true}): Location? =
            getClosestEntity(entity, maxDistance, { (it !== entity) && filter(it) })

}