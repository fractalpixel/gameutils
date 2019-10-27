package org.fractalpixel.gameutils.controls.inputproviders

import org.kwrench.symbol.Symbol


/**
 * Describes an input of some type, e.g. a key, a mouse axis, a mouse button, a controller axis, a controller key, etc.
 * @param code Compact unique representation for this InputElement
 * @param description Human readable description/name of this input element.
 * @param inputProvider the [InputProvider] that provides this input element.
 * @param internalId internal id for this input element (e.g. scancode).  Should be unique.
 */
data class InputElement(val code: Symbol, val description: String, val inputProvider: InputProvider, val internalId: Int) {
}
