package org.fractalpixel.gameutils.controls.inputproviders

import java.util.*

/**
 * Forward input provider listener events to all registered listeners.
 */
class DelegatingInputProviderListener(): InputProviderListener, InputElementProvider {
    private val listeners = ArrayList<InputProviderListener>()

    override fun addListener(listener: InputProviderListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: InputProviderListener) {
        listeners.remove(listener)
    }

    override fun onInputElementChange(inputElement: InputElement, value: Float) {
        for (listener in listeners) {
            listener.onInputElementChange(inputElement, value)
        }
    }
}