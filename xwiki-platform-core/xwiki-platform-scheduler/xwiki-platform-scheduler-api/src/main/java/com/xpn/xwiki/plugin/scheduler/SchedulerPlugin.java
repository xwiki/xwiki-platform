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

import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.inject.Provider;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.context.concurrent.ExecutionContextRunnable;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.script.service.ScriptServiceManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.plugin.scheduler.internal.SchedulerJobClassDocumentInitializer;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiResponse;
import com.xpn.xwiki.web.XWikiServletRequest;
import com.xpn.xwiki.web.XWikiServletRequestStub;
import com.xpn.xwiki.web.XWikiServletResponseStub;

/**
 * See {@link com.xpn.xwiki.plugin.scheduler.SchedulerPluginApi} for documentation.
 * 
 * @version $Id$
 */
public class SchedulerPlugin extends XWikiDefaultPlugin implements EventListener
{
    /**
     * Log object to log messages in this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerPlugin.class);

    /**
     * Fullname of the XWiki Scheduler Job Class representing a job that can be scheduled by this plugin.
     * 
     * @deprecated use {@link #XWIKI_JOB_CLASSREFERENCE} instead
     */
    @Deprecated
    public static final String XWIKI_JOB_CLASS = "XWiki.SchedulerJobClass";

    /**
     * Local reference of the XWiki Scheduler Job Class representing a job that can be scheduled by this plugin.
     */
    public static final EntityReference XWIKI_JOB_CLASSREFERENCE =
        SchedulerJobClassDocumentInitializer.XWIKI_JOB_CLASSREFERENCE;

    private static final List<Event> EVENTS = Arrays.<Event>asList(new DocumentCreatedEvent(),
        new DocumentDeletedEvent(), new DocumentUpdatedEvent());

    /**
     * Default Quartz scheduler instance.
     */
    private Scheduler scheduler;

    /**
     * Default plugin constructor.
     * 
     * @see XWikiDefaultPlugin#XWikiDefaultPlugin(String,String,com.xpn.xwiki.XWikiContext)
     */
    public SchedulerPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
    }

    @Override
    public void init(XWikiContext context)
    {
        Thread thread = new Thread(new ExecutionContextRunnable(new Runnable()
        {
            @Override
            public void run()
            {
                initAsync();
            }
        }, Utils.getComponentManager()));
        thread.setName("XWiki Scheduler initialization");
        thread.setDaemon(true);

        thread.start();

        // Start listening to documents modifications
        Utils.getComponent(ObservationManager.class).addListener(this);
    }

    private void initAsync()
    {
        XWikiContext xcontext = Utils.<Provider<XWikiContext>>getComponent(XWikiContext.TYPE_PROVIDER).get();

        try {
            String initialDb = !xcontext.getWikiId().equals("") ? xcontext.getWikiId() : xcontext.getMainXWiki();

            List<String> wikiServers = new ArrayList<String>();
            try {
                wikiServers = xcontext.getWiki().getVirtualWikisDatabaseNames(xcontext);
            } catch (Exception e) {
                LOGGER.error("error getting list of wiki servers!", e);
            }

            // Before we start the thread ensure that Quartz will create daemon threads so that
            // the JVM can exit properly.
            System.setProperty("org.quartz.scheduler.makeSchedulerThreadDaemon", "true");
            System.setProperty("org.quartz.threadPool.makeThreadsDaemons", "true");

            setScheduler(getDefaultSchedulerInstance());
            setStatusListener();
            getScheduler().start();

            // Restore jobs

            try {
                // Iterate on all virtual wikis
                for (String wikiName : wikiServers) {
                    xcontext.setWikiId(wikiName);
                    restoreExistingJobs(xcontext);
                }
            } finally {
                xcontext.setWikiId(initialDb);
            }
        } catch (SchedulerException e) {
            LOGGER.error("Failed to start the scheduler", e);
        } catch (SchedulerPluginException e) {
            LOGGER.error("Failed to initialize the scheduler", e);
        }
    }

    /**
     * Create and feed a stub context for the job execution thread. Stub context data are retrieved from job object
     * fields "contextUser", "contextLang", "contextDatabase". If one of this field is empty (this would typically
     * happen on the first schedule operation), it is instead retrieved from the passed context, and the job object is
     * updated with this value. This mean that this method may modify the passed object.
     * 
     * @param job the job for which the context will be prepared
     * @param context the XWikiContext at preparation time. This is a real context associated with a servlet request
     * @return the stub context prepared with job data
     */
    private XWikiContext prepareJobStubContext(BaseObject job, XWikiContext context) throws SchedulerPluginException
    {
        boolean jobNeedsUpdate = false;
        String cUser = job.getStringValue("contextUser");
        if (cUser.equals("")) {
            // The context user has not been filled yet.
            // We can suppose it's the first scheduling. Let's assume it's the context user
            cUser = context.getUser();
            job.setStringValue("contextUser", cUser);
            jobNeedsUpdate = true;
        }
        String cLang = job.getStringValue("contextLang");
        if (cLang.equals("")) {
            cLang = context.getLanguage();
            job.setStringValue("contextLang", cLang);
            jobNeedsUpdate = true;
        }
        String iDb = context.getWikiId();
        String cDb = job.getStringValue("contextDatabase");
        if (cDb.equals("") || !cDb.equals(iDb)) {
            cDb = context.getWikiId();
            job.setStringValue("contextDatabase", cDb);
            jobNeedsUpdate = true;
        }

        if (jobNeedsUpdate) {
            try {
                context.setWikiId(cDb);
                XWikiDocument jobHolder = context.getWiki().getDocument(job.getName(), context);
                jobHolder.setMinorEdit(true);
                context.getWiki().saveDocument(jobHolder, context);
            } catch (XWikiException e) {
                throw new SchedulerPluginException(
                    SchedulerPluginException.ERROR_SCHEDULERPLUGIN_UNABLE_TO_PREPARE_JOB_CONTEXT,
                    "Failed to prepare context for job with job name " + job.getStringValue("jobName"), e);
            } finally {
                context.setWikiId(iDb);
            }
        }

        // lets now build the stub context
        XWikiContext scontext = context.clone();
        scontext.setWiki(context.getWiki());
        context.getWiki().getStore().cleanUp(context);

        // We are sure the context request is a real servlet request
        // So we force the dummy request with the current host
        XWikiServletRequestStub dummy = new XWikiServletRequestStub();
        dummy.setHost(context.getRequest().getHeader("x-forwarded-host"));
        dummy.setScheme(context.getRequest().getScheme());
        dummy.setContextPath(context.getRequest().getContextPath());
        XWikiServletRequest request = new XWikiServletRequest(dummy);
        scontext.setRequest(request);

        // Force forged context response to a stub response, since the current context response
        // will not mean anything anymore when running in the scheduler's thread, and can cause
        // errors.
        XWikiResponse stub = new XWikiServletResponseStub();
        scontext.setResponse(stub);

        // feed the dummy context
        scontext.setUser(cUser);
        scontext.setLanguage(cLang);
        scontext.setWikiId(cDb);
        scontext.setMainXWiki(context.getMainXWiki());
        if (scontext.getURL() == null) {
            try {
                scontext.setURL(new URL("http://www.mystuburl.com/"));
            } catch (Exception e) {
                // the URL is well formed, I promise
            }
        }

        com.xpn.xwiki.web.XWikiURLFactory xurf = context.getURLFactory();
        if (xurf == null) {
            xurf = context.getWiki().getURLFactoryService().createURLFactory(context.getMode(), context);
        }
        scontext.setURLFactory(xurf);

        try {
            XWikiDocument cDoc = context.getWiki().getDocument(job.getDocumentReference(), context);
            scontext.setDoc(cDoc);
        } catch (Exception e) {
            throw new SchedulerPluginException(
                SchedulerPluginException.ERROR_SCHEDULERPLUGIN_UNABLE_TO_PREPARE_JOB_CONTEXT,
                "Failed to prepare context for job with job name " + job.getStringValue("jobName"), e);
        }

        return scontext;
    }

    /**
     * Restore the existing job, by looking up for such job in the database and re-scheduling those according to their
     * stored status. If a Job is stored with the status "Normal", it is just scheduled If a Job is stored with the
     * status "Paused", then it is both scheduled and paused. Jobs with other status (None, Complete) are not
     * rescheduled.
     * 
     * @param context The XWikiContext when initializing the plugin
     */
    private void restoreExistingJobs(XWikiContext context)
    {
        String hql = ", BaseObject as obj where obj.name=doc.fullName and obj.className='XWiki.SchedulerJobClass'";
        try {
            List<DocumentReference> jobDocReferences =
                context.getWiki().getStore().searchDocumentReferences(hql, context);
            for (DocumentReference docReference : jobDocReferences) {
                try {
                    XWikiDocument jobDoc = context.getWiki().getDocument(docReference, context);

                    register(jobDoc, context);
                } catch (Exception e) {
                    LOGGER.error("Failed to restore job with in document [{}] and wiki [{}]", docReference,
                        context.getWikiId(), e);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to restore existing scheduler jobs in wiki [{}]", context.getWikiId(), e);
        }
    }

    private void register(XWikiDocument jobDoc, XWikiContext context) throws SchedulerPluginException
    {
        BaseObject jobObj = jobDoc.getXObject(XWIKI_JOB_CLASSREFERENCE);

        register(jobObj, context);
    }

    private void register(BaseObject jobObj, XWikiContext context) throws SchedulerPluginException
    {
        String status = jobObj.getStringValue("status");
        if (status.equals(JobState.STATE_NORMAL) || status.equals(JobState.STATE_PAUSED)) {
            scheduleJob(jobObj, context);
        }
        if (status.equals(JobState.STATE_PAUSED)) {
            pauseJob(jobObj, context);
        }
    }

    private void unregister(BaseObject jobObj, XWikiContext context) throws SchedulerPluginException
    {
        String status = jobObj.getStringValue("status");
        if (status.equals(JobState.STATE_NORMAL) || status.equals(JobState.STATE_PAUSED)) {
            scheduleJob(jobObj, context);
        }
        if (status.equals(JobState.STATE_PAUSED)) {
            pauseJob(jobObj, context);
        }
    }

    /**
     * Retrieve the job's status of a given {@link com.xpn.xwiki.plugin.scheduler.SchedulerPlugin#XWIKI_JOB_CLASS} job
     * XObject, by asking the actual job status to the quartz scheduler instance. It's the actual status, as the one
     * stored in the XObject may be changed manually by users.
     * 
     * @param object the XObject to give the status of
     * @return the status of the Job inside the quartz scheduler, as {@link com.xpn.xwiki.plugin.scheduler.JobState}
     *         instance
     */
    public JobState getJobStatus(BaseObject object, XWikiContext context) throws SchedulerException
    {
        int state = getScheduler().getTriggerState(getObjectUniqueId(object, context), Scheduler.DEFAULT_GROUP);
        return new JobState(state);
    }

    public boolean scheduleJob(BaseObject object, XWikiContext context) throws SchedulerPluginException
    {
        boolean scheduled = true;
        try {
            JobDataMap data = new JobDataMap();

            // compute the job unique Id
            String xjob = getObjectUniqueId(object, context);

            JobDetail job =
                new JobDetail(xjob, Scheduler.DEFAULT_GROUP, Class.forName(object.getStringValue("jobClass")));

            Trigger trigger =
                new CronTrigger(xjob, Scheduler.DEFAULT_GROUP, xjob, Scheduler.DEFAULT_GROUP,
                    object.getStringValue("cron"));

            // Let's prepare an execution context...
            XWikiContext stubContext = prepareJobStubContext(object, context);

            data.put("context", stubContext);
            data.put("xcontext", stubContext);
            data.put("xwiki", new com.xpn.xwiki.api.XWiki(context.getWiki(), stubContext));
            data.put("xjob", object);
            data.put("services", Utils.getComponent(ScriptServiceManager.class));

            job.setJobDataMap(data);

            getScheduler().addJob(job, true);

            JobState status = getJobStatus(object, context);

            switch (status.getState()) {
                case Trigger.STATE_PAUSED:
                    // a paused job must be resumed, not scheduled
                    break;
                case Trigger.STATE_NORMAL:
                    if (getTrigger(object, context).compareTo(trigger) != 0) {
                        LOGGER.debug("Reschedule Job: [{}]", object.getStringValue("jobName"));
                    }
                    getScheduler().rescheduleJob(trigger.getName(), trigger.getGroup(), trigger);
                    break;
                case Trigger.STATE_NONE:
                    LOGGER.debug("Schedule Job: [{}]", object.getStringValue("jobName"));
                    getScheduler().scheduleJob(trigger);
                    LOGGER.info("XWiki Job Status: [{}]", object.getStringValue("status"));
                    if (object.getStringValue("status").equals("Paused")) {
                        getScheduler().pauseJob(xjob, Scheduler.DEFAULT_GROUP);
                        saveStatus("Paused", object, context);
                    } else {
                        saveStatus("Normal", object, context);
                    }
                    break;
                default:
                    LOGGER.debug("Schedule Job: [{}]", object.getStringValue("jobName"));
                    getScheduler().scheduleJob(trigger);
                    saveStatus("Normal", object, context);
                    break;
            }
        } catch (SchedulerException e) {
            throw new SchedulerPluginException(SchedulerPluginException.ERROR_SCHEDULERPLUGIN_SCHEDULE_JOB,
                "Error while scheduling job " + object.getStringValue("jobName"), e);
        } catch (ParseException e) {
            throw new SchedulerPluginException(SchedulerPluginException.ERROR_SCHEDULERPLUGIN_BAD_CRON_EXPRESSION,
                "Error while parsing cron expression for job " + object.getStringValue("jobName"), e);
        } catch (ClassNotFoundException e) {
            throw new SchedulerPluginException(SchedulerPluginException.ERROR_SCHEDULERPLUGIN_JOB_XCLASS_NOT_FOUND,
                "Error while loading job class for job : " + object.getStringValue("jobName"), e);
        } catch (XWikiException e) {
            throw new SchedulerPluginException(SchedulerPluginException.ERROR_SCHEDULERPLUGIN_JOB_XCLASS_NOT_FOUND,
                "Error while saving job status for job : " + object.getStringValue("jobName"), e);
        }
        return scheduled;
    }

    /**
     * Pause the job with the given name by pausing all of its current triggers.
     * 
     * @param object the non-wrapped XObject Job to be paused
     */
    public void pauseJob(BaseObject object, XWikiContext context) throws SchedulerPluginException
    {
        try {
            getScheduler().pauseJob(getObjectUniqueId(object, context), Scheduler.DEFAULT_GROUP);
            saveStatus("Paused", object, context);
        } catch (SchedulerException e) {
            throw new SchedulerPluginException(SchedulerPluginException.ERROR_SCHEDULERPLUGIN_PAUSE_JOB,
                "Error occured while trying to pause job " + object.getStringValue("jobName"), e);
        } catch (XWikiException e) {
            throw new SchedulerPluginException(SchedulerPluginException.ERROR_SCHEDULERPLUGIN_PAUSE_JOB,
                "Error occured while trying to save status of job " + object.getStringValue("jobName"), e);
        }
    }

    /**
     * Resume the job with the given name (un-pause)
     * 
     * @param object the non-wrapped XObject Job to be resumed
     */
    public void resumeJob(BaseObject object, XWikiContext context) throws SchedulerPluginException
    {
        try {
            getScheduler().resumeJob(getObjectUniqueId(object, context), Scheduler.DEFAULT_GROUP);
            saveStatus("Normal", object, context);
        } catch (SchedulerException e) {
            throw new SchedulerPluginException(SchedulerPluginException.ERROR_SCHEDULERPLUGIN_RESUME_JOB,
                "Error occured while trying to resume job " + object.getStringValue("jobName"), e);
        } catch (XWikiException e) {
            throw new SchedulerPluginException(SchedulerPluginException.ERROR_SCHEDULERPLUGIN_RESUME_JOB,
                "Error occured while trying to save status of job " + object.getStringValue("jobName"), e);
        }
    }

    /**
     * Trigger a job (execute it now)
     * 
     * @param object the non-wrapped XObject Job to be triggered
     * @param context the XWiki context
     */
    public void triggerJob(BaseObject object, XWikiContext context) throws SchedulerPluginException
    {
        try {
            getScheduler().triggerJob(getObjectUniqueId(object, context), Scheduler.DEFAULT_GROUP);
        } catch (SchedulerException e) {
            throw new SchedulerPluginException(SchedulerPluginException.ERROR_SCHEDULERPLUGIN_TRIGGER_JOB,
                "Error occured while trying to trigger job " + object.getStringValue("jobName"), e);
        }
    }

    /**
     * Unschedule the given job
     * 
     * @param object the unwrapped XObject job to be unscheduled
     */
    public void unscheduleJob(BaseObject object, XWikiContext context) throws SchedulerPluginException
    {
        try {
            getScheduler().deleteJob(getObjectUniqueId(object, context), Scheduler.DEFAULT_GROUP);
            saveStatus("None", object, context);
        } catch (SchedulerException e) {
            throw new SchedulerPluginException(SchedulerPluginException.ERROR_SCHEDULERPLUGIN_JOB_XCLASS_NOT_FOUND,
                "Error while unscheduling job " + object.getStringValue("jobName"), e);
        } catch (XWikiException e) {
            throw new SchedulerPluginException(SchedulerPluginException.ERROR_SCHEDULERPLUGIN_JOB_XCLASS_NOT_FOUND,
                "Error while saving status of job " + object.getStringValue("jobName"), e);
        }
    }

    /**
     * Get Trigger object of the given job
     * 
     * @param object the unwrapped XObject to be retrieve the trigger for
     * @param context the XWiki context
     * @return the trigger object of the given job
     */
    private Trigger getTrigger(BaseObject object, XWikiContext context) throws SchedulerPluginException
    {
        String job = getObjectUniqueId(object, context);
        Trigger trigger;
        try {
            trigger = getScheduler().getTrigger(job, Scheduler.DEFAULT_GROUP);
        } catch (SchedulerException e) {
            throw new SchedulerPluginException(SchedulerPluginException.ERROR_SCHEDULERPLUGIN_JOB_XCLASS_NOT_FOUND,
                "Error while getting trigger for job " + job, e);
        }
        if (trigger == null) {
            throw new SchedulerPluginException(SchedulerPluginException.ERROR_SCHEDULERPLUGIN_JOB_DOES_NOT_EXITS,
                "Job does not exists");
        }

        return trigger;
    }

    /**
     * Give, for a BaseObject job in a {@link JobState#STATE_NORMAL} state, the previous date at which the job has been
     * executed. Note that this method does not compute a date from the CRON expression, it only returns a date value
     * which is set each time the job is executed. If the job has never been fired this method will return null.
     * 
     * @param object unwrapped XObject job for which the next fire time will be given
     * @param context the XWiki context
     * @return the next Date the job will be fired at, null if the job has never been fired
     */
    public Date getPreviousFireTime(BaseObject object, XWikiContext context) throws SchedulerPluginException
    {
        return getTrigger(object, context).getPreviousFireTime();
    }

    /**
     * Get the next fire time for the given job name SchedulerJob
     * 
     * @param object unwrapped XObject job for which the next fire time will be given
     * @return the next Date the job will be fired at
     */
    public Date getNextFireTime(BaseObject object, XWikiContext context) throws SchedulerPluginException
    {
        return getTrigger(object, context).getNextFireTime();
    }

    @Override
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new SchedulerPluginApi((SchedulerPlugin) plugin, context);
    }

    @Override
    public String getName()
    {
        return "scheduler";
    }

    /**
     * @param scheduler the scheduler to use
     */
    public void setScheduler(Scheduler scheduler)
    {
        this.scheduler = scheduler;
    }

    /**
     * @return the scheduler in use
     */
    public Scheduler getScheduler()
    {
        return this.scheduler;
    }

    /**
     * @return the default Scheduler instance
     * @throws SchedulerPluginException if the default Scheduler instance failed to be retrieved for any reason. Note
     *             that on the first call the default scheduler is also initialized.
     */
    private synchronized Scheduler getDefaultSchedulerInstance() throws SchedulerPluginException
    {
        Scheduler scheduler;
        try {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
        } catch (SchedulerException e) {
            throw new SchedulerPluginException(SchedulerPluginException.ERROR_SCHEDULERPLUGIN_GET_SCHEDULER,
                "Error getting default Scheduler instance", e);
        }
        return scheduler;
    }

    /**
     * Associates the scheduler with a StatusListener
     * 
     * @throws SchedulerPluginException if the status listener failed to be set properly
     */
    private void setStatusListener() throws SchedulerPluginException
    {
        StatusListener listener = new StatusListener();
        try {
            getScheduler().addSchedulerListener(listener);
            getScheduler().addGlobalJobListener(listener);
        } catch (SchedulerException e) {
            throw new SchedulerPluginException(
                SchedulerPluginException.ERROR_SCHEDULERPLUGIN_INITIALIZE_STATUS_LISTENER,
                "Error while initializing the status listener", e);
        }
    }

    private void saveStatus(String status, BaseObject object, XWikiContext context) throws XWikiException
    {
        XWikiDocument jobHolder = context.getWiki().getDocument(object.getDocumentReference(), context);
        // We need to retrieve the object BaseObject the document again. Otherwise, modifications made to the
        // BaseObject passed as argument will not be saved (XWikiDocument#getObject clones the document
        // and returns the BaseObject from the clone)
        // TODO refactor the plugin in order to stop passing BaseObject around, passing document references instead.
        BaseObject job = jobHolder.getXObject(XWIKI_JOB_CLASSREFERENCE);
        job.setStringValue("status", status);
        jobHolder.setMinorEdit(true);
        context.getWiki().saveDocument(jobHolder, context);
    }

    /**
     * Compute a cross-document unique {@link com.xpn.xwiki.objects.BaseObject} id, by concatenating its name (it's
     * document holder full name, such as "SomeSpace.SomeDoc") and it's instance number inside this document.
     * <p/>
     * The scheduler uses this unique object id to assure the unicity of jobs
     * 
     * @return a unique String that can identify the object
     */
    private String getObjectUniqueId(BaseObject object, XWikiContext context)
    {
        return context.getWikiId() + ":" + object.getName() + "_" + object.getNumber();
    }

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiContext xcontext = (XWikiContext) data;
        XWikiDocument document = (XWikiDocument) source;
        XWikiDocument originalDocument = document.getOriginalDocument();

        BaseObject jobObj = document.getXObject(XWIKI_JOB_CLASSREFERENCE);
        BaseObject originalJobObj = originalDocument.getXObject(XWIKI_JOB_CLASSREFERENCE);

        if (jobObj == null) {
            if (originalJobObj != null) {
                // Job deleted
                try {
                    unregister(originalJobObj, xcontext);
                } catch (SchedulerPluginException e) {
                    LOGGER.warn("Failed to register job in document [{}]: {}", document.getDocumentReference(),
                        ExceptionUtils.getRootCauseMessage(e));
                }
            }
        } else {
            if (originalJobObj == null) {
                // New job
                try {
                    register(jobObj, xcontext);
                } catch (SchedulerPluginException e) {
                    LOGGER.warn("Failed to register job in document [{}]: {}", document.getDocumentReference(),
                        ExceptionUtils.getRootCauseMessage(e));
                }
            }
        }
    }
}
