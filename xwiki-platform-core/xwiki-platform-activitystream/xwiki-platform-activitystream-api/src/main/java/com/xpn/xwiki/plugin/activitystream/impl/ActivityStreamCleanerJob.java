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
package com.xpn.xwiki.plugin.activitystream.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.xpn.xwiki.plugin.activitystream.api.ActivityEvent;
import com.xpn.xwiki.plugin.activitystream.api.ActivityStreamException;
import com.xpn.xwiki.plugin.activitystream.plugin.ActivityStreamPlugin;
import com.xpn.xwiki.plugin.scheduler.AbstractJob;

/**
 * This job deletes all the events older than a configured number of days in the activitystream datastore.
 *
 * @version $Id$
 */
public class ActivityStreamCleanerJob extends AbstractJob implements Job
{
    @Override
    protected void executeJob(JobExecutionContext jobContext) throws JobExecutionException
    {
        ActivityStreamPlugin plugin = (ActivityStreamPlugin) getXWikiContext().getWiki()
            .getPlugin(ActivityStreamPlugin.PLUGIN_NAME, getXWikiContext());
        List<Object> parameters = new ArrayList<Object>();
        int days = ActivityStreamCleaner.getNumberOfDaysToKeep(getXWikiContext());

        if (days > 0) {
            parameters.add(DateUtils.addDays(new Date(), days * -1));
            try {
                List<ActivityEvent> events = plugin.getActivityStream().searchEvents("date < ?1", false, true, 0, 0,
                    parameters, getXWikiContext());
                for (ActivityEvent event : events) {
                    plugin.getActivityStream().deleteActivityEvent(event, getXWikiContext());
                }
            } catch (ActivityStreamException e) {
                // TODO
            }
        }
    }
}
