package org.fractalpixel.gameutils.controls.inputproviders

import org.entityflakes.World


/**
 * Provides some input events, e.g. for keyboard, mouse, joystick, or other.
 */
interface InputProvider : InputElementProvider {

    /**
     * Called to initialize the provider.
     */
    fun init(world: World)

    /**
     * [InputElement]s provided by this [InputProvider]
     */
    val inputElements: List<InputElement>

    /**
     * Called when the provider will no more be used.
     */
    fun dispose(world: World)

}