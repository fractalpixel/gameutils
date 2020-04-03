package org.fractalpixel.gameutils.utils

/**
 * Interface for types that can be added to [RecyclingPool]s.
 */
interface Recyclable {

    /**
     * Prepare this object for re-use.
     * Called when this object is placed in a pool after being used.
     * Could zero any fields and such.
     */
    fun reset()

    /**
     * Called when this object will no longer be used,
     * should free any memory or resources it has.
     * May be called after reset.
     */
    fun dispose()

}