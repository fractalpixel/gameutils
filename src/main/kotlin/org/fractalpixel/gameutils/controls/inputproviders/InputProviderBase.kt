package org.fractalpixel.gameutils.controls.inputproviders

import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.ObjectMap
import org.entityflakes.World
import org.kwrench.symbol.Symbol
import java.util.*

/**
 *
 */
abstract class InputProviderBase() : InputProvider {

    final override val inputElements: MutableList<InputElement> = ArrayList()
    private val inputElementsLookup = IntMap<InputElement>()
    private val inputElementsIdLookup = ObjectMap<Symbol, InputElement>()
    private val listeners = ArrayList<InputProviderListener>()

    final override fun addListener(listener: InputProviderListener) {
        listeners.add(listener)
    }

    final override fun removeListener(listener: InputProviderListener) {
        listeners.remove(listener)
    }

    /**
     * @param id Compact unique representation for this InputElement
     * @param desc Human readable description/name of this input element.
     * @param internalId internal id for this input element (e.g. scancode).  Should be unique.
     */
    final protected fun registerInputElement(id: Symbol, desc: String, internalId: Int) {
        val inputElement = InputElement(id, desc, this, internalId)
        if (inputElementsLookup.containsKey(internalId)) throw IllegalArgumentException("Problem when registering input element '$id' for ${javaClass.simpleName}: The internal id $internalId is already used.")
        if (inputElementsIdLookup.containsKey(id)) throw IllegalArgumentException("Problem when registering input element '$id' for ${javaClass.simpleName}: The id $id is already used.")
        inputElementsLookup.put(internalId, inputElement)
        inputElementsIdLookup.put(id, inputElement)
        inputElements.add(inputElement)
    }

    /**
     * Notify listeners using the numerical internal id of the input element.
     */
    final protected fun notifyListners(inputElementInternalId: Int, value: Float) {
        for (listener in listeners) {
            listener.onInputElementChange(inputElementsLookup.get(inputElementInternalId), value)
        }
    }

    /**
     * Notify listeners using the id symbol of the input element.
     */
    final protected fun notifyListners(inputElementId: Symbol, value: Float) {
        for (listener in listeners) {
            listener.onInputElementChange(inputElementsIdLookup.get(inputElementId), value)
        }
    }

    final override fun dispose(world: World) {
        doDispose(world);
        listeners.clear()
        inputElements.clear()
        inputElementsLookup.clear()
        inputElementsIdLookup.clear()
    }

    protected open fun doDispose(world: World) {
    }
}