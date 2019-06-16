package org.fractalpixel.gameutils.texture

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.tools.texturepacker.TexturePacker
import org.entityflakes.World
import org.entityflakes.processor.ProcessorBase
import org.fractalpixel.gameutils.GameService
import org.fractalpixel.gameutils.libgdxutils.buildTextureAtlas
import org.fractalpixel.gameutils.libgdxutils.createDefaultTextureAtlasSettings
import org.fractalpixel.gameutils.libgdxutils.loadTextureAtlasSettings
import org.fractalpixel.gameutils.libgdxutils.saveTextureAtlasSettings
import org.mistutils.strings.toSymbol
import org.mistutils.symbol.Symbol
import java.io.File
import java.util.*
import java.util.logging.Logger

/**
 * Service that loads textures at startup and allows fast retrieval of them.
 *
 * NOTE: In dev mode, when creating the texture atlas, for some reason it doesn't flush properly or something such,
 * so texture changes are only available after restarting once.
 *
 * @param textureAtlasDirName name of the directory for textures under the root resource path, e.g. "textures/"
 * Also used as the name of the texture atlas.
 * @param texturePackingSettingsFileName settings for the texture packer, should be placed in the texture source directory.
 * Defaults to "textureAtlas.pack"
 */
class TextureService(val textureAtlasDirName: String = "textures",
                     var texturePackingSettingsFileName: String = "textureAtlasSettings.json",
                     var createPackingSettingsFileIfMissing: Boolean = true): ProcessorBase() {

    private val textures = HashMap<Symbol, TextureRegion>()

    lateinit var textureAtlasPath: String
    lateinit var textureAtlas: TextureAtlas

    override fun doInit(world: World) {
        val gameService = world[GameService::class]

        // Build path to textures
        textureAtlasPath = "${gameService.resourcePath}$textureAtlasDirName/$textureAtlasDirName.atlas"

        // Create textures if dev mode
        if (gameService.developmentMode) {
            buildTextures()
        }

        // Load if available
        val internalTextureAtlasPath = Gdx.files.internal(textureAtlasPath)
        if (internalTextureAtlasPath.exists()) {
            // Load textures
            textureAtlas = TextureAtlas(internalTextureAtlasPath)
        }
        else {
            // Warn and create empty atlas
            Gdx.app.log("TextureService", "No texture atlas found at '${textureAtlasPath}'!")
            textureAtlas = TextureAtlas()
        }
    }

    override fun doDispose() {
        textureAtlas.dispose()
    }

    /**
     * @return the TextureRegion with the specified name.
     * Caches the texture regions for faster retrieval than TextureAtlas (that iterates through them when searching).
     * Throws an exception if there was no such texture region.
     */
    fun getTexture(textureName: String, usePlaceholderIfNotFound: Boolean = false): TextureRegion = getTexture(textureName.toSymbol(), usePlaceholderIfNotFound)

    /**
     * @return the TextureRegion with the specified id.
     * Caches the texture regions for faster retrieval than TextureAtlas (that iterates through them when searching).
     * Faster than accessing textures by name if the id Symbol is stored instead of retrieved on each call.
     * Throws an exception if there was no such texture region.
     */
    fun getTexture(textureId: Symbol, usePlaceholderIfNotFound: Boolean = false): TextureRegion {
        var texture = textures[textureId]
        if (texture == null) {
            texture = textureAtlas.findRegion(textureId.string)
            if (texture == null) {
                val msg = "No texture region named '$textureId' found!"
                // TODO: Use sensible logging utility..
                println(msg)
                if (usePlaceholderIfNotFound) {
                    return getTexture(PLACEHOLDER_NAME, false)
                }
                else {
                    throw IllegalArgumentException(msg)
                }
            }
            textures.put(textureId, texture)
        }
        return texture
    }

    /**
     * Builds a texture atlas from all the images in the specified source path, and saves them to the textureAtlasPath.
     * Utility function that can be used during development.
     * @param projectResourcePath target path under project root where the resources will be added in the packagePath + texturePath
     *                     e.g."src/main/resources/"
     * @param defaultSettings texture packer settings to use if no settings are found in the input texture directory.
     *  If input settings found, they take precedence.
     */
    fun buildTextures(projectResourcePath: String = "src/main/resources/",
                      defaultSettings: TexturePacker.Settings = createDefaultTextureAtlasSettings()) {


        val gameService = world[GameService::class]
        val textureAtlasDirectory = projectResourcePath + gameService.resourcePath + textureAtlasDirName
        val assetSourcePath = gameService.assetSourcePath + textureAtlasDirName

        // Load settings if available, if not use defaults
        val settingsFile = File(assetSourcePath + (if (!assetSourcePath.endsWith("/")) "/" else "") + texturePackingSettingsFileName)
        val loadedSettings = loadTextureAtlasSettings(settingsFile)
        val settings = loadedSettings ?: defaultSettings

        // Save settings if not found if desired
        if (loadedSettings == null && createPackingSettingsFileIfMissing) {
            saveTextureAtlasSettings(settingsFile, settings)
        }

        buildTextureAtlas(assetSourcePath, textureAtlasDirectory, textureAtlasDirName, settings)
    }

    companion object {
        /**
         * Name of placeholder texture to use if a specified texture name is invalid / not found.
         */
        val PLACEHOLDER_NAME = "placeholder_texture"
    }

}