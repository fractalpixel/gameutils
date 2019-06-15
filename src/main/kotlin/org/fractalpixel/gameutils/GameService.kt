package org.fractalpixel.gameutils

import org.entityflakes.processor.ProcessorBase


/**
 * Service that provides access to the libgdx application,
 * for things like input listening, screen resizes, pause & resume, and preferences.
 */
class GameService(val game: Game) : ProcessorBase(), GameInterface by game {

}