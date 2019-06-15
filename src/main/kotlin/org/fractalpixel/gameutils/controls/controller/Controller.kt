package org.fractalpixel.gameutils.controls.controller

import org.entityflakes.Component
import org.entityflakes.PolymorphicComponent
import org.mistutils.symbol.Symbol


/**
 * Controls zero or more controllables.
 */
interface Controller : PolymorphicComponent {

    /**
     * @listener listener that is notified of control signals from this controller.
     */
    fun addListener(listener: ControllerListener)

    /**
     * @listener listener to remove.
     */
    fun removeListener(listener: ControllerListener)

    /**
     * The value for the specified control, or the default value if that value is not controlled.
     */
    fun getControl(controlId: Symbol, defaultValue: Float = 0f): Float

    override val componentCategory: Class<out Component> get() = Controller::class.java
}