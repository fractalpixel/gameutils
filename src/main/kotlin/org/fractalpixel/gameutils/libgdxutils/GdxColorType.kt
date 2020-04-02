package org.fractalpixel.gameutils.libgdxutils

import com.badlogic.gdx.graphics.Color
import org.kwrench.color.colortype.ColorType
import org.kwrench.geometry.double3.Double3
import org.kwrench.geometry.double3.MutableDouble3

/**
 * Adapter between LibGDX color type and generic color type / generic color manipulation functions.
 */
object GdxColorType: ColorType<Color>() {

    fun getRed(color: Color): Double = color.r.toDouble()
    fun getGreen(color: Color): Double = color.g.toDouble()
    fun getBlue(color: Color): Double = color.b.toDouble()
    override fun getOpacity(color: Color): Double = color.a.toDouble()

    override fun getColorValues(color: Color, out: MutableDouble3): Double3 {
        out.x = getRed(color)
        out.y = getGreen(color)
        out.z = getBlue(color)
        return out
    }

    override fun createColorRGB(red: Double, green: Double, blue: Double, alpha: Double, colorOut: Color?): Color {

        // Use provided output color if provided
        val color = colorOut ?: Color()

        // Set colors (the set method also clamps them to the 0..1 range).
        color.set(red.toFloat(), green.toFloat(), blue.toFloat(), alpha.toFloat())

        return color
    }
}