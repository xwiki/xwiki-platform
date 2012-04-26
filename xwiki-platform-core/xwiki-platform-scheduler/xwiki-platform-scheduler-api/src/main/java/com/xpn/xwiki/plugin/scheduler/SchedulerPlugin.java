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
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptServiceManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiResponse;
import com.xpn.xwiki.web.XWikiServletRequest;

/**
 * See {@link com.xpn.xwiki.plugin.scheduler.SchedulerPluginApi} for documentation.
 * 
 * @version $Id$
 */
public class SchedulerPlugin extends XWikiDefaultPlugin
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
    public static final EntityReference XWIKI_JOB_CLASSREFERENCE = new EntityReference("SchedulerJobClass",
        EntityType.DOCUMENT, new EntityReference("XWiki", EntityType.SPACE));

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
        try {
            String initialDb = !context.getDatabase().equals("") ? context.getDatabase() : context.getMainXWiki();

            List<String> wikiServers;
            if (context.getWiki().isVirtualMode()) {
                try {
                    wikiServers = context.getWiki().getVirtualWikisDatabaseNames(context);
                } catch (Exception e) {
                    LOGGER.error("error getting list of wiki servers!", e);
                    wikiServers = new ArrayList<String>();
                }
            } else {
                wikiServers = new ArrayList<String>();
            }

            if (!wikiServers.contains(context.getMainXWiki())) {
                wikiServers.add(context.getMainXWiki());
            }

            // Init class

            try {
                for (String wikiName : new ArrayList<String>(wikiServers)) {
                    context.setDatabase(wikiName);
                    try {
                        updateSchedulerJobClass(context);
                    } catch (Exception e) {
                        LOGGER.error("Failed to update scheduler job class for in wiki [{}]", wikiName, e);

                        // Removing the wiki from the list since it will be impossible start jobs for it
                        wikiServers.remove(wikiName);
                    }
                }
            } finally {
                context.setDatabase(initialDb);
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
                    context.setDatabase(wikiName);
                    restoreExistingJobs(context);
                }
            } finally {
                context.setDatabase(initialDb);
            }
        } catch (SchedulerException e) {
            LOGGER.error("Failed to start the scheduler", e);
        } catch (SchedulerPluginException e) {
            LOGGER.error("Failed to initialize the scheduler", e);
        }

        super.init(context);
    }

    @Override
    public void virtualInit(XWikiContext context)
    {
        super.virtualInit(context);
    }

    /**
     * Create and feed a stub context for the job execution thread. Stub context data are retrieved from job object
     * fields "contextUser", "contextLang", "contextDatabase". If one of this field is empty (this would typically
     * happen on the first schedule operation), it is instead retrieved from the passed context, and the job object is
     * updated with this value. This mean that this method may modify the passed object.
     * 
     * @param job the job for which the context will be prepared
     * @param context the XWikiContext at preparation time. This is a real context associated with a servlet request
     * @return the stub context prepared with job datas.
     */
    private XWikiContext prepareJobStubContext(BaseObject job, XWikiContext context) throws SchedulerPluginException
    {
        boolean jobNeedsUpdate = true;
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
        String iDb = context.getDatabase();
        String cDb = job.getStringValue("contextDatabase");
        if (cDb.equals("") || !cDb.equals(iDb)) {
            cDb = context.getDatabase();
            job.setStringValue("contextDatabase", cDb);
            jobNeedsUpdate = true;
        }

        if (jobNeedsUpdate) {
            try {
                context.setDatabase(cDb);
                XWikiDocument jobHolder = context.getWiki().getDocument(job.getName(), context);
                jobHolder.setMinorEdit(true);
                context.getWiki().saveDocument(jobHolder, context);
            } catch (XWikiException e) {
                throw new SchedulerPluginException(
                    SchedulerPluginException.ERROR_SCHEDULERPLUGIN_UNABLE_TO_PREPARE_JOB_CONTEXT,
                    "Failed to prepare context for job with job name " + job.getStringValue("jobName"), e);
            } finally {
                context.setDatabase(iDb);
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
        scontext.setDatabase(cDb);
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
            XWikiDocument cDoc = context.getWiki().getDocument(job.getName(), context);
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
     * status "Paused", then it is both scheduled and paused Jobs with other status (None, Complete) are not
     * rescheduled.
     * 
     * @param context The XWikiContext when initializing the plugin
     */
    private void restoreExistingJobs(XWikiContext context)
    {
        String hql = ", BaseObject as obj where obj.name=doc.fullName and obj.className='XWiki.SchedulerJobClass'";
        try {
            List<String> jobsDocsNames = context.getWiki().getStore().searchDocumentsNames(hql, context);
            for (String docName : jobsDocsNames) {
                try {
                    XWikiDocument jobDoc = context.getWiki().getDocument(docName, context);
                    BaseObject jobObj = jobDoc.getXObject(XWIKI_JOB_CLASSREFERENCE);

                    String status = jobObj.getStringValue("status");
                    if (status.equals(JobState.STATE_NORMAL) || status.equals(JobState.STATE_PAUSED)) {
                        this.scheduleJob(jobObj, context);
                    }
                    if (status.equals(JobState.STATE_PAUSED)) {
                        this.pauseJob(jobObj, context);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to restore job with in document [{}] and wiki [{}]", new Object[] {docName,
                    context.getDatabase()}, e);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to restore existing scheduler jobs in wiki [{}]", context.getDatabase(), e);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new SchedulerPluginApi((SchedulerPlugin) plugin, context);
    }

    /**
     * {@inheritDoc}
     */
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
        XWikiDocument jobHolder = context.getWiki().getDocument(object.getName(), context);
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
        return context.getDatabase() + ":" + object.getName() + "_" + object.getNumber();
    }

    private boolean setSchedulerClassesDocumentFields(XWikiDocument doc, String title)
    {
        boolean needsUpdate = false;

        if (StringUtils.isBlank(doc.getCreator())) {
            needsUpdate = true;
            doc.setCreator("superadmin");
        }
        if (StringUtils.isBlank(doc.getAuthor())) {
            needsUpdate = true;
            doc.setAuthor(doc.getCreator());
        }
        if (StringUtils.isBlank(doc.getParent())) {
            needsUpdate = true;
            doc.setParent("XWiki.XWikiClasses");
        }
        if (StringUtils.isBlank(doc.getTitle())) {
            needsUpdate = true;
            doc.setTitle(title);
        }
        if (StringUtils.isBlank(doc.getContent()) || !Syntax.XWIKI_2_0.equals(doc.getSyntax())) {
            needsUpdate = true;
            doc.setContent("{{include document=\"XWiki.ClassSheet\" /}}");
            doc.setSyntax(Syntax.XWIKI_2_0);
        }
        if (!doc.isHidden()) {
            needsUpdate = true;
            doc.setHidden(true);
        }

        return needsUpdate;
    }

    /**
     * Creates the XWiki SchedulerJob XClass if it does not exist in the wiki. Update it if it exists but is missing
     * some properties.
     * 
     * @param context the XWiki context
     * @throws SchedulerPluginException if the updated SchedulerJob XClass failed to be saved
     */
    private void updateSchedulerJobClass(XWikiContext context) throws SchedulerPluginException
    {
        XWiki xwiki = context.getWiki();
        boolean needsUpdate = false;

        DocumentReference jobClassReference =
            Utils.<DocumentReferenceResolver<EntityReference>> getComponent(DocumentReferenceResolver.TYPE_REFERENCE,
                "current").resolve(SchedulerPlugin.XWIKI_JOB_CLASSREFERENCE, EntityType.DOCUMENT);

        XWikiDocument doc;
        try {
            doc = xwiki.getDocument(jobClassReference, context);
        } catch (Exception e) {
            LOGGER.error("Failed to get scheduler job class document", e);

            doc = new XWikiDocument(jobClassReference);
            needsUpdate = true;
        }

        BaseClass bclass = doc.getXClass();
        needsUpdate |= bclass.addTextField("jobName", "Job Name", 60);
        needsUpdate |= bclass.addTextAreaField("jobDescription", "Job Description", 45, 10);
        needsUpdate |= bclass.addTextField("jobClass", "Job Class", 60);
        needsUpdate |= bclass.addTextField("status", "Status", 30);
        needsUpdate |= bclass.addTextField("cron", "Cron Expression", 30);
        needsUpdate |= bclass.addTextAreaField("script", "Job Script", 60, 10);
        // make sure that the script field is of type pure text so that wysiwyg editor is never used for it
        TextAreaClass scriptField = (TextAreaClass) bclass.getField("script");
        // Note: getEditor() returns lowercase but the values are actually camelcase...
        if (!scriptField.getEditor().equals("puretext")) {
            scriptField.setStringValue("editor", "PureText");
            needsUpdate = true;
        }
        needsUpdate |= bclass.addTextField("contextUser", "Job execution context user", 30);
        needsUpdate |= bclass.addTextField("contextLang", "Job execution context lang", 30);
        needsUpdate |= bclass.addTextField("contextDatabase", "Job execution context database", 30);
        needsUpdate |= setSchedulerClassesDocumentFields(doc, "XWiki Scheduler Job Class");

        if (needsUpdate) {
            try {
                xwiki.saveDocument(doc, context.getMessageTool().get("xe.scheduler.updateJobClassComment"), true,
                    context);
            } catch (XWikiException ex) {
                throw new SchedulerPluginException(SchedulerPluginException.ERROR_SCHEDULERPLUGIN_SAVE_JOB_CLASS,
                    "Error while saving " + doc + " class document in XWiki", ex);
            }
        }
    }
}
