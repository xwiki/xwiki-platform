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

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.api.Context;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import java.text.ParseException;
import java.util.Date;

/**
 * See {@link com.xpn.xwiki.plugin.scheduler.SchedulerPluginApi} for documentation.
 *
 * @version $Id: $
 */
public class SchedulerPlugin extends XWikiDefaultPlugin
{
    /**
     * Log object to log messages in this class.
     */
    private static final Log LOG = LogFactory.getLog(SchedulerPlugin.class);

    /**
     * Fullname of the XWiki Scheduler Job Class representing a job that can be scheduled by this
     * plugin.
     */
    public static final String XWIKI_JOB_CLASS = "XWiki.SchedulerJobClass";

    /**
     * Default Quartz scheduler instance.
     */
    private Scheduler scheduler;

    /**
     * {@inheritDoc}
     *
     * @see XWikiDefaultPlugin#XWikiDefaultPlugin(String,String,com.xpn.xwiki.XWikiContext)
     */
    public SchedulerPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
        init(context);
        //TODO: restaure existing jobs (according to their stored status)
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.plugin.XWikiPluginInterface#init(com.xpn.xwiki.XWikiContext)
     */
    public void init(XWikiContext context)
    {
        super.init(context);
        try {
            updateSchedulerJobClass(context);
            setScheduler(getDefaultSchedulerInstance());
            setStatusListener();
            getScheduler().start();
        } catch (SchedulerException e) {
            LOG.error("Failed to start the scheduler", e);
        } catch (SchedulerPluginException e) {
            LOG.error("Failed to initialize the scheduler", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.plugin.XWikiPluginInterface#virtualInit(com.xpn.xwiki.XWikiContext)
     */
    public void virtualInit(XWikiContext context)
    {
        super.virtualInit(context);
        init(context);
    }

    /**
     * Retrieve the job's status of a given {@link com.xpn.xwiki.plugin.scheduler.SchedulerPlugin#XWIKI_JOB_CLASS}
     * job XObject, by asking the actual job status to the quartz scheduler instance. It's the
     * actual status, as the one stored in the XObject may be changed manually by users.
     *
     * @param object the XObject to give the status of
     * @return the status of the Job inside the quartz scheduler, as {@link
     *         com.xpn.xwiki.plugin.scheduler.JobState} instance
     */
    public JobState getJobStatus(BaseObject object) throws SchedulerException
    {
        int state = getScheduler()
            .getTriggerState(getObjectUniqueId(object), Scheduler.DEFAULT_GROUP);
        return new JobState(state);
    }

    public boolean scheduleJob(BaseObject object, XWikiContext context)
        throws SchedulerPluginException
    {
        boolean scheduled = true;
        try {
            JobDataMap data = new JobDataMap();

            // compute the job unique Id
            String xjob = getObjectUniqueId(object);

            JobDetail job = new JobDetail(xjob, Scheduler.DEFAULT_GROUP,
                Class.forName(object.getStringValue("jobClass")));

            Trigger trigger = new CronTrigger(xjob, Scheduler.DEFAULT_GROUP, xjob,
                Scheduler.DEFAULT_GROUP, object.getStringValue("cron"));

            data.put("xjob", object);

            // We offer the Job a wrapped copy of the request context and of the XWiki API
            Context copy = new Context((XWikiContext) context.clone());
            data.put("context", copy);
            data.put("xwiki", new com.xpn.xwiki.api.XWiki(copy.getXWiki(), copy.getContext()));

            job.setJobDataMap(data);

            getScheduler().addJob(job, true);

            JobState status = getJobStatus(object);

            switch (status.getState()) {
                case Trigger.STATE_PAUSED:
                    // a paused job must be resumed, not scheduled
                    break;
                case Trigger.STATE_NORMAL:
                    if (getTrigger(object).compareTo(trigger) != 0) {
                        LOG.debug("Reschedule Job : " + object.getStringValue("jobName"));
                    }
                    getScheduler().rescheduleJob(trigger.getName(), trigger.getGroup(), trigger);
                    break;
                case Trigger.STATE_NONE:
                    LOG.debug("Schedule Job : " + object.getStringValue("jobName"));
                    getScheduler().scheduleJob(trigger);
                    LOG.info("XWiki Job Status :" + object.getStringValue("status"));
                    if (object.getStringValue("status").equals("Paused")) {
                        getScheduler().pauseJob(xjob, Scheduler.DEFAULT_GROUP);
                        saveStatus("Paused", object, context);
                    } else {
                        saveStatus("Normal", object, context);
                    }
                    break;
                default:
                    LOG.debug("Schedule Job : " + object.getStringValue("jobName"));
                    getScheduler().scheduleJob(trigger);
                    saveStatus("Normal", object, context);
                    break;
            }
        } catch (SchedulerException e) {
            throw new SchedulerPluginException(
                SchedulerPluginException.ERROR_SCHEDULERPLUGIN_SCHEDULE_JOB,
                "Error while scheduling job " + object.getStringValue("jobName"), e);
        } catch (ParseException e) {
            throw new SchedulerPluginException(
                SchedulerPluginException.ERROR_SCHEDULERPLUGIN_BAD_CRON_EXPRESSION,
                "Error while parsing cron expression for job " + object.getStringValue("jobName"),
                e);
        } catch (ClassNotFoundException e) {
            throw new SchedulerPluginException(
                SchedulerPluginException.ERROR_SCHEDULERPLUGIN_JOB_XCLASS_NOT_FOUND,
                "Error while loading job class for job : " + object.getStringValue("jobName"),
                e);
        } catch (XWikiException e) {
            throw new SchedulerPluginException(
                SchedulerPluginException.ERROR_SCHEDULERPLUGIN_JOB_XCLASS_NOT_FOUND,
                "Error while saving job status for job : " + object.getStringValue("jobName"),
                e);
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
            getScheduler().pauseJob(getObjectUniqueId(object), Scheduler.DEFAULT_GROUP);
            saveStatus("Paused", object, context);
        } catch (SchedulerException e) {
            throw new SchedulerPluginException(
                SchedulerPluginException.ERROR_SCHEDULERPLUGIN_PAUSE_JOB,
                "Error occured while trying to pause job " + object.getStringValue("jobName"), e);
        } catch (XWikiException e) {
            throw new SchedulerPluginException(
                SchedulerPluginException.ERROR_SCHEDULERPLUGIN_PAUSE_JOB,
                "Error occured while trying to save status of job " +
                    object.getStringValue("jobName"), e);
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
            getScheduler().resumeJob(getObjectUniqueId(object), Scheduler.DEFAULT_GROUP);
            saveStatus("Normal", object, context);
        } catch (SchedulerException e) {
            throw new SchedulerPluginException(
                SchedulerPluginException.ERROR_SCHEDULERPLUGIN_RESUME_JOB,
                "Error occured while trying to resume job " + object.getStringValue("jobName"), e);
        } catch (XWikiException e) {
            throw new SchedulerPluginException(
                SchedulerPluginException.ERROR_SCHEDULERPLUGIN_RESUME_JOB,
                "Error occured while trying to save status of job " +
                    object.getStringValue("jobName"), e);
        }
    }

    /**
     * Unschedule the given job
     *
     * @param object the unwrapped XObject job to be unscheduled
     */
    public void unscheduleJob(BaseObject object, XWikiContext context)
        throws SchedulerPluginException
    {
        try {
            getScheduler().deleteJob(getObjectUniqueId(object), Scheduler.DEFAULT_GROUP);
            saveStatus("None", object, context);
        } catch (SchedulerException e) {
            throw new SchedulerPluginException(
                SchedulerPluginException.ERROR_SCHEDULERPLUGIN_JOB_XCLASS_NOT_FOUND,
                "Error while unscheduling job " + object.getStringValue("jobName"), e);
        } catch (XWikiException e) {
            throw new SchedulerPluginException(
                SchedulerPluginException.ERROR_SCHEDULERPLUGIN_JOB_XCLASS_NOT_FOUND,
                "Error while saving status of job " + object.getStringValue("jobName"), e);
        }
    }

    /**
     * Get Trigger object of the given job
     *
     * @param object the unwrapped XObject to be retrieve the trigger for
     * @return the trigger object of the given job
     */
    private Trigger getTrigger(BaseObject object) throws SchedulerPluginException
    {
        String job = getObjectUniqueId(object);
        Trigger trigger;
        try {
            trigger = getScheduler().getTrigger(job, Scheduler.DEFAULT_GROUP);
        } catch (SchedulerException e) {
            throw new SchedulerPluginException(
                SchedulerPluginException.ERROR_SCHEDULERPLUGIN_JOB_XCLASS_NOT_FOUND,
                "Error while getting trigger for job " + job, e);
        }
        if (trigger == null) {
            throw new SchedulerPluginException(
                SchedulerPluginException.ERROR_SCHEDULERPLUGIN_JOB_DOES_NOT_EXITS,
                "Job does not exists");
        }

        return trigger;
    }

    /**
     * Get the next fire time for the given job name SchedulerJob
     *
     * @param object unwrapped XObject job for which the next fire time will be given
     * @return the next Date the job will be fired at
     */
    public Date getNextFireTime(BaseObject object) throws SchedulerPluginException
    {
        return getTrigger(object).getNextFireTime();
    }

    /**
     * {@inheritDoc}
     */
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new SchedulerPluginApi((SchedulerPlugin) plugin, context);
    }

    /**
     * {@inheritDoc}
     */
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
     * @throws SchedulerPluginException if the default Scheduler instance failed to be retrieved for
     * any reason. Note that on the first call the default scheduler is also initialized.
     */
    private synchronized Scheduler getDefaultSchedulerInstance()
        throws SchedulerPluginException
    {
        Scheduler scheduler;
        try {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
        } catch (SchedulerException e) {
            throw new SchedulerPluginException(
                SchedulerPluginException.ERROR_SCHEDULERPLUGIN_GET_SCHEDULER,
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

    private void saveStatus(String status, BaseObject object, XWikiContext context)
        throws XWikiException
    {
        XWikiDocument jobHolder = context.getWiki().getDocument(object.getName(), context);
        object.setStringValue("status", status);
        context.getWiki().saveDocument(jobHolder, context);
    }

    /**
     * Compute a cross-document unique {@link com.xpn.xwiki.objects.BaseObject} id, by concatenating
     * its name (it's document holder full name, such as "SomeSpace.SomeDoc") and it's instance
     * number inside this document. <p/> The scheduler uses this unique object id to assure the
     * unicity of jobs
     *
     * @return a unique String that can identify the object
     */
    private String getObjectUniqueId(BaseObject object)
    {
        return object.getName() + "_" + object.getNumber();
    }

    /**
     * Creates the XWiki SchedulerJob XClass if it does not exist in the wiki. Update it if it
     * exists but is missing some properties.
     *
     * @param context the XWiki context
     * @throws SchedulerPluginException if the updated SchedulerJob XClass failed to be saved
     */
    private void updateSchedulerJobClass(XWikiContext context) throws SchedulerPluginException
    {
        XWiki xwiki = context.getWiki();
        boolean needsUpdate = false;

        XWikiDocument doc;
        try {
            doc = xwiki.getDocument(SchedulerPlugin.XWIKI_JOB_CLASS, context);
        } catch (Exception e) {
            doc = new XWikiDocument();
            doc.setFullName(SchedulerPlugin.XWIKI_JOB_CLASS);
            needsUpdate = true;
        }

        BaseClass bclass = doc.getxWikiClass();
        bclass.setName(SchedulerPlugin.XWIKI_JOB_CLASS);
        needsUpdate |= bclass.addTextField("jobName", "Job Name", 60);
        needsUpdate |= bclass.addTextAreaField("jobDescription", "Job Description", 45, 10);
        needsUpdate |= bclass.addTextField("jobClass", "Job Class", 60);
        needsUpdate |= bclass.addTextField("status", "Status", 30);
        needsUpdate |= bclass.addTextField("cron", "Cron Expression", 30);
        needsUpdate |= bclass.addTextAreaField("script", "Job Script", 45, 10);

        if (needsUpdate) {
            try {
                xwiki.saveDocument(doc, context);
            } catch (XWikiException ex) {
                throw new SchedulerPluginException(
                    SchedulerPluginException.ERROR_SCHEDULERPLUGIN_SAVE_JOB_CLASS,
                    "Error while saving " + SchedulerPlugin.XWIKI_JOB_CLASS
                        + " class document in XWiki", ex);
            }
        }
    }
}
