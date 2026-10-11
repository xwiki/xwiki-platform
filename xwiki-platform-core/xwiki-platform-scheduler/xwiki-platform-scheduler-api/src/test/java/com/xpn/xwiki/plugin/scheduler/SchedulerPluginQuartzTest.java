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
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.simpl.RAMJobStore;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.TriggerFiredResult;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.ApplicationStoppedEvent;
import org.xwiki.script.service.ScriptServiceManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.plugin.scheduler.internal.SchedulerJobsInitializedEvent;
import com.xpn.xwiki.plugin.scheduler.internal.SchedulersClassLoaderManager;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xpn.xwiki.web.XWikiServletRequestStub;
import com.xpn.xwiki.web.XWikiURLFactoryService;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Runs {@link SchedulerPlugin} with a real Quartz scheduler (RAM job store).
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
class SchedulerPluginQuartzTest
{
    private static final long WAIT = 10000;

    /**
     * A schedule which doesn't fire during the test.
     */
    private static final String NEVER = "0 0 0 1 1 ? 2099";

    private static final String SCRIPT = "script";

    /**
     * Longer than the period of the job (one second): a job still registered would fire during that time.
     */
    private static final long QUIET_PERIOD = 1200;

    /**
     * One permit per execution of {@link CountingJob}.
     */
    private static final Semaphore EXECUTED = new Semaphore(0);

    /**
     * One permit per job fired by the scheduler, see {@link FiringCountingJobStore}.
     */
    private static final Semaphore FIRED = new Semaphore(0);

    /**
     * Counts its executions.
     *
     * @version $Id$
     */
    public static class CountingJob implements Job
    {
        @Override
        public void execute(JobExecutionContext context)
        {
            EXECUTED.release();
        }
    }

    /**
     * Counts the jobs fired by the scheduler. A job is fired under the lock of the job store, which
     * {@link Scheduler#clear()} also takes: once {@link SchedulerPlugin#stopScheduler()} returns, no job can be fired
     * anymore. Counting the executions instead is not reliable: a job fired just before the stop is executed by
     * another thread, which may start it after the stop returned (running jobs are not interrupted).
     *
     * @version $Id$
     */
    public static class FiringCountingJobStore extends RAMJobStore
    {
        @Override
        public List<TriggerFiredResult> triggersFired(List<OperableTrigger> firedTriggers)
        {
            synchronized (this.lock) {
                List<TriggerFiredResult> results = super.triggersFired(firedTriggers);
                for (TriggerFiredResult result : results) {
                    if (result.getTriggerFiredBundle() != null) {
                        FIRED.release();
                    }
                }

                return results;
            }
        }
    }

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private final DocumentReference jobDocumentReference = new DocumentReference("xwiki", "Scheduler", "Job");

    private final JobKey jobKey = new JobKey("xwiki:Scheduler.Job_0");

    private SchedulerPlugin plugin;

    private Scheduler scheduler;

    private ObservationManager observation;

    @BeforeEach
    void beforeEach() throws Exception
    {
        EXECUTED.drainPermits();
        FIRED.drainPermits();

        Properties properties = new Properties();
        properties.setProperty(StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME, "SchedulerPluginQuartzTest");
        properties.setProperty("org.quartz.threadPool.threadCount", "2");
        properties.setProperty(StdSchedulerFactory.PROP_JOB_STORE_CLASS, FiringCountingJobStore.class.getName());
        this.scheduler = new StdSchedulerFactory(properties).getScheduler();

        this.plugin =
            spy(new SchedulerPlugin("scheduler", SchedulerPlugin.class.getName(), this.oldcore.getXWikiContext()));
        this.plugin.setScheduler(this.scheduler);
        SchedulersClassLoaderManager classLoaderManager = mock(SchedulersClassLoaderManager.class);
        doReturn(CountingJob.class).when(classLoaderManager).loadClassAndRegister(any(), any());
        ReflectionUtils.setFieldValue(this.plugin, "schedulersClassLoaderManager", classLoaderManager);
        ReflectionUtils.setFieldValue(this.plugin, "enabled", true);

        this.observation = this.oldcore.getMocker().registerMockComponent(ObservationManager.class);
        this.oldcore.getMocker().registerMockComponent(ExecutionContextManager.class);
        this.oldcore.getMocker().registerMockComponent(ScriptServiceManager.class);
        this.oldcore.getXWikiContext().setRequest(new XWikiServletRequestStub());
        doReturn(mock(XWikiURLFactoryService.class)).when(this.oldcore.getSpyXWiki()).getURLFactoryService();
        doReturn(List.of("xwiki")).when(this.oldcore.getSpyXWiki()).getVirtualWikisDatabaseNames(any());
        when(this.oldcore.getMockStore().searchDocumentReferences(anyString(), any(XWikiContext.class)))
            .thenReturn(List.of(this.jobDocumentReference));
    }

    @AfterEach
    void afterEach() throws Exception
    {
        // Wait for the jobs fired just before (they don't block), so that they don't count in the next test
        this.scheduler.shutdown(true);
    }

    private BaseObject saveJob(String status, String cron, UserReference author) throws Exception
    {
        XWikiContext xcontext = this.oldcore.getXWikiContext();
        XWikiDocument document = this.oldcore.getSpyXWiki().getDocument(this.jobDocumentReference, xcontext).clone();
        BaseObject job = document.getXObject(SchedulerPlugin.XWIKI_JOB_CLASSREFERENCE);
        if (job == null) {
            job = document.newXObject(SchedulerPlugin.XWIKI_JOB_CLASSREFERENCE, xcontext);
            job.setStringValue("jobName", "Job");
            job.setStringValue("jobClass", CountingJob.class.getName());
            job.setStringValue("contextUser", "XWiki.Admin");
            job.setStringValue("contextLang", "en");
            job.setStringValue("contextDatabase", "xwiki");
        }
        job.setStringValue("status", status);
        job.setStringValue("cron", cron);
        document.getAuthors().setEffectiveMetadataAuthor(author);
        this.oldcore.getSpyXWiki().saveDocument(document, xcontext);

        return job;
    }

    private XWikiDocument getJobDocument() throws Exception
    {
        return this.oldcore.getSpyXWiki().getDocument(this.jobDocumentReference, this.oldcore.getXWikiContext());
    }

    /**
     * Save the job page and send the event the plugin receives (locally or from another cluster member).
     */
    private void editJob(String status, String cron, UserReference author) throws Exception
    {
        XWikiDocument previous = getJobDocument().clone();
        saveJob(status, cron, author);
        XWikiDocument document = getJobDocument().clone();
        document.setOriginalDocument(previous);
        this.plugin.onEvent(new DocumentUpdatedEvent(this.jobDocumentReference), document,
            this.oldcore.getXWikiContext());
    }

    /**
     * Modify the job page, save it and send the event the plugin receives (locally or from another cluster member).
     *
     * @param modification the modification of the job xobject
     * @return the version of the saved page
     */
    private String editJob(Consumer<BaseObject> modification) throws Exception
    {
        XWikiContext xcontext = this.oldcore.getXWikiContext();
        XWikiDocument previous = getJobDocument().clone();
        XWikiDocument document = previous.clone();
        modification.accept(document.getXObject(SchedulerPlugin.XWIKI_JOB_CLASSREFERENCE));
        this.oldcore.getSpyXWiki().saveDocument(document, xcontext);
        document = getJobDocument().clone();
        document.setOriginalDocument(previous);
        this.plugin.onEvent(new DocumentUpdatedEvent(this.jobDocumentReference), document, xcontext);

        return document.getVersion();
    }

    /**
     * Start the scheduler and wait until the jobs are restored from their pages.
     */
    private void start()
    {
        this.plugin.startScheduler();
        verify(this.observation, timeout(WAIT)).notify(any(SchedulerJobsInitializedEvent.class), isNull());
        clearInvocations(this.plugin);
    }

    private TriggerState getTriggerState() throws Exception
    {
        return this.scheduler.getTriggerState(new TriggerKey(this.jobKey.getName()));
    }

    /**
     * Stop the scheduler and check that it doesn't fire any job anymore.
     */
    private void stopAndCheckNothingFires() throws Exception
    {
        this.plugin.stopScheduler();
        // The jobs fired before the stop returned don't count
        FIRED.drainPermits();

        assertTrue(this.scheduler.isInStandbyMode());
        assertTrue(this.scheduler.getJobKeys(GroupMatcher.anyJobGroup()).isEmpty());
        Thread.sleep(QUIET_PERIOD);
        assertEquals(0, FIRED.availablePermits());
    }

    private BaseObject getScheduledJobObject() throws Exception
    {
        JobDetail jobDetail = this.scheduler.getJobDetail(this.jobKey);

        return (BaseObject) jobDetail.getJobDataMap().get("xjob");
    }

    @Test
    void startStopAndEdit() throws Exception
    {
        UserReference author = mock(UserReference.class, "author");
        UserReference editor = mock(UserReference.class, "editor");
        saveJob("Normal", "* * * * * ?", author);
        XWikiContext xcontext = this.oldcore.getXWikiContext();

        // Start: the job is restored from its page and fires
        this.plugin.startScheduler();
        verify(this.observation, timeout(WAIT)).notify(any(SchedulerJobsInitializedEvent.class), isNull());
        assertTrue(EXECUTED.tryAcquire(WAIT, MILLISECONDS), "The job was not executed");

        // Stop (the instance is no longer the cluster singleton node): no job anymore, nothing fires
        stopAndCheckNothingFires();
        // The page answers
        assertEquals(JobState.STATE_NORMAL, this.plugin.getJobStatus(getJobDocument().clone()
            .getXObject(SchedulerPlugin.XWIKI_JOB_CLASSREFERENCE), xcontext).getValue());

        // Start again: restored and fired again, without saving the page
        this.plugin.startScheduler();
        verify(this.observation, timeout(WAIT).times(2)).notify(any(SchedulerJobsInitializedEvent.class), isNull());
        assertTrue(FIRED.tryAcquire(WAIT, MILLISECONDS), "The job was not fired after the new start");
        assertEquals("1.1", getJobDocument().getVersion());
        BaseObject scheduledJob = getScheduledJobObject();
        assertSame(author, scheduledJob.getOwnerDocument().getAuthors().getEffectiveMetadataAuthor());

        // Paused by editing the page: the job is rebuilt from the page, with its new author
        editJob("Paused", "* * * * * ?", editor);
        assertEquals(TriggerState.PAUSED, this.scheduler.getTriggerState(new TriggerKey(this.jobKey.getName())));
        assertSame(editor, getScheduledJobObject().getOwnerDocument().getAuthors().getEffectiveMetadataAuthor());

        // Resumed by editing the page
        editJob("Normal", "* * * * * ?", author);
        assertEquals(TriggerState.NORMAL, this.scheduler.getTriggerState(new TriggerKey(this.jobKey.getName())));
        assertSame(author, getScheduledJobObject().getOwnerDocument().getAuthors().getEffectiveMetadataAuthor());

        // New schedule
        editJob("Normal", "0 0 0 1 1 ? 2099", editor);
        assertEquals(2099, 1900 + this.scheduler.getTrigger(new TriggerKey(this.jobKey.getName())).getNextFireTime()
            .getYear());
        assertSame(editor, getScheduledJobObject().getOwnerDocument().getAuthors().getEffectiveMetadataAuthor());

        // Unscheduled
        editJob("None", "0 0 0 1 1 ? 2099", author);
        assertEquals(TriggerState.NONE, this.scheduler.getTriggerState(new TriggerKey(this.jobKey.getName())));

        // XWiki stops: the scheduler is shut down and can't be started anymore
        this.plugin.onEvent(new ApplicationStoppedEvent(), null, null);
        assertTrue(this.scheduler.isShutdown());
        this.plugin.startScheduler();
        Thread.sleep(500);
        verify(this.observation, times(2)).notify(any(SchedulerJobsInitializedEvent.class), isNull());
    }

    @Test
    void stopBeforeTheSchedulerIsStarted() throws Exception
    {
        saveJob("Normal", "* * * * * ?", mock(UserReference.class));

        // Hold the start before it starts Quartz
        CountDownLatch startInProgress = new CountDownLatch(1);
        CountDownLatch stopDone = new CountDownLatch(1);
        doAnswer(invocation -> {
            startInProgress.countDown();
            stopDone.await(WAIT, MILLISECONDS);
            return List.of("xwiki");
        }).when(this.oldcore.getSpyXWiki()).getVirtualWikisDatabaseNames(any());

        this.plugin.startScheduler();
        assertTrue(startInProgress.await(WAIT, MILLISECONDS));
        this.plugin.stopScheduler();
        stopDone.countDown();

        // The start gives up: Quartz is not started and no job is restored
        verify(this.observation, timeout(WAIT)).notify(any(SchedulerJobsInitializedEvent.class), isNull());
        assertFalse(this.scheduler.isStarted());
        assertTrue(this.scheduler.getJobKeys(GroupMatcher.anyJobGroup()).isEmpty());
        verify(this.oldcore.getMockStore(), never()).searchDocumentReferences(anyString(), any(XWikiContext.class));
    }

    @Test
    void stopWhileTheJobsAreRestored() throws Exception
    {
        saveJob("Normal", "* * * * * ?", mock(UserReference.class));

        // Hold the start once Quartz is started, before the jobs are restored
        CountDownLatch startInProgress = new CountDownLatch(1);
        CountDownLatch stopDone = new CountDownLatch(1);
        when(this.oldcore.getMockStore().searchDocumentReferences(anyString(), any(XWikiContext.class)))
            .thenAnswer(invocation -> {
                startInProgress.countDown();
                stopDone.await(WAIT, MILLISECONDS);
                return List.of(this.jobDocumentReference);
            });
        clearInvocations(this.oldcore.getSpyXWiki());

        this.plugin.startScheduler();
        assertTrue(startInProgress.await(WAIT, MILLISECONDS));
        assertTrue(this.scheduler.isStarted());
        assertFalse(this.scheduler.isInStandbyMode());
        this.plugin.stopScheduler();
        stopDone.countDown();

        // The start gives up: the job is not restored after the stop and nothing fires
        verify(this.observation, timeout(WAIT)).notify(any(SchedulerJobsInitializedEvent.class), isNull());
        verify(this.oldcore.getSpyXWiki(), never()).getDocument(eq(this.jobDocumentReference), any(XWikiContext.class));
        assertTrue(this.scheduler.isInStandbyMode());
        assertTrue(this.scheduler.getJobKeys(GroupMatcher.anyJobGroup()).isEmpty());
        Thread.sleep(QUIET_PERIOD);
        assertEquals(0, FIRED.availablePermits());
    }

    @Test
    void editTheScriptOfAScheduledJob() throws Exception
    {
        saveJob("Paused", NEVER, mock(UserReference.class));
        start();
        assertEquals(TriggerState.PAUSED, getTriggerState());

        String version = editJob(job -> job.setLargeStringValue(SCRIPT, "new script"));

        // Reloaded with its new definition, in its current state, without saving the page
        verify(this.plugin).reloadJob(any(BaseObject.class), any(XWikiContext.class));
        verify(this.plugin, never()).scheduleJob(any(BaseObject.class), any(XWikiContext.class));
        assertEquals(TriggerState.PAUSED, getTriggerState());
        assertEquals("new script", getScheduledJobObject().getLargeStringValue(SCRIPT));
        assertEquals(version, getJobDocument().getVersion());
    }

    @Test
    void editTheScriptOfAJobUnknownToTheScheduler() throws Exception
    {
        saveJob("None", NEVER, mock(UserReference.class));
        start();
        assertEquals(TriggerState.NONE, getTriggerState());

        String version = editJob(job -> job.setLargeStringValue(SCRIPT, "new script"));

        // Left alone: it gets registered with its new definition when it's scheduled
        verify(this.plugin, never()).reloadJob(any(BaseObject.class), any(XWikiContext.class));
        verify(this.plugin, never()).scheduleJob(any(BaseObject.class), any(XWikiContext.class));
        assertTrue(this.scheduler.getJobKeys(GroupMatcher.anyJobGroup()).isEmpty());
        assertEquals(version, getJobDocument().getVersion());
    }

    @Test
    void editTheStatusAndTheScriptOfAScheduledJob() throws Exception
    {
        saveJob("Normal", NEVER, mock(UserReference.class));
        start();
        assertEquals(TriggerState.NORMAL, getTriggerState());

        String version = editJob(job -> {
            job.setStringValue("status", "Paused");
            job.setLargeStringValue(SCRIPT, "new script");
        });

        // The stored state is applied: the job is registered again, with its new definition, without saving the page
        verify(this.plugin, never()).reloadJob(any(BaseObject.class), any(XWikiContext.class));
        verify(this.plugin).scheduleJob(any(BaseObject.class), any(XWikiContext.class));
        verify(this.plugin).pauseJob(any(BaseObject.class), any(XWikiContext.class));
        assertEquals(TriggerState.PAUSED, getTriggerState());
        assertEquals("new script", getScheduledJobObject().getLargeStringValue(SCRIPT));
        assertEquals(version, getJobDocument().getVersion());
    }

    @Test
    void editAJobWhenTheSchedulerIsNotRunning() throws Exception
    {
        saveJob("Normal", NEVER, mock(UserReference.class));
        start();
        // This instance is no longer the cluster singleton node
        this.plugin.stopScheduler();
        clearInvocations(this.plugin);

        editJob(job -> job.setLargeStringValue(SCRIPT, "new script"));
        String version = editJob(job -> job.setStringValue("status", "Paused"));

        // The instance running the scheduler takes care of the changes
        verify(this.plugin, never()).reloadJob(any(BaseObject.class), any(XWikiContext.class));
        verify(this.plugin, never()).scheduleJob(any(BaseObject.class), any(XWikiContext.class));
        verify(this.plugin, never()).pauseJob(any(BaseObject.class), any(XWikiContext.class));
        assertTrue(this.scheduler.getJobKeys(GroupMatcher.anyJobGroup()).isEmpty());
        assertEquals(version, getJobDocument().getVersion());
    }
}
