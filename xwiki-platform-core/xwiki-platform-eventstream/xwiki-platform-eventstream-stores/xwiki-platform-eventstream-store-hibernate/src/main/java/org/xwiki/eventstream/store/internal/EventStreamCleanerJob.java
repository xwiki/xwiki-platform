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
package org.xwiki.eventstream.store.internal;

import com.xpn.xwiki.web.Utils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import com.xpn.xwiki.plugin.scheduler.AbstractJob;

/**
 * This job deletes all the events older than a configured number of days in the activitystream datastore.
 *
 * @version $Id$
 */
public class EventStreamCleanerJob extends AbstractJob implements Job
{
    @Override
    protected void executeJob(JobExecutionContext jobContext) throws JobExecutionException
    {
        EventStreamCleaner cleaner = Utils.getComponent(EventStreamCleaner.class);
        cleaner.clean();
    }
}
