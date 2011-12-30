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
package org.xwiki.extension.job.event.status;

import java.util.List;

import org.xwiki.extension.job.Request;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.LogQueue;
import org.xwiki.logging.event.LogEvent;

/**
 * Describe the current status of a job.
 * 
 * @version $Id$
 */
public interface JobStatus
{
    /**
     * Job status.
     * 
     * @version $Id$
     */
    enum State
    {
        /**
         * Default status, generally mean that the task has not been started yet.
         */
        NONE,

        /**
         * The task has been paused.
         */
        PAUSED,

        /**
         * The task is running.
         */
        RUNNING,

        /**
         * The task is done.
         */
        FINISHED
    }

    /**
     * @return the general state of the job
     */
    State getState();

    /**
     * @return the job request provided when starting it
     */
    Request getRequest();

    /**
     * @return the log sent during job execution
     */
    LogQueue getLog();

    /**
     * @param level the level of the log
     * @return the log sent with the provided level
     */
    List<LogEvent> getLog(LogLevel level);

    /**
     * @return progress information about the job (percent, etc.)
     */
    JobProgress getProgress();
}
