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

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.xwiki.observation.event.ApplicationStoppedEvent;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;

import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SchedulerPlugin}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
class SchedulerPluginTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    private SchedulerPlugin plugin;

    private Scheduler scheduler;

    @BeforeEach
    void beforeEach()
    {
        this.plugin =
            new SchedulerPlugin("scheduler", SchedulerPlugin.class.getName(), this.oldcore.getXWikiContext());
        this.scheduler = mock(Scheduler.class);
        this.plugin.setScheduler(this.scheduler);
    }

    @AfterEach
    void afterEach()
    {
        // Make sure an interruption simulated by a test doesn't leak
        Thread.interrupted();
    }

    private JobExecutionContext mockRunningJob(String name)
    {
        JobExecutionContext jobContext = mock(JobExecutionContext.class);
        JobDetail jobDetail = mock(JobDetail.class);
        when(jobContext.getJobDetail()).thenReturn(jobDetail);
        when(jobDetail.getKey()).thenReturn(new JobKey(name));

        return jobContext;
    }

    @Test
    void stopWithoutRunningJobs() throws SchedulerException
    {
        when(this.scheduler.getCurrentlyExecutingJobs()).thenReturn(List.of());

        this.plugin.onEvent(new ApplicationStoppedEvent(), null, null);

        var inOrder = inOrder(this.scheduler);
        inOrder.verify(this.scheduler).standby();
        inOrder.verify(this.scheduler).shutdown(false);
        verify(this.scheduler, never()).shutdown(true);
    }

    @Test
    void stopWaitsForRunningJobs() throws SchedulerException
    {
        JobExecutionContext runningJob = mockRunningJob("xwiki:Scheduler.Job_0");
        when(this.scheduler.getCurrentlyExecutingJobs()).thenReturn(List.of(runningJob), List.of(runningJob),
            List.of());

        this.plugin.onEvent(new ApplicationStoppedEvent(), null, null);

        var inOrder = inOrder(this.scheduler);
        inOrder.verify(this.scheduler).standby();
        inOrder.verify(this.scheduler).shutdown(false);
        verify(this.scheduler, never()).shutdown(true);
        assertEquals("Waiting up to [30] seconds for the following scheduler jobs to finish: [xwiki:Scheduler.Job_0]",
            this.logCapture.getMessage(0));
    }

    @Test
    void stopWhenRunningJobsDontFinish() throws SchedulerException
    {
        JobExecutionContext runningJob = mockRunningJob("xwiki:Scheduler.Job_0");
        when(this.scheduler.getCurrentlyExecutingJobs()).thenReturn(List.of(runningJob));
        // Don't wait for 30 seconds
        this.plugin.shutdownTimeout = 0;

        this.plugin.onEvent(new ApplicationStoppedEvent(), null, null);

        verify(this.scheduler).standby();
        verify(this.scheduler).shutdown(false);
        verify(this.scheduler, never()).shutdown(true);
        assertEquals("Waiting up to [0] seconds for the following scheduler jobs to finish: [xwiki:Scheduler.Job_0]",
            this.logCapture.getMessage(0));
        assertEquals("Stopping the scheduler while the following jobs are still running after waiting for [0]"
            + " seconds: [xwiki:Scheduler.Job_0]", this.logCapture.getMessage(1));
    }

    @Test
    void stopWhenTheWaitIsInterrupted() throws SchedulerException
    {
        JobExecutionContext runningJob = mockRunningJob("xwiki:Scheduler.Job_0");
        when(this.scheduler.getCurrentlyExecutingJobs()).thenReturn(List.of(runningJob));

        Thread.currentThread().interrupt();
        this.plugin.onEvent(new ApplicationStoppedEvent(), null, null);

        assertTrue(Thread.interrupted());
        verify(this.scheduler).shutdown(false);
        assertEquals("Waiting up to [30] seconds for the following scheduler jobs to finish: [xwiki:Scheduler.Job_0]",
            this.logCapture.getMessage(0));
        assertEquals("Stopping the scheduler while the following jobs are still running after waiting for [30]"
            + " seconds: [xwiki:Scheduler.Job_0]", this.logCapture.getMessage(1));
    }

    @Test
    void stopWhenShutdownFails() throws SchedulerException
    {
        when(this.scheduler.getCurrentlyExecutingJobs()).thenReturn(List.of());
        doThrow(new SchedulerException("error")).when(this.scheduler).shutdown(false);

        this.plugin.onEvent(new ApplicationStoppedEvent(), null, null);

        assertEquals("Failed to stop the scheduler: [SchedulerException: error]", this.logCapture.getMessage(0));
    }

    @Test
    void stopWhenSchedulerNotStarted()
    {
        this.plugin.setScheduler(null);

        // Nothing to stop
        this.plugin.onEvent(new ApplicationStoppedEvent(), null, null);

        assertTrue(this.plugin.getEvents().stream().anyMatch(event -> event.matches(new ApplicationStoppedEvent())));
    }
}
