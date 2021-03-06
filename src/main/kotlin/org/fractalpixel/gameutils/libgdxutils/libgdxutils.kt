package org.fractalpixel.gameutils.libgdxutils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Shader
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.tools.texturepacker.TexturePacker
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Json
import org.kwrench.geometry.double2.Double2
import org.kwrench.geometry.double3.Double3
import org.kwrench.geometry.double3.MutableDouble3
import org.kwrench.geometry.int2.Int2
import org.kwrench.geometry.int2.MutableInt2
import org.kwrench.geometry.int3.Int3
import org.kwrench.geometry.int3.MutableInt3
import org.kwrench.geometry.volume.MutableVolume
import org.kwrench.geometry.volume.Volume
import org.kwrench.math.*
import java.io.File
import java.io.FileReader
import java.lang.IllegalStateException
import kotlin.math.max
import kotlin.math.min


// Utilities to make libgdx work a bit smoother with some kotlin features, as well as with the used utility libraries.



/**
 * Replaces the default font in the skin with the specified font of the specified size.
 */
fun loadSkinWithFont(skinPathAndName: String, fontPathAndName: String, sizePixelsHigh: Int, fontStyleName: String = "default-font"): Skin {
    return loadSkinWithFonts(skinPathAndName, "", listOf(FontInfo(fontPathAndName,
        sizePixelsHigh,
        styleName = fontStyleName)))
}

/**
 * Loads the specified TTF fonts as bitmap fonts of the specified sizes with the skin.
 * The styleName of the default skin font is generally 'default-font'
 */
fun loadSkinWithFonts(skinPathAndName: String, fontPath: String, fontInfos: List<FontInfo>): Skin {

    // Create skin
    val skin = Skin(TextureAtlas(Gdx.files.internal(skinPathAndName + ".atlas")))

    // Create bitmap fonts of correct size for TTF fonts.
    for (fontInfo in fontInfos) {
        skin.add(fontInfo.styleName, fontInfo.getFont(fontPath), BitmapFont::class.java)
    }

    // Load the skin, so fonts get used with the correct widgets.
    skin.load(Gdx.files.internal(skinPathAndName + ".json"))

    return skin
}



/**
 * Builds a texture atlas from all the images in the specified source path.
 */
fun buildTextureAtlas(assetSourcePath: String,
                      textureAtlasPath: String?,
                      settings: TexturePacker.Settings = createDefaultTextureAtlasSettings(),
                      log: Boolean = true) {
    if (textureAtlasPath == null) {
        if (log) println("Can not generate textures, textureAtlasPath is null.")
    }
    else  {
        val target = textureAtlasPath.replaceAfterLast('/', "")
        val atlasFileName = textureAtlasPath.replaceBeforeLast('/', "").removePrefix("/")
        buildTextureAtlas(assetSourcePath, target, atlasFileName, settings, log)
    }
}


/**
 * Builds a texture atlas from all the images in the specified source path.
 */
fun buildTextureAtlas(assetSourcePath: String,
                      textureAtlasDirectory: String,
                      textureAtlasName: String,
                      settings: TexturePacker.Settings = createDefaultTextureAtlasSettings(),
                      log: Boolean = true) {
    // Clear out previous atlas
    val textureAtlasFileName = textureAtlasName
    val targetAtlas = File(textureAtlasDirectory + textureAtlasFileName)
    if (targetAtlas.exists()) {
        if (log) println("Deleting old texture atlas file at $targetAtlas")
        targetAtlas.delete()
    }

    // Generate atlas
    if (log) println("Updating texture atlas if source files modified, reading textures from $assetSourcePath and creating a texture atlas in $textureAtlasDirectory named $textureAtlasFileName")
    val updated = TexturePacker.processIfModified(settings, assetSourcePath, textureAtlasDirectory, textureAtlasFileName);
    if (log) {
        if (updated) {
            println("Input textures or settings were modified, so texture atlas was updated")
        }
        else {
            println("No modifications detected to input textures or settings, so texture atlas was not updated")
        }
    }
}

fun createDefaultTextureAtlasSettings(): TexturePacker.Settings {
    val settings = TexturePacker.Settings()
    settings.maxWidth = 2048
    settings.maxHeight = 2048
    settings.square = false
    settings.duplicatePadding = false
    settings.useIndexes = false
    settings.premultiplyAlpha = false
    settings.bleed = false
    settings.paddingX = 0
    settings.paddingY = 0
    settings.edgePadding = false
    settings.stripWhitespaceX = false
    settings.stripWhitespaceY = false
    settings.rotation = false
    settings.ignoreBlankImages = false
    return settings
}

/**
 * Loads the texture generation / packer settings from the specified file if found, if not returns null.
 */
fun loadTextureAtlasSettings(file: File): TexturePacker.Settings? {
    return if (file.exists()) {
        Gdx.app.log("loadTextureAtlasSettings", "Loading texture atlas generation settings from '$file'.")
        Json().fromJson(TexturePacker.Settings::class.java, FileReader(file))
    }
    else {
        Gdx.app.log("loadTextureAtlasSettings", "No texture atlas generation settings found at '$file', using defaults.")
        return null
    }
}

/**
 * Saves the texture generation / packer settings to the specified file.
 */
fun saveTextureAtlasSettings(file: File, settings: TexturePacker.Settings) {
    Gdx.app.log("saveTextureAtlasSettings", "Saving texture atlas generation settings to '$file'.")
    file.writeText(Json().toJson(settings, TexturePacker.Settings::class.java))
}

private val tempMatrix = object : ThreadLocal<Matrix4>() {
    override fun initialValue(): Matrix4? {
        return Matrix4()
    }
}

private val tempQuat = object : ThreadLocal<Quaternion>() {
    override fun initialValue(): Quaternion? {
        return Quaternion()
    }
}

private val tempVec = object : ThreadLocal<Vector3>() {
    override fun initialValue(): Vector3? {
        return Vector3()
    }
}

/**
 * Set this quaternion to the direction from the position towards the target, with the specified up direction (defaults to positive Y axis).
 */
fun Quaternion.setFromDirection(position: Vector3, target: Vector3, up: Vector3 = Vector3.Y, temporaryMatrix: Matrix4 = tempMatrix.get()) {
    temporaryMatrix.idt().setToLookAt(position, target, up).getRotation(this)
}

/**
 * Set this quaternion to the specified direction vector, with the specified up direction (defaults to positive Y axis).
 */
fun Quaternion.setFromDirection(direction: Vector3, up: Vector3 = Vector3.Y, temporaryMatrix: Matrix4 = tempMatrix.get()) {
    temporaryMatrix.idt().setToLookAt(direction, up).getRotation(this)
}

/**
 * Adds rotation around the specified axles to this quaternion, and normalizes this quaternion afterwards.
 * @param rotationAroundX contains the rotation around the X axis, as number of turns.
 * @param rotationAroundY contains the rotation around the X axis, as number of turns.
 * @param rotationAroundZ contains the rotation around the X axis, as number of turns.
 * @param scale scale to apply to the added rotations, defaults to 1.
 * @param temporaryVector a temporary vector to use, by default uses a thread local vector instance.
 * @param temporaryQuaternion temporary quaternion to use, by default uses a thread local quaternion instance.
 */
fun Quaternion.addRotation(rotationAroundX: Float,
                           rotationAroundY: Float,
                           rotationAroundZ: Float,
                           scale: Float = 1f,
                           temporaryVector: Vector3 = tempVec.get(),
                           temporaryQuaternion: Quaternion = tempQuat.get()) {
    addRotation(temporaryVector.set(rotationAroundX, rotationAroundY, rotationAroundZ), scale, temporaryVector, temporaryQuaternion)
}

/**
 * Adds rotation around the specified axles to this quaternion, and normalizes this quaternion afterwards.
 * @param rotationAroundAxles contains the rotation around each axis, in the member variables.
 * @param scale scale to apply to the added rotations, defaults to 1.
 * @param temporaryVector temporary vector to use, by default uses a thread local vector instance.
 * @param temporaryQuaternion temporary quaternion to use, by default uses a thread local quaternion instance.
 */
fun Quaternion.addRotation(rotationAroundAxles: Vector3,
                           scale: Float = 1f,
                           temporaryVector: Vector3 = tempVec.get(),
                           temporaryQuaternion: Quaternion = tempQuat.get()) {
    temporaryVector.set(rotationAroundAxles)
    val len = temporaryVector.len()
    if (len > 0f) {
        temporaryVector.scl(1f / len)
        temporaryQuaternion.setFromAxisRad(temporaryVector, scale * len * TauFloat)
        this.mulLeft(temporaryQuaternion)
        this.nor()
    }
}

/**
 * Adds another vector to this vector, first rotating the vector to be added with the specified direction.
 * @param direction direction to rotate the delta vector with.
 * @param deltaX value to add to the x axis as seen using the direction.
 * @param deltaY value to add to the y axis as seen using the direction.
 * @param deltaZ value to add to the z axis as seen using the direction.
 * @param scaling scaling to multiply the added vector with when it is added (does not modify the delta vector).
 * @param temporaryVector temporary vector to use, by default uses a thread local vector instance.
 */
fun Vector3.addInDirection(direction: Quaternion, deltaX: Float, deltaY: Float, deltaZ: Float, scaling: Float = 1f, temporaryVector: Vector3 = tempVec.get()) {
    addInDirection(direction, temporaryVector.set(deltaX, deltaY, deltaZ), scaling, temporaryVector)
}

/**
 * Adds another vector to this vector, first rotating the vector to be added with the specified direction.
 * @param direction direction to rotate the delta vector with.
 * @param delta delta vector to be added
 * @param scaling scaling to multiply the added vector with when it is added (does not modify the delta vector).
 * @param temporaryVector temporary vector to use, by default uses a thread local vector instance.
 */
fun Vector3.addInDirection(direction: Quaternion, delta: Vector3, scaling: Float = 1f, temporaryVector: Vector3 = tempVec.get()) {
    temporaryVector.set(delta).scl(scaling)
    direction.transform(temporaryVector)
    add(temporaryVector)
}

/**
 * Set this vector to the value of the input vector, and return this vector.
 */
inline fun Vector3.set(v: Double3): Vector3 {
    x = v.x.toFloat()
    y = v.y.toFloat()
    z = v.z.toFloat()
    return this
}

/**
 * Set this vector to the value of the input vector, and return this vector.
 */
inline fun Vector2.set(v: Double2): Vector2 {
    x = v.x.toFloat()
    y = v.y.toFloat()
    return this
}

/**
 * Set this vector to the value of the input vector, and return this vector.
 */
inline fun Vector3.set(v: Int3): Vector3 {
    x = v.x.toFloat()
    y = v.y.toFloat()
    z = v.z.toFloat()
    return this
}

/**
 * Set this vector to the value of the input vector, and return this vector.
 */
inline fun Vector2.set(v: Int2, zValue: Int = 0): Vector2 {
    x = v.x.toFloat()
    y = v.y.toFloat()
    return this
}


/**
 * Set this vector to the value of the input vector, and return this vector.
 */
inline fun MutableDouble3.set(v: Vector3): Double3 {
    x = v.x.toDouble()
    y = v.y.toDouble()
    z = v.z.toDouble()
    return this
}

/**
 * Set this vector to the value of the input vector, and return this vector.
 * Rounds down to nearest integer (uses floor).
 */
inline fun MutableInt3.setWithFloor(v: Vector3): MutableInt3 {
    x = v.x.fastFloor()
    y = v.y.fastFloor()
    z = v.z.fastFloor()
    return this
}

/**
 * Set this vector to the value of the input vector multiplied with the specified scalar and with the
 * offset added to all components, and return this vector.
 * Rounds down to nearest integer after the multiplication (uses floor).
 */
inline fun MutableInt3.setWithScaleAddAndFloor(v: Vector3, scale: Float = 1f, offset: Float = 0f): MutableInt3 {
    x = (scale * v.x + offset).fastFloor()
    y = (scale * v.y + offset).fastFloor()
    z = (scale * v.z + offset).fastFloor()
    return this
}

/**
 * Set this vector to the value of the input vector, and return this vector.
 * Rounds to closest integer (uses round).
 */
inline fun MutableInt3.setWithRound(v: Vector3): MutableInt3 {
    x = v.x.round()
    y = v.y.round()
    z = v.z.round()
    return this
}

/**
 * Set this vector to the value of the input vector multiplied with the specified scalar and with the offset added
 * to all components, and return this vector.
 * Rounds to closest integer after multiplication (uses round).
 */
inline fun MutableInt3.setWithScaleAddAndRound(v: Vector3, scale: Float = 1f, offset: Float = 0f): MutableInt3 {
    x = (scale * v.x + offset).round()
    y = (scale * v.y + offset).round()
    z = (scale * v.z + offset).round()
    return this
}

/**
 * Set this vector to the value of the input vector, and return this vector.
 * Rounds down to nearest integer (uses floor).
 */
inline fun MutableInt2.setWithFloor(v: Vector2): MutableInt2 {
    x = v.x.fastFloor()
    y = v.y.fastFloor()
    return this
}

/**
 * Set this vector to the value of the input vector, and return this vector.
 * Rounds to closest integer (uses round).
 */
inline fun MutableInt2.setWithRound(v: Vector2): MutableInt2 {
    x = v.x.round()
    y = v.y.round()
    return this
}


inline fun Int3.toVector3(vectorOut: Vector3 = Vector3()): Vector3 {
    vectorOut.set(x.toFloat(),
                  y.toFloat(),
                  z.toFloat())
    return vectorOut
}

inline fun Double3.toVector3(vectorOut: Vector3 = Vector3()): Vector3 {
    vectorOut.set(x.toFloat(),
                  y.toFloat(),
                  z.toFloat())
    return vectorOut
}

inline fun Int2.toVector2(vectorOut: Vector2 = Vector2()): Vector2 {
    vectorOut.set(x.toFloat(),
                  y.toFloat())
    return vectorOut
}

inline fun Double2.toVector2(vectorOut: Vector2 = Vector2()): Vector2 {
    vectorOut.set(x.toFloat(),
                  y.toFloat())
    return vectorOut
}

/**
 * A draw method that takes a TextureRegion and provides flips and coloring as well
 * @param x position to draw the sprite on
 * @param y position to draw the sprite on
 * @param originX position in the sprite to rotate around (and place at the x,y coordiante?)
 * @param originY position in the sprite to rotate around (and place at the x,y coordiante?)
 * @param width width to draw the sprite with
 * @param height height to draw the sprite with
 * @param scaleX scaling to apply to the final rendering after rotation (?)
 * @param scaleY scaling to apply to the final rendering after rotation (?)
 * @param rotation rotation of the sprite, counterclockwise, in degrees (360 to a turn)
 * @param flipX true to flip the image along the x axis
 * @param flipY true to flip the image along the y axis
 * @param color color to use to tint this picture.  Multiplied with the image when rendering it.
 */
fun SpriteBatch.draw(region: TextureRegion,
                     x: Float,
                     y: Float,
                     originX: Float,
                     originY: Float,
                     width: Float,
                     height: Float,
                     scaleX: Float,
                     scaleY: Float,
                     rotation: Float,
                     flipX: Boolean,
                     flipY: Boolean,
                     color: Color = Color.WHITE) {

    val oldColor = this.color
    this.color = color
    this.draw(region.texture, x, y, originX, originY, width, height, scaleX, scaleY, rotation,
              region.regionX, region.regionY, region.regionWidth, region.regionHeight, flipX, flipY)
    this.color = oldColor
}

/**
 * Creates a wireframe axis-aligned box for debugging purposes.
 * Supports construction out of multiple [segments], as the used z-buffer transformation doesn't deal well with long
 * shapes near the camera.
 */
fun ModelBuilder.buildWireframeBoxPart(position: Vector3,
                                       sizeX : Float,
                                       sizeY: Float = sizeX,
                                       sizeZ: Float = sizeX,
                                       color: Color = Color(0.1f, 0.5f, 0.1f, 1f),
                                       segments: Int = 16,
                                       center: Boolean = false,
                                       onlyShowCorners: Boolean = false,
                                       dashed: Boolean = false,
                                       doBeginEnd: Boolean = false,
                                       id: String = "wireframe" ) {
    if (doBeginEnd) begin()

    // Material
    val wireframeMaterial = Material()
    wireframeMaterial.set(ColorAttribute.createDiffuse(color))

    // Part builder
    val gridBuilder: MeshPartBuilder = part(id, GL20.GL_LINES, VertexAttributes.Usage.Position.toLong(), wireframeMaterial)

    // Corner vertexes
    val corner = if (center) position.cpy().sub(sizeX/2, sizeY/2, sizeZ/2) else position
    val vx = corner.cpy().add(sizeX, 0f, 0f)
    val vy = corner.cpy().add(0f, sizeY, 0f)
    val vz = corner.cpy().add(0f, 0f, sizeZ)
    val vxy = corner.cpy().add(sizeX, sizeY, 0f)
    val vyz = corner.cpy().add(0f, sizeY, sizeZ)
    val vxz = corner.cpy().add(sizeX, 0f, sizeZ)
    val vxyz = corner.cpy().add(sizeX, sizeY, sizeZ)

    // Lines
    gridBuilder.segmentedLine(corner, vx, segments, onlyShowCorners, dashed)
    gridBuilder.segmentedLine(corner, vy, segments, onlyShowCorners, dashed)
    gridBuilder.segmentedLine(corner, vz, segments, onlyShowCorners, dashed)
    gridBuilder.segmentedLine(vx, vxy, segments, onlyShowCorners, dashed)
    gridBuilder.segmentedLine(vx, vxz, segments, onlyShowCorners, dashed)
    gridBuilder.segmentedLine(vy, vxy, segments, onlyShowCorners, dashed)
    gridBuilder.segmentedLine(vy, vyz, segments, onlyShowCorners, dashed)
    gridBuilder.segmentedLine(vz, vxz, segments, onlyShowCorners, dashed)
    gridBuilder.segmentedLine(vz, vyz, segments, onlyShowCorners, dashed)
    gridBuilder.segmentedLine(vxy, vxyz, segments, onlyShowCorners, dashed)
    gridBuilder.segmentedLine(vyz, vxyz, segments, onlyShowCorners, dashed)
    gridBuilder.segmentedLine(vxz, vxyz, segments, onlyShowCorners, dashed)

    if (doBeginEnd) end()
}

/**
 * Creates a line that consists of many segments.
 */
fun MeshPartBuilder.segmentedLine(start: Vector3, end: Vector3, segments: Int, onlyIncludeFirstAndLast: Boolean = false, dashed: Boolean = false) {
    val p1 = Vector3()
    val p2 = Vector3()

    for (i in 0 until segments) {
        // Determine if the segment should be rendered
        var include = true
        if (onlyIncludeFirstAndLast && i > 0 && i < segments - 1) include = false
        else if (dashed && i % 2 == 1) include = false
        if (include) {
            // Determine start and end of this segment
            val relativePos1 = relPos(i, 0, segments)
            val relativePos2 = relPos(i+1, 0, segments)
            p1.set(start).lerp(end, relativePos1.toFloat())
            p2.set(start).lerp(end, relativePos2.toFloat())

            line(p1, p2)
        }
    }
}

fun loadTextFile(name: String): String {
    // Try to find in resources dir
    val file = File("./src/main/resources/$name")
    if (file.exists()) return file.readText()

    // Try to load internal
    return Gdx.files.internal(name).readString()
}


fun loadShaderProvider(internalPath: String): ShaderProvider {
    val vert = loadTextFile("$internalPath.vertex.glsl")
    val frag = loadTextFile("$internalPath.fragment.glsl")
    return DefaultShaderProvider(vert, frag)
}

/**
 * Loads shader program from the specified internal path.  The .vertex.glsl and .fragment.glsl extensions are added
 * automatically.  If the program doesn't compile, a GdxRuntimeException is thrown.
 */
fun loadShaderProgram(internalPath: String): ShaderProgram {
    val vert = loadTextFile("$internalPath.vertex.glsl")
    val frag = loadTextFile("$internalPath.fragment.glsl")
    val shaderProgram = ShaderProgram(vert, frag)

    if (!shaderProgram.isCompiled) {
        throw GdxRuntimeException(shaderProgram.log)
    }

    return shaderProgram
}

/**
 * Set this bounding box to the specified volume.
 */
fun BoundingBox.set(volume: Volume) {
    if (volume.empty) {
        this.inf()
    }
    else {
        this.min.x = volume.minX.toFloat()
        this.min.y = volume.minY.toFloat()
        this.min.z = volume.minZ.toFloat()
        this.max.x = volume.maxX.toFloat()
        this.max.y = volume.maxY.toFloat()
        this.max.z = volume.maxZ.toFloat()
    }
}

/**
 * Set this volume to the specified bounding box.
 */
fun MutableVolume.set(boundingBox: BoundingBox) {
    this.set(
        boundingBox.min.x.toDouble(),
        boundingBox.min.y.toDouble(),
        boundingBox.min.z.toDouble(),
        boundingBox.max.x.toDouble(),
        boundingBox.max.y.toDouble(),
        boundingBox.max.z.toDouble(),
        false
    )
}

/**
 * Stores this vector to the given position in the specified float array.
 * If the array is not specified, a new 3-length float array is created.
 */
fun Vector3.toFloatArray(arrayOut: FloatArray = FloatArray(3), arrayStartPos: Int = 0): FloatArray {
    arrayOut[arrayStartPos + 0] = x
    arrayOut[arrayStartPos + 1] = y
    arrayOut[arrayStartPos + 2] = z
    return arrayOut
}

/**
 * Stores this vector to the given position in the specified float array.
 * If the array is not specified, a new 2-length float array is created.
 */
fun Vector2.toFloatArray(arrayOut: FloatArray = FloatArray(2), arrayStartPos: Int = 0): FloatArray {
    arrayOut[arrayStartPos + 0] = x
    arrayOut[arrayStartPos + 1] = y
    return arrayOut
}


/**
 * Set each coordinate of this vector to the minimum of the coordinate in this vector and in the other vector.
 * Returns this vector.
 */
fun Vector3.setMin(other: Vector3): Vector3 {
    x = min(x, other.x)
    y = min(y, other.y)
    z = min(z, other.z)
    return this
}

/**
 * Set each coordinate of this vector to the maximum of the coordinate in this vector and in the other vector.
 * Returns this vector.
 */
fun Vector3.setMax(other: Vector3): Vector3 {
    x = max(x, other.x)
    y = max(y, other.y)
    z = max(z, other.z)
    return this
}


