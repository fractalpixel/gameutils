package org.fractalpixel.gameutils.rendering

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector2
import org.kwrench.geometry.double3.Double3
import org.kwrench.geometry.double3.MutableDouble3
import org.kwrench.geometry.rectangle.MutableRect
import org.kwrench.geometry.volume.MutableVolume
import org.kwrench.math.map

/**
 * Camera for 2d projection.
 */
// TODO: Extract interface
class Camera2D(val worldFocus: MutableDouble3 = MutableDouble3(),
               var visibleWorldDiameter: Double = 10.0,
               var worldYPerZ: Double = 1.0,
               var zVisibleUnderFocus: Double = 10.0,
               var zVisibleAboveFocus: Double = 10.0) {

    /**
     * Stores the screen screen area that this camera projects to.
     * Use setToGdxScreenSize() to initialize it to the whole screen.
     */
    val screenArea: MutableRect = MutableRect(0.0, 0.0, 1.0, 1.0)

    val visibleWorldWidth: Double get() {
        return if (screenIsEmpty) 0.0
        else if (screenArea.w <= screenArea.h) visibleWorldDiameter
        else visibleWorldDiameter * (screenArea.h / screenArea.w)
    }

    val visibleWorldHeight: Double get() {
        return if (screenIsEmpty) 0.0
        else if (screenArea.h <= screenArea.w) visibleWorldDiameter
        else visibleWorldDiameter * (screenArea.w / screenArea.h)
    }

    fun getVisibleWorldArea(worldAreaOut: MutableRect = MutableRect()): MutableRect {
        if (screenIsEmpty) {
            worldAreaOut.set(worldFocus.x, worldFocus.y, 0.0, 0.0)
            worldAreaOut.empty = true
        }
        else {
            val worldW = visibleWorldWidth
            val worldH = visibleWorldHeight
            worldAreaOut.set(worldFocus.x - 0.5 * worldW,
                             worldFocus.y - 0.5 * worldH,
                             worldW,
                             worldH)
        }
        return worldAreaOut
    }

    fun getVisibleWorldVolume(worldVolumeOut: MutableVolume = MutableVolume()): MutableVolume {
        if (screenIsEmpty) {
            worldVolumeOut.clear()
        }
        else {
            val worldW = visibleWorldWidth
            val worldH = visibleWorldHeight
            worldVolumeOut.set(worldFocus.x - 0.5 * worldW,
                               worldFocus.y - 0.5 * worldH,
                               worldFocus.z - zVisibleUnderFocus,
                               worldFocus.x + 0.5 * worldW,
                               worldFocus.y + 0.5 * worldH,
                               worldFocus.z + zVisibleAboveFocus)
        }
        return worldVolumeOut
    }

    val screenIsEmpty: Boolean get() = screenArea.empty || screenArea.w <= 0 || screenArea.h <= 0

    fun setScreenSize(width: Int, height: Int) {
        screenArea.set(0.0, 0.0, width.toDouble(), height.toDouble())
    }

    fun projectToScreen(worldPos: Double3, screenPosOut: Vector2): Vector2 {
        if (screenIsEmpty) {
            screenPosOut.x = 0f
            screenPosOut.y = 0f
        } else {
            val w = visibleWorldWidth
            val h = visibleWorldHeight
            val posY = worldPos.y + worldPos.z * worldYPerZ
            val focusY = worldFocus.y + worldFocus.z * worldYPerZ
            screenPosOut.x = map(worldPos.x,
                                 worldFocus.x - 0.5 * w,
                                 worldFocus.x + 0.5 * w,
                                 screenArea.minX,
                                 screenArea.maxX).toFloat()
            screenPosOut.y = map(posY,
                                 focusY - 0.5 * h,
                                 focusY + 0.5 * h,
                                 screenArea.minY,
                                 screenArea.maxY).toFloat()
        }

        return screenPosOut
    }

    fun projectToWorld(screenPos: Vector2,
                       worldZ: Double = worldFocus.z,
                       worldPosOut: MutableDouble3 = MutableDouble3()): MutableDouble3 {
        if (screenIsEmpty) {
            worldPosOut.zero()
        } else {
            val w = visibleWorldWidth
            val h = visibleWorldHeight
            worldPosOut.x = map(screenPos.x.toDouble(),
                                screenArea.minX,
                                screenArea.maxX,
                                worldFocus.x - 0.5 * w,
                                worldFocus.x + 0.5 * w)
            worldPosOut.y = map(screenPos.y.toDouble(),
                                screenArea.minY,
                                screenArea.maxY,
                                worldFocus.y - 0.5 * h,
                                worldFocus.y + 0.5 * h) -
                            worldZ * worldYPerZ
            worldPosOut.z = worldZ
        }

        return worldPosOut
    }

    /**
     * @return width on screen for the specified width in the world.
     */
    fun projectWidthToScreen(worldWidth: Double): Float {
        return map(worldWidth, 0.0, visibleWorldWidth, 0.0, screenArea.width).toFloat()
    }

    /**
     * @return height on screen for the specified height (or y-distance) in the world.
     */
    fun projectHeightToScreen(worldHeight: Double): Float {
        return map(worldHeight, 0.0, visibleWorldHeight, 0.0, screenArea.height).toFloat()
    }

    /**
     * Project a size in the world to a size on the screen.
     * By default the world size is 1,1.
     */
    fun projectSizeToScreen(worldWidth: Double = 1.0,
                            WorldHeight: Double = 1.0,
                            screenSize: Vector2 = Vector2()): Vector2 {
        screenSize.x = projectWidthToScreen(worldWidth)
        screenSize.y = projectHeightToScreen(WorldHeight)
        return screenSize
    }

    /**
     * @return width in the world for the specified width on the screen.
     */
    fun projectWidthToWorld(screenWidth: Float): Double {
        return map(screenWidth.toDouble(), 0.0, screenArea.width, 0.0, visibleWorldWidth)
    }

    /**
     * @return height (or y-distance) in the world for the specified height on the screen.
     */
    fun projectHeightToWorld(screenHeight: Float): Double {
        return map(screenHeight.toDouble(), 0.0, screenArea.height, 0.0, visibleWorldHeight)
    }

    fun setToGdxScreenSize() {
        setScreenSize(Gdx.graphics.width, Gdx.graphics.height)
    }

}