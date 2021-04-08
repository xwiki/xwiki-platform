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
package org.xwiki.notifications.notifiers.internal;

import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.job.GroupedJobInitializer;
import org.xwiki.job.JobGroupPath;
import org.xwiki.notifications.NotificationConfiguration;

/**
 * {@link GroupedJobInitializer} for the {@link DefaultAsyncNotificationRenderer}.
 * Jobs created for the async notification renderer will use the properties defined here to be executed.
 *
 * @since 12.5RC1
 * @version $Id$
 */
@Component
@Named("AsyncNotificationRenderer")
@Singleton
public class AsyncNotificationRendererJobInitializer implements GroupedJobInitializer
{
    private static final JobGroupPath NOTIFICATION_JOBGROUPPATH =
        new JobGroupPath(Collections.singletonList("notifications"));

    @Inject
    private NotificationConfiguration notificationConfiguration;
    
    @Override
    public JobGroupPath getId()
    {
        return NOTIFICATION_JOBGROUPPATH;
    }

    @Override
    public int getPoolSize()
    {
        return this.notificationConfiguration.getAsyncPoolSize();
    }

    /**
     * @return a lower priority than {@link Thread#NORM_PRIORITY} since notifications are resource consuming and we
     *          want other threads to have the priority.
     */
    @Override
    public int getDefaultPriority()
    {
        return Thread.NORM_PRIORITY - 1;
    }
}
