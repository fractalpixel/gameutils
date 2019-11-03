package org.fractalpixel.gameutils

import org.entityflakes.system.SystemBase


/**
 * Service that provides access to the libgdx application,
 * for things like input listening, screen resizes, pause & resume, and preferences.
 */
class GameSystem(val game: Game) : SystemBase(), GameInterface by game {

}