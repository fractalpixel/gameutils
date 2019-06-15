package org.fractalpixel.gameutils.rendering

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch

/**
 *
 */
interface RenderingContext3D: RenderingContext {

    var camera: Camera

    val modelBatch: ModelBatch

    var environment: Environment?
}