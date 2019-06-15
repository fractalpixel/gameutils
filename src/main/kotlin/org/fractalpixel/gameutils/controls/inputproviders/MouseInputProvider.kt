package org.fractalpixel.gameutils.controls.inputproviders

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import org.entityflakes.World
import org.fractalpixel.gameutils.GameService
import org.mistutils.strings.toSpaceSeparated
import org.mistutils.strings.toSymbol
import org.mistutils.symbol.Symbol

/**
 *
 */
class MouseInputProvider : InputProviderBase() {

    private var nextFreeInternalId: Int = 1

    private val mouseListener: InputAdapter = object : InputAdapter() {
        override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
            notifyListenersOfPosition(mouseX, mouseY, mouseXNormalized, mouseYNormalized, screenX, screenY)
            return false
        }

        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            when(pointer) {
                0 -> notifyListners(touch0Pressed, 1f)
                1 -> notifyListners(touch1Pressed, 1f)
                2 -> notifyListners(touch2Pressed, 1f)
            }
            when(button) {
                Input.Buttons.LEFT -> notifyListners(leftMousePressed, 1f)
                Input.Buttons.RIGHT -> notifyListners(rightMousePressed, 1f)
                Input.Buttons.MIDDLE -> notifyListners(midMousePressed, 1f)
                Input.Buttons.BACK -> notifyListners(backMousePressed, 1f)
                Input.Buttons.FORWARD -> notifyListners(forwardMousePressed, 1f)
            }
            return false
        }

        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            when(pointer) {
                0 -> notifyListners(touch0Pressed, 0f)
                1 -> notifyListners(touch1Pressed, 0f)
                2 -> notifyListners(touch2Pressed, 0f)
            }
            when(button) {
                Input.Buttons.LEFT -> notifyListners(leftMousePressed, 0f)
                Input.Buttons.RIGHT -> notifyListners(rightMousePressed, 0f)
                Input.Buttons.MIDDLE -> notifyListners(midMousePressed, 0f)
                Input.Buttons.BACK -> notifyListners(backMousePressed, 0f)
                Input.Buttons.FORWARD -> notifyListners(forwardMousePressed, 0f)
            }
            return false
        }

        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            when(pointer) {
                0 -> notifyListenersOfPosition(touch0X, touch0Y, touch0XNormalized, touch0YNormalized, screenX, screenY)
                1 -> notifyListenersOfPosition(touch1X, touch1Y, touch1XNormalized, touch1YNormalized, screenX, screenY)
                2 -> notifyListenersOfPosition(touch2X, touch2Y, touch2XNormalized, touch2YNormalized, screenX, screenY)
            }
            return false
        }
    }

    override fun init(world: World) {
        registerMouseInputs()

        // Listen to inputs
        world[GameService::class].addInputListener(mouseListener)
    }


    override fun doDispose(world: World) {
        // Stop listening to inputs
        world[GameService::class].removeInputListener(mouseListener)
    }

    private fun notifyListenersOfPosition(positionX: Symbol, positionY: Symbol,
                                          positionXNor: Symbol, positionYNor: Symbol,
                                          screenX: Int, screenY: Int) {
        val normalizedX = 2f * screenX / Gdx.graphics.width - 1f
        val normalizedY = 2f * screenY / Gdx.graphics.height - 1f
        notifyListners(positionX, screenX.toFloat())
        notifyListners(positionY, screenY.toFloat())
        notifyListners(positionXNor, normalizedX)
        notifyListners(positionYNor, normalizedY)
    }


    private fun registerMouseInputs() {
        registerInput(mouseX)
        registerInput(mouseY)
        registerInput(mouseXNormalized)
        registerInput(mouseYNormalized)
        registerInput(leftMousePressed)
        registerInput(rightMousePressed)
        registerInput(midMousePressed)
        registerInput(backMousePressed)
        registerInput(forwardMousePressed)
        registerInput(touch0Pressed)
        registerInput(touch1Pressed)
        registerInput(touch2Pressed)
        registerInput(touch0X)
        registerInput(touch0Y)
        registerInput(touch1X)
        registerInput(touch1Y)
        registerInput(touch2X)
        registerInput(touch2Y)
        registerInput(touch0XNormalized)
        registerInput(touch0YNormalized)
        registerInput(touch1XNormalized)
        registerInput(touch1YNormalized)
        registerInput(touch2XNormalized)
        registerInput(touch2YNormalized)
    }

    private fun registerInput(id: Symbol) {
        registerInputElement(id, id.string.toSpaceSeparated(), nextFreeInternalId++)
    }

    companion object {
        val mouseX = "mouseX".toSymbol()
        val mouseY = "mouseY".toSymbol()

        val mouseXNormalized = "mouseXNormalized".toSymbol()
        val mouseYNormalized = "mouseYNormalized".toSymbol()

        val leftMousePressed = "leftMousePressed".toSymbol()
        val rightMousePressed = "rightMousePressed".toSymbol()
        val midMousePressed = "midMousePressed".toSymbol()
        val backMousePressed = "backMousePressed".toSymbol()
        val forwardMousePressed = "forwardMousePressed".toSymbol()

        val touch0Pressed = "touch0Pressed".toSymbol()
        val touch1Pressed = "touch1Pressed".toSymbol()
        val touch2Pressed = "touch2Pressed".toSymbol()

        val touch0X = "touch0X".toSymbol()
        val touch0Y = "touch0Y".toSymbol()
        val touch1X = "touch1X".toSymbol()
        val touch1Y = "touch1Y".toSymbol()
        val touch2X = "touch2X".toSymbol()
        val touch2Y = "touch2Y".toSymbol()

        val touch0XNormalized = "touch0XNormalized".toSymbol()
        val touch0YNormalized = "touch0YNormalized".toSymbol()
        val touch1XNormalized = "touch1XNormalized".toSymbol()
        val touch1YNormalized = "touch1YNormalized".toSymbol()
        val touch2XNormalized = "touch2XNormalized".toSymbol()
        val touch2YNormalized = "touch2YNormalized".toSymbol()
    }

}