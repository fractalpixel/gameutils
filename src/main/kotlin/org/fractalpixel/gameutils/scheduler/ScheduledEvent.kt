package org.fractalpixel.gameutils.scheduler

import org.entityflakes.Entity
import org.mistutils.math.relPos
import org.mistutils.time.Time

/**
 * Event that will happen to some entity at a specified time
 * Can also happen over some time, for example for transitions and fades.
 * @param startGameTime game time when the event starts
 * @param durationGameSeconds duration of the event
 * @param action action to run each fame during the event.
 * The progress parameter goes from 0 to 1 over the duration of the event (or 0.5 if instanteous)
 * @param positionInterpolator interpolator to use for the progress parameter.
 */
class ScheduledEvent(val startGameTime: Double,
                     val action: (entity: Entity, progress: Double, gameTime: Time) -> Unit,
                     val durationGameSeconds: Double? = null,
                     val positionInterpolator: ((progress: Double) -> Double)? = null) {

    /**
     * Calls the event action if needed.
     * @return true if the event should be kept, false if it should be discarded
     */
    fun updateEvent(entity: Entity, time: Time): Boolean {
        val currentTime = time.secondsSinceStart
        if (durationGameSeconds != null) {
            // Intervall
            val relPos = relPos(currentTime, startGameTime, startGameTime + durationGameSeconds)
            if (relPos in 0.0..1.0) {
                val interpolator = positionInterpolator
                val progress = if (interpolator != null) interpolator(relPos) else relPos

                action(entity, progress, time)
            }

            // Check if event still on
            return currentTime < startGameTime + (durationGameSeconds ?: 0.0)
        }
        else {
            // Instanteous
            if (currentTime >= startGameTime) {
                action(entity, 0.5, time)
                return false
            }
            else return true
        }
    }
}