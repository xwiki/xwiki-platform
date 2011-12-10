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

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.Utils;

/**
 * Base class for any XWiki Quartz Job. This class take care of initializing ExecutionContext properly.
 * <p>
 * A class extending {@link AbstractJob} should implements {@link #executeJob(JobExecutionContext)}.
 * 
 * @since 1.8
 * @version $Id$
 */
public abstract class AbstractJob implements Job
{
    @Override
    public final void execute(JobExecutionContext jobContext) throws JobExecutionException
    {
        JobDataMap data = jobContext.getJobDetail().getJobDataMap();

        // The XWiki context was saved in the Job execution data map. Get it as we'll retrieve
        // the script to execute from it.
        XWikiContext xwikiContext = (XWikiContext) data.get("context");

        // Init execution context
        Execution execution;
        try {
            ExecutionContextManager ecim = Utils.getComponent(ExecutionContextManager.class);
            execution = Utils.getComponent(Execution.class);

            ExecutionContext context = new ExecutionContext();

            // Bridge with old XWiki Context, required for old code.
            context.setProperty("xwikicontext", xwikiContext);

            ecim.initialize(context);
        } catch (ExecutionContextException e) {
            throw new JobExecutionException("Fail to initialize execution context", e);
        }

        try {
            // Execute the job
            executeJob(jobContext);
        } finally {
            // We must ensure we clean the ThreadLocal variables located in the Execution
            // component as otherwise we will have a potential memory leak.
            execution.removeContext();
        }
    }

    protected abstract void executeJob(JobExecutionContext jobContext) throws JobExecutionException;
}
