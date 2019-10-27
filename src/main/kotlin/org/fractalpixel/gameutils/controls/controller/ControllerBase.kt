package org.fractalpixel.gameutils.controls.controller

import com.badlogic.gdx.utils.ObjectFloatMap
import org.kwrench.symbol.Symbol
import java.util.*

/**
 *
 */
abstract class ControllerBase() : Controller {

    private val listeners = ArrayList<ControllerListener>(3)

    private val controlValues = ObjectFloatMap<Symbol>(10)

    /**
     * Set the value for the specified control, and notify listeners if the value changed.
     */
    protected final fun setControlValue(controlId: Symbol, value: Float) {
        if (value != controlValues.get(controlId, value+1f)) {
            controlValues.put(controlId, value)
            notifyControlChange(controlId, value)
        }
    }

    /**
     * Get the current value for the control, or the default value (defaults to 0f) if the control does not have any assigned value.
     */
    override final fun getControl(controlId: Symbol, defaultValue: Float): Float {
        return controlValues.get(controlId, defaultValue)
    }

    protected final fun removeControlValue(controlId: Symbol) {
        controlValues.remove(controlId, 0f)
    }

    protected final fun clearControlValues() {
        controlValues.clear()
    }

    final override fun addListener(listener: ControllerListener) {
        listeners.add(listener)
    }

    final override fun removeListener(listener: ControllerListener) {
        listeners.remove(listener)
    }

    private fun notifyControlChange(control: Symbol, value: Float) {
        for (listener in listeners) {
            listener.onControlChanged(control, value)
        }
    }

    protected final fun clearListeners() {
        listeners.clear()
    }
}