package org.fractalpixel.gameutils.voxel.renderer

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.Shader
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import org.fractalpixel.gameutils.libgdxutils.loadShaderProgram

class VoxelTerrainShader(): Shader {

    private lateinit var program: ShaderProgram
    private var u_cameraWorldPosition: Int = 0
    private var u_lodFadeDistances: Int = 0
    private var u_projViewTrans: Int = 0
    private var u_worldTrans: Int = 0
    private var u_color: Int = 0

    private var camera: Camera? = null
    private var renderContext: RenderContext? = null

    private var currentContext: RenderContext? = null

    override fun init() {
        program = loadShaderProgram("shaders/voxelterrain")

        // DEBUG: Show values
        println("\nUniforms: " + program.uniforms.joinToString())
        println("Attributes: " + program.attributes.joinToString())
        println("Log: \n" + program.log)
        println("Source:\n" + program.fragmentShaderSource)

        // When true, requires that uniform / attribute is present and used
        val pedanticMode = true

//        u_cameraWorldPosition = program.fetchUniformLocation("u_cameraWorldPosition", pedanticMode)
//        u_lodFadeDistances = program.fetchUniformLocation("u_lodFadeDistances", pedanticMode)
        u_projViewTrans = program.fetchUniformLocation("u_projViewTrans", pedanticMode)
        u_worldTrans = program.fetchUniformLocation("u_worldTrans", pedanticMode)
        u_color = program.fetchUniformLocation("u_color", pedanticMode)

        // TODO: Projection matrix, pos, normal, lights,
        // TODO: Use as reference: https://xoppa.github.io/blog/creating-a-shader-with-libgdx/

    }

    override fun canRender(instance: Renderable): Boolean {
        return renders(instance)
    }

    override fun begin(camera: Camera, context: RenderContext) {
        this.camera = camera
        this.renderContext = context

        // TODO: Set proj view
        // TODO: Activate depth test, backface culling

        program.begin()
//        program.setUniformf(u_cameraWorldPosition, camera!!.position)
        program.setUniformMatrix(u_projViewTrans, camera.combined)
//        program.setUniformf(u_lodFadeDistances, 0f, 1f, 2f, 3f) // TODO: Get LOD fade distances

        currentContext = context


    }

    override fun render(renderable: Renderable) {
        val context = currentContext!!
        context.setCullFace(GL20.GL_BACK)
        context.setDepthTest(GL20.GL_LEQUAL, camera!!.near, 100000.0f) // camera!!.far
        context.setDepthMask(true)

        // Object color.  Used for debugging // LATER: Disable debugging color eventually?
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
            return renderable.meshPart.id == VoxelRenderChunk.voxelTerrainChunkId
        }
    }
}