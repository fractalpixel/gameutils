package org.fractalpixel.gameutils.captionservice

import com.badlogic.gdx.graphics.Color
import org.entityflakes.World
import org.entityflakes.processor.ProcessorBase
import org.fractalpixel.gameutils.layer.layers.CenteredTextLayer
import org.fractalpixel.gameutils.libgdxutils.FontInfo
import org.fractalpixel.gameutils.scheduler.Scheduled
import org.fractalpixel.gameutils.scheduler.ScheduledEvent
import org.mistutils.interpolation.interpolators.CosineInterpolator
import org.mistutils.time.Time

/**
 * Used for displaying large messages to the player.
 */
class CaptionService: ProcessorBase() {

    var fadeInStartColor: Color = Color(0f, 0f, 0.5f, 0f)
    var fadeOutEndColor: Color = Color(0.5f, 0f, 0.0f, 0f)

    private val textLayer = CenteredTextLayer("",
                                              FontInfo(sizePixelsHigh = 100),
                                              Color(0f, 0f, 0f, 0f))
    private val textSchedule = Scheduled()

    override fun doInit(world: World) {
        // Create the caption entity
        world.createEntity(textLayer, textSchedule)
    }

    /**
     * Show the specified text with the specified color for the specified duration
     */
    fun showText(text: String,
                 color: Color = Color.WHITE,
                 duration: Double = 2.0,
                 preDelay: Double = 0.0,
                 fadeInTime: Double = duration * 0.5,
                 fadeOutTime: Double = fadeInTime*1.5) {
        // Start after the end of the previous message, or immediately if there is no previous message
        val startTime = (textSchedule.endTime ?: world.time.secondsSinceStart) + preDelay
        textSchedule.addEvent(textChange(startTime, text))
        textSchedule.addEvent(colorFade(startTime, fadeInTime, fadeInStartColor, color))
        textSchedule.addEvent(colorFade(startTime+fadeInTime+duration, fadeOutTime, color, fadeOutEndColor))
    }

    private fun textChange(time: Double, text: String): ScheduledEvent {
        //println("Scheduled text $text at time $time")
        return ScheduledEvent(time, { _, _, _ -> textLayer.text = text })
    }

    private fun colorFade(startTime: Double, duration: Double, c1: Color, c2: Color): ScheduledEvent {
        return ScheduledEvent(startTime, { _, progress, _ ->
            textLayer.color.set(c1).lerp(c2, progress.toFloat())
        }, duration, CosineInterpolator())
    }

    override fun update(time: Time) {
        // Not used
    }



}