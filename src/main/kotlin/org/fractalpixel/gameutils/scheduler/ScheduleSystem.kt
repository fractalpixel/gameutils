package org.fractalpixel.gameutils.scheduler

import org.entityflakes.Entity
import org.entityflakes.entitymanager.ComponentRef
import org.entityflakes.system.EntitySystemBase
import org.kwrench.time.Time
import org.kwrench.updating.strategies.VariableTimestepStrategy


/**
 * Processes entities with Scheduled components, running scheduled actions.
 */
class ScheduleSystem: EntitySystemBase(VariableTimestepStrategy(),false, Scheduled::class) {

    private val scheduledRef = ComponentRef(Scheduled::class)

    override fun updateEntity(entity: Entity, time: Time) {
        scheduledRef[entity].update(time)
    }

}