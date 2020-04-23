package org.fractalpixel.gameutils.space

import org.entityflakes.Component
import org.entityflakes.PolymorphicComponent

/**
 * Something that contains entities (typically in some spatial arrangement).
 * Notified by the Location component of the entity when entities are added or removed, or moved.
 * If the space is disposed, it should notify all Locations that they are no longer in a Space.
 */
// TODO: Combine Space and EntitySpace?
interface Space: PolymorphicComponent {

    /**
     * Called by the Location object when the space of it is set to this space.
     * Should not be called by client code.
     */
    fun addLocatedEntity(location: Location)

    /**
     * Called by the Location object when the space of it is set to some other space.
     * Should not be called by client code.
     */
    fun removeLocatedEntity(location: Location)

    /**
     * Called by the Location when the position is changed.
     * Should not be called by client code.
     * @param location object, also has new new position.
     * @param oldX Previous position of the entity.
     * @param oldY Previous position of the entity.
     * @param oldZ Previous position of the entity.
     */
    fun updateLocatedEntityPosition(location: Location,
                                    oldX: Double,
                                    oldY: Double,
                                    oldZ: Double)

    /**
     * Called when the bounding sphere diameter of a located entity changes.
     */
    fun updateLocatedEntityDiameter(location: Location, oldDiameter: Double)

    override val componentCategory: Class<out Component> get() = Space::class.java
}