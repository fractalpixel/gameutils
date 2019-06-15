package org.fractalpixel.gameutils.controls.inputproviders

/**
 *
 */
interface InputElementProvider {

    fun addListener(listener: InputProviderListener)

    fun removeListener(listener: InputProviderListener)



}