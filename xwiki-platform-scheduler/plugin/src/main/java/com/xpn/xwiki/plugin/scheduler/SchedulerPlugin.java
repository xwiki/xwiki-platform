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
     * Fullname of the XWiki Task Class representing a task that can be scheduled by this plugin.
     */
    public static final String TASK_CLASS = "XWiki.Task";
    
    /**
     * Default Quartz scheduler instance.
     */
    private Scheduler scheduler;

    /**
     * Map that holds Job execution data.
     */
    private JobDataMap data = new JobDataMap();

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
     * {@inheritDoc}
     * @see com.xpn.xwiki.plugin.XWikiPluginInterface#init(com.xpn.xwiki.XWikiContext)
     */
    public void init(XWikiContext context)
    {
        super.init(context);
        try {
            updateTaskClass(context);
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
     * @see com.xpn.xwiki.plugin.XWikiPluginInterface#virtualInit(com.xpn.xwiki.XWikiContext)
     */
    public void virtualInit(XWikiContext context)
    {
        super.virtualInit(context);
        init(context);
    }

    /**
     * Schedule the given task by creating a job and associating a cron trigger with it
     *
     * @param object the XWiki Task object 
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

            // We offer the Job a wrapped copy of the request context and of the XWiki API
            Context copy = new Context((XWikiContext) context.clone());
            data.put("context", copy);
            data.put("xwiki", new com.xpn.xwiki.api.XWiki(copy.getXWiki(), copy.getContext()));

            job.setJobDataMap(data);

            getScheduler().addJob(job, true);
            int state = getScheduler().getTriggerState(task, Scheduler.DEFAULT_GROUP);
            switch (state) {
                case Trigger.STATE_PAUSED:
                    object.setStringValue("status", "Paused");
                    break;
                case Trigger.STATE_NORMAL:
                    if (getTrigger(task).compareTo(trigger) != 0) {
                        LOG.debug("Reschedule Task : " + object.getStringValue("taskName"));
                    }
                    getScheduler().rescheduleJob(trigger.getName(), trigger.getGroup(), trigger);
                    object.setStringValue("status", "Scheduled");
                    break;
                case Trigger.STATE_NONE:
                    LOG.debug("Schedule Task : " + object.getStringValue("taskName"));
                    getScheduler().scheduleJob(trigger);
                    LOG.info("XWiki Task Status :" + object.getStringValue("status"));
                    if (object.getStringValue("status").equals("Paused")) {
                        getScheduler().pauseJob(task, Scheduler.DEFAULT_GROUP);
                        object.setStringValue("status", "Paused");
                    } else {
                        object.setStringValue("status", "Scheduled");
                    }
                    break;
                default:
                    LOG.debug("Schedule Task : " + object.getStringValue("taskName"));
                    getScheduler().scheduleJob(trigger);
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
     * Pause the task with the given name by pausing all of its current triggers.
     *
     * @param taskName the name of the task to be paused
     */
    public void pauseTask(String taskName) throws SchedulerPluginException
    {
        try {
            getScheduler().pauseJob(taskName, Scheduler.DEFAULT_GROUP);
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
            getScheduler().resumeJob(taskName, Scheduler.DEFAULT_GROUP);
        } catch (SchedulerException e) {
            throw new SchedulerPluginException(
                SchedulerPluginException.ERROR_SCHEDULERPLUGIN_RESUME_TASK,
                "Error occured while trying to resume task " + taskName, e);
        }
    }

    /**
     * Unschedule the given task
     *
     * @param taskName the task name for the task to be unscheduled
     */
    public void unscheduleTask(String taskName) throws SchedulerPluginException
    {
        try {
            getScheduler().deleteJob(taskName, Scheduler.DEFAULT_GROUP);
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
    private Trigger getTrigger(String task) throws SchedulerPluginException
    {
        Trigger trigger;
        try {
            trigger = getScheduler().getTrigger(task, Scheduler.DEFAULT_GROUP);
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
     * @exception SchedulerPluginException if the default Scheduler instance failed to be
     *            retrieved for any reason. Note that on the first call the default scheduler is
     *            also initialized.
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
     * @exception SchedulerPluginException if the status listener failed to be set properly
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

    /**
     * Creates the XWiki Task Class if it does not exist in the wiki. Update it if it exists but
     * is missing some properties.
     *
     * @param context the XWiki context
     * @exception SchedulerPluginException if the updated Task Class failed to be saved
     */
    private void updateTaskClass(XWikiContext context) throws SchedulerPluginException
    {
        XWiki xwiki = context.getWiki();
        boolean needsUpdate = false;

        XWikiDocument doc;
        try {
            doc = xwiki.getDocument(SchedulerPlugin.TASK_CLASS, context);
        } catch (Exception e) {
            doc = new XWikiDocument();
            doc.setFullName(SchedulerPlugin.TASK_CLASS);
            needsUpdate = true;
        }

        BaseClass bclass = doc.getxWikiClass();
        bclass.setName(SchedulerPlugin.TASK_CLASS);
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
                    "Error while saving " + SchedulerPlugin.TASK_CLASS
                    + " class document in XWiki", ex);
            }
        }
    }
}
