package org.fractalpixel.gameutils.controls.inputproviders

import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import org.entityflakes.World
import org.fractalpixel.gameutils.GameService
import org.mistutils.strings.toIdentifier
import org.mistutils.strings.toSymbol

/**
 * Listens to keyboard inputs.
 */
class KeyboardInputProvider : InputProviderBase() {

    private val keyListener: InputAdapter = object : InputAdapter() {
        override fun keyDown(keycode: Int): Boolean {
            notifyListners(keycode, 1f)
            return false
        }

        override fun keyUp(keycode: Int): Boolean {
            notifyListners(keycode, 0f)
            return false
        }
    }

    override fun init(world: World) {
        // Register each available key as an InputElement
        registerKeys()

        // Listen to inputs
        world[GameService::class].addInputListener(keyListener)
    }

    override fun doDispose(world: World) {
        // Stop listening to inputs
        world[GameService::class].removeInputListener(keyListener)
    }

    private fun registerKeys() {
        for (keyCode in 0..255) {
            val keyName: String? = Input.Keys.toString(keyCode)
            if (keyName != null) {

                // Convert some symbols or names that start with numbers used by libgdx to identifiers
                val keyId = when (keyName) {
                    "0" -> "Number_0"
                    "1" -> "Number_1"
                    "2" -> "Number_2"
                    "3" -> "Number_3"
                    "4" -> "Number_4"
                    "5" -> "Number_5"
                    "6" -> "Number_6"
                    "7" -> "Number_7"
                    "8" -> "Number_8"
                    "9" -> "Number_9"
                    "@" -> "At"
                    ":" -> "Colon"
                    "," -> "Comma"
                    "." -> "Dot"
                    ";" -> "Semicolon"
                    "`" -> "Accent"
                    "'" -> "Quote"
                    "*" -> "Star"
                    "#" -> "Hash"
                    "[" -> "Left_bracket"
                    "]" -> "Right_bracket"
                    "=" -> "Equals"
                    "-" -> "Minus"
                    "+" -> "Plus"
                    "\\" -> "Backslash"
                    "/" -> "Forward_slash"
                    else -> keyName
                }

                var id = keyId.toIdentifier(strictIdentifier = true).toSymbol()

                // Workaround in case there is still some other symbol
                if (id == "_".toSymbol()) id = ("KEYCODE_" + keyCode).toSymbol()

                registerInputElement(id, keyName, keyCode)
            }
        }
    }
}