package org.fractalpixel.gameutils.voxel.renderer

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.Shader
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import org.fractalpixel.gameutils.libgdxutils.loadShaderProgram

class WireframeShader(): Shader {

    private lateinit var program: ShaderProgram
    private var u_projViewTrans: Int = 0
    private var u_worldTrans: Int = 0
    private var u_color: Int = 0

    private var camera: Camera? = null
    private var renderContext: RenderContext? = null

    private var currentContext: RenderContext? = null

    override fun init() {
        program = loadShaderProgram("shaders/wireframe")

        // When true, requires that uniform / attribute is present and used
        val pedanticMode = true

        u_projViewTrans = program.fetchUniformLocation("u_projViewTrans", pedanticMode)
        u_worldTrans = program.fetchUniformLocation("u_worldTrans", pedanticMode)
        u_color = program.fetchUniformLocation("u_color", pedanticMode)
    }

    override fun canRender(instance: Renderable): Boolean {
        return renders(instance)
    }

    override fun begin(camera: Camera, context: RenderContext) {
        this.camera = camera
        this.renderContext = context

        program.begin()
        program.setUniformMatrix(u_projViewTrans, camera.combined)

        currentContext = context
    }

    override fun render(renderable: Renderable) {
        val context = currentContext!!
        context.setCullFace(GL20.GL_BACK)
        context.setDepthTest(GL20.GL_LEQUAL, camera!!.near, 100000.0f) // camera!!.far
        context.setDepthMask(true)

        // Object color.  Used for debugging
        val color = (renderable.material.get(ColorAttribute.Diffuse) as ColorAttribute).color
        program.setUniformf(u_color, color)

        // Set world trans
        program.setUniformMatrix(u_worldTrans, renderable.worldTransform)

        // Render mesh using our shader
        renderable.meshPart.render(program)
    }

    override fun end() {
        program.end()
    }

    override fun compareTo(other: Shader?): Int {
        if (other == null) return -1
        return 0
    }

    override fun dispose() {
        program.dispose()
    }

    companion object {
        fun renders(renderable: Renderable): Boolean {
            return renderable.meshPart.id == VoxelRenderChunk.wireframeId
        }
    }

}