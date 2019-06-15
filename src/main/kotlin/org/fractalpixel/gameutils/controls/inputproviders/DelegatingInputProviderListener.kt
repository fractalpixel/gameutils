package org.fractalpixel.gameutils.controls.inputproviders

import java.util.*

/**
 *
 */
class DelegatingInputProviderListener(): InputProviderListener, InputElementProvider {
    private val listeners = ArrayList<InputProviderListener>()

    override final fun addListener(listener: InputProviderListener) {
        listeners.add(listener)
    }

    override final fun removeListener(listener: InputProviderListener) {
        listeners.remove(listener)
    }

    override fun onInputElementChange(inputElement: InputElement, value: Float) {
        for (listener in listeners) {
            listener.onInputElementChange(inputElement, value)
        }
    }
}