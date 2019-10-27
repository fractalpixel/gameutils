package org.fractalpixel.gameutils.controls

import org.entityflakes.Component
import org.entityflakes.Entity
import org.entityflakes.EntityRef
import org.entityflakes.entitymanager.ComponentRef
import org.fractalpixel.gameutils.controls.controller.Controller
import org.kwrench.strings.toSymbol
import org.kwrench.symbol.Symbol
import java.util.*

/**
 * A component that provides some controls, and can be controlled by a controller.
 * Keeps track of the current control values.
 * Can be used as base class for components that are controllable.
 */
open class Controllable(vararg initialControls: Symbol) : Component {

    constructor (vararg initialControls: String) : this( *(initialControls.map { it.toSymbol() }.toTypedArray()))

    /**
     * The controls provided by this Controllable.
     */
    final val controls: MutableSet<Symbol> = LinkedHashSet(initialControls.toList())

    protected fun addControl(control: Symbol) {
        controls.add(control)
    }

    protected fun removeControl(control: Symbol) {
        controls.remove(control)
    }

    /**
     * The entity with the controller that will control this controllable.
     */
    val controller = EntityRef()

    /**
     * @return the value of the specified control, or the default value if there is no controller for this controllable.
     * Note that this is somewhat slower than caching and using a symbol value for the control id.
     */
    fun getControlValue(control: String, defaultValue: Float = 0f): Float = getControlValue(control.toSymbol(), defaultValue)

    /**
     * @return the value of the specified control, or the default value if there is no controller for this controllable.
     */
    open fun getControlValue(control: Symbol, defaultValue: Float = 0f): Float {
        return if (controls.contains(control)) {
            controller.entity?.get(controllerRef)?.getControl(control, defaultValue) ?: defaultValue
        }
        else {
            defaultValue
        }
    }

    fun resetControls() {
        controller.entity = null
        controls.clear()
    }

    override fun dispose() {
        resetControls()
    }

    override fun init(entity: Entity) {
        // By default use the entity as the controller, if we don't already have a controller set.
        if (controller.entity == null) controller.entity = entity
    }

    companion object {
        val controllerRef = ComponentRef(Controller::class)
    }

}