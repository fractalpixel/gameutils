package org.fractalpixel.gameutils.layer.layers

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import org.entityflakes.Entity
import org.fractalpixel.gameutils.layer.Layer2D
import org.fractalpixel.gameutils.rendering.RenderingContext2D


/**
 *
 */
class ShaderLayer(val fragmentShader: String = defaultFragmentShader,
                  val vertexShader: String = defaultVertexShader): Layer2D() {

    private lateinit var sprite: Sprite
    private lateinit var emptyTexture: Texture
    private lateinit var shaderProgram: ShaderProgram

    init {
        clearStencilBuffer = true
        clearDepthBuffer = true
        clearColorBufferToColor = Color.BLACK
    }

    override fun doInit(entity: Entity) {
        emptyTexture = Texture(64, 64, Pixmap.Format.RGB888)
        sprite = Sprite(emptyTexture)

        shaderProgram = ShaderProgram(vertexShader, fragmentShader)
    }

    override fun render(context: RenderingContext2D) {
        sprite.setSize(Gdx.graphics.width.toFloat(),
                       Gdx.graphics.height.toFloat())

        val batch = context.spriteBatch
        val prevShader = batch.shader
        batch.shader = shaderProgram
        batch.draw(sprite,
                   sprite.x, sprite.y,
                   sprite.width, sprite.height);
        batch.shader = prevShader
    }

    override fun doDispose() {
        shaderProgram.dispose()
        emptyTexture.dispose()
    }

    companion object {
        val defaultVertexShader: String =
            """
            attribute vec4 a_position;
            attribute vec4 a_color;
            attribute vec2 a_texCoord;

            uniform mat4 u_projTrans;

            varying vec4 v_color;
            varying vec2 v_texCoords;

            void main()
            {
                v_color = a_color;
                v_color.a = v_color.a * (256.0/255.0);
                v_texCoords = a_texCoord + 0;
                gl_Position =  u_projTrans * a_position;
            }
            """

        val defaultFragmentShader: String =
                """
                #ifdef GL_ES
                #define LOWP lowp
                    precision mediump float;
                #else
                    #define LOWP
                #endif

                varying LOWP vec4 v_color;
                varying vec2 v_texCoords;

                uniform sampler2D u_texture;

                void main()
                {
                    gl_FragColor = v_color * texture2D(u_texture, v_texCoords);
                }
                """
    }
}