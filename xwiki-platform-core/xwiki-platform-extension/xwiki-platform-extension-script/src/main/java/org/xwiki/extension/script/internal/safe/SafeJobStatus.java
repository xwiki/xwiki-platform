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
package org.xwiki.extension.script.internal.safe;

import java.util.Date;
import java.util.List;

import org.xwiki.extension.internal.safe.AbstractSafeObject;
import org.xwiki.extension.internal.safe.ScriptSafeProvider;
import org.xwiki.job.Request;
import org.xwiki.job.event.status.JobProgress;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.LogQueue;
import org.xwiki.logging.event.LogEvent;

/**
 * Provide a public script access to a job status.
 * 
 * @param <J> the type of the job status
 * @version $Id$
 * @since 4.0M2
 */
public class SafeJobStatus<J extends JobStatus> extends AbstractSafeObject<J> implements JobStatus
{
    /**
     * @param status the wrapped job status
     * @param safeProvider the provider of instances safe for public scripts
     */
    public SafeJobStatus(J status, ScriptSafeProvider< ? > safeProvider)
    {
        super(status, safeProvider);
    }

    @Override
    public State getState()
    {
        return getWrapped().getState();
    }

    @Override
    public Request getRequest()
    {
        return getWrapped().getRequest();
    }

    @Override
    public LogQueue getLog()
    {
        return getWrapped().getLog();
    }

    @Override
    public List<LogEvent> getLog(LogLevel level)
    {
        return getWrapped().getLog(level);
    }

    @Override
    public JobProgress getProgress()
    {
        return getWrapped().getProgress();
    }

    @Override
    public void ask(Object question) throws InterruptedException
    {
        getWrapped().ask(question);
    }

    @Override
    public Object getQuestion()
    {
        return getWrapped().getQuestion();
    }

    @Override
    public void answered()
    {
        getWrapped().answered();
    }

    @Override
    public Date getStartDate()
    {
        return getWrapped().getStartDate();
    }

    @Override
    public Date getEndDate()
    {
        return getWrapped().getEndDate();
    }
}
