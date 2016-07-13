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
package com.xpn.xwiki.plugin.watchlist;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * WatchList abstract implementation of Quartz's Job.
 * 
 * @version $Id$
 */
@Deprecated
public class WatchListJob implements Job
{
    /**
     * The wrapped watchlist job object.
     */
    org.xwiki.watchlist.internal.job.WatchListJob wrappedJob = new org.xwiki.watchlist.internal.job.WatchListJob();

    /**
     * Sets objects required by the Job : XWiki, XWikiContext, WatchListPlugin, etc.
     * 
     * @param jobContext Context of the request
     * @throws Exception when the init of components fails
     */
    public void init(JobExecutionContext jobContext) throws Exception
    {
        wrappedJob.init(jobContext);
    }

    /**
     * @return ID of the job
     */
    public String getId()
    {
        return wrappedJob.getId();
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException
    {
        this.wrappedJob.execute(context);
    }
}
