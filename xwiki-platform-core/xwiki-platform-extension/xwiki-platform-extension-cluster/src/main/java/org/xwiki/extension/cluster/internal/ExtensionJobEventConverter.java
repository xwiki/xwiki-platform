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
package org.xwiki.extension.cluster.internal;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.job.internal.InstallJob;
import org.xwiki.extension.job.internal.UninstallJob;
import org.xwiki.job.AbstractRequest;
import org.xwiki.job.DefaultRequest;
import org.xwiki.job.Request;
import org.xwiki.job.event.JobStartedEvent;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.converter.AbstractEventConverter;

/**
 * Convert {@link JobStartedEvent} to and from remote event.
 * 
 * @version $Id$
 * @since 4.0M1
 */
@Component
@Singleton
@Named("ExtensionJob")
public class ExtensionJobEventConverter extends AbstractEventConverter
{
    /**
     * The events supported by this converter. We only share install and uninstall jobs since other job don't touch
     * anything.
     */
    private static final Set<String> JOBS = new HashSet<String>()
    {
        {
            add(UninstallJob.JOBTYPE);
            add(InstallJob.JOBTYPE);
        }
    };

    @Override
    public boolean toRemote(LocalEventData localEvent, RemoteEventData remoteEvent)
    {
        if (localEvent.getEvent() instanceof JobStartedEvent) {
            JobStartedEvent jobEvent = (JobStartedEvent) localEvent.getEvent();

            if (JOBS.contains(jobEvent.getJobId())) {
                remoteEvent.setEvent(jobEvent);

                return true;
            }
        }

        return false;
    }

    @Override
    public boolean fromRemote(RemoteEventData remoteEvent, LocalEventData localEvent)
    {
        if (remoteEvent.getEvent() instanceof JobStartedEvent) {
            JobStartedEvent jobEvent = (JobStartedEvent) remoteEvent.getEvent();

            Request request = jobEvent.getRequest();

            // Indicate the job has been triggered by a remote event
            if (!(request instanceof AbstractRequest)) {
                request = new DefaultRequest(request);
            }
            ((AbstractRequest) request).setRemote(true);

            // We don't want to directly simulate a new JobStartedEvent event but we want to start a new job which
            // will generate a new JobStartedEvent
            localEvent.setEvent(new RemoteJobStartedEvent(jobEvent.getJobType(), request));

            return true;
        }

        return false;
    }
}
