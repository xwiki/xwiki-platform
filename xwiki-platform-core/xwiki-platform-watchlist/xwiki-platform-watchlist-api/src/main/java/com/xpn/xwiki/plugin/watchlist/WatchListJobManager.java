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
package com.xpn.xwiki.plugin.watchlist;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.plugin.scheduler.SchedulerPlugin;
import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Manager for WatchList jobs.
 * 
 * @version $Id$
 */
public class WatchListJobManager
{
    /**
     * WatchList Job class.
     */
    public static final String WATCHLIST_JOB_CLASS = "XWiki.WatchListJobClass";

    /**
     * WatchList Job email template property name.
     */
    public static final String WATCHLIST_JOB_EMAIL_PROP = "template";

    /**
     * WatchList Job last fire time property name.
     */
    public static final String WATCHLIST_JOB_LAST_FIRE_TIME_PROP = "last_fire_time";

    /**
     * Name of the groups property in the XWiki rights class.
     */
    public static final String XWIKI_RIGHTS_CLASS_GROUPS_PROPERTY = "groups";

    /**
     * Name of the levels property in the XWiki rights class.
     */
    public static final String XWIKI_RIGHTS_CLASS_LEVELS_PROPERTY = "levels";

    /**
     * Name of the allow property in the XWiki rights class.
     */
    public static final String XWIKI_RIGHTS_CLASS_ALLOW_PROPERTY = "allow";

    /**
     * Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(WatchListJobManager.class);

    /**
     * XWiki Rights class name.
     */
    private static final String XWIKI_RIGHTS_CLASS = "XWiki.XWikiRights";

    /**
     * Name of the space where default Scheduler jobs are located.
     */
    private static final String SCHEDULER_SPACE = "Scheduler";

    /**
     * Set watchlist common documents fields.
     * 
     * @param doc document used for this job.
     * @return true if the fields have been modified, false otherwise
     */
    private boolean setWatchListCommonDocumentsFields(XWikiDocument doc)
    {
        boolean needsUpdate = false;

        if (doc.getCreatorReference() == null) {
            needsUpdate = true;
            doc.setCreator(WatchListPlugin.DEFAULT_DOC_AUTHOR);
        }
        if (doc.getAuthorReference() == null) {
            needsUpdate = true;
            doc.setAuthorReference(doc.getCreatorReference());
        }

        if (StringUtils.isBlank(doc.getParent())) {
            needsUpdate = true;
            doc.setParent("XWiki.WatchListClass");
        }

        return needsUpdate;
    }

    /**
     * Create or update the watchlist job class properties.
     * 
     * @param watchListJobClass document in which the class must be created
     * @param context the XWiki context
     * @return true if the class properties have been created or modified
     */
    private boolean initWatchListJobClassProperties(XWikiDocument watchListJobClass, XWikiContext context)
    {
        boolean needsUpdate = false;
        BaseClass bclass = watchListJobClass.getXClass();

        bclass.setName(WATCHLIST_JOB_CLASS);
        needsUpdate |= bclass.addTextField(WATCHLIST_JOB_EMAIL_PROP, "Email template to use", 30);
        needsUpdate |=
            bclass.addDateField(WATCHLIST_JOB_LAST_FIRE_TIME_PROP, "Last notifier fire time", "dd/MM/yyyy HH:mm:ss", 1);

        return needsUpdate;
    }

    /**
     * Creates the WatchList xwiki class.
     * 
     * @param context Context of the request
     * @throws XWikiException if class fields cannot be created
     */
    private void initWatchListJobClass(XWikiContext context) throws XWikiException
    {
        XWikiDocument doc;
        boolean needsUpdate = false;

        try {
            doc = context.getWiki().getDocument(WATCHLIST_JOB_CLASS, context);
        } catch (Exception e) {
            doc = new XWikiDocument();
            String[] spaceAndName = StringUtils.split(WATCHLIST_JOB_CLASS, '.');
            doc.setSpace(spaceAndName[0]);
            doc.setName(spaceAndName[1]);
            needsUpdate = true;
        }

        needsUpdate |= initWatchListJobClassProperties(doc, context);
        needsUpdate |= setWatchListCommonDocumentsFields(doc);

        if (StringUtils.isBlank(doc.getTitle())) {
            needsUpdate = true;
            doc.setTitle("XWiki WatchList Notifier Class");
        }
        if (StringUtils.isBlank(doc.getContent()) || !Syntax.XWIKI_2_0.equals(doc.getSyntax())) {
            needsUpdate = true;
            doc.setContent("{{include reference=\"XWiki.ClassSheet\" /}}");
            doc.setSyntax(Syntax.XWIKI_2_0);
        }
        if (!doc.isHidden()) {
            needsUpdate = true;
            doc.setHidden(true);
        }

        if (needsUpdate) {
            context.getWiki().saveDocument(doc, "", true, context);
        }
    }

    /**
     * Create the watchlist job object in the scheduler job document.
     * 
     * @param doc Scheduler job document
     * @param emailTemplate email template to use for the job
     * @param context the XWiki context
     * @return true if the document has been updated, false otherwise
     * @throws XWikiException if the object creation fails
     */
    private boolean createWatchListJobObject(XWikiDocument doc, String emailTemplate, XWikiContext context)
        throws XWikiException
    {
        BaseObject obj = null;
        boolean needsupdate = false;

        obj = doc.getObject(WATCHLIST_JOB_CLASS);
        if (obj == null) {
            doc.createNewObject(WATCHLIST_JOB_CLASS, context);
            needsupdate = true;
        }

        obj = doc.getObject(WATCHLIST_JOB_CLASS);

        if (StringUtils.isBlank(obj.getStringValue(WATCHLIST_JOB_EMAIL_PROP))) {
            obj.setStringValue(WATCHLIST_JOB_EMAIL_PROP, emailTemplate);
            needsupdate = true;
        }

        if (obj.getDateValue(WATCHLIST_JOB_LAST_FIRE_TIME_PROP) == null) {
            obj.setDateValue(WATCHLIST_JOB_LAST_FIRE_TIME_PROP, new Date());
            needsupdate = true;
        }

        return needsupdate;
    }

    /**
     * Create the XWiki rights object in the scheduler job document.
     * 
     * @param doc Scheduler job document
     * @param context the XWiki context
     * @return true if the document has been updated, false otherwise
     * @throws XWikiException if the object creation fails
     */
    private boolean createWatchListJobRightsObject(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        boolean needsUpdate = false;
        BaseObject editRights = doc.getObject(XWIKI_RIGHTS_CLASS, 0);
        BaseObject viewRights = doc.getObject(XWIKI_RIGHTS_CLASS, 1);

        if (editRights == null) {
            int index = doc.createNewObject(XWIKI_RIGHTS_CLASS, context);
            editRights = doc.getObject(XWIKI_RIGHTS_CLASS, index);
            editRights.setLargeStringValue(XWIKI_RIGHTS_CLASS_GROUPS_PROPERTY, "XWiki.XWikiAdminGroup");
            editRights.setStringValue(XWIKI_RIGHTS_CLASS_LEVELS_PROPERTY, "edit,delete");
            editRights.setIntValue(XWIKI_RIGHTS_CLASS_ALLOW_PROPERTY, 1);
            needsUpdate = true;
        }

        if (viewRights == null) {
            int index = doc.createNewObject(XWIKI_RIGHTS_CLASS, context);
            viewRights = doc.getObject(XWIKI_RIGHTS_CLASS, index);
            viewRights.setLargeStringValue(XWIKI_RIGHTS_CLASS_GROUPS_PROPERTY, "XWiki.XWikiAllGroup");
            viewRights.setStringValue(XWIKI_RIGHTS_CLASS_LEVELS_PROPERTY, "view");
            viewRights.setIntValue(XWIKI_RIGHTS_CLASS_ALLOW_PROPERTY, 1);
            needsUpdate = true;
        }

        if (needsUpdate) {
            // Make sure the XWikiRights class actually exists.
            context.getWiki().getRightsClass(context);
        }

        return needsUpdate;
    }

    /**
     * Creates a WatchList job in the XWiki Scheduler application (XWiki Object).
     * 
     * @param jobDocReference the reference to the document storing the job. For example
     *        {@code Scheduler.WatchListDailyNotifier}
     * @param name Job name (example: Watchlist daily notifier)
     * @param nameResource (example: platform.plugin.watchlist.job.daily)
     * @param emailTemplate email template to use for this job (example: XWiki.WatchListMessage)
     * @param cron CRON expression (see quartz CRON expressions)
     * @param context Context of the request
     * @throws XWikiException if the jobs creation fails.
     */
    private void initWatchListJob(EntityReference jobDocReference, String name, String nameResource,
        String emailTemplate, String cron, XWikiContext context) throws XWikiException
    {
        XWikiDocument doc;
        boolean needsUpdate = false;
        BaseObject job;

        try {
            doc = context.getWiki().getDocument(jobDocReference, context);

            job = doc.getXObject(SchedulerPlugin.XWIKI_JOB_CLASSREFERENCE);
            if (job == null) {
                needsUpdate = true;
                job = doc.newXObject(SchedulerPlugin.XWIKI_JOB_CLASSREFERENCE, context);
                job.setStringValue("jobName", name);
                job.setStringValue("jobClass", WatchListJob.class.getName());
                job.setStringValue("cron", cron);
                job.setStringValue("contextUser", XWikiRightService.SUPERADMIN_USER_FULLNAME);
                job.setStringValue("contextLang", "en");
                job.setStringValue("contextDatabase", "xwiki");
            }

            needsUpdate |= createWatchListJobRightsObject(doc, context);
            needsUpdate |= createWatchListJobObject(doc, emailTemplate, context);
            needsUpdate |= setWatchListCommonDocumentsFields(doc);

            if (StringUtils.isBlank(doc.getTitle())) {
                needsUpdate = true;
                doc.setTitle("$services.localization.render('" + nameResource + "')");
                doc.setSyntax(Syntax.XWIKI_2_1);
            }

            if (!doc.isHidden()) {
                needsUpdate = true;
                doc.setHidden(true);
            }

            if (needsUpdate) {
                context.getWiki().saveDocument(doc, "", true, context);
                ((SchedulerPlugin) context.getWiki().getPlugin("scheduler", context)).scheduleJob(job, context);
            }
        } catch (Exception e) {
            LOGGER.error("Cannot initialize WatchListJob", e);
        }
    }
    
    /**
     * Get the list of available jobs (list of {@link XWikiDocument}).
     * 
     * @param context Context of the request
     * @return the list of available jobs
     */
    public List<Document> getJobs(XWikiContext context)
    {
        String oriDatabase = context.getWikiId();
        List<Object> params = new ArrayList<Object>();
        List<Document> results = new ArrayList<Document>();

        try {
            context.setWikiId(context.getMainXWiki());
            params.add(WATCHLIST_JOB_CLASS);
            List<String> docNames = context.getWiki().getStore().searchDocumentsNames(
                ", BaseObject obj where doc.fullName=obj.name and obj.className=?", 0, 0, params, context);
            for (String docName : docNames) {
                XWikiDocument doc = context.getWiki().getDocument(docName, context);
                results.add(new Document(doc, context));
            }
        } catch (Exception e) {
            LOGGER.error("error getting list of available watchlist jobs", e);
        } finally {
            context.setWikiId(oriDatabase);
        }

        return results;
    }

    /**
     * Create default WatchList jobs in the wiki.
     * 
     * @param context Context of the request
     * @throws XWikiException When a job creation fails
     */
    public void init(XWikiContext context) throws XWikiException
    {
        initWatchListJobClass(context);

        initWatchListJob(new LocalDocumentReference(SCHEDULER_SPACE, "WatchListHourlyNotifier"),
            "WatchList hourly notifier", "watchlist.job.hourly",
            WatchListNotifier.DEFAULT_EMAIL_TEMPLATE, "0 0 * * * ?", context);
        initWatchListJob(new LocalDocumentReference(SCHEDULER_SPACE, "WatchListDailyNotifier"),
            "WatchList daily notifier", "watchlist.job.daily",
            WatchListNotifier.DEFAULT_EMAIL_TEMPLATE, "0 0 0 * * ?", context);
        initWatchListJob(new LocalDocumentReference(SCHEDULER_SPACE, "WatchListWeeklyNotifier"),
            "WatchList weekly notifier", "watchlist.job.weekly",
            WatchListNotifier.DEFAULT_EMAIL_TEMPLATE, "0 0 0 ? * MON", context);
    }
}
