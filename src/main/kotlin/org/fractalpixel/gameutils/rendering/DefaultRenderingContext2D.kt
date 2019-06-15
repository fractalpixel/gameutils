package org.fractalpixel.gameutils.rendering

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import org.entityflakes.World

class DefaultRenderingContext2D(val targetWholeScreen: Boolean = true) : RenderingContext2D {

    override var camera2D: Camera2D = Camera2D()
    override lateinit var spriteBatch: SpriteBatch

    // Used for calculating an orthogonal projection for the sprite batch
    private val projectionCamera: OrthographicCamera = OrthographicCamera()

    // Keep track of screen size changes
    private var previousScreenWidth: Int = 0
    private var previousScreenHeight: Int = 0

    override fun init(world: World) {
        spriteBatch = SpriteBatch()

        updateCamera()
    }

    override fun begin() {
        updateCamera()
        spriteBatch.begin()
    }

    override fun end() {
        spriteBatch.end()
    }

    override fun dispose() {
        spriteBatch.dispose()
    }

    /**
     * Update camera and projection if needed.
     */
    fun updateCamera() {
        if (checkIfScreenResolutionChanged()) {
            updateScreen2DProjection()
            camera2D.setToGdxScreenSize()
        }

        if (targetWholeScreen) {
            camera2D.screenArea.set(0.0,
                                    0.0,
                                    Gdx.graphics.width.toDouble(),
                                    Gdx.graphics.height.toDouble())
        }
    }

    private fun updateScreen2DProjection() {
        // Update sprite batch projection when the screen size changed
        projectionCamera.viewportWidth = Gdx.graphics.width.toFloat()
        projectionCamera.viewportHeight = Gdx.graphics.height.toFloat()
        projectionCamera.position.set(0.5f * projectionCamera.viewportWidth, 0.5f * projectionCamera.viewportHeight, 0f)
        projectionCamera.update()
        spriteBatch.projectionMatrix = projectionCamera.combined
    }

    private fun checkIfScreenResolutionChanged(): Boolean {
        val currentWidth = Gdx.graphics.width
        val currentHeight = Gdx.graphics.height
        if (previousScreenWidth != currentWidth ||
            previousScreenHeight != currentHeight) {

            previousScreenWidth = currentWidth
            previousScreenHeight = currentHeight
            return true
        } else {
            return false
        }
    }
}
