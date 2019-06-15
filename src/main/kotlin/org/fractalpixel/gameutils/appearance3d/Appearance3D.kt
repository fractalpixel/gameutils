package org.fractalpixel.gameutils.appearance3d

import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import org.entityflakes.Component
import org.entityflakes.PolymorphicComponent
import org.fractalpixel.gameutils.rendering.RenderingContext3D

/**
 *
 */
interface Appearance3D: PolymorphicComponent {
    override val componentCategory: Class<out Component> get() = Appearance3D::class.java

    fun render(pos: Vector3,
               dir: Quaternion,
               renderContext: RenderingContext3D
    )
}