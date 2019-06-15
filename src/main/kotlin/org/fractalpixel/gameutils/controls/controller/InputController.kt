package org.fractalpixel.gameutils.controls.controller

import org.entityflakes.Component
import org.entityflakes.Entity
import org.fractalpixel.gameutils.controls.InputControlProcessor
import org.fractalpixel.gameutils.controls.InputMapping
import org.fractalpixel.gameutils.controls.inputproviders.InputElement
import org.fractalpixel.gameutils.controls.inputproviders.InputProviderListener


/**
 * Uses keyboard, mouse, joystick or other InputElements to control some registered controllable(s),
 * using the specified input mapping.
 */
class InputController(var bindings: InputMapping = InputMapping()) : ControllerBase(), Component,
    InputProviderListener {

    private lateinit var inputControlProcessor: InputControlProcessor

    override fun init(entity: Entity) {
        inputControlProcessor = entity.world[InputControlProcessor::class]
        inputControlProcessor.addListener(this)
    }

    override fun onInputElementChange(inputElement: InputElement, value: Float) {
        val control = bindings[inputElement]
        if (control != null) setControlValue(control, value)
    }

    override fun dispose() {
        inputControlProcessor.removeListener(this)
        clearListeners()
        clearControlValues()
    }
}