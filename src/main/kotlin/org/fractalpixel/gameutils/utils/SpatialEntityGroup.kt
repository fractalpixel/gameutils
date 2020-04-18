package org.fractalpixel.gameutils.utils

import org.entityflakes.Entity
import org.entityflakes.EntityListener
import org.entityflakes.entityfilters.EntityFilter
import org.entityflakes.entitygroup.EntityGroup
import org.entityflakes.entitygroup.EntityGroupListener
import org.entityflakes.entitymanager.ComponentRef
import org.entityflakes.entitymanager.EntityManager
import org.fractalpixel.gameutils.space.Location
import org.kwrench.collections.bag.Bag
import org.kwrench.geometry.volume.Volume
import java.util.ArrayList

/**
 * Keeps track of entities matching a certain filter, and allows querying the ones in a given volume.
 * Stores the entities in an efficient spatial look-up structure.
 */
// TODO: Needs to be thread / co-routine safe...?
class SpatialEntityGroup(val filter: EntityFilter, entityManager: EntityManager): EntityGroup {

    private val entities_ = Bag<Entity>()
    private val listeners = ArrayList<EntityGroupListener>()

    private val locationRef = ComponentRef(Location::class)

    // TODO: Use actual spatial storage system.  Maybe multi-level hash grid?  Need radius information for objects to place them in correct level.
    // TODO: Needs bounding sphere in Location
    fun forEachEntityInVolume(volume: Volume, minimumSize: Double, entityVisitor: (Entity) -> Unit) {

        // DEBUG: Just brute force for now
        for (entity in entities_) {
            val location = locationRef[entity]
            // TODO: Add containsWithRadius or similar? (basically distance to point squared < radius squared)
            if (volume.contains(location)) {
                entityVisitor(entity)
            }
        }
    }


    private val entityDeletionListener = object : EntityListener {
        override fun onEntityRemoved(entity: Entity) {
            removeEntity(entity)
        }
    }

    /**
     * The entities currently in this group.
     */
    val entities: Iterable<Entity> = entities_

    override fun forEachEntity(entityVisitor: (Entity) -> Unit) {
        for (i in 0 until entities_.size()) {
            entityVisitor(entities_[i])
        }
    }

    override fun contains(entityId: Int): Boolean {
        for (i in 0 until entities_.size()) {
            if (entities_[i].id == entityId) return true
        }
        return false
    }

    override fun contains(entity: Entity): Boolean {
        return entities_.contains(entity)
    }

    /**
     * Add entity, if not already added.
     * Notifies listeners of this group.
     */
    fun addEntity(entity: Entity) {
        if (!entities_.contains(entity)) {
            entities_.add(entity)
            entity.addListener(entityDeletionListener)
            notifyEntityAdded(entity)
        }
    }

    /**
     * Remove entity, if contained.
     * Notifies listeners of this group.
     */
    fun removeEntity(entity: Entity) {
        val removed = entities_.remove(entity)
        if (removed) {
            entity.removeListener(entityDeletionListener)
            notifyEntityRemoved(entity)
        }
    }

    /**
     * Removes all entities in this group.
     * Notifies listeners.
     */
    fun removeAllEntities() {
        while (!entities_.isEmpty) {
            // Get last
            val e = entities_.get(entities_.size() - 1)

            // Remove it
            removeEntity(e)
        }
    }

    override fun addListener(listener: EntityGroupListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: EntityGroupListener) {
        listeners.remove(listener)
    }

    private fun notifyEntityAdded(entity: Entity) {
        for (listener in listeners) {
            listener.onEntityAdded(entity)
        }
    }

    private fun notifyEntityRemoved(entity: Entity) {
        for (listener in listeners) {
            listener.onEntityRemoved(entity)
        }
    }

}