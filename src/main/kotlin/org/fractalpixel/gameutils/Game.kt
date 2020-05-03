package org.fractalpixel.gameutils

import com.badlogic.gdx.*
import org.entityflakes.DefaultWorld
import org.entityflakes.World
import org.fractalpixel.gameutils.layer.LayerSystem
import org.fractalpixel.gameutils.libgdxutils.ApplicationPreferenceChangeListener
import org.fractalpixel.gameutils.rendering.RenderingSystem
import org.fractalpixel.gameutils.scheduler.ScheduleSystem
import org.fractalpixel.gameutils.screenclear.ScreenClearSystem
import org.fractalpixel.gameutils.texture.TextureSystem
import org.kwrench.metrics.DefaultMetrics
import org.kwrench.strings.toIdentifier
import org.kwrench.strings.toSymbol
import org.kwrench.symbol.Symbol
import java.util.ArrayList

/**
 * A LibGDX game with an entity system.
 */
// FEATURE: Some kind of immediate mode (debugging) UI, that can show e.g. memory usage graphs and other stats.
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
     * Processor that takes care of clearing the screen to the background color.
     * Throws exception if it has been removed.
     */
    val screenClearProcessor: ScreenClearSystem get() = world[ScreenClearSystem::class]

    /**
     * @return processor that can be used to schedule events.
     */
    val scheduleSystem: ScheduleSystem get() = world[ScheduleSystem::class]

    /**
     * @return processor that renders layers
     */
    val layerProcessor: LayerSystem get() = world[LayerSystem::class]

    override fun create() {
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
    }

    /**
     * Add a number of default processors to the game world before it is initialized.
     */
    protected open fun createDefaultProcessors(world: World) {
        // TODO: Service for getting resource root path (through game service)?
        world.addSystem(GameSystem(this))
        world.addSystem(ScheduleSystem())
        world.addSystem(TextureSystem())
        world.addSystem(ScreenClearSystem())
        world.addSystem(LayerSystem())
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

        // Render any systems that implement RenderingSystem
        for (system in world.systems) {
            if (system is RenderingSystem) system.render()
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
        notifyGameListeners { it.onShutdown(this) }

        world.shutdown()
    }

    override fun onPreferencesChanged() {
        val prefs = preferences
        notifyGameListeners { it.onPreferencesChanged(this, prefs) }
    }

    override val preferences: Preferences get() = Gdx.app.getPreferences(preferencesKey.string)

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

    private fun notifyGameListeners(notify: (GameListener) -> Unit) {
        // Notify systems implementing the correct listener interface
        for (systems in world.systems) {
            if (systems is GameListener) {
                notify(systems)
            }
        }

        // Notify listeners
        for (listener in listeners) {
            notify(listener)
        }
    }
}
