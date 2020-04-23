package org.fractalpixel.gameutils.space

import com.badlogic.gdx.math.Vector3
import org.entityflakes.Entity
import org.entityflakes.ReusableComponentBase
import org.kwrench.geometry.double3.Double3
import java.lang.IllegalArgumentException


/**
 * Component for something located in some space.
 * @param initialX the initial position of the entity.
 * @param initialY the initial position of the entity.
 * @param initialZ the initial position of the entity.
 * @param initialDiameter the initial bounding diameter of the entity, or 0 if not relevant.
 * @param initialSpace the space that the entity is initially located in, or null if nowhere.
 */
// TODO: Add function to get/set/modify world position of entity by applying transformations for any spaces it is located in
class Location(initialX: Double = 0.0,
               initialY: Double = 0.0,
               initialZ: Double = 0.0,
               initialDiameter: Double = 0.0,
               initialSpace: Space? = null): ReusableComponentBase(), Double3 {

    /**
     * @param initialPos the initial position of the entity.
     * @param initialSpace the space that the entity is initially located in, or null if nowhere.
     */
    constructor(initialPos: Double3,
                initialDiameter: Double = 0.0,
                initialSpace: Space? = null): this(initialPos.x,
                                                   initialPos.y,
                                                   initialPos.z,
                                                   initialDiameter,
                                                   initialSpace)

    // Position
    override var x: Double = initialX
        private set
    override var y: Double = initialY
        private set
    override var z: Double = initialZ
        private set

    // Diameter
    var diameter: Double = initialDiameter
        private set

    val radius: Double get() = diameter * 0.5

    /**
     * The space that the entity is currently located in.
     */
    var space: Space? = initialSpace
        set(newSpace) {
            val oldSpace = field
            if (isInitialized()) oldSpace?.removeLocatedEntity(this)
            field = newSpace
            if (isInitialized()) newSpace?.addLocatedEntity(this)
        }

    /**
     * Set a new position
     */
    fun setPosition(newPos: Double3) {
        setPosition(newPos.x, newPos.y, newPos.z)
    }

    /**
     * Set a new position
     */
    fun setPosition(newPos: Vector3) {
        setPosition(newPos.x.toDouble(), newPos.y.toDouble(), newPos.z.toDouble())
    }

    /**
     * Set a new position
     */
    fun setPosition(newX: Double = 0.0,
                    newY: Double = 0.0,
                    newZ: Double = 0.0) {
        // Check if position differs
        if (newX != x ||
            newY != y ||
            newZ != z) {

            val oldX = x
            val oldY = y
            val oldZ = z

            x = newX
            y = newY
            z = newZ

            // Notify space about move
            if (isInitialized()) space?.updateLocatedEntityPosition(this, oldX, oldY, oldZ)
        }
    }

    fun setBoundingSphereDiameter(newDiameter: Double) {
        if (diameter != newDiameter) {
            if (newDiameter.isNaN() || newDiameter < 0.0) throw IllegalArgumentException("The diameter should be positive, but was $newDiameter")

            val oldDiameter = diameter
            diameter = newDiameter

            // Notify space about diameter change
            if (isInitialized()) space?.updateLocatedEntityDiameter(this, oldDiameter)
        }
    }

    /**
     * Adds the specified vector to the position
     */
    fun movePosition(positionDelta: Double3) {
        setPosition(x + positionDelta.x,
                    y + positionDelta.y,
                    z + positionDelta.z)
    }

    /**
     * Adds the specified vector to the position
     */
    fun movePosition(positionDelta: Vector3) {
        setPosition(x + positionDelta.x,
                    y + positionDelta.y,
                    z + positionDelta.z)
    }

    /**
     * Adds the specified vector to the position
     */
    fun movePosition(deltaX: Double = 0.0,
                     deltaY: Double = 0.0,
                     deltaZ: Double = 0.0) {
        setPosition(x + deltaX,
                    y + deltaY,
                    z + deltaZ)
    }

    override fun doInit(entity: Entity) {
        // Add to space if we have one
        space?.addLocatedEntity(this)
    }



    override fun doReset(oldEntity: Entity) {
        clear()
    }

    override fun doDispose() {
        clear()
    }

    /**
     * Removes this location from any space and sets its position to 0,0,0
     */
    fun clear() {
        space = null
        x = 0.0
        y = 0.0
        z = 0.0
    }

    /**
     * True if this component is currently assigned to an entity.
     */
    private inline fun isInitialized() = entity != null
}
