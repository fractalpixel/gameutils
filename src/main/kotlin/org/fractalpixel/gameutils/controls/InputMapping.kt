package org.fractalpixel.gameutils.controls

import com.badlogic.gdx.Gdx
import org.fractalpixel.gameutils.GameService
import org.fractalpixel.gameutils.controls.inputproviders.InputElement
import org.mistutils.symbol.Symbol
import java.util.*

/**
 * Maps from InputElements to Controls.
 * @param bindings map from input element id to the id of the control that it controls.
 * @param bindingsKey id used when saving these bindings in preferences.
 */
class InputMapping(val bindings: MutableMap<Symbol, Symbol> = LinkedHashMap<Symbol, Symbol>(),
                   var bindingsKey: String = "default") {

    /**
     * Initialize this input mapping from the specified bindings conf of control=input pairs.
     * @param bindingsConf configuration file content as a string
     * @param bindingsKey id used when saving these bindings in preferences.
     */
    constructor (bindingsConf: String, bindingsKey: String = "default") : this(bindingsKey = bindingsKey) {
        readBindingConfiguration(bindingsConf)
    }

    operator fun get(inputElementId: Symbol): Symbol? = bindings[inputElementId]
    operator fun get(inputElement: InputElement): Symbol? = bindings[inputElement.code]

    operator fun set(inputElementId: Symbol, controlId: Symbol) {
        bindings[inputElementId] = controlId
    }

    fun remove(inputElement: InputElement) {
        remove(inputElement.code)
    }

    fun remove(inputElementId: Symbol) {
        bindings.remove(inputElementId)
    }

    fun clear() {
        bindings.clear()
    }

    /**
     * The bindings as a formatted binding configuration
     */
    fun getBindingConfiguration(): String {
        val sb = StringBuilder()
        for (binding in bindings) {
            sb.append("${binding.value} = ${binding.key}\n")
        }
        return sb.toString()
    }

    /**
     * Load bindings from the specified configuration string.
     * Ignore any errors in it.
     */
    fun readBindingConfiguration(configuration: String) {
        try {
            val loadedBindings: LinkedHashMap<Symbol, Symbol> = LinkedHashMap() /* bindingsLanguage.parse(configuration)  */
            bindings.clear()
            bindings.putAll(loadedBindings)
        }
        catch (e: Exception) {
            // Log error, but do not crash
            Gdx.app.error("Problem when reading keybindings '$bindingsKey':", e.message)
        }
    }

    /**
     * Save bindings to application preferences.
     */
    fun saveBindingConfiguration(gameService: GameService) {
        val preferences = gameService.preferences
        preferences.putString(bindings_preference_base_key + bindingsKey, getBindingConfiguration())
        preferences.flush()
    }

    /**
     * Load bindings from the application preferences.
     * Ignore any errors in it.
     */
    fun loadBindingConfiguration(gameService: GameService) {
        readBindingConfiguration(gameService.preferences.getString(bindings_preference_base_key + bindingsKey, "\n"))
    }

    companion object {
        // TODO: Implement load & save for bindings, use json?
        /*
        /**
         * Language that can parse bindings
         */
        private val bindingsLanguage = object : LanguageBase<LinkedHashMap<Symbol, Symbol>>("keybindings") {
            override val parser: Parser = whitespace + zeroOrMore(
                    (identifierAsSymbol - "=" - oneOrMoreWithSeparator(identifierAsSymbol + whitespace, + "," + whitespace)).generates { Pair<Symbol, List<Symbol>>(it.pop(1), it.pop()) }
            ).generatesContentList().generates {
                val map = LinkedHashMap<Symbol, Symbol>()

                // Map from command = inputs to input = command
                val entries: List<Pair<Symbol, List<Symbol>>> = it.pop()
                for (entry in entries) {
                    val command = entry.first
                    val inputs = entry.second
                    for (input in inputs) {
                        map.put(input, command)
                    }
                }

                map
            }
        }
        */

        val bindings_preference_base_key = "InputMapping_bindings_"
    }

}