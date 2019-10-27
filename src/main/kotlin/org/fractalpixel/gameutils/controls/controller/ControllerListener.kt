package org.fractalpixel.gameutils.controls.controller

import org.kwrench.symbol.Symbol


/**
 *
 */
interface ControllerListener {

    /**
     * Called when the specified control changed value.
     */
    fun onControlChanged(control: Symbol, value: Float)

}