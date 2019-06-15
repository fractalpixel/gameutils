package org.fractalpixel.gameutils

import com.badlogic.gdx.Preferences

/**
 * Listens to application events and preferences changes.
 */
interface GameListener {

    /**
     * Called when the game is created.
     */
    fun onCreated(game: GameInterface)  {}

    /**
     * Called when the [Game] is resized. This can happen at any point during a non-paused state but will never happen
     * before a call to [.create].
     * @param width the new width in pixels
     * @param height the new height in pixels
     */
    fun onResize(game: GameInterface, width: Int, height: Int) {}

    /**
     * Called when the [Game] is paused, usually when it's not active or visible on screen. An Application is also
     * paused before it is destroyed.
     * @param preferences preferences to save state to, in case the application is destroyed after being paused.
     *                    The preferences will be flushed by the caller.
     */
    fun onPause(game: GameInterface, preferences: Preferences) {}

    /**
     * Called when the [Game] is resumed from a paused state, usually when it regains focus.
     */
    fun onResume(game: GameInterface) {}

    /**
     * Called when the game is shut down.
     */
    fun onShutdown(game: GameInterface) {}


    fun onPreferencesChanged(game: GameInterface, preferences: Preferences) {}
}