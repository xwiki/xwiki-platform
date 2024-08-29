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
package org.xwiki.eventstream.script;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.eventstream.EventStream;
import org.xwiki.eventstream.internal.LegacyEventMigrationJob;
import org.xwiki.eventstream.internal.LegacyEventMigrationRequest;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.job.JobStatusStore;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.query.QueryException;

/**
 * Script services for the Event Stream Module.
 *
 * @version $Id$
 * @since 9.2RC1
 */
@Component
@Singleton
@Named("eventstream")
public class LegacyEventStreamScriptService extends EventStreamScriptService
{
    private static final List<String> LEGACY_MIGRATOR_ID = Arrays.asList("event", "legacy", "migrator");

    @Inject
    private JobExecutor jobs;

    @Inject
    private JobStatusStore statuses;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    private EventStream eventStream;

    /**
     * Get the status of the job responsible for copying legacy event in the new store.
     * 
     * @return the status of the process
     * @since 12.6
     */
    public JobStatus getLegacyMigrationStatus()
    {
        Job job = this.jobs.getJob(LEGACY_MIGRATOR_ID);

        if (job != null) {
            return job.getStatus();
        }

        return this.statuses.getJobStatus(LEGACY_MIGRATOR_ID);
    }

    /**
     * @param since the date after which to copy the events, null for all time
     * @return the status of the started process
     * @throws JobException when failing to start the job
     * @since 12.6
     */
    public JobStatus startLegacyMigration(Date since) throws JobException
    {
        return this.jobs
            .execute(LegacyEventMigrationJob.JOBTYPE, new LegacyEventMigrationRequest(since, LEGACY_MIGRATOR_ID))
            .getStatus();
    }

    private EventStream getEventStream()
    {
        if (this.eventStream == null && this.componentManager.hasComponent(EventStream.class)) {
            try {
                this.eventStream = this.componentManager.getInstance(EventStream.class);
            } catch (ComponentLookupException e) {
                this.logger.error("Failed to lookup EventStream legacy component", e);
            }
        }

        return this.eventStream;
    }

    /**
     * @return the total number of events in the legacy store
     * @throws QueryException when failing to query the events
     * @since 12.6.1
     * @since 12.7RC1
     */
    public long getLegacyEventCount() throws QueryException
    {
        return getEventStream() != null ? this.eventStream.countEvents() : 0;
    }
}
