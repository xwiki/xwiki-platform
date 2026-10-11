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
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerKey;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.ApplicationStoppedEvent;
import org.xwiki.script.service.ScriptServiceManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.plugin.scheduler.internal.SchedulerClusterSingletonListener;
import com.xpn.xwiki.plugin.scheduler.internal.SchedulerJobsInitializedEvent;
import com.xpn.xwiki.plugin.scheduler.internal.SchedulersClassLoaderManager;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xpn.xwiki.web.XWikiServletRequestStub;
import com.xpn.xwiki.web.XWikiURLFactoryService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    private SchedulersClassLoaderManager schedulersClassLoaderManager;

    private final DocumentReference jobDocumentReference = new DocumentReference("xwiki", "Scheduler", "Job");

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

    private void enable() throws Exception
    {
        this.schedulersClassLoaderManager = mock(SchedulersClassLoaderManager.class);
        doReturn(GroovyJob.class).when(this.schedulersClassLoaderManager).loadClassAndRegister(any(), any());
        ReflectionUtils.setFieldValue(this.plugin, "schedulersClassLoaderManager", this.schedulersClassLoaderManager);
        ReflectionUtils.setFieldValue(this.plugin, "enabled", true);
    }

    private BaseObject saveJob(String status) throws XWikiException
    {
        return saveJob(status, "en");
    }

    private BaseObject saveJob(String status, String contextLang) throws XWikiException
    {
        XWikiContext xcontext = this.oldcore.getXWikiContext();
        XWikiDocument document = new XWikiDocument(this.jobDocumentReference);
        BaseObject job = document.newXObject(SchedulerPlugin.XWIKI_JOB_CLASSREFERENCE, xcontext);
        job.setStringValue("jobName", "Job");
        job.setStringValue("jobClass", GroovyJob.class.getName());
        job.setStringValue("cron", "0 0 * * * ?");
        job.setStringValue("status", status);
        job.setStringValue("contextUser", "XWiki.Admin");
        job.setStringValue("contextLang", contextLang);
        job.setStringValue("contextDatabase", "xwiki");
        this.oldcore.getSpyXWiki().saveDocument(document, xcontext);

        return getJob();
    }

    private BaseObject getJob() throws XWikiException
    {
        XWikiContext xcontext = this.oldcore.getXWikiContext();

        return this.oldcore.getSpyXWiki().getDocument(this.jobDocumentReference, xcontext).clone()
            .getXObject(SchedulerPlugin.XWIKI_JOB_CLASSREFERENCE);
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

    private void mockJobContextPreparation() throws Exception
    {
        this.oldcore.getMocker().registerMockComponent(ScriptServiceManager.class);
        this.oldcore.getXWikiContext().setRequest(new XWikiServletRequestStub());
        doReturn(mock(XWikiURLFactoryService.class)).when(this.oldcore.getSpyXWiki()).getURLFactoryService();
    }

    @Test
    void scheduleJobWithStoredStatus() throws Exception
    {
        enable();
        mockJobContextPreparation();
        BaseObject job = saveJob("Normal");
        // The scheduler doesn't know the job yet, which is the case of all the jobs at startup
        when(this.scheduler.getTriggerState(any(TriggerKey.class))).thenReturn(TriggerState.NONE);

        this.plugin.scheduleJob(job, this.oldcore.getXWikiContext());

        verify(this.scheduler).scheduleJob(any(Trigger.class));
        assertEquals("1.1", getJob().getOwnerDocument().getVersion());
        assertEquals("XWiki Job Status: [Normal]", this.logCapture.getMessage(0));
    }

    @Test
    void scheduleJobFillsTheJobContext() throws Exception
    {
        enable();
        mockJobContextPreparation();
        this.oldcore.getXWikiContext().setLocale(Locale.FRENCH);
        BaseObject job = saveJob("Normal", "");
        when(this.scheduler.getTriggerState(any(TriggerKey.class))).thenReturn(TriggerState.NONE);

        this.plugin.scheduleJob(job, this.oldcore.getXWikiContext());

        // Only the job context is saved, as a minor edit
        BaseObject savedJob = getJob();
        assertEquals("fr", savedJob.getStringValue("contextLang"));
        assertEquals("1.2", savedJob.getOwnerDocument().getVersion());
        assertTrue(savedJob.getOwnerDocument().isMinorEdit());
        assertEquals("XWiki Job Status: [Normal]", this.logCapture.getMessage(0));
    }

    @Test
    void pauseJobSavesTheChangedStatus() throws Exception
    {
        enable();
        BaseObject job = saveJob("Normal");

        this.plugin.pauseJob(job, this.oldcore.getXWikiContext());

        verify(this.scheduler).pauseJob(new JobKey("xwiki:Scheduler.Job_0"));
        BaseObject savedJob = getJob();
        assertEquals("Paused", savedJob.getStringValue("status"));
        // Saved as a minor edit
        assertEquals("1.2", savedJob.getOwnerDocument().getVersion());
        assertTrue(savedJob.getOwnerDocument().isMinorEdit());
    }

    @Test
    void pauseJobDoesNotSaveTheSameStatus() throws Exception
    {
        enable();
        BaseObject job = saveJob("Paused");

        this.plugin.pauseJob(job, this.oldcore.getXWikiContext());

        verify(this.scheduler).pauseJob(new JobKey("xwiki:Scheduler.Job_0"));
        assertEquals("1.1", getJob().getOwnerDocument().getVersion());
    }

    @Test
    void unscheduleJob() throws Exception
    {
        enable();
        BaseObject job = saveJob("Normal");

        this.plugin.unscheduleJob(job, this.oldcore.getXWikiContext());

        verify(this.scheduler).deleteJob(new JobKey("xwiki:Scheduler.Job_0"));
        verify(this.schedulersClassLoaderManager).removeScheduler(job.getReference());
        assertEquals("None", getJob().getStringValue("status"));
    }

    private void stopSchedulerMock() throws SchedulerException
    {
        // A scheduler which was never started or was stopped is in standby mode
        when(this.scheduler.isInStandbyMode()).thenReturn(true);
    }

    private ObservationManager getObservationManager() throws Exception
    {
        if (this.oldcore.getMocker().hasComponent(ObservationManager.class)) {
            return this.oldcore.getMocker().getInstance(ObservationManager.class);
        }

        return this.oldcore.getMocker().registerMockComponent(ObservationManager.class);
    }

    private void notifyJobUpdated(BaseObject previousJob) throws Exception
    {
        XWikiDocument document =
            this.oldcore.getSpyXWiki().getDocument(this.jobDocumentReference, this.oldcore.getXWikiContext()).clone();
        document.setOriginalDocument(previousJob.getOwnerDocument());
        this.plugin.onEvent(new DocumentUpdatedEvent(this.jobDocumentReference), document,
            this.oldcore.getXWikiContext());
    }

    @Test
    void initWithClusterSingleton() throws Exception
    {
        ObservationManager observation = getObservationManager();
        this.oldcore.getMocker().registerMockComponent(SchedulersClassLoaderManager.class);
        SchedulerClusterSingletonListener singletonListener =
            this.oldcore.getMocker().registerMockComponent(SchedulerClusterSingletonListener.class);
        when(singletonListener.isSingletonNodeRequired()).thenReturn(true);
        this.plugin.setScheduler(null);

        this.plugin.init(this.oldcore.getXWikiContext());

        assertTrue(this.plugin.isEnabled());
        verify(singletonListener).setSchedulerPlugin(this.plugin);
        verify(observation).addListener(singletonListener);
        verify(observation).addListener(this.plugin);
        // The listener starts the scheduler on the cluster singleton node
        assertNull(this.plugin.getScheduler());
    }

    @Test
    void initWithoutClusterSingleton() throws Exception
    {
        ObservationManager observation = getObservationManager();
        this.oldcore.getMocker().registerMockComponent(SchedulersClassLoaderManager.class);
        this.oldcore.getMocker().registerMockComponent(ExecutionContextManager.class);
        SchedulerClusterSingletonListener singletonListener =
            this.oldcore.getMocker().registerMockComponent(SchedulerClusterSingletonListener.class);
        doReturn(List.of()).when(this.oldcore.getSpyXWiki()).getVirtualWikisDatabaseNames(any());

        this.plugin.init(this.oldcore.getXWikiContext());

        // Started right away, whatever the cluster singleton node (the default, and today's behaviour)
        verify(observation, timeout(10000)).notify(any(SchedulerJobsInitializedEvent.class), isNull());
        verify(this.scheduler).start();
        verify(singletonListener, never()).setSchedulerPlugin(any());
        verify(observation, never()).addListener(singletonListener);
        verify(observation).addListener(this.plugin);
    }

    @Test
    void initWhenDisabled() throws Exception
    {
        ObservationManager observation = getObservationManager();
        this.oldcore.getMocker().registerMockComponent(SchedulersClassLoaderManager.class);
        SchedulerClusterSingletonListener singletonListener =
            this.oldcore.getMocker().registerMockComponent(SchedulerClusterSingletonListener.class);
        this.oldcore.getConfigurationSource().setProperty("scheduler.enabled", false);

        this.plugin.init(this.oldcore.getXWikiContext());

        assertFalse(this.plugin.isEnabled());
        verify(singletonListener, never()).setSchedulerPlugin(any());
        verifyNoInteractions(observation);
    }

    @Test
    void initWhenDisabledWithClusterSingleton() throws Exception
    {
        ObservationManager observation = getObservationManager();
        this.oldcore.getMocker().registerMockComponent(SchedulersClassLoaderManager.class);
        SchedulerClusterSingletonListener singletonListener =
            this.oldcore.getMocker().registerMockComponent(SchedulerClusterSingletonListener.class);
        when(singletonListener.isSingletonNodeRequired()).thenReturn(true);
        this.oldcore.getConfigurationSource().setProperty("scheduler.enabled", false);

        this.plugin.init(this.oldcore.getXWikiContext());

        // Registered to warn if this instance becomes the cluster singleton node
        verify(singletonListener).setSchedulerPlugin(this.plugin);
        verify(observation).addListener(singletonListener);
        verify(observation, never()).addListener(this.plugin);
    }

    @Test
    void startSchedulerRestoresTheJobs() throws Exception
    {
        enable();
        mockJobContextPreparation();
        ObservationManager observation = getObservationManager();
        this.oldcore.getMocker().registerMockComponent(ExecutionContextManager.class);
        saveJob("Normal");
        doReturn(List.of("xwiki")).when(this.oldcore.getSpyXWiki()).getVirtualWikisDatabaseNames(any());
        when(this.oldcore.getMockStore().searchDocumentReferences(anyString(), any(XWikiContext.class)))
            .thenReturn(List.of(this.jobDocumentReference));
        when(this.scheduler.getTriggerState(any(TriggerKey.class))).thenReturn(TriggerState.NONE);

        this.plugin.startScheduler();

        verify(observation, timeout(10000)).notify(any(SchedulerJobsInitializedEvent.class), isNull());
        verify(this.scheduler).start();
        verify(this.scheduler).scheduleJob(any(Trigger.class));
        assertEquals("XWiki Job Status: [Normal]", this.logCapture.getMessage(0));
        // Nothing to save
        assertEquals("1.1", getJob().getOwnerDocument().getVersion());
    }

    @Test
    void startSchedulerWhenStopping() throws Exception
    {
        enable();
        when(this.scheduler.getCurrentlyExecutingJobs()).thenReturn(List.of());
        this.plugin.onEvent(new ApplicationStoppedEvent(), null, null);

        this.plugin.startScheduler();
        this.plugin.stopScheduler();

        verify(this.scheduler, never()).start();
        verify(this.scheduler, never()).clear();
    }

    @Test
    void stopScheduler() throws Exception
    {
        enable();

        this.plugin.stopScheduler();

        var inOrder = inOrder(this.scheduler);
        inOrder.verify(this.scheduler).standby();
        inOrder.verify(this.scheduler).clear();
    }

    @Test
    void stopSchedulerWhenNeverStarted()
    {
        this.plugin.setScheduler(null);

        // Nothing to stop
        this.plugin.stopScheduler();

        assertNull(this.plugin.getScheduler());
    }

    @Test
    void stopSchedulerFailure() throws Exception
    {
        enable();
        doThrow(new SchedulerException("error")).when(this.scheduler).clear();

        this.plugin.stopScheduler();

        assertEquals("Failed to stop the scheduler: [SchedulerException: error]", this.logCapture.getMessage(0));
    }

    @Test
    void readMethodsWhenNotRunning() throws Exception
    {
        enable();
        stopSchedulerMock();
        XWikiContext xcontext = this.oldcore.getXWikiContext();

        BaseObject job = saveJob("Normal");
        assertEquals(JobState.STATE_NORMAL, this.plugin.getJobStatus(job, xcontext).getValue());
        assertNotNull(this.plugin.getNextFireTime(job, xcontext));
        assertNull(this.plugin.getPreviousFireTime(job, xcontext));

        job.setStringValue("status", "Paused");
        assertEquals(JobState.STATE_PAUSED, this.plugin.getJobStatus(job, xcontext).getValue());
        assertNull(this.plugin.getNextFireTime(job, xcontext));

        job.setStringValue("status", "None");
        assertEquals(JobState.STATE_NONE, this.plugin.getJobStatus(job, xcontext).getValue());

        job.setStringValue("status", "Normal");
        job.setStringValue("cron", "invalid");
        assertNull(this.plugin.getNextFireTime(job, xcontext));

        verify(this.scheduler, never()).getTriggerState(any());
        verify(this.scheduler, never()).getTrigger(any());
    }

    @Test
    void readMethodsWhenDisabled() throws Exception
    {
        this.plugin.setScheduler(null);
        XWikiContext xcontext = this.oldcore.getXWikiContext();
        BaseObject job = saveJob("Normal");

        assertNull(this.plugin.getJobStatus(job, xcontext));
        assertNull(this.plugin.getNextFireTime(job, xcontext));
        assertNull(this.plugin.getPreviousFireTime(job, xcontext));
    }

    @Test
    void actionsWhenNotRunning() throws Exception
    {
        enable();
        stopSchedulerMock();
        XWikiContext xcontext = this.oldcore.getXWikiContext();
        BaseObject job = saveJob("None", "");

        // Only the job page is saved: the instance running the scheduler applies it
        this.plugin.scheduleJob(job, xcontext);
        BaseObject savedJob = getJob();
        assertEquals("Normal", savedJob.getStringValue("status"));
        assertEquals("en", savedJob.getStringValue("contextLang"));
        assertEquals("1.2", savedJob.getOwnerDocument().getVersion());

        this.plugin.pauseJob(job, xcontext);
        assertEquals("Paused", getJob().getStringValue("status"));

        this.plugin.resumeJob(job, xcontext);
        assertEquals("Normal", getJob().getStringValue("status"));

        this.plugin.unscheduleJob(job, xcontext);
        assertEquals("None", getJob().getStringValue("status"));

        // A paused job stays paused when it's scheduled again
        job = saveJob("Paused");
        this.plugin.scheduleJob(job, xcontext);
        assertEquals("Paused", getJob().getStringValue("status"));

        SchedulerPluginException exception =
            assertThrows(SchedulerPluginException.class, () -> this.plugin.triggerJob(getJob(), xcontext));
        assertTrue(exception.getMessage().contains("The scheduler is not running on this instance"));

        // Nothing to reload
        this.plugin.reloadJob(job, xcontext);

        verify(this.scheduler, never()).addJob(any(), anyBoolean());
        verify(this.scheduler, never()).pauseJob(any(JobKey.class));
        verify(this.scheduler, never()).resumeJob(any(JobKey.class));
        verify(this.scheduler, never()).deleteJob(any());
        verify(this.scheduler, never()).triggerJob(any(JobKey.class));
    }

    @Test
    void documentEventWhenNotRunning() throws Exception
    {
        enable();
        stopSchedulerMock();
        BaseObject previousJob = saveJob("Normal");
        BaseObject job = getJob();
        job.setStringValue("status", "Paused");
        this.oldcore.getSpyXWiki().saveDocument(job.getOwnerDocument(), this.oldcore.getXWikiContext());

        notifyJobUpdated(previousJob);

        verify(this.scheduler, never()).pauseJob(any(JobKey.class));
    }

    @Test
    void documentEventAlreadyApplied() throws Exception
    {
        enable();
        XWikiContext xcontext = this.oldcore.getXWikiContext();
        // Paused on this instance: the scheduler is updated before the page is saved
        BaseObject previousJob = saveJob("Normal");
        when(this.scheduler.getTriggerState(any(TriggerKey.class))).thenReturn(TriggerState.PAUSED);
        BaseObject job = getJob();
        job.setStringValue("status", "Paused");
        this.oldcore.getSpyXWiki().saveDocument(job.getOwnerDocument(), xcontext);

        notifyJobUpdated(previousJob);

        verify(this.scheduler, never()).deleteJob(any());
        verify(this.scheduler, never()).addJob(any(), anyBoolean());
    }

    @Test
    void documentEventWithInvalidCron() throws Exception
    {
        enable();
        mockJobContextPreparation();
        XWikiContext xcontext = this.oldcore.getXWikiContext();
        BaseObject previousJob = saveJob("Normal");
        when(this.scheduler.getTriggerState(any(TriggerKey.class))).thenReturn(TriggerState.NORMAL);
        BaseObject job = getJob();
        job.setStringValue("cron", "invalid");
        this.oldcore.getSpyXWiki().saveDocument(job.getOwnerDocument(), xcontext);

        notifyJobUpdated(previousJob);

        assertTrue(this.logCapture.getMessage(0).startsWith("Failed to update job in document [xwiki:Scheduler.Job]"));
    }

    @Test
    void pauseTheSecondJobOfAPage() throws Exception
    {
        enable();
        XWikiContext xcontext = this.oldcore.getXWikiContext();
        saveJob("Normal");
        XWikiDocument document = this.oldcore.getSpyXWiki().getDocument(this.jobDocumentReference, xcontext).clone();
        BaseObject secondJob = document.newXObject(SchedulerPlugin.XWIKI_JOB_CLASSREFERENCE, xcontext);
        secondJob.setStringValue("status", "Normal");
        this.oldcore.getSpyXWiki().saveDocument(document, xcontext);
        secondJob = this.oldcore.getSpyXWiki().getDocument(this.jobDocumentReference, xcontext).clone()
            .getXObject(SchedulerPlugin.XWIKI_JOB_CLASSREFERENCE, 1);

        this.plugin.pauseJob(secondJob, xcontext);

        verify(this.scheduler).pauseJob(new JobKey("xwiki:Scheduler.Job_1"));
        XWikiDocument savedDocument = this.oldcore.getSpyXWiki().getDocument(this.jobDocumentReference, xcontext);
        assertEquals("Normal",
            savedDocument.getXObject(SchedulerPlugin.XWIKI_JOB_CLASSREFERENCE, 0).getStringValue("status"));
        assertEquals("Paused",
            savedDocument.getXObject(SchedulerPlugin.XWIKI_JOB_CLASSREFERENCE, 1).getStringValue("status"));
    }
}
