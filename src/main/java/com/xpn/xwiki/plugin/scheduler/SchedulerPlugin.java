/*
 * Copyright 2005-2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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

import java.text.ParseException;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.api.Context;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.plugin.PluginException;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

/**
 * See {@link com.xpn.xwiki.plugin.scheduler.SchedulerPluginApi} for documentation.
 *
 * @version $Id: $
 */
public class SchedulerPlugin extends XWikiDefaultPlugin implements XWikiPluginInterface
{
    /**
     * Log object to log messages in this class.
     */
    private static final Log LOG = LogFactory.getLog(SchedulerPlugin.class);

    /**
     * Quartz scheduler instance
     */
    protected static Scheduler scheduler;

    /**
     * Map that holds state information for Job instances.
     */
    private JobDataMap data = new JobDataMap();

    /**
     * Fullname of the XClass representing a task that can be scheduled by this plugin
     */
    public static final String TASK_CLASS = "XWiki.Task";

    /**
     * name of the XWiki class representing a schedulable task
     */
    protected static final String TASK_NAME = "Task";

    /**
     * XWiki Web space in which the schedulable task XClass is located
     */
    protected static final String TASK_WEB = "XWiki";

    /**
     * {@inheritDoc}
     *
     * @see XWikiDefaultPlugin#XWikiDefaultPlugin(String,String,com.xpn.xwiki.XWikiContext)
     */
    public SchedulerPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
        init(context);
    }

    /**
     * Creates the Task XClass if it does not exists in the wiki. Update the XWiki Task class if it
     * already exists but needs update.
     *
     * @param context the XWiki context
     */
    protected void updateTaskClass(XWikiContext context) throws SchedulerPluginException
    {
        XWikiDocument doc;
        XWiki xwiki = context.getWiki();
        boolean needsUpdate = false;

        try {
            doc = xwiki.getDocument(TASK_CLASS, context);
        } catch (Exception e) {
            doc = new XWikiDocument();

            doc.setSpace(TASK_WEB);
            doc.setName(TASK_NAME);
            needsUpdate = true;
        }

        BaseClass bclass = doc.getxWikiClass();
        bclass.setName(TASK_CLASS);
        needsUpdate |= bclass.addTextField("taskName", "Task Name", 30);
        needsUpdate |= bclass.addTextField("taskClass", "Task Class", 30);
        needsUpdate |= bclass.addTextField("status", "Status", 30);
        needsUpdate |= bclass.addTextField("cron", "Cron Expression", 30);
        needsUpdate |= bclass.addTextAreaField("script", "Groovy Script", 45, 10);

        if (needsUpdate) {
            try {
                xwiki.saveDocument(doc, context);
            } catch (XWikiException ex) {
                throw new SchedulerPluginException(
                    SchedulerPluginException.ERROR_SCHEDULERPLUGIN_SAVE_TASK_CLASS,
                    "Error while saving " + TASK_CLASS + " class document in XWiki", ex);
            }
        }
    }

    /**
     * Obtains the Scheduler instance
     *
     * @return the Scheduler instance
     */
    private static synchronized Scheduler getSchedulerInstance() throws SchedulerException
    {
        if (scheduler == null) {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
        }
        return scheduler;
    }

    /**
     * Associates the scheduler with a StatusListener
     */
    private void setStatusListener() throws SchedulerPluginException
    {
        StatusListener listener = new StatusListener();
        try {
            scheduler.addSchedulerListener(listener);
            scheduler.addGlobalJobListener(listener);
        } catch (SchedulerException ex) {
            throw new SchedulerPluginException(
                SchedulerPluginException.ERROR_SCHEDULERPLUGIN_INITIALIZE_STATUS_LISTENER,
                "Error while initializing the StatusListener", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Initialization method for the SchedulerPlugin
     *
     * @param context the XWikiContext needed to create/update the XWiki Task class
     */
    public void init(XWikiContext context)
    {
        super.init(context);
        try {
            updateTaskClass(context);
            scheduler = getSchedulerInstance();
            setStatusListener();
            scheduler.start();
        } catch (XWikiException e) {
            LOG.error("Cannot init Scheduler plugin", e);
        } catch (SchedulerException e) {
            LOG.error("Cannot init Scheduler plugin", e);
        }
    }

    /**
     * Pause the task with the given name by pausing all of its current triggers.
     *
     * @param taskName the name of the task to be paused
     */
    public void pauseTask(String taskName) throws SchedulerPluginException
    {
        try {
            scheduler.pauseJob(taskName, Scheduler.DEFAULT_GROUP);
        } catch (SchedulerException e) {
            throw new SchedulerPluginException(
                SchedulerPluginException.ERROR_SCHEDULERPLUGIN_PAUSE_TASK,
                "Error occured while trying to pause task " + taskName, e);
        }
    }

    /**
     * Resume the task with the given name (un-pause)
     *
     * @param taskName the name of the task to be paused
     */
    public void resumeTask(String taskName) throws PluginException
    {
        try {
            scheduler.resumeJob(taskName, Scheduler.DEFAULT_GROUP);
        } catch (SchedulerException e) {
            throw new SchedulerPluginException(
                SchedulerPluginException.ERROR_SCHEDULERPLUGIN_RESUME_TASK,
                "Error occured while trying to resume task " + taskName, e);
        }
    }

    /**
     * Schedule the given task by creating a job and associating a cron trigger with it
     *
     * @param object the XWiki task object
     * @param context the XWiki context
     */
    public boolean scheduleTask(BaseObject object, XWikiContext context)
        throws SchedulerPluginException
    {
        boolean scheduled = true;
        try {
            String task = String.valueOf(object.getNumber());

            JobDetail job = new JobDetail(task, Scheduler.DEFAULT_GROUP,
                Class.forName(object.getStringValue("taskClass")), true, false, true);
            Trigger trigger = new CronTrigger(task, Scheduler.DEFAULT_GROUP, task,
                Scheduler.DEFAULT_GROUP, object.getStringValue("cron"));

            data.put("task", object.getNumber());
            //we offer the Job a wrapped copy of the request context and of the XWiki API
            Context copy = new Context((XWikiContext) context.clone());
            data.put("context", copy);
            data.put("xwiki", new com.xpn.xwiki.api.XWiki(copy.getXWiki(), copy.getContext()));

            job.setJobDataMap(data);

            scheduler.addJob(job, true);
            int state = scheduler.getTriggerState(task, Scheduler.DEFAULT_GROUP);
            switch (state) {
                case Trigger.STATE_PAUSED:
                    object.setStringValue("status", "Paused");
                    break;
                case Trigger.STATE_NORMAL:
                    if (getTrigger(task).compareTo(trigger) != 0) {
                        LOG.debug("Reschedule Task : " + object.getStringValue("taskName"));
                    }
                    scheduler.rescheduleJob(trigger.getName(), trigger.getGroup(), trigger);
                    object.setStringValue("status", "Scheduled");
                    break;
                case Trigger.STATE_NONE:
                    LOG.debug("Schedule Task : " + object.getStringValue("taskName"));
                    scheduler.scheduleJob(trigger);
                    LOG.info("XWiki Task Status :" + object.getStringValue("status"));
                    if (object.getStringValue("status").equals("Paused")) {
                        scheduler.pauseJob(task, Scheduler.DEFAULT_GROUP);
                        object.setStringValue("status", "Paused");
                    } else {
                        object.setStringValue("status", "Scheduled");
                    }
                    break;
                default:
                    LOG.debug("Schedule Task : " + object.getStringValue("taskName"));
                    scheduler.scheduleJob(trigger);
                    object.setStringValue("status", "Scheduled");
                    break;
            }
        } catch (SchedulerException e) {
            throw new SchedulerPluginException(
                SchedulerPluginException.ERROR_SCHEDULERPLUGIN_SCHEDULE_TASK,
                "Error while scheduling task " + object.getStringValue("taskName"), e);
        } catch (ParseException e) {
            throw new SchedulerPluginException(
                SchedulerPluginException.ERROR_SCHEDULERPLUGIN_BAD_CRON_EXPRESSION,
                "Error while parsing cron expression for task " + object.getStringValue("taskName"),
                e);
        } catch (ClassNotFoundException e) {
            throw new SchedulerPluginException(
                SchedulerPluginException.ERROR_SCHEDULERPLUGIN_TASK_CLASS_NOT_FOUND,
                "Error while loading task class for task : " + object.getStringValue("taskName"),
                e);
        }
        return scheduled;
    }

    /**
     * Unschedule the given task
     *
     * @param taskName the task name for the task to be unscheduled
     */
    public void unscheduleTask(String taskName) throws SchedulerPluginException
    {
        try {
            scheduler.deleteJob(taskName, Scheduler.DEFAULT_GROUP);
        } catch (SchedulerException e) {
            throw new SchedulerPluginException(
                SchedulerPluginException.ERROR_SCHEDULERPLUGIN_TASK_CLASS_NOT_FOUND,
                "Error while unscheduling task " + taskName, e);
        }
    }

    /**
     * Get Trigger object of the given task
     *
     * @param task the task name for which the trigger will be given
     * @return the trigger object of the given task
     */
    public Trigger getTrigger(String task) throws SchedulerPluginException
    {
        Trigger trigger;
        try {
            trigger = scheduler.getTrigger(task, Scheduler.DEFAULT_GROUP);
        } catch (SchedulerException e) {
            throw new SchedulerPluginException(
                SchedulerPluginException.ERROR_SCHEDULERPLUGIN_TASK_CLASS_NOT_FOUND,
                "Error while getting trigger for task " + task, e);
        }
        if (trigger == null) {
            throw new SchedulerPluginException(
                SchedulerPluginException.ERROR_SCHEDULERPLUGIN_TASK_DOES_NOT_EXITS,
                "Task does not exists");
        }

        return trigger;
    }

    /**
     * Get the next fire time for the given task name Task
     *
     * @param task the task name for which the next fire time will be given
     * @return the next Date the task will be fired at
     */
    public Date getNextFireTime(String task) throws SchedulerPluginException
    {
        return getTrigger(task).getNextFireTime();
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
     * {@inheritDoc}
     */
    public void virtualInit(XWikiContext context)
    {
        init(context);
    }
}
