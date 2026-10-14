/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xpn.xwiki.plugin.scheduler;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.quartz.Trigger.TriggerState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit tests for {@link JobState}.
 *
 * @version $Id$
 */
class JobStateTest
{
    @ParameterizedTest
    @CsvSource({
        "NORMAL, Normal",
        "PAUSED, Paused",
        "BLOCKED, Blocked",
        "COMPLETE, Complete",
        "ERROR, Error",
        "NONE, None"
    })
    void getValue(TriggerState quartzState, String expectedValue)
    {
        JobState state = new JobState(quartzState);

        assertEquals(expectedValue, state.getValue());
        assertSame(quartzState, state.getQuartzState());
        assertEquals(quartzState.ordinal(), state.getState());
    }

    @ParameterizedTest
    @CsvSource({
        "NORMAL, PAUSED, Paused",
        "PAUSED, NORMAL, Normal"
    })
    void setQuartzState(TriggerState initialState, TriggerState newState, String expectedValue)
    {
        JobState state = new JobState(initialState);

        state.setQuartzState(newState);

        assertEquals(expectedValue, state.getValue());
        assertSame(newState, state.getQuartzState());
    }
}
