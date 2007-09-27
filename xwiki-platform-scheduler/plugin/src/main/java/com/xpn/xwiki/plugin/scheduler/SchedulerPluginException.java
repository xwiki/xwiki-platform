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

import com.xpn.xwiki.plugin.PluginException;

public class SchedulerPluginException extends PluginException
{
    protected static final int ERROR_SCHEDULERPLUGIN_SAVE_JOB_CLASS = 90000;

    protected static final int ERROR_SCHEDULERPLUGIN_INITIALIZE_STATUS_LISTENER = 90001;

    protected static final int ERROR_SCHEDULERPLUGIN_PAUSE_JOB = 90002;

    protected static final int ERROR_SCHEDULERPLUGIN_RESUME_JOB = 90003;

    protected static final int ERROR_SCHEDULERPLUGIN_SCHEDULE_JOB = 90004;

    protected static final int ERROR_SCHEDULERPLUGIN_BAD_CRON_EXPRESSION = 90005;

    protected static final int ERROR_SCHEDULERPLUGIN_JOB_XCLASS_NOT_FOUND = 90006;

    protected static final int ERROR_SCHEDULERPLUGIN_JOB_DOES_NOT_EXITS = 90007;

    protected static final int ERROR_SCHEDULERPLUGIN_GET_SCHEDULER = 90007;

    protected static final int ERROR_SCHEDULERPLUGIN_RESTORE_JOBS = 90008;

    public SchedulerPluginException(int code, String message)
    {
        super(SchedulerPlugin.class, code, message);
    }

    public SchedulerPluginException(int code, String message, Throwable e, Object[] args)
    {
        super(SchedulerPlugin.class, code, message, e, args);
    }

    public SchedulerPluginException(int code, String message, Throwable e)
    {
        super(SchedulerPlugin.class, code, message, e);
    }
}
