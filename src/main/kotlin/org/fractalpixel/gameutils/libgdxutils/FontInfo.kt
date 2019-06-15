package org.fractalpixel.gameutils.libgdxutils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator

/**
 * Holds information about a bitmap font.
 * Can be used to get or create the font as well
 *
 * @param fileName name of the font file under the font path
 * @param sizePixelsHigh height of the font in pixels.
 * @param styleName name to use for the font inside the skin. 'default-font' is the font name used in most UI components.
 * @param shadow whether to add a drop shadow
 * @param border whether to add a black thin border
 */
data class FontInfo(val fileName: String = "DefaultFont.ttf",
                    val sizePixelsHigh: Int = 16,
                    val shadow: Boolean = false,
                    val border: Boolean = true,
                    val styleName: String = "default-font") {

    private var cachedFont: BitmapFont? = null

    /**
     * Get or create the bitmap font with these settings.
     *
     * @param fontDirectoryPath resource path where fonts are stored.
     * @return the bitmap font based on this FontInfo.
     *         The BitmapFont is created the first time it is retrieved, but cached for future retrievals.
     *         Note that it is not automatically disposed, call disposeFont when it is no longer needed.
     */
    fun getFont(fontDirectoryPath: String = "fonts"): BitmapFont {
        if (cachedFont == null) {
            cachedFont = createFont(fontDirectoryPath)
        }
        return cachedFont!!
    }

    /**
     * Dispose the font if it has been created.
     */
    fun disposeFont() {
        if (cachedFont != null) {
            cachedFont!!.dispose()
            cachedFont = null
        }
    }

    /**
     * @param path path where true type fonts are stored.
     */
    private fun createFont(path: String): BitmapFont {
        val fontPath = if (path.isEmpty() || path.endsWith("/")) path else path + "/"
        val fullFontPath = fontPath + fileName
        val fontFile = Gdx.files.internal(fullFontPath) ?: throw IllegalArgumentException("No font file '$fullFontPath' found!")
        val generator = FreeTypeFontGenerator(fontFile)
        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()

        // Adjust size
        parameter.size = sizePixelsHigh

        // Add shadows if desired
        if (shadow) {
            parameter.shadowOffsetX = Math.max(parameter.size / 16, 1)
            parameter.shadowOffsetY = Math.max(parameter.size / 16, 1)
            parameter.shadowColor = Color(0f, 0f, 0f, 0.4f)
        }

        // Add border if desired
        if (border) {
            parameter.borderWidth = 1.5f
            parameter.borderColor = Color.BLACK
        }

        val font = generator.generateFont(parameter)

        // Dispose generator
        generator.dispose()

        return font
    }


}

