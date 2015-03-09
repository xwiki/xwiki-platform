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
package org.xwiki.watchlist.internal.job;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.watchlist.internal.DefaultWatchListStore;
import org.xwiki.watchlist.internal.WatchListEventMatcher;
import org.xwiki.watchlist.internal.api.WatchList;
import org.xwiki.watchlist.internal.api.WatchListEvent;
import org.xwiki.watchlist.internal.api.WatchedElementType;
import org.xwiki.watchlist.internal.documents.WatchListJobClassDocumentInitializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.plugin.scheduler.AbstractJob;
import com.xpn.xwiki.web.Utils;

/**
 * WatchList abstract implementation of Quartz's Job.
 * 
 * @version $Id$
 */
public class WatchListJob extends AbstractJob implements Job
{
    /**
     * Wiki page which contains the default watchlist email template.
     */
    public static final String DEFAULT_EMAIL_TEMPLATE = "XWiki.WatchListMessage";

    /**
     * Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(WatchListJob.class);

    /**
     * Scheduler Job XObject.
     */
    private BaseObject schedulerJobObject;

    /**
     * Watchlist Job XObject.
     */
    private BaseObject watchListJobObject;

    /**
     * XWiki context.
     */
    private XWikiContext context;

    /**
     * Caller component.
     */
    private WatchList watchlist;

    /**
     * Sets objects required by the Job : XWiki, XWikiContext, WatchListPlugin, etc.
     * 
     * @param jobContext Context of the request
     * @throws Exception when the init of components fails
     */
    public void init(JobExecutionContext jobContext) throws Exception
    {
        JobDataMap data = jobContext.getJobDetail().getJobDataMap();
        // clone the context to make sure we have a new one per run
        this.context = ((XWikiContext) data.get("context")).clone();
        // clean up the database connections
        this.context.getWiki().getStore().cleanUp(this.context);
        this.watchlist = Utils.getComponent(WatchList.class);
        this.schedulerJobObject = (BaseObject) data.get("xjob");
        this.watchListJobObject =
            this.context.getWiki().getDocument(this.schedulerJobObject.getDocumentReference(), this.context)
                .getXObject(WatchListJobClassDocumentInitializer.DOCUMENT_REFERENCE);
        initializeComponents(this.context);
    }

    /**
     * Initialize container context.
     * 
     * @param xcontext the XWiki context
     * @throws Exception if the execution context initialization fails
     */
    protected void initializeComponents(XWikiContext xcontext) throws Exception
    {
        try {
            ExecutionContextManager ecim = Utils.getComponent(ExecutionContextManager.class);
            ExecutionContext econtext = new ExecutionContext();

            // Bridge with old XWiki Context, required for old code.
            xcontext.declareInExecutionContext(econtext);

            ecim.initialize(econtext);
        } catch (Exception e) {
            throw new Exception("Failed to initialize Execution Context", e);
        }
    }

    /**
     * Clean the container context.
     */
    protected void cleanupComponents()
    {
        Execution execution = Utils.getComponent(Execution.class);

        // We must ensure we clean the ThreadLocal variables located in the Execution component as otherwise we will
        // have a potential memory leak.
        execution.removeContext();
    }

    /**
     * @return ID of the job
     */
    public String getId()
    {
        String className = this.getClass().getName();
        return className.substring(className.lastIndexOf(".") + 1);
    }

    /**
     * @return the previous job fire time
     */
    private Date getPreviousFireTime()
    {
        return this.watchListJobObject.getDateValue(WatchListJobClassDocumentInitializer.LAST_FIRE_TIME_FIELD);
    }

    /**
     * Save the date of the execution in the watchlist job object.
     * 
     * @throws XWikiException if the job document can't be retrieved or if the save action fails
     */
    private void setPreviousFireTime() throws XWikiException
    {
        XWikiDocument doc =
            this.context.getWiki().getDocument(this.watchListJobObject.getDocumentReference(), this.context);

        this.watchListJobObject.setDateValue(WatchListJobClassDocumentInitializer.LAST_FIRE_TIME_FIELD, new Date());

        // Prevent version changes
        doc.setMetaDataDirty(false);
        doc.setContentDirty(false);

        this.context.getWiki().saveDocument(doc, "Updated last fire time", true, this.context);
    }

    /**
     * @param userWiki wiki from which the user comes from
     * @return the name of the page that should be used as email template for this job
     */
    private String getEmailTemplate(String userWiki)
    {
        String fullName = this.watchListJobObject.getStringValue(WatchListJobClassDocumentInitializer.TEMPLATE_FIELD);
        String prefixedFullName;

        if (fullName.contains(DefaultWatchListStore.WIKI_SPACE_SEP)) {
            // If the configured template is already an absolute reference it's meant to force the template.
            prefixedFullName = fullName;
        } else {
            prefixedFullName = userWiki + DefaultWatchListStore.WIKI_SPACE_SEP + fullName;
            if (this.context.getWiki().exists(prefixedFullName, this.context)) {
                // If the configured template exists in the user wiki, use it.
                return prefixedFullName;
            }
        }

        return fullName;
    }

    /**
     * Retrieves all the XWiki.XWikiUsers who have requested to be notified by changes, i.e. who have an Object of class
     * WATCHLIST_CLASS attached AND who have chosen the current job for their notifications.
     * 
     * @return a collection of document names pointing to the XWikiUsers wishing to get notified.
     */
    private Collection<String> getSubscribers()
    {
        return this.watchlist.getStore().getSubscribers(this.schedulerJobObject.getName());
    }

    /**
     * @return true if this job has subscribers, false otherwise
     */
    private boolean hasSubscribers()
    {
        Collection<String> subscribers = getSubscribers();

        return !subscribers.isEmpty();
    }

    /**
     * Method called from the scheduler.
     * 
     * @param jobContext Context of the request
     * @throws JobExecutionException if the job execution fails.
     */
    @Override
    public void executeJob(JobExecutionContext jobContext) throws JobExecutionException
    {
        try {
            init(jobContext);

            if (this.watchListJobObject == null) {
                return;
            }

            Collection<String> subscribers = getSubscribers();

            // Stop here if nobody is interested.
            if (!hasSubscribers()) {
                return;
            }

            // Determine what happened since the last execution for everybody.
            Date previousFireTime = getPreviousFireTime();
            WatchListEventMatcher eventMatcher = new WatchListEventMatcher(previousFireTime, this.context);
            setPreviousFireTime();

            // Stop here if nothing happened in the meantime.
            if (eventMatcher.getEventNumber() == 0) {
                return;
            }

            // Notify all interested subscribers, one at a time.
            for (String subscriber : subscribers) {
                try {
                    Collection<String> wikis =
                        this.watchlist.getStore().getWatchedElements(subscriber, WatchedElementType.WIKI);
                    Collection<String> spaces =
                        this.watchlist.getStore().getWatchedElements(subscriber, WatchedElementType.SPACE);
                    Collection<String> documents =
                        this.watchlist.getStore().getWatchedElements(subscriber, WatchedElementType.DOCUMENT);
                    Collection<String> users =
                        this.watchlist.getStore().getWatchedElements(subscriber, WatchedElementType.USER);

                    // Determine what happened since the last execution on the watched elements of the current
                    // subscriber only.
                    List<WatchListEvent> matchingEvents =
                        eventMatcher.getMatchingEvents(wikis, spaces, documents, users, subscriber, this.context);
                    String userWiki = StringUtils.substringBefore(subscriber, DefaultWatchListStore.WIKI_SPACE_SEP);

                    // If events have occurred on at least one element watched by the user, send the email
                    if (matchingEvents.size() > 0) {
                        this.watchlist.getNotifier().sendNotification(subscriber, matchingEvents,
                            getEmailTemplate(userWiki), previousFireTime);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to send watchlist notification to user [{}]", subscriber, e);
                }
            }
        } catch (Exception e) {
            // We're in a job, we don't throw exceptions
            LOGGER.error("Exception while running job", e);
        } finally {
            this.context.getWiki().getStore().cleanUp(this.context);
            cleanupComponents();
        }
    }
}
