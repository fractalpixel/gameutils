package org.fractalpixel.gameutils.controls

import org.entityflakes.World
import org.entityflakes.system.SystemBase
import org.fractalpixel.gameutils.controls.inputproviders.*
import org.kwrench.symbol.Symbol
import java.util.*

/**
 * Keeps track of input providers, and allows listening to InputElements.
 */
class InputControlSystem(vararg val initialInputProviders: InputProvider = arrayOf(KeyboardInputProvider(), MouseInputProvider())): SystemBase(),
    InputElementProvider {

    private val inputElements_: MutableMap<Symbol, InputElement> = LinkedHashMap()

    /**
     * The currently registered input providers.
     */
    private val inputProviders = ArrayList<InputProvider>()

    private var initializeInputProviders = false

    private val inputProviderListenerDelegate = DelegatingInputProviderListener()

    /**
     * The currently available [InputElement]s
     */
    val inputElements: Map<Symbol, InputElement> get() = inputElements_


    fun registerInputProvider(inputProvider: InputProvider) {
        // Register the inputElements provided by the input provider
        for (inputElement in inputProvider.inputElements) {
            inputElements_.put(inputElement.code, inputElement)
        }

        // Store input provider
        inputProviders.add(inputProvider)

        // Listen to input
        inputProvider.addListener(inputProviderListenerDelegate)

        // Initialize the input provider if init has already been called
        if (initializeInputProviders) inputProvider.init(world)
    }


    override fun doInit(world: World) {
        // Add input providers specified in the constructor
        for (inputProvider in initialInputProviders) {
            registerInputProvider(inputProvider)
        }

        // Initialize all current input providers
        for (inputProvider in inputProviders) {
            inputProvider.init(world)
        }

        // Initialize subsequently added input providers
        initializeInputProviders = true
    }

    override fun doDispose() {
        for (inputProvider in inputProviders) {
            inputProvider.removeListener(inputProviderListenerDelegate)
            inputProvider.dispose(world)
        }
    }

    override fun addListener(listener: InputProviderListener) {
        inputProviderListenerDelegate.addListener(listener)
    }

    override fun removeListener(listener: InputProviderListener) {
        inputProviderListenerDelegate.removeListener(listener)
    }

}