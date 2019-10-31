package org.fractalpixel.gameutils.space

import com.badlogic.gdx.math.Quaternion
import org.entityflakes.ReusableComponent

/**
 * Represents the direction some object is facing in 3D coordinates, using a quaternion.
 */
class Facing(initialDirection: Quaternion? = null): ReusableComponent {

    /**
     * Facing direction, as a quaternion.
     */
    val direction: Quaternion = Quaternion(initialDirection ?: IDENTITY_QUATERNION)


    override fun reset() {
        // Reset direction before reuse.  Not strictly necessary.
        direction.idt()
    }

    companion object {
        private val IDENTITY_QUATERNION = Quaternion().idt()
    }
}