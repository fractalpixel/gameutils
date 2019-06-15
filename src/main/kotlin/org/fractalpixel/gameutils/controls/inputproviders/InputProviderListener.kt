package org.fractalpixel.gameutils.controls.inputproviders

/**
 *
 */
interface InputProviderListener {

    /**
     * Called when the specified input element changes in value.
     */
    fun onInputElementChange(inputElement: InputElement, value: Float)

}