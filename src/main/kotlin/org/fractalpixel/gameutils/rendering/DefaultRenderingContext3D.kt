package org.fractalpixel.gameutils.rendering

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.Shader
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider
import org.entityflakes.World

class DefaultRenderingContext3D(override val shaderProvider: ShaderProvider? = null) : RenderingContext3D {

    override var camera: Camera = PerspectiveCamera()
    override lateinit var modelBatch: ModelBatch
    override var environment: Environment? = null

    override fun init(world: World) {
        modelBatch = ModelBatch(shaderProvider)
    }

    override fun begin() {
        camera.viewportHeight = Gdx.graphics.height.toFloat()
        camera.viewportWidth = Gdx.graphics.width.toFloat()
        camera.update()
        modelBatch.begin(camera)
    }

    override fun end() {
        modelBatch.end()
    }

    override fun dispose() {
        modelBatch.dispose()
    }
}