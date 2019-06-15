package org.fractalpixel.gameutils.rendering

import com.badlogic.gdx.graphics.g2d.SpriteBatch

/**
 *
 */
interface RenderingContext2D: RenderingContext {

    var camera2D: Camera2D

    val spriteBatch: SpriteBatch

}