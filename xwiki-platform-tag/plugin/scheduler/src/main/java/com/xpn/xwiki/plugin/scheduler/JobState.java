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

import org.quartz.Trigger;

/**
 * Wrapper around the Quartz trigger's inner state of a Scheduler Job. This class allows to query
 * the actual status of a Job as a String, typically to be displayed inside the Wiki
 */
public class JobState
{
    public static final String NORMAL = "Normal";

    public static final String PAUSED = "Paused";

    public static final String BLOCKED = "Blocked";

    public static final String COMPLETE = "Complete";

    public static final String ERROR = "Error";

    public static final String NONE = "None";

    private int state;

    public JobState(int state)
    {
        setState(state);
    }

    public void setState(int state)
    {
        this.state = state;
    }

    public int getState()
    {
        return this.state;
    }

    public String getValue()
    {
        switch (this.state) {
            case Trigger.STATE_NORMAL:
                return JobState.NORMAL;
            case Trigger.STATE_BLOCKED:
                return JobState.BLOCKED;
            case Trigger.STATE_COMPLETE:
                return JobState.COMPLETE;
            case Trigger.STATE_ERROR:
                return JobState.ERROR;
            case Trigger.STATE_PAUSED:
                return JobState.PAUSED;
            case Trigger.STATE_NONE:
            default:
                return JobState.NONE;
        }
    }
}
