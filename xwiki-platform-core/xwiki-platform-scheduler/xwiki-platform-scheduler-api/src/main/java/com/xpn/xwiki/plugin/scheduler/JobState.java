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

import org.quartz.Trigger.TriggerState;

/**
 * Wrapper around the Quartz trigger's inner state of a Scheduler Job. This class allows to query the actual status of a
 * Job as a String, typically to be displayed inside the Wiki
 * 
 * @version $Id$
 */
public class JobState
{
    public static final String STATE_NORMAL = "Normal";

    public static final String STATE_PAUSED = "Paused";

    public static final String STATE_BLOCKED = "Blocked";

    public static final String STATE_COMPLETE = "Complete";

    public static final String STATE_ERROR = "Error";

    public static final String STATE_NONE = "None";

    private TriggerState state;

    @Deprecated
    public JobState(int state)
    {
        setState(state);
    }

    public JobState(TriggerState state)
    {
        setQuartzState(state);
    }

    @Deprecated
    public void setState(int state)
    {
        this.state = TriggerState.values()[state];
    }

    public void setQuartzState(TriggerState state)
    {
        this.state = state;
    }

    public int getState()
    {
        return this.state.ordinal();
    }

    public TriggerState getQuartzState()
    {
        return this.state;
    }

    public String getValue()
    {
        switch (this.state) {
            case NORMAL:
                return JobState.STATE_NORMAL;
            case BLOCKED:
                return JobState.STATE_BLOCKED;
            case COMPLETE:
                return JobState.STATE_COMPLETE;
            case ERROR:
                return JobState.STATE_ERROR;
            case PAUSED:
                return JobState.STATE_PAUSED;
            case NONE:
            default:
                return JobState.STATE_NONE;
        }
    }
}
