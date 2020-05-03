package org.fractalpixel.gameutils.libgdxutils

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.Shader
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.GdxRuntimeException

/**
 * Shader baseclass without all the complications of the libgdx shader base class.
 *
 * Creates the shader from the given vertex and fragment shader sources.
 *
 * If [includeTransformUniforms] is true (the default), the u_projViewTrans and u_worldTrans will be provided to the shader
 * from the camera and the renderable, respectively.  If [pedanticMode] is true, an exception will be thrown if
 * some uniform is not present.  Note that uniforms are optimized away by the opengl drivers if they are not used by
 * the shader, in which case exceptions will be thrown.  Defaults to false.
 *
 * Provides default implementations for most routine shader construction and usage tasks.
 */
open class SimpleShader(val vertexShaderSource: String,
                        val fragmentShaderSource: String,
                        val includeTransformUniforms: Boolean = true,
                        val pedanticMode: Boolean = false): Shader {

    /**
     * Create the shader given the internal shader path.
     *
     * If [includeTransformUniforms] is true (the default), the u_projViewTrans and u_worldTrans will be provided to the shader
     * from the camera and the renderable, respectively.  If [pedanticMode] is true, an exception will be thrown if
     * some uniform is not present.  Note that uniforms are optimized away by the opengl drivers if they are not used by
     * the shader, in which case exceptions will be thrown.  Defaults to false.
     */
    constructor(internalShaderPath: String, includeTransformUniforms: Boolean = true, pedanticMode: Boolean = false): this(
        loadTextFile("$internalShaderPath.vertex.glsl"),
        loadTextFile("$internalShaderPath.fragment.glsl"),
        includeTransformUniforms,
        pedanticMode
    )

    /**
     * The camera projection view transformation uniform location in the shader.  Used if [includeTransformUniforms] is true.
     */
    protected var u_projViewTrans: Int = 0

    /**
     * The world transformation uniform location in the shader.  Used if [includeTransformUniforms] is true.
     */
    protected var u_worldTrans: Int = 0


    /**
     * The shader program.  Initialized in [init].
     */
    protected lateinit var shaderProgram: ShaderProgram

    /**
     * Builds the shader program
     */
    override fun init() {
        shaderProgram = createShaderProgram(vertexShaderSource, fragmentShaderSource)
    }

    /**
     * Creates the shader program given the sources.  Throws a runtime exception if there are any errors in the shader.
     */
    protected open fun createShaderProgram(vertexShaderSource: String, fragmentShaderSource: String): ShaderProgram {
        val program = ShaderProgram(vertexShaderSource, fragmentShaderSource)

        // Check for any compilation errors
        if (!program.isCompiled) {
            throw GdxRuntimeException(program.log)
        }

        // Fetch uniform locations
        fetchUniforms(program, pedanticMode)

        return program
    }

    /**
     * Override to fetch additional uniform locations and store them in your own int properties.
     */
    protected open fun fetchUniforms(shaderProgram: ShaderProgram, pedanticMode: Boolean) {
        if (includeTransformUniforms) {
            u_projViewTrans = shaderProgram.fetchUniformLocation("u_projViewTrans", pedanticMode)
            u_worldTrans = shaderProgram.fetchUniformLocation("u_worldTrans", pedanticMode)
        }
    }

    /**
     * By returns true for all renderables.
     */
    override fun canRender(instance: Renderable): Boolean {
        return true
    }

    /**
     * Override to do custom initializing when a pass with this shader begins,
     * such as setting general uniforms for the shader program and clearing / setting buffers.
     *
     * By default this calls begin for the shader program, and sets the u_projViewTrans if [includeTransformUniforms]
     * is true.
     */
    override fun begin(camera: Camera, context: RenderContext) {
        shaderProgram.begin()

        // Set camera projection, if desired
        if (includeTransformUniforms) {
            shaderProgram.setUniformMatrix(u_projViewTrans, camera.combined)
        }
    }

    /**
     * Override to do custom rendering, such as setting renderable-specific uniforms for the shader program.
     *
     * By default this renders the mesh in the renderable with the shader program, and sets world projection if
     * [includeTransformUniforms] is true.
     */
    override fun render(renderable: Renderable) {
        // Set world transformation, if desired
        if (includeTransformUniforms) {
            shaderProgram.setUniformMatrix(u_worldTrans, renderable.worldTransform)
        }

        renderable.meshPart.render(shaderProgram)
    }

    /**
     * Override to do anything special when the render pass with this shader ends.
     *
     * By default this calls end for the shader program.
     */
    override fun end() {
        shaderProgram.end()
    }

    /**
     * Disposes the loaded shader program.
     */
    override fun dispose() {
        shaderProgram.dispose()
    }

    override fun compareTo(other: Shader?): Int {
        if (other == null) return -1
        return 0
    }
}