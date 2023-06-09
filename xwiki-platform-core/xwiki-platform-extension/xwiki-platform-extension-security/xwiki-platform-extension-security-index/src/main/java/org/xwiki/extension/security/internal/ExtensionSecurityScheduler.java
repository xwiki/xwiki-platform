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
package org.xwiki.extension.security.internal;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Disposable;
import org.xwiki.job.Job;
import org.xwiki.job.JobExecutor;

/**
 * Periodically check for new security vulnerabilities associated with components that are part of the current wiki
 * instance.
 *
 * @version $Id$
 * @since 15.5RC1
 */
@Component(roles = ExtensionSecurityScheduler.class)
@Singleton
public class ExtensionSecurityScheduler implements Runnable, Disposable
{
    @Inject
    private JobExecutor jobExecutor;

    @Inject
    private Logger logger;

    private ScheduledExecutorService executor;

    /**
     * Initialize and start the scheduler.
     */
    public void start()
    {

        // Start the scheduling
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setName("Extension Security scanner");
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            thread.setDaemon(true);

            return thread;
        });

        // TODO: make configurable!
        this.executor.scheduleWithFixedDelay(this, 0, 24, TimeUnit.HOURS);
    }

    @Override
    public void run()
    {
        try {
            // Execute job
            Job job = this.jobExecutor.execute(ExtensionSecurityJob.JOBTYPE, new ExtensionSecurityRequest());

            // Wait for the job to finish
            job.join();
        } catch (InterruptedException e) {
            // Restore interrupted state...
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            this.logger.error("Failed to execute job", e);
        }
    }

    @Override
    public void dispose()
    {
        this.executor.shutdownNow();
    }
}
