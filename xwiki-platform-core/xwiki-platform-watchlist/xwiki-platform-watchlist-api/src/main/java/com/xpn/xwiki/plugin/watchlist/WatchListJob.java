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

import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletContainerException;
import org.xwiki.container.servlet.ServletContainerInitializer;
import org.xwiki.context.Execution;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.plugin.scheduler.AbstractJob;
import com.xpn.xwiki.plugin.watchlist.WatchListStore.ElementType;
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
    private static final Log LOG = LogFactory.getLog(WatchListPlugin.class);

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
     * Caller plugin.
     */
    private WatchListPlugin plugin;

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
        this.context = (XWikiContext) ((XWikiContext) data.get("context")).clone();
        // clean up the database connections
        this.context.getWiki().getStore().cleanUp(this.context);
        this.plugin = (WatchListPlugin) this.context.getWiki().getPlugin(WatchListPlugin.ID, this.context);
        this.schedulerJobObject = (BaseObject) data.get("xjob");
        this.watchListJobObject =
            this.context.getWiki().getDocument(this.schedulerJobObject.getName(), this.context)
                .getObject(WatchListJobManager.WATCHLIST_JOB_CLASS);
        initializeComponents(this.context);
    }

    /**
     * Initialize container context.
     * 
     * @param context The XWiki context.
     * @throws ServletException If the container initialization fails.
     */
    protected void initializeComponents(XWikiContext context) throws ServletException
    {
        // Initialize the Container fields (request, response, session).
        // Note that this is a bridge between the old core and the component architecture.
        // In the new component architecture we use ThreadLocal to transport the request,
        // response and session to components which require them.
        // In the future this Servlet will be replaced by the XWikiPlexusServlet Servlet.
        ServletContainerInitializer containerInitializer = Utils.getComponent(ServletContainerInitializer.class);

        try {
            containerInitializer.initializeRequest(context.getRequest().getHttpServletRequest(), context);
            containerInitializer.initializeResponse(context.getResponse().getHttpServletResponse());
            containerInitializer.initializeSession(context.getRequest().getHttpServletRequest());
        } catch (ServletContainerException e) {
            throw new ServletException("Failed to initialize Request/Response or Session", e);
        }
    }

    /**
     * Clean the container context.
     */
    protected void cleanupComponents()
    {
        Container container = Utils.getComponent(Container.class);
        Execution execution = Utils.getComponent(Execution.class);

        // We must ensure we clean the ThreadLocal variables located in the Container and Execution
        // components as otherwise we will have a potential memory leak.
        container.removeRequest();
        container.removeResponse();
        container.removeSession();
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
        return this.watchListJobObject.getDateValue(WatchListJobManager.WATCHLIST_JOB_LAST_FIRE_TIME_PROP);
    }

    /**
     * Save the date of the execution in the watchlist job object.
     * 
     * @throws XWikiException if the job document can't be retrieved or if the save action fails
     */
    private void setPreviousFireTime() throws XWikiException
    {
        XWikiDocument doc = this.context.getWiki().getDocument(this.watchListJobObject.getName(), this.context);
        this.watchListJobObject.setDateValue(WatchListJobManager.WATCHLIST_JOB_LAST_FIRE_TIME_PROP, new Date());
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
        String fullName = this.watchListJobObject.getStringValue(WatchListJobManager.WATCHLIST_JOB_EMAIL_PROP);
        String prefixedFullName;

        if (fullName.contains(WatchListStore.WIKI_SPACE_SEP)) {
            // If the configured template is already an absolute reference it's meant to force the template.
            prefixedFullName = fullName;
        } else {
            prefixedFullName = userWiki + WatchListStore.WIKI_SPACE_SEP + fullName;
            if (this.context.getWiki().exists(prefixedFullName, this.context)) {
                // If the configured template exists in the user wiki, use it.
                return prefixedFullName;
            }
        }

        return fullName;
    }

    /**
     * Retrieves all the XWiki.XWikiUsers who have requested to be notified by changes, i.e. who have an Object of class
     * WATCHLIST_CLASS attached AND who have choosen the current job for their notifications.
     * 
     * @return a collection of document names pointing to the XWikiUsers wishing to get notified.
     */
    private List<String> getSubscribers()
    {
        return this.plugin.getStore().getSubscribersForJob(this.schedulerJobObject.getName());
    }

    /**
     * @return true if this job has subscribers, false otherwise
     */
    private boolean hasSubscribers()
    {
        List<String> subscribers = getSubscribers();

        if (subscribers.isEmpty()) {
            return false;
        }

        return true;
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

            List<String> subscribers = getSubscribers();
            Date previousFireTime = getPreviousFireTime();
            WatchListEventMatcher eventMatcher = new WatchListEventMatcher(previousFireTime, this.context);
            setPreviousFireTime();

            if (!hasSubscribers()) {
                return;
            }

            if (eventMatcher.getEventNumber() == 0) {
                return;
            }

            for (String subscriber : subscribers) {
                try {
                    List<String> wikis =
                        this.plugin.getStore().getWatchedElements(subscriber, ElementType.WIKI, this.context);
                    List<String> spaces =
                        this.plugin.getStore().getWatchedElements(subscriber, ElementType.SPACE, this.context);
                    List<String> documents =
                        this.plugin.getStore().getWatchedElements(subscriber, ElementType.DOCUMENT, this.context);
                    List<String> users =
                        this.plugin.getStore().getWatchedElements(subscriber, ElementType.USER, this.context);
                    List<WatchListEvent> matchingEvents =
                        eventMatcher.getMatchingEvents(wikis, spaces, documents, users, subscriber, this.context);
                    String userWiki = StringUtils.substringBefore(subscriber, WatchListStore.WIKI_SPACE_SEP);

                    // If events have occurred on at least one element watched by the user, send the email
                    if (matchingEvents.size() > 0) {
                        this.plugin.getNotifier().sendEmailNotification(subscriber, matchingEvents,
                            getEmailTemplate(userWiki), previousFireTime, this.context);
                    }
                } catch (Exception e) {
                    LOG.error("Failed to send watchlist notification to user [" + subscriber + "]", e);
                }
            }
        } catch (Exception e) {
            // We're in a job, we don't throw exceptions
            LOG.error("Exception while running job", e);
        } finally {
            this.context.getWiki().getStore().cleanUp(this.context);
            cleanupComponents();
        }
    }
}
