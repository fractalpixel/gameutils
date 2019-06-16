package org.fractalpixel.gameutils.utils

import javafx.application.Application
import javafx.stage.Stage

/**
 * Javafx application that should just initialize javafx.
 * Used so that it is possible to open views with performance tracking data and such if necessary.
 */
class DummyJavaFXApp: Application() {
    override fun start(primaryStage: Stage?) {
        // Just exist.
    }
}