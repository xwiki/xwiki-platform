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
import java.util.Iterator;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.Object;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * @version $Id: $
 */
public class SchedulerPluginApi extends Api
{
    /**
     * Log object to log messages in this class.
     */
    private static final Log LOG = LogFactory.getLog(SchedulerPluginApi.class);

    private SchedulerPlugin plugin;

    public SchedulerPluginApi(SchedulerPlugin plugin, XWikiContext context)
    {
        super(context);
        setPlugin(plugin);
    }

    public boolean pauseTask(String number)
    {
        return pauseTask(context.getDoc().getObject(SchedulerPlugin.TASK_CLASS,
            Integer.valueOf(number).intValue()));
    }

    public boolean pauseTask(Object object)
    {
        return pauseTask(object.getXWikiObject());
    }

    public boolean pauseTask(BaseObject object)
    {
        try {
            plugin.pauseTask(String.valueOf(object.getNumber()));
            saveStatus("Paused", object);
            LOG.debug("Pause Task : " + object.getStringValue("taskName"));
            return true;
        } catch (XWikiException e) {
            context.put("error", e.getMessage());
            return false;
        }
    }

    /**
     * Schedule the given XObject to be executed according to its parameters. Errors are returned in
     * the context map. Scheduling can be called for example: <code> #if($xwiki.scheduler.scheduleTask($task)!=true)
     * #error($context.get("error") #else #info("Task scheduled") #end </code> Where $task is an
     * XObject, instance of the XWiki.Task XClass
     *
     * @param object the XObject to be scheduled, an instance of the XClass XWiki.Task
     * @return true on success, false on failure
     */
    public boolean scheduleTask(Object object)
    {
        return scheduleTask(object.getXWikiObject());
    }

    public boolean scheduleTask(BaseObject object)
    {
        try {
            plugin.scheduleTask(object, context);
            saveStatus("Scheduled", object);
            return true;
        } catch (Exception e) {
            context.put("error", e.getMessage());
            return false;
        }
    }

    /**
     * Schedule for execution all XWiki Tasks found in the passed Document object.
     */
    public boolean scheduleTasks(Document document)
    {
        try {
            Vector objects = document.getObjects(SchedulerPlugin.TASK_CLASS);
            for (Iterator iterator = objects.iterator(); iterator.hasNext();) {
                Object object = (Object) iterator.next();
                scheduleTask(object.getXWikiObject());
            }
            saveDocument(document.getDocument());
            return true;
        } catch (XWikiException e) {
            context.put("error", e.getMessage());
            return false;
        }
    }

    public boolean resumeTask(String number)
    {
        return resumeTask(context.getDoc().getObject(SchedulerPlugin.TASK_CLASS,
            Integer.valueOf(number).intValue()));
    }

    public boolean resumeTask(Object object)
    {
        return resumeTask(object.getXWikiObject());
    }

    public boolean resumeTask(BaseObject object)
    {
        try {
            plugin.resumeTask(String.valueOf(object.getNumber()));
            saveStatus("Scheduled", object);
            LOG.debug("Resume Task : " + object.getStringValue("taskName"));
            return true;
        } catch (XWikiException e) {
            context.put("error", e.getMessage());
            return false;
        }
    }

    public boolean unscheduleTask(String number)
    {
        return unscheduleTask(context.getDoc().getObject(SchedulerPlugin.TASK_CLASS,
            Integer.valueOf(number).intValue()));
    }

    public boolean unscheduleTask(Object object)
    {
        return unscheduleTask(object.getXWikiObject());
    }

    public boolean unscheduleTask(BaseObject object)
    {
        try {
            saveStatus("Unscheduled", object);
            plugin.unscheduleTask(String.valueOf(object.getNumber()));
            LOG.debug("Delete Task : " + object.getStringValue("taskName"));
            return true;
        } catch (XWikiException e) {
            context.put("error", e.getMessage());
            return false;
        }
    }

    public Date getNextFireTime(Object object)
    {
        try {
            return plugin.getNextFireTime(String.valueOf(object.getXWikiObject().getNumber()));
        } catch (SchedulerPluginException e) {
            context.put("error", e.getMessage());
            return null;
        }
    }

    public SchedulerPlugin getPlugin()
    {
        return plugin;
    }

    private void saveStatus(String status, BaseObject object)
        throws XWikiException
    {
        context.getDoc().getObject(SchedulerPlugin.TASK_CLASS,
            object.getNumber()).setStringValue("status", status);
        saveDocument(context.getDoc());
    }

    private void saveDocument(XWikiDocument document) throws XWikiException
    {
        context.getWiki().saveDocument(document, context);
    }

    public void setPlugin(SchedulerPlugin plugin)
    {
        this.plugin = plugin;
    }
}
