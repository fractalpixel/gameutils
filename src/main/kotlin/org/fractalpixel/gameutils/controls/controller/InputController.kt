package org.fractalpixel.gameutils.controls.controller

import org.entityflakes.Component
import org.entityflakes.Entity
import org.fractalpixel.gameutils.controls.InputControlSystem
import org.fractalpixel.gameutils.controls.InputMapping
import org.fractalpixel.gameutils.controls.inputproviders.InputElement
import org.fractalpixel.gameutils.controls.inputproviders.InputProviderListener


/**
 * Uses keyboard, mouse, joystick or other InputElements to control some registered controllable(s),
 * using the specified input mapping.
 */
class InputController(var bindings: InputMapping = InputMapping()) : ControllerBase(), Component,
    InputProviderListener {

    private lateinit var inputControlSystem: InputControlSystem

    override fun init(entity: Entity) {
        inputControlSystem = entity.world[InputControlSystem::class]
        inputControlSystem.addListener(this)
    }

    override fun onInputElementChange(inputElement: InputElement, value: Float) {
        val control = bindings[inputElement]
        if (control != null) setControlValue(control, value)
    }

    override fun dispose() {
        inputControlSystem.removeListener(this)
        clearListeners()
        clearControlValues()
    }
}