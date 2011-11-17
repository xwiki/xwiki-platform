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
package org.xwiki.extension.unmodifiable;

import java.util.List;

import org.xwiki.extension.job.JobProgress;
import org.xwiki.extension.job.JobStatus;
import org.xwiki.extension.job.Request;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.LogQueue;
import org.xwiki.logging.event.LogEvent;

/**
 * Provide a readonly access to a job status.
 * 
 * @version $Id$
 */
public class UnmodifiableJobStatus implements JobStatus
{
    /**
     * The wrapped job status.
     */
    private JobStatus status;

    /**
     * @param status the wrapped job status
     */
    public UnmodifiableJobStatus(JobStatus status)
    {
        this.status = status;
    }

    @Override
    public State getState()
    {
        return this.status.getState();
    }

    @Override
    public Request getRequest()
    {
        return this.status.getRequest();
    }

    @Override
    public LogQueue getLog()
    {
        return this.status.getLog();
    }

    @Override
    public List<LogEvent> getLog(LogLevel level)
    {
        return this.status.getLog(level);
    }

    @Override
    public JobProgress getProgress()
    {
        return this.status.getProgress();
    }
}
