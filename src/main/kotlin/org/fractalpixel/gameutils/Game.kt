package org.fractalpixel.gameutils

import com.badlogic.gdx.*
import javafx.application.Platform
import org.entityflakes.DefaultWorld
import org.entityflakes.World
import org.fractalpixel.gameutils.layer.LayerProcessor
import org.fractalpixel.gameutils.libgdxutils.ApplicationPreferenceChangeListener
import org.fractalpixel.gameutils.rendering.RenderingProcessor
import org.fractalpixel.gameutils.screenclear.ScreenClearProcessor
import org.fractalpixel.gameutils.texture.TextureService
import org.fractalpixel.gameutils.utils.DummyJavaFXApp
import org.mistutils.metrics.DefaultMetrics
import org.mistutils.metrics.view.MetricsView
import org.mistutils.strings.toIdentifier
import org.mistutils.strings.toSymbol
import org.mistutils.symbol.Symbol
import java.util.ArrayList
import java.util.logging.Logger

/**
 * A LibGDX game with an entity system.
 */
abstract class Game(override val applicationName: String,
                    override val resourcePath: String = "",
                    override val preferencesKey: Symbol = (applicationName + "_preferences").toIdentifier().toSymbol(),
                    override val developmentMode: Boolean = false,
                    override val assetSourcePath: String = "asset_sources/",
                    val world: World = DefaultWorld(sleepForSurplusTime = false)
) : ApplicationListener, ApplicationPreferenceChangeListener, GameInterface {

    private val listeners = ArrayList<GameListener>()

    val inputMultiplexer: InputMultiplexer = InputMultiplexer()

    override val gameWorld: World get() = world

    /**
     * Use for reporting performance and other metrics
     */
    var metrics = DefaultMetrics()

    /**
     * Use for logging messages.
     */
    var log: Logger = Logger.getAnonymousLogger(Game::class.simpleName)

    /**
     * Processor that takes care of clearing the screen to the background color.
     * Throws exception if it has been removed.
     */
    val screenClearProcessor: ScreenClearProcessor get() = world[ScreenClearProcessor::class]

    /**
     * @return processor that renders layers
     */
    val layerProcessor: LayerProcessor get() = world[LayerProcessor::class]

    override fun create() {
        log.info("Creating")

        // Set application title
        Gdx.graphics.setTitle(applicationName)

        // Setup input listener
        val currentInputProcessor = Gdx.input.inputProcessor
        if (currentInputProcessor is InputMultiplexer) {
            // Use existing multiplexing processor (may be needed on Android to run multiple apps, e.g. a live wallpaper and the settings app for it).
            currentInputProcessor.addProcessor(inputMultiplexer)
        }
        else {
            Gdx.input.inputProcessor = inputMultiplexer
        }

        // Setup processors
        createDefaultProcessors(world)
        createProcessors(world)

        // Initialize the world and all processors in it
        world.init()

        // Do any additional setup (e.g. entities)
        setupWorld(world)

        // Notify listeners
        notifyGameListeners { it.onCreated(this) }

        log.info("Done creating")
    }

    /**
     * Add a number of default processors to the game world before it is initialized.
     */
    protected open fun createDefaultProcessors(world: World) {
        // TODO: Service for getting resource root path (through game service)?
        world.addProcessor(GameService(this))
        world.addProcessor(TextureService())
        world.addProcessor(ScreenClearProcessor())
        world.addProcessor(LayerProcessor())
    }

    /**
     * Add any additional processors to the game world before it is initialized.
     */
    protected abstract fun createProcessors(world: World)

    /**
     * Called after processors have been added to the world and the world (and processors) initialized.
     * Can be used to add initial entities to the world.
     */
    protected abstract fun setupWorld(world: World)

    override fun resize(width: Int, height: Int) {
        notifyGameListeners { it.onResize(this, width, height) }
    }

    override fun render() {
        // Advance world time and update the world
        world.step()

        // Check if world.stop() was called.
        if (world.stopRequested) Gdx.app.exit()

        // Render any processors that implement RenderingProcessor
        for (processor in world.processors) {
            if (processor is RenderingProcessor) processor.render()
        }
    }

    override fun pause() {
        val prefs = preferences

        notifyGameListeners { it.onPause(this, prefs) }

        // Save preferences
        prefs.flush()
    }

    override fun resume() {
        notifyGameListeners { it.onResume(this) }
    }

    override fun dispose() {
        log.info("Shutting down")

        notifyGameListeners { it.onShutdown(this) }

        world.shutdown()

        log.info("Shutdown done")
    }

    override fun onPreferencesChanged() {
        val prefs = preferences
        notifyGameListeners { it.onPreferencesChanged(this, prefs) }
    }

    override val preferences: Preferences
        get() {
        return Gdx.app.getPreferences(preferencesKey.string)
    }

    override fun addInputListener(inputProcessor: InputProcessor) {
        inputMultiplexer.addProcessor(inputProcessor)
    }

    override fun removeInputListener(inputProcessor: InputProcessor) {
        inputMultiplexer.removeProcessor(inputProcessor)
    }

    override fun addGameListener(listener: GameListener) {
        listeners.add(listener)
    }

    override fun removeGameListener(listener: GameListener) {
        listeners.remove(listener)
    }

    private var metricsView: MetricsView? = null
    private var javaFxCreated = false

    /**
     * Show metrics window.  Use metrics.report() to add metrics reports each frame or similar.
     * If [createJavaFX] a JavaFX application is started up, so that the metrics window has a javafx context to use.
     * @returns the created MetricsView
     */
    fun showMetrics(createJavaFX: Boolean = true): MetricsView {
        val currentMetricsView = metricsView

        return if (currentMetricsView != null) {
            currentMetricsView.show()
            currentMetricsView
        }
        else {
            // Create javafx if needed
            if (createJavaFX && !javaFxCreated) {
                javafx.application.Application.launch(DummyJavaFXApp::class.java)
                javaFxCreated = true
            }

            // Create metrics view
            val newMetricsView = MetricsView(metrics)
            metricsView = newMetricsView
            newMetricsView.show()
            newMetricsView
        }
    }

    private fun notifyGameListeners(notify: (GameListener) -> Unit) {
        // Notify processors implementing the correct listener interface
        for (processor in world.processors) {
            if (processor is GameListener) {
                notify(processor)
            }
        }

        // Notify listeners
        for (listener in listeners) {
            notify(listener)
        }
    }
}
