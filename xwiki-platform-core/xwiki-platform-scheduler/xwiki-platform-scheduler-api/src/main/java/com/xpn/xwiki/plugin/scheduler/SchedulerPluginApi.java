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

import java.util.Date;
import java.util.List;

import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.Object;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.plugin.PluginApi;

/**
 * A Scheduler plugin to plan execution of Jobs from XWiki with cron expressions. The plugin uses Quartz's scheduling
 * library. <p> Jobs are represented by {@link com.xpn.xwiki.api.Object} XObjects, instances of the
 * {@link SchedulerPlugin#XWIKI_JOB_CLASS} XClass. These XObjects do store a job name, the implementation class name of
 * the job to be executed, the cron expression to precise when the job should be fired, and possibly a groovy script
 * with the job's program. <p> The plugin offers a {@link GroovyJob} Groovy Job wrapper to execute groovy scripts
 * (typically for use inside the Wiki), but can also be used with any Java class implementing {@link org.quartz.Job}
 * 
 * @version $Id$
 */
public class SchedulerPluginApi extends PluginApi<SchedulerPlugin>
{
    /**
     * Log object to log messages in this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerPluginApi.class);

    public SchedulerPluginApi(SchedulerPlugin plugin, XWikiContext context)
    {
        super(plugin, context);
    }

    /**
     * Return the trigger state of the given {@link com.xpn.xwiki.plugin.scheduler.SchedulerPlugin#XWIKI_JOB_CLASS}
     * XObject job. Possible values are : None (the trigger does not exist yet, or has been deleted), Normal, Blocked,
     * Complete, Error and Paused
     * 
     * @param object the XObject job to give the state of
     * @return a String representing this state
     */
    public String getStatus(Object object)
    {
        try {
            return getJobStatus(object.getXWikiObject()).getValue();
        } catch (Exception e) {
            this.context.put("error", e.getMessage());
            return null;
        }
    }

    /**
     * Return the trigger state as a ${@link JobState}, that holds both the integer trigger's inner value of the state
     * and a String as a human readable representation of that state
     */
    public JobState getJobStatus(BaseObject object) throws SchedulerException
    {
        return getProtectedPlugin().getJobStatus(object, this.context);
    }

    public JobState getJobStatus(Object object) throws SchedulerException, SchedulerPluginException
    {
        return getProtectedPlugin().getJobStatus(retrieveBaseObject(object), this.context);
    }

    /**
     * This function allow to retrieve a com.xpn.xwiki.objects.BaseObject from a com.xpn.xwiki.api.Object without that
     * the current user needs programming rights (as in com.xpn.xwiki.api.Object#getXWikiObject(). The function is used
     * internally by this api class and allows wiki users to call methods from the scheduler without having programming
     * right. The programming right is only needed at script execution time.
     * 
     * @return object the unwrapped version of the passed api object
     */
    private BaseObject retrieveBaseObject(Object object) throws SchedulerPluginException
    {
        String docName = object.getName();
        int objNb = object.getNumber();
        try {

            XWikiDocument jobHolder = this.context.getWiki().getDocument(docName, this.context);
            BaseObject jobObject = jobHolder.getXObject(SchedulerPlugin.XWIKI_JOB_CLASSREFERENCE, objNb);
            return jobObject;
        } catch (XWikiException e) {
            throw new SchedulerPluginException(SchedulerPluginException.ERROR_SCHEDULERPLUGIN_UNABLE_TO_RETRIEVE_JOB,
                "Job in document [" + docName + "] with object number [" + objNb + "] could not be retrieved.", e);
        }
    }

    /**
     * Schedule the given XObject to be executed according to its parameters. Errors are returned in the context map.
     * Scheduling can be called for example: <code> #if($xwiki.scheduler.scheduleJob($job)!=true)
     * #error($xcontext.get("error") #else #info("Job scheduled") #end </code>
     * Where $job is an XObject, instance of the {@link SchedulerPlugin#XWIKI_JOB_CLASS} XClass
     * 
     * @param object the XObject to be scheduled, an instance of the XClass XWiki.SchedulerJobClass
     * @return true on success, false on failure
     */
    public boolean scheduleJob(Object object)
    {
        try {
            return scheduleJob(retrieveBaseObject(object));
        } catch (Exception e) {
            // we don't need to push the exception message in the context here
            // as it should already have been pushed by the throwing exception
            return false;
        }
    }

    public boolean scheduleJob(BaseObject object)
    {
        try {
            getProtectedPlugin().scheduleJob(object, this.context);
            return true;
        } catch (Exception e) {
            this.context.put("error", e.getMessage());
            return false;
        }
    }

    /**
     * Schedule all {@link com.xpn.xwiki.plugin.scheduler.SchedulerPlugin#XWIKI_JOB_CLASS} XObjects stored inside the
     * given Wiki document, according to each XObject own parameters.
     * 
     * @param document the document holding the XObjects Jobs to be scheduled
     * @return true on success, false on failure.
     */
    public boolean scheduleJobs(Document document)
    {
        boolean result = true;
        try {
            XWikiDocument doc = this.context.getWiki().getDocument(document.getFullName(), this.context);
            List<BaseObject> objects = doc.getXObjects(SchedulerPlugin.XWIKI_JOB_CLASSREFERENCE);
            for (BaseObject object : objects) {
                result &= scheduleJob(object);
            }
        } catch (Exception e) {
            this.context.put("error", e.getMessage());
            return false;
        }
        return result;
    }

    /**
     * Pause the given XObject job by pausing all of its current triggers. Can be called the same way as
     * {@link #scheduleJob(Object)}
     * 
     * @param object the wrapped XObject Job to be paused
     * @return true on success, false on failure.
     */
    public boolean pauseJob(Object object)
    {
        try {
            return pauseJob(retrieveBaseObject(object));
        } catch (Exception e) {
            // we don't need to push the exception message in the context here
            // as it should already have been pushed by the throwing exception
            return false;
        }
    }

    public boolean pauseJob(BaseObject object)
    {
        try {
            getProtectedPlugin().pauseJob(object, this.context);
            LOGGER.debug("Pause Job: [{}]", object.getStringValue("jobName"));
            return true;
        } catch (XWikiException e) {
            this.context.put("error", e.getMessage());
            return false;
        }
    }

    /**
     * Resume a XObject job that is in a {@link JobState#STATE_PAUSED} state. Can be called the same way as
     * {@link #scheduleJob(Object)}
     * 
     * @param object the wrapped XObject Job to be paused
     * @return true on success, false on failure.
     */
    public boolean resumeJob(Object object)
    {
        try {
            return resumeJob(retrieveBaseObject(object));
        } catch (Exception e) {
            // we don't need to push the exception message in the context here
            // as it should already have been pushed by the throwing exception
            return false;
        }
    }

    public boolean resumeJob(BaseObject object)
    {
        try {
            getProtectedPlugin().resumeJob(object, this.context);
            LOGGER.debug("Resume Job: [{}]", object.getStringValue("jobName"));
            return true;
        } catch (XWikiException e) {
            this.context.put("error", e.getMessage());
            return false;
        }
    }

    /**
     * Unschedule a XObject job by deleting it from the jobs table. Can be called the same way as
     * {@link #scheduleJob(Object)}
     * 
     * @param object the wrapped XObject Job to be paused
     * @return true on success, false on failure.
     */
    public boolean unscheduleJob(Object object)
    {
        try {
            return unscheduleJob(retrieveBaseObject(object));
        } catch (Exception e) {
            // we don't need to push the exception message in the context here
            // as it should already have been pushed by the throwing exception
            return false;
        }
    }

    public boolean unscheduleJob(BaseObject object)
    {
        try {
            getProtectedPlugin().unscheduleJob(object, this.context);
            LOGGER.debug("Delete Job: [{}]", object.getStringValue("jobName"));
            return true;
        } catch (XWikiException e) {
            this.context.put("error", e.getMessage());
            return false;
        }
    }
    
    /**
     * Trigger a XObject job (execute it now).
     * 
     * @param object the wrapped XObject Job to be triggered
     * @return true on success, false on failure.
     */
    public boolean triggerJob(Object object)
    {
        try {
            return triggerJob(retrieveBaseObject(object));
        } catch (Exception e) {
            // we don't need to push the exception message in the context here
            // as it should already have been pushed by the throwing exception
            return false;
        }
    }

    /**
     * Trigger a BaseObject job (execute it now).
     * 
     * @param object the BaseObject Job to be triggered
     * @return true on success, false on failure.
     */
    public boolean triggerJob(BaseObject object)
    {
        try {
            getProtectedPlugin().triggerJob(object, this.context);
            LOGGER.debug("Trigger Job: [{}]", object.getStringValue("jobName"));
            return true;
        } catch (XWikiException e) {
            this.context.put("error", e.getMessage());
            return false;
        }
    }
    
    /**
     * Give, for a XObject job in a {@link JobState#STATE_NORMAL} state, the previous date at which the job has been
     * executed, the fire time is not computed from the CRON expression, this method will return null if the .
     * 
     * @param object the wrapped XObject for which to give the fire time
     * @return the date the job has been executed
     */
    public Date getPreviousFireTime(Object object)
    {
        try {
            return getPreviousFireTime(retrieveBaseObject(object));
        } catch (Exception e) {
            // we don't need to push the exception message in the context here
            // as it should already have been pushed by the throwing exception
            return null;
        }
    }

    /**
     * Give, for a BaseObject job in a {@link JobState#STATE_NORMAL} state, the previous date at which the job has been
     * executed. Note that this method does not compute a date from the CRON expression, it only returns a date value 
     * which is set each time the job is executed. If the job has never been fired this method will return null.
     * 
     * @param object the BaseObject for which to give the fire time
     * @return the date the job has been executed
     */
    public Date getPreviousFireTime(BaseObject object)
    {
        try {
            return getProtectedPlugin().getPreviousFireTime(object, this.context);
        } catch (SchedulerPluginException e) {
            this.context.put("error", e.getMessage());
            return null;
        }
    }

    /**
     * Give, for a XObject job in a {@link JobState#STATE_NORMAL} state, the next date at which the job will be
     * executed, according to its cron expression. Errors are returned in the context map. Can be called for example:
     * <code> #set($firetime = $xwiki.scheduler.getNextFireTime($job))
     * #if (!$firetime || $firetime=="") #error($xcontext.get("error") #else #info("Fire time :
     * $firetime") #end </code>
     * Where $job is an XObject, instance of the {@link SchedulerPlugin#XWIKI_JOB_CLASS} XClass
     * 
     * @param object the wrapped XObject for which to give the fire date
     * @return the date the job will be executed
     */
    public Date getNextFireTime(Object object)
    {
        try {
            return getNextFireTime(retrieveBaseObject(object));
        } catch (Exception e) {
            // we don't need to push the exception message in the context here
            // as it should already have been pushed by the throwing exception
            return null;
        }
    }

    public Date getNextFireTime(BaseObject object)
    {
        try {
            return getProtectedPlugin().getNextFireTime(object, this.context);
        } catch (SchedulerPluginException e) {
            this.context.put("error", e.getMessage());
            return null;
        }
    }
}
