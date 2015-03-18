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
package org.xwiki.watchlist.internal.documents;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.watchlist.internal.DefaultWatchListNotifier;
import org.xwiki.watchlist.internal.job.WatchListJob;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.AbstractMandatoryDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.plugin.scheduler.JobState;
import com.xpn.xwiki.plugin.scheduler.SchedulerPlugin;
import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Abstract Document initializer for the default {@value WatchListJobClassDocumentInitializer#DOCUMENT_FULL_NAME}.
 * documents.
 * 
 * @version $Id$
 */
public abstract class AbstractWatchListJobDocumentInitializer extends AbstractMandatoryDocumentInitializer
{
    /**
     * Name of the groups property in the XWiki rights class.
     */
    protected static final String XWIKI_RIGHTS_CLASS_GROUPS_PROPERTY = "groups";

    /**
     * Name of the levels property in the XWiki rights class.
     */
    protected static final String XWIKI_RIGHTS_CLASS_LEVELS_PROPERTY = "levels";

    /**
     * Name of the allow property in the XWiki rights class.
     */
    protected static final String XWIKI_RIGHTS_CLASS_ALLOW_PROPERTY = "allow";

    /**
     * Name of the space where default Scheduler jobs are located.
     */
    protected static final String SCHEDULER_SPACE = "Scheduler";

    /**
     * Used to access the XWiki model.
     */
    @Inject
    private Provider<XWikiContext> contextProvider;

    /**
     * Logging framework.
     */
    @Inject
    private Logger logger;

    /**
     * @param spaceName the space name of the document
     * @param documentName the document name of the document
     */
    public AbstractWatchListJobDocumentInitializer(String spaceName, String documentName)
    {
        super(spaceName, documentName);
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        XWikiContext context = contextProvider.get();
        boolean needsUpdate = false;

        try {
            // Create objects.
            needsUpdate |= createSchedulerJobObject(document, getJobName(), getCron(), context);

            needsUpdate |= createWatchListJobRightsObject(document, context);

            needsUpdate |= createWatchListJobObject(document, getMessageTemplateDocument(), context);

            // Set basic document fields.
            needsUpdate |= setDocumentFields(document, getDocumentTitle());
        } catch (Exception e) {
            logger.error("Failed to initialize document [{}]", getDocumentReference(), e);
        }

        return needsUpdate;
    }

    /**
     * @return the cron expression used to run the Scheduler job
     */
    protected abstract String getCron();

    /**
     * @return the name of the Scheduler job
     */
    protected abstract String getJobName();

    /**
     * @return the document title
     */
    protected String getDocumentTitle()
    {
        String title = String.format("$services.localization.render('%s')", getDocumentTitleTranslationKey());
        return title;
    }

    /**
     * @return the translation key to be used in the document's title
     */
    protected abstract String getDocumentTitleTranslationKey();

    /**
     * @return the name of the document where the message template for this job is located
     */
    protected String getMessageTemplateDocument()
    {
        return DefaultWatchListNotifier.DEFAULT_EMAIL_TEMPLATE;
    }

    /**
     * @param doc the document to update
     * @param jobName the scheduler job name
     * @param cron the cron expression
     * @param context the XWiki context
     * @return true if the document has been updated, false otherwise
     * @throws XWikiException
     */
    private static boolean createSchedulerJobObject(XWikiDocument document, String jobName, String cron,
        XWikiContext context) throws XWikiException
    {
        boolean needsUpdate = false;

        BaseObject job = document.getXObject(SchedulerPlugin.XWIKI_JOB_CLASSREFERENCE);
        if (job == null) {
            needsUpdate = true;
            job = document.newXObject(SchedulerPlugin.XWIKI_JOB_CLASSREFERENCE, context);
            job.setStringValue("jobName", jobName);
            job.setStringValue("jobClass", WatchListJob.class.getName());
            job.setStringValue("cron", cron);
            job.setStringValue("contextUser", XWikiRightService.SUPERADMIN_USER_FULLNAME);
            job.setStringValue("contextLang", "en");
            job.setStringValue("contextDatabase", "xwiki");
            job.setStringValue("status", JobState.STATE_NORMAL);
        }

        return needsUpdate;
    }

    /**
     * Create the XWiki rights object in the scheduler job document.
     * 
     * @param doc the document to update
     * @param context the XWiki context
     * @return true if the document has been updated, false otherwise
     * @throws XWikiException if the object creation fails
     */
    private static boolean createWatchListJobRightsObject(XWikiDocument doc, XWikiContext context)
        throws XWikiException
    {
        boolean needsUpdate = false;
        DocumentReference righsClassReference = context.getWiki().getRightsClass(context).getDocumentReference();

        BaseObject editRights = doc.getXObject(righsClassReference, 0);
        BaseObject viewRights = doc.getXObject(righsClassReference, 1);

        if (editRights == null) {
            editRights = doc.newXObject(righsClassReference, context);
            editRights.setLargeStringValue(XWIKI_RIGHTS_CLASS_GROUPS_PROPERTY, "XWiki.XWikiAdminGroup");
            editRights.setStringValue(XWIKI_RIGHTS_CLASS_LEVELS_PROPERTY, "edit,delete");
            editRights.setIntValue(XWIKI_RIGHTS_CLASS_ALLOW_PROPERTY, 1);
            needsUpdate = true;
        }

        if (viewRights == null) {
            viewRights = doc.newXObject(righsClassReference, context);
            viewRights.setLargeStringValue(XWIKI_RIGHTS_CLASS_GROUPS_PROPERTY, "XWiki.XWikiAllGroup");
            viewRights.setStringValue(XWIKI_RIGHTS_CLASS_LEVELS_PROPERTY, "view");
            viewRights.setIntValue(XWIKI_RIGHTS_CLASS_ALLOW_PROPERTY, 1);
            needsUpdate = true;
        }

        return needsUpdate;
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
    private static boolean createWatchListJobObject(XWikiDocument doc, String emailTemplate, XWikiContext context)
        throws XWikiException
    {
        BaseObject obj = null;
        boolean needsupdate = false;

        obj = doc.getXObject(WatchListJobClassDocumentInitializer.DOCUMENT_REFERENCE);
        if (obj == null) {
            obj = doc.newXObject(WatchListJobClassDocumentInitializer.DOCUMENT_REFERENCE, context);
            needsupdate = true;
        }

        if (StringUtils.isBlank(obj.getStringValue(WatchListJobClassDocumentInitializer.TEMPLATE_FIELD))) {
            obj.setStringValue(WatchListJobClassDocumentInitializer.TEMPLATE_FIELD, emailTemplate);
            needsupdate = true;
        }

        if (obj.getDateValue(WatchListJobClassDocumentInitializer.LAST_FIRE_TIME_FIELD) == null) {
            obj.setDateValue(WatchListJobClassDocumentInitializer.LAST_FIRE_TIME_FIELD, new Date());
            needsupdate = true;
        }

        return needsupdate;
    }
}
