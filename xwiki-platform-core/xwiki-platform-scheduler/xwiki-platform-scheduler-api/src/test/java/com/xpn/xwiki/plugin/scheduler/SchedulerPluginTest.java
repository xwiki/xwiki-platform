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

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.quartz.CronTrigger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.KeyMatcher;
import org.quartz.listeners.JobListenerSupport;
import org.quartz.listeners.SchedulerListenerSupport;
import org.quartz.simpl.RAMJobStore;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.classloader.internal.ClassLoaderResetEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptServiceManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
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
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SchedulerPlugin}, using an in-memory Quartz scheduler which is only started by the tests which
 * need to execute jobs, so that the jobs are otherwise registered but never executed.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
class SchedulerPluginTest
{
    private static final String WIKI = "xwiki";

    private static final DocumentReference JOB_DOCUMENT = new DocumentReference(WIKI, "Scheduler", "Job");

    private static final String CRON = "0 0 12 * * ?";

    private static final String OTHER_CRON = "0 0 6 * * ?";

    private static final String STATUS = "status";

    private static final String CRON_FIELD = "cron";

    /**
     * A cron expression which doesn't fire during the tests.
     */
    private static final String FAR_CRON = "0 0 12 1 1 ? 2099";

    private static final JobKey PROBE_JOB = new JobKey("probe");

    private static final long TIMEOUT = 10;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    private SchedulersClassLoaderManager schedulersClassLoaderManager;

    @MockComponent
    private ScriptServiceManager scriptServiceManager;

    private XWikiContext context;

    private Scheduler scheduler;

    private SchedulerPlugin plugin;

    @BeforeEach
    void beforeEach() throws Exception
    {
        this.context = this.oldcore.getXWikiContext();
        this.context.setWikiId(WIKI);
        this.context.setUserReference(new DocumentReference(WIKI, "XWiki", "Admin"));
        this.context.setLanguage("fr");
        this.context.setRequest(new XWikiServletRequestStub());
        doReturn(mock(XWikiURLFactoryService.class)).when(this.oldcore.getSpyXWiki()).getURLFactoryService();

        doReturn(GroovyJob.class).when(this.schedulersClassLoaderManager).loadClassAndRegister(any(), any());

        // Initialize the plugin as disabled so that it doesn't start the default Quartz scheduler, and enable it
        // afterwards.
        this.oldcore.getConfigurationSource().setProperty("scheduler.enabled", false);
        this.plugin = new SchedulerPlugin("scheduler", SchedulerPlugin.class.getName(), this.context);
        this.plugin.init(this.context);
        this.plugin.setEnabled(true);

        Properties properties = new Properties();
        properties.setProperty(StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME, "SchedulerPluginTest");
        properties.setProperty(StdSchedulerFactory.PROP_JOB_STORE_CLASS, RAMJobStore.class.getName());
        properties.setProperty("org.quartz.threadPool.threadCount", "1");
        this.scheduler = new StdSchedulerFactory(properties).getScheduler();
        this.plugin.setScheduler(this.scheduler);
    }

    @AfterEach
    void afterEach() throws Exception
    {
        // Don't leave a running job behind.
        BlockingJob.reset();
        // Wait for the Quartz threads to stop, otherwise they log after the log capture is restored, i.e. on the
        // console, which fails whichever test is running at that time.
        this.scheduler.shutdown(true);
    }

    @Test
    void scheduleJob() throws Exception
    {
        BaseObject job = createJob(JOB_DOCUMENT, "");

        assertTrue(this.plugin.scheduleJob(job, this.context));

        assertEquals(TriggerState.NORMAL, getTriggerState(JOB_DOCUMENT));
        assertEquals(CRON, getCronExpression(JOB_DOCUMENT));
        assertEquals(JobState.STATE_NORMAL, this.plugin.getJobStatus(job, this.context).getValue());
        assertEquals(this.scheduler.getTrigger(getTriggerKey(JOB_DOCUMENT)).getNextFireTime(),
            this.plugin.getNextFireTime(job, this.context));
        // The job has never been executed.
        assertNull(this.plugin.getPreviousFireTime(job, this.context));

        // The status and the context in which the job is executed are saved in the job object.
        BaseObject savedJob = loadJob(JOB_DOCUMENT);
        assertEquals(JobState.STATE_NORMAL, savedJob.getStringValue(STATUS));
        assertEquals("XWiki.Admin", savedJob.getStringValue("contextUser"));
        assertEquals("fr", savedJob.getStringValue("contextLang"));
        assertEquals(WIKI, savedJob.getStringValue("contextDatabase"));
    }

    @Test
    void scheduleJobWithPausedStatus() throws Exception
    {
        BaseObject job = createJob(JOB_DOCUMENT, JobState.STATE_PAUSED);

        this.plugin.scheduleJob(job, this.context);

        assertEquals(TriggerState.PAUSED, getTriggerState(JOB_DOCUMENT));
        assertEquals(JobState.STATE_PAUSED, loadJob(JOB_DOCUMENT).getStringValue(STATUS));
    }

    @Test
    void scheduleJobUsesTheContextStoredInTheJob() throws Exception
    {
        XWikiDocument document = new XWikiDocument(JOB_DOCUMENT);
        BaseObject job = addJobObject(document, "");
        job.setStringValue("contextUser", "XWiki.JobUser");
        job.setStringValue("contextLang", "de");
        // The job document was copied from another wiki.
        job.setStringValue("contextDatabase", "otherwiki");
        this.oldcore.getSpyXWiki().saveDocument(document, this.context);

        this.plugin.scheduleJob(loadJob(JOB_DOCUMENT), this.context);

        XWikiContext jobContext = (XWikiContext) this.scheduler.getJobDetail(getJobKey(JOB_DOCUMENT))
            .getJobDataMap().get("context");
        assertEquals("XWiki.JobUser", jobContext.getUser());
        assertEquals("de", jobContext.getLanguage());
        assertEquals(WIKI, jobContext.getWikiId());
        assertEquals(JOB_DOCUMENT, jobContext.getDoc().getDocumentReference());
        assertNotNull(jobContext.getRequest());
        assertNotNull(jobContext.getResponse());
        assertNotNull(jobContext.getURL());

        // The context user and language are kept, but the wiki is the one in which the job document is now located.
        BaseObject savedJob = loadJob(JOB_DOCUMENT);
        assertEquals("XWiki.JobUser", savedJob.getStringValue("contextUser"));
        assertEquals("de", savedJob.getStringValue("contextLang"));
        assertEquals(WIKI, savedJob.getStringValue("contextDatabase"));
    }

    @Test
    void scheduleJobAlreadyScheduledUpdatesItsTrigger() throws Exception
    {
        this.plugin.scheduleJob(createJob(JOB_DOCUMENT, ""), this.context);

        BaseObject job = loadJob(JOB_DOCUMENT);
        job.setStringValue(CRON_FIELD, OTHER_CRON);
        this.plugin.scheduleJob(job, this.context);

        assertEquals(TriggerState.NORMAL, getTriggerState(JOB_DOCUMENT));
        assertEquals(OTHER_CRON, getCronExpression(JOB_DOCUMENT));
    }

    @Test
    void scheduleJobAlreadyPausedKeepsItPaused() throws Exception
    {
        this.plugin.scheduleJob(createJob(JOB_DOCUMENT, ""), this.context);
        this.plugin.pauseJob(loadJob(JOB_DOCUMENT), this.context);

        BaseObject job = loadJob(JOB_DOCUMENT);
        job.setStringValue(CRON_FIELD, OTHER_CRON);
        this.plugin.scheduleJob(job, this.context);

        // A paused job must be resumed, not scheduled.
        assertEquals(TriggerState.PAUSED, getTriggerState(JOB_DOCUMENT));
        assertEquals(CRON, getCronExpression(JOB_DOCUMENT));
    }

    @Test
    void scheduleJobWithUnknownJobClass() throws Exception
    {
        when(this.schedulersClassLoaderManager.loadClassAndRegister(any(), any()))
            .thenThrow(new ClassNotFoundException("Unknown"));
        BaseObject job = createJob(JOB_DOCUMENT, "");

        SchedulerPluginException exception =
            assertThrows(SchedulerPluginException.class, () -> this.plugin.scheduleJob(job, this.context));

        assertEquals(SchedulerPluginException.ERROR_SCHEDULERPLUGIN_JOB_XCLASS_NOT_FOUND, exception.getCode());
        assertFalse(this.scheduler.checkExists(getJobKey(JOB_DOCUMENT)));
        assertEquals("", loadJob(JOB_DOCUMENT).getStringValue(STATUS));
    }

    @Test
    void pauseAndResumeJob() throws Exception
    {
        this.plugin.scheduleJob(createJob(JOB_DOCUMENT, ""), this.context);

        this.plugin.pauseJob(loadJob(JOB_DOCUMENT), this.context);

        assertEquals(TriggerState.PAUSED, getTriggerState(JOB_DOCUMENT));
        assertEquals(JobState.STATE_PAUSED, loadJob(JOB_DOCUMENT).getStringValue(STATUS));

        this.plugin.resumeJob(loadJob(JOB_DOCUMENT), this.context);

        assertEquals(TriggerState.NORMAL, getTriggerState(JOB_DOCUMENT));
        assertEquals(JobState.STATE_NORMAL, loadJob(JOB_DOCUMENT).getStringValue(STATUS));
    }

    @Test
    void unscheduleJob() throws Exception
    {
        this.plugin.scheduleJob(createJob(JOB_DOCUMENT, ""), this.context);
        BaseObject job = loadJob(JOB_DOCUMENT);

        this.plugin.unscheduleJob(job, this.context);

        assertFalse(this.scheduler.checkExists(getJobKey(JOB_DOCUMENT)));
        assertEquals(TriggerState.NONE, getTriggerState(JOB_DOCUMENT));
        assertEquals(JobState.STATE_NONE, loadJob(JOB_DOCUMENT).getStringValue(STATUS));
        verify(this.schedulersClassLoaderManager).removeScheduler(job.getReference());
    }

    @Test
    void triggerJob() throws Exception
    {
        this.plugin.scheduleJob(createJob(JOB_DOCUMENT, ""), this.context);

        this.plugin.triggerJob(loadJob(JOB_DOCUMENT), this.context);

        // Triggering a job adds a one-shot trigger next to the cron one.
        assertEquals(2, this.scheduler.getTriggersOfJob(getJobKey(JOB_DOCUMENT)).size());
    }

    @Test
    void getNextFireTimeOfUnknownJob() throws Exception
    {
        BaseObject job = createJob(JOB_DOCUMENT, "");

        SchedulerPluginException exception =
            assertThrows(SchedulerPluginException.class, () -> this.plugin.getNextFireTime(job, this.context));

        assertEquals(SchedulerPluginException.ERROR_SCHEDULERPLUGIN_JOB_DOES_NOT_EXITS, exception.getCode());
    }

    @Test
    void getJobStatusWithoutScheduler() throws Exception
    {
        // The scheduler is set asynchronously when the plugin is initialized.
        this.plugin.setScheduler(null);

        assertNull(this.plugin.getJobStatus(createJob(JOB_DOCUMENT, ""), this.context));
    }

    @Test
    void operationsWhenDisabled() throws Exception
    {
        BaseObject job = createJob(JOB_DOCUMENT, "");
        this.plugin.setEnabled(false);

        assertDisabled(() -> this.plugin.scheduleJob(job, this.context));
        assertDisabled(() -> this.plugin.pauseJob(job, this.context));
        assertDisabled(() -> this.plugin.resumeJob(job, this.context));
        assertDisabled(() -> this.plugin.triggerJob(job, this.context));
        assertDisabled(() -> this.plugin.unscheduleJob(job, this.context));
        assertDisabled(() -> this.plugin.reloadJob(job, this.context));
        assertNull(this.plugin.getNextFireTime(job, this.context));
        assertNull(this.plugin.getPreviousFireTime(job, this.context));
        assertFalse(this.scheduler.checkExists(getJobKey(JOB_DOCUMENT)));
    }

    @Test
    void reloadJob() throws Exception
    {
        this.plugin.scheduleJob(createJob(JOB_DOCUMENT, ""), this.context);
        BaseObject job = loadJob(JOB_DOCUMENT);
        job.setStringValue(CRON_FIELD, OTHER_CRON);

        this.plugin.reloadJob(job, this.context);

        assertEquals(TriggerState.NORMAL, getTriggerState(JOB_DOCUMENT));
        assertEquals(OTHER_CRON, getCronExpression(JOB_DOCUMENT));
    }

    @Test
    void reloadJobUnknownToTheScheduler() throws Exception
    {
        createJob(JOB_DOCUMENT, "");

        this.plugin.reloadJob(loadJob(JOB_DOCUMENT), this.context);

        // Nothing is registered, and the job isn't even built (which saves the context in which it's executed).
        assertFalse(this.scheduler.checkExists(getJobKey(JOB_DOCUMENT)));
        verify(this.schedulersClassLoaderManager, never()).loadClassAndRegister(any(), any());
        assertEquals("", loadJob(JOB_DOCUMENT).getStringValue("contextUser"));
    }

    @Test
    void reloadJobWhileItIsRunning() throws Exception
    {
        BlockingJob.reset();
        doReturn(BlockingJob.class).when(this.schedulersClassLoaderManager).loadClassAndRegister(any(), any());
        scheduleJobWithFarCron();
        this.scheduler.start();
        this.plugin.triggerJob(loadJob(JOB_DOCUMENT), this.context);
        assertTrue(BlockingJob.started.await(TIMEOUT, TimeUnit.SECONDS));
        // The job doesn't allow concurrent executions.
        assertEquals(TriggerState.BLOCKED, getTriggerState(JOB_DOCUMENT));

        this.plugin.reloadJob(loadJob(JOB_DOCUMENT), this.context);

        // The reloaded job waits for the end of the current execution.
        assertEquals(TriggerState.BLOCKED, getTriggerState(JOB_DOCUMENT));

        BlockingJob.release.countDown();
        waitForPreviousExecutions();

        assertEquals(TriggerState.NORMAL, getTriggerState(JOB_DOCUMENT));
    }

    @Test
    void reloadJobInError() throws Exception
    {
        doReturn(FailingJob.class).when(this.schedulersClassLoaderManager).loadClassAndRegister(any(), any());
        scheduleJobWithFarCron();
        CountDownLatch schedulerError = new CountDownLatch(1);
        this.scheduler.getListenerManager().addSchedulerListener(new SchedulerListenerSupport()
        {
            @Override
            public void schedulerError(String msg, SchedulerException cause)
            {
                schedulerError.countDown();
            }
        });
        this.scheduler.start();
        this.plugin.triggerJob(loadJob(JOB_DOCUMENT), this.context);
        assertTrue(schedulerError.await(TIMEOUT, TimeUnit.SECONDS));
        waitForPreviousExecutions();
        // The job could not be instantiated.
        assertEquals(TriggerState.ERROR, getTriggerState(JOB_DOCUMENT));
        assertEquals("An error occurred instantiating job to be executed. job= 'DEFAULT.xwiki:Scheduler.Job_0'",
            this.logCapture.getMessage(0));

        // The job class has been fixed, e.g. by reinstalling the extension providing it.
        doReturn(NoopJob.class).when(this.schedulersClassLoaderManager).loadClassAndRegister(any(), any());
        this.plugin.reloadJob(loadJob(JOB_DOCUMENT), this.context);

        assertEquals(TriggerState.NORMAL, getTriggerState(JOB_DOCUMENT));
    }

    @ParameterizedTest
    @CsvSource({
        "Normal, NORMAL",
        "Paused, PAUSED",
        "None, NONE",
        "Complete, NONE"
    })
    void onDocumentCreated(String status, TriggerState expectedState) throws Exception
    {
        createJob(JOB_DOCUMENT, status);
        XWikiDocument document = loadDocument(JOB_DOCUMENT);
        document.setOriginalDocument(new XWikiDocument(JOB_DOCUMENT));

        this.plugin.onEvent(new DocumentCreatedEvent(JOB_DOCUMENT), document, this.context);

        assertEquals(expectedState, getTriggerState(JOB_DOCUMENT));
    }

    @Test
    void onDocumentDeleted() throws Exception
    {
        this.plugin.scheduleJob(createJob(JOB_DOCUMENT, ""), this.context);
        XWikiDocument document = new XWikiDocument(JOB_DOCUMENT);
        document.setOriginalDocument(loadDocument(JOB_DOCUMENT));

        this.plugin.onEvent(new DocumentDeletedEvent(JOB_DOCUMENT), document, this.context);

        assertFalse(this.scheduler.checkExists(getJobKey(JOB_DOCUMENT)));
    }

    @Test
    void onDocumentUpdatedWithOnlyTheStatusModified() throws Exception
    {
        this.plugin.scheduleJob(createJob(JOB_DOCUMENT, ""), this.context);
        clearInvocations(this.schedulersClassLoaderManager);

        // The scheduler saves the job status itself whenever it registers or unregisters a job, which must not
        // reload the job.
        updateJob(job -> job.setStringValue(STATUS, JobState.STATE_PAUSED));

        verify(this.schedulersClassLoaderManager, never()).loadClassAndRegister(any(), any());
        assertEquals(TriggerState.NORMAL, getTriggerState(JOB_DOCUMENT));
    }

    @Test
    void onDocumentUpdatedWithTheCronModified() throws Exception
    {
        this.plugin.scheduleJob(createJob(JOB_DOCUMENT, ""), this.context);
        clearInvocations(this.schedulersClassLoaderManager);

        updateJob(job -> job.setStringValue(CRON_FIELD, OTHER_CRON));

        verify(this.schedulersClassLoaderManager).loadClassAndRegister(any(), any());
        assertEquals(TriggerState.NORMAL, getTriggerState(JOB_DOCUMENT));
        assertEquals(OTHER_CRON, getCronExpression(JOB_DOCUMENT));
    }

    @Test
    void onDocumentUpdatedOfPausedJob() throws Exception
    {
        this.plugin.scheduleJob(createJob(JOB_DOCUMENT, JobState.STATE_PAUSED), this.context);

        updateJob(job -> job.setLargeStringValue("script", "println 'updated'"));

        // The job is reloaded with its new definition, and stays paused.
        assertEquals(TriggerState.PAUSED, getTriggerState(JOB_DOCUMENT));
        BaseObject scheduledJob =
            (BaseObject) this.scheduler.getJobDetail(getJobKey(JOB_DOCUMENT)).getJobDataMap().get("xjob");
        assertEquals("println 'updated'", scheduledJob.getLargeStringValue("script"));
    }

    @Test
    void onDocumentUpdatedOfNotScheduledJob() throws Exception
    {
        createJob(JOB_DOCUMENT, "");

        updateJob(job -> job.setStringValue(CRON_FIELD, OTHER_CRON));

        // A job unknown to the scheduler is registered with its new definition when it's scheduled.
        verify(this.schedulersClassLoaderManager, never()).loadClassAndRegister(any(), any());
        assertFalse(this.scheduler.checkExists(getJobKey(JOB_DOCUMENT)));
    }

    @Test
    void onWikiDeleted() throws Exception
    {
        DocumentReference fooJob = new DocumentReference("foo", "Scheduler", "Job");
        DocumentReference foobarJob = new DocumentReference("foobar", "Scheduler", "Job");
        this.plugin.scheduleJob(createJob(JOB_DOCUMENT, ""), this.context);
        this.plugin.scheduleJob(createJob(fooJob, ""), this.context);
        this.plugin.scheduleJob(createJob(foobarJob, ""), this.context);

        this.plugin.onEvent(new WikiDeletedEvent("foo"), null, null);

        assertFalse(this.scheduler.checkExists(getJobKey(fooJob)));
        assertTrue(this.scheduler.checkExists(getJobKey(foobarJob)));
        assertTrue(this.scheduler.checkExists(getJobKey(JOB_DOCUMENT)));
        verify(this.schedulersClassLoaderManager).removeSchedulers("wiki:foo");
    }

    @Test
    void onClassLoaderReset()
    {
        this.plugin.onEvent(new ClassLoaderResetEvent("wiki:foo"), null, null);

        verify(this.schedulersClassLoaderManager).onClassLoaderReset("wiki:foo");
    }

    private void scheduleJobWithFarCron() throws Exception
    {
        XWikiDocument document = new XWikiDocument(JOB_DOCUMENT);
        addJobObject(document, "").setStringValue(CRON_FIELD, FAR_CRON);
        this.oldcore.getSpyXWiki().saveDocument(document, this.context);
        this.plugin.scheduleJob(loadJob(JOB_DOCUMENT), this.context);
    }

    /**
     * Execute a probe job and wait for it to start. Since the scheduler has a single thread, this guarantees that the
     * scheduler is done with the jobs triggered before, including updating the state of their triggers.
     */
    private void waitForPreviousExecutions() throws Exception
    {
        CountDownLatch probeStarted = new CountDownLatch(1);
        this.scheduler.getListenerManager().addJobListener(new JobListenerSupport()
        {
            @Override
            public String getName()
            {
                return PROBE_JOB.getName();
            }

            @Override
            public void jobToBeExecuted(JobExecutionContext jobContext)
            {
                probeStarted.countDown();
            }
        }, KeyMatcher.keyEquals(PROBE_JOB));
        this.scheduler.addJob(JobBuilder.newJob(NoopJob.class).withIdentity(PROBE_JOB).storeDurably().build(), true);
        this.scheduler.triggerJob(PROBE_JOB);

        assertTrue(probeStarted.await(TIMEOUT, TimeUnit.SECONDS));
    }

    private void assertDisabled(Executable executable)
    {
        SchedulerPluginException exception = assertThrows(SchedulerPluginException.class, executable);
        assertEquals(SchedulerPluginException.ERROR_SCHEDULERPLUGIN_DISABLED, exception.getCode());
    }

    /**
     * Save a modification of the job and send the corresponding document updated event.
     */
    private void updateJob(Consumer<BaseObject> modification) throws XWikiException
    {
        XWikiDocument originalDocument = loadDocument(JOB_DOCUMENT);
        XWikiDocument document = loadDocument(JOB_DOCUMENT);
        modification.accept(document.getXObject(SchedulerPlugin.XWIKI_JOB_CLASSREFERENCE));
        this.oldcore.getSpyXWiki().saveDocument(document, this.context);

        XWikiDocument updatedDocument = loadDocument(JOB_DOCUMENT);
        updatedDocument.setOriginalDocument(originalDocument);
        this.plugin.onEvent(new DocumentUpdatedEvent(JOB_DOCUMENT), updatedDocument, this.context);
    }

    private BaseObject createJob(DocumentReference documentReference, String status) throws XWikiException
    {
        XWikiDocument document = new XWikiDocument(documentReference);
        addJobObject(document, status);
        this.oldcore.getSpyXWiki().saveDocument(document, this.context);

        return loadJob(documentReference);
    }

    private BaseObject addJobObject(XWikiDocument document, String status) throws XWikiException
    {
        BaseObject job = document.newXObject(SchedulerPlugin.XWIKI_JOB_CLASSREFERENCE, this.context);
        job.setStringValue("jobName", "Test Job");
        job.setStringValue("jobClass", GroovyJob.class.getName());
        job.setStringValue(CRON_FIELD, CRON);
        job.setStringValue(STATUS, status);
        return job;
    }

    private XWikiDocument loadDocument(DocumentReference documentReference) throws XWikiException
    {
        // Clone to avoid modifying the cached document, as the plugin does.
        return this.oldcore.getSpyXWiki().getDocument(documentReference, this.context).clone();
    }

    private BaseObject loadJob(DocumentReference documentReference) throws XWikiException
    {
        return loadDocument(documentReference).getXObject(SchedulerPlugin.XWIKI_JOB_CLASSREFERENCE);
    }

    private String getJobId(DocumentReference documentReference)
    {
        return documentReference.getWikiReference().getName() + ":Scheduler.Job_0";
    }

    private JobKey getJobKey(DocumentReference documentReference)
    {
        return new JobKey(getJobId(documentReference));
    }

    private TriggerKey getTriggerKey(DocumentReference documentReference)
    {
        return new TriggerKey(getJobId(documentReference));
    }

    private TriggerState getTriggerState(DocumentReference documentReference) throws Exception
    {
        return this.scheduler.getTriggerState(getTriggerKey(documentReference));
    }

    private String getCronExpression(DocumentReference documentReference) throws Exception
    {
        return ((CronTrigger) this.scheduler.getTrigger(getTriggerKey(documentReference))).getCronExpression();
    }

    /**
     * A job which does nothing.
     */
    public static class NoopJob implements Job
    {
        @Override
        public void execute(JobExecutionContext jobContext)
        {
            // Nothing to do.
        }
    }

    /**
     * A job which cannot be instantiated.
     */
    public static class FailingJob extends NoopJob
    {
        /**
         * Always fails.
         */
        public FailingJob()
        {
            throw new IllegalStateException("Cannot instantiate");
        }
    }

    /**
     * A job which doesn't allow concurrent executions and runs until it's released.
     */
    @DisallowConcurrentExecution
    public static class BlockingJob implements Job
    {
        private static CountDownLatch started;

        private static CountDownLatch release;

        static void reset()
        {
            if (release != null) {
                release.countDown();
            }
            started = new CountDownLatch(1);
            release = new CountDownLatch(1);
        }

        @Override
        public void execute(JobExecutionContext jobContext) throws JobExecutionException
        {
            started.countDown();
            try {
                release.await(TIMEOUT, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new JobExecutionException(e);
            }
        }
    }
}
