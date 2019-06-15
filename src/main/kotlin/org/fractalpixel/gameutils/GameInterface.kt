package org.fractalpixel.gameutils

import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.Preferences
import org.entityflakes.World
import org.mistutils.symbol.Symbol

/**
 * Contains the methods provided by the [Game] for client usage
 */
interface GameInterface {

    /**
     * The game world, contains the game entities and the processors that update the entities.
     */
    val gameWorld: World

    /**
     * User readable name of the application.
     * Used e.g. for the window title on the desktop version.
     */
    val applicationName: String

    /**
     * Unique identifier for this game, used to store preferences so that they don't get mixed up with other preferences.
     */
    val preferencesKey: Symbol

    /**
     * Loads the current version of the preferences for this game and returns them.
     * After changing them, remember to call flush.
     */
    val preferences: Preferences

    /**
     * Internal file path to resources.
     * Normally an empty string, but could be e.g. application name or package name.
     * If non-empty, should end in a directory path separator "/"
     */
    val resourcePath: String

    /**
     * Path to asset sources during development.
     * Used for things like building texture atlases when in development mode.
     */
    val assetSourcePath: String

    /**
     * If true, the game is running in development mode, and can e.g. package textures on startup,
     * and show / log debug information.
     */
    val developmentMode: Boolean

    /**
     * @param inputProcessor an InputProcessor implementation that will be notified about input events such as keypresses and mouse movement.
     */
    fun addInputListener(inputProcessor: InputProcessor)

    /**
     * @param inputProcessor input processor to remove.
     */
    fun removeInputListener(inputProcessor: InputProcessor)

    /**
     * @param listener a listener that is notified about app events such as screen resize, pause & resume, and preferences change.
     */
    fun addGameListener(listener: GameListener)

    /**
     * @param listener GameListener to remove.
     */
    fun removeGameListener(listener: GameListener)

    /**
     * Stops the game after some time (typically after the next update call).
     * A shorthand for calling gameWorld.stop()
     */
    fun stop() = gameWorld.stop()
}