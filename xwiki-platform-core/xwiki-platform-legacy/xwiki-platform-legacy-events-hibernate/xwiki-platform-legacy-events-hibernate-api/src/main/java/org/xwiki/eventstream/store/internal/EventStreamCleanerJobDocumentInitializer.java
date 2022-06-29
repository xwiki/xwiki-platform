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
package org.xwiki.eventstream.store.internal;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import org.apache.commons.lang3.StringUtils;
import com.xpn.xwiki.plugin.scheduler.SchedulerPlugin;
import org.slf4j.Logger;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.bridge.event.WikiReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.internal.document.DefaultDocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.user.SuperAdminUserReference;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Manager for the event stream cleaning feature. The cleaning consist in deleting old events to prevent infinite
 * growth of the event stream table in the database.
 *
 * @version $Id$
 */
@Component
@Singleton
@Named(EventStreamCleanerJobDocumentInitializer.CLEANER_JOB_NAME)
public class EventStreamCleanerJobDocumentInitializer extends AbstractEventListener
{
    /**
     * Document holding the cleaner Job.
     */
    public static final String CLEANER_JOB_NAME = "Event Stream cleaner";

    /**
     * Document holding the cleaner Job.
     */
    private static final LocalDocumentReference CLEANER_JOB_REF = new LocalDocumentReference("Scheduler",
            "EventStreamCleanerJob");

    /**
     * Document holding the cleaner Job.
     */
    private static final String CLEANER_JOB_CRON = "0 0 0 ? * SUN";

    /**
     * Main wiki.
     */
    private static final String MAIN_WIKI = "xwiki";

    /**
     * XWiki space.
     */
    private static final String XWIKI = "XWiki";

    /**
     * XWiki Default Admin account.
     */
    private static final DocumentReference SUPER_ADMIN = new DocumentReference(MAIN_WIKI, XWIKI, "superadmin");

    /**
     * XWiki Rights class name.
     */
    private static final LocalDocumentReference XWIKI_RIGHTS_CLASS = new LocalDocumentReference(XWIKI, "XWikiRights");

    @Inject
    private Logger logger;

    @Inject
    private LegacyEventStreamStoreConfiguration configuration;

    @Inject
    private Provider<XWikiContext> contextProvider;

    /**
     * Default constructor.
     */
    public EventStreamCleanerJobDocumentInitializer()
    {
        super(CLEANER_JOB_NAME, new WikiReadyEvent(), new ApplicationReadyEvent());
    }

    /**
     * Set cleaner common documents fields.
     *
     * @param doc document to modify
     * @return true if the fields have been modified, false otherwise
     */
    private boolean setCleanerCommonDocumentsFields(XWikiDocument doc)
    {
        boolean needsUpdate = false;

        DefaultDocumentAuthors authors = new DefaultDocumentAuthors(doc);
        if (authors.getEffectiveMetadataAuthor() == null) {
            needsUpdate = true;
            authors.setEffectiveMetadataAuthor(SuperAdminUserReference.INSTANCE);
        }
        if (authors.getCreator() == null) {
            needsUpdate = true;
            authors.setCreator(SuperAdminUserReference.INSTANCE);
        }

        if (doc.getParentReference() == null) {
            needsUpdate = true;
            doc.setParentReference(new LocalDocumentReference(CLEANER_JOB_REF.getParent().getName(), "WebHome"));
        }

        return needsUpdate;
    }

    /**
     * Create the XWiki rights object in the cleaner job document.
     *
     * @param doc Cleaner job document
     * @param context the XWiki context
     * @return true if the document has been updated, false otherwise
     * @throws XWikiException if the object creation fails
     */
    private boolean createWatchListJobRightsObject(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        BaseObject rights = doc.getXObject(XWIKI_RIGHTS_CLASS);
        if (rights == null) {
            int index = doc.createXObject(XWIKI_RIGHTS_CLASS, context);
            rights = doc.getXObject(XWIKI_RIGHTS_CLASS, index);
            rights.setLargeStringValue("groups", "XWiki.XWikiAdminGroup");
            rights.setStringValue("levels", "edit,delete");
            rights.setIntValue("allow", 1);
            return true;
        }

        return false;
    }

    /**
     * Create the cleaner job document in the wiki.
     */
    private void initCleanerJob()
    {
        XWikiContext context = contextProvider.get();

        try {
            XWikiDocument doc = context.getWiki().getDocument(CLEANER_JOB_REF, context);
            boolean needsUpdate = setCleanerCommonDocumentsFields(doc);

            BaseObject job = doc.getXObject(SchedulerPlugin.XWIKI_JOB_CLASSREFERENCE);
            if (job == null) {
                needsUpdate = true;
                job = doc.newXObject(SchedulerPlugin.XWIKI_JOB_CLASSREFERENCE, context);
                job.setStringValue("jobName", CLEANER_JOB_NAME);
                job.setStringValue("jobClass", EventStreamCleanerJob.class.getName());
                job.setStringValue("cron", CLEANER_JOB_CRON);
                job.setStringValue("contextUser", "xwiki:XWiki.superadmin");
                job.setStringValue("contextLang", "en");
                job.setStringValue("contextDatabase", MAIN_WIKI);
            }

            needsUpdate |= createWatchListJobRightsObject(doc, context);

            if (StringUtils.isBlank(doc.getContent())) {
                needsUpdate = true;
                doc.setContent("{{include reference=\"XWiki.SchedulerJobSheet\"/}}");
                doc.setSyntax(Syntax.XWIKI_2_0);
            }

            if (needsUpdate) {
                context.getWiki().saveDocument(doc, "", true, context);
                ((SchedulerPlugin) context.getWiki().getPlugin("scheduler", context)).scheduleJob(job, context);
            }
        } catch (Exception e) {
            logger.error("Cannot initialize EventStreamCleanerJob", e);
        }
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (configuration.getNumberOfDaysToKeep() > 0) {
            initCleanerJob();
        }
    }
}
