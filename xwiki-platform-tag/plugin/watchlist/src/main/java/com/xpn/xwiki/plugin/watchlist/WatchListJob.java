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

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.Object;
import com.xpn.xwiki.api.Context;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.plugin.mailsender.MailSenderPlugin;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Months;
import org.joda.time.Weeks;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.xwiki.context.Execution;
import org.xwiki.container.servlet.ServletContainerInitializer;
import org.xwiki.container.servlet.ServletContainerException;
import org.xwiki.container.Container;

import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * WatchList implementation of Quartz's Job
 *
 * This class behaves as follow when the execute method is called: 1) it selects the persons who have requested a
 * notification matching the frequency of this Job and returns Set1 2) if Set1 is not void, then it selects the
 * documents that have changed during the last period and stores them in Set2 3) if Set2 is not void, then for each
 * person Pi in Set1, it intersects the following sets: Set2, matching criteria of Pi, documents that can be read by Pi
 * 4) it sends an email to Pi
 */
public class WatchListJob implements Job
{
    private static final Log LOG = LogFactory.getLog(WatchListPlugin.class);

    protected BaseObject xjob = null;

    protected XWikiContext context = null;

    protected WatchListPlugin plugin = null;

    protected int interval = 0;

    protected String jobMailTemplate;

    protected String logprefix;

    protected Execution execution = null;

    /**
     * Sets objects required by the Job : XWiki, XWikiContext, WatchListPlugin, etc
     *
     * @param jobContext Context of the request
     */
    public void init(JobExecutionContext jobContext) throws Exception
    {
        JobDataMap data = jobContext.getJobDetail().getJobDataMap();
        // clone the context to make sure we have a new one per run
        context = (XWikiContext) ((XWikiContext) data.get("context")).clone();
        // clean up the database connections
        context.getWiki().getStore().cleanUp(context);
        plugin = (WatchListPlugin) context.getWiki().getPlugin(WatchListPlugin.ID, context);
        xjob = (BaseObject) data.get("xjob");
        jobMailTemplate = xjob.getLargeStringValue("script").trim();
        // retreive the interval from job name (1=hourly, 2=daily, etc)
        interval = Integer.parseInt(xjob.getName().substring(xjob.getName().length() - 1));
        logprefix = "WatchList job " + context.getDatabase() + ":" + xjob.getName() + " ";
    }

    protected void initializeContainerComponent(XWikiContext context) throws ServletException
    {
        // Initialize the Container fields (request, response, session).
        // Note that this is a bridge between the old core and the component architecture.
        // In the new component architecture we use ThreadLocal to transport the request,
        // response and session to components which require them.
        // In the future this Servlet will be replaced by the XWikiPlexusServlet Servlet.
        ServletContainerInitializer containerInitializer =
            (ServletContainerInitializer) Utils.getComponent(ServletContainerInitializer.class);

        try {
            containerInitializer.initializeRequest(context.getRequest().getHttpServletRequest(),
                context);
            containerInitializer.initializeResponse(context.getResponse()
                .getHttpServletResponse());
            containerInitializer.initializeSession(context.getRequest().getHttpServletRequest());
        } catch (ServletContainerException e) {
            throw new ServletException("Failed to initialize Request/Response or Session", e);
        }
    }

    protected void cleanupComponents()
    {
        Container container = (Container) Utils.getComponent(Container.class);
        Execution execution = (Execution) Utils.getComponent(Execution.class);

        // We must ensure we clean the ThreadLocal variables located in the Container and Execution
        // components as otherwise we will have a potential memory leak.
        container.removeRequest();
        container.removeResponse();
        container.removeSession();
        execution.removeContext();
    }

    /**
     * Method called from the scheduler
     *
     * @param jobContext Context of the request
     */
    public void execute(JobExecutionContext jobContext) throws JobExecutionException
    {
        try {
            try {
                // Set required objects
                init(jobContext);
                // init components (needed for velocity to work)
                initializeContainerComponent(this.context);
            } catch (Exception e) {
                LOG.error(logprefix + "exception while initializing watchlist job", e);
            }

            // Retreive notification subscribers (all wikis)
            Collection subscribers = retrieveNotificationSubscribers();
            if (subscribers != null && subscribers.size() > 0) {
                // Retreive updated documents
                List updatedDocuments = retrieveUpdatedDocuments();
                LOG.info(logprefix + "updatedDocumentsNumber : [" + updatedDocuments.size() + "]");
                Iterator it = subscribers.iterator();
                while (it.hasNext()) {
                    try {
                        // Retreive WatchList Object for each subscribers
                        Document subscriber = new Document(
                            context.getWiki().getDocument((String) it.next(), context), context);
                        LOG.info(logprefix + "checkingDocumentsForUser " +
                            subscriber.getFullName());
                        Object userObj = subscriber.getObject("XWiki.XWikiUsers");
                        Object notificationCriteria =
                            subscriber.getObject(WatchListPlugin.WATCHLIST_CLASS);
                        if (userObj == null || notificationCriteria == null) {
                            continue;
                        }
                        // Filter documents according to lists in the WatchList Object
                        List matchingDocuments =
                            filter(updatedDocuments, notificationCriteria,
                                subscriber.getFullName());

                        // If there are matching documents, sends the notification
                        if (matchingDocuments.size() > 0) {
                            LOG.info(logprefix + "matchingDocumentsForUser " +
                                subscriber.getFullName() + ": [" + matchingDocuments.size() + "]");
                            try {
                                sendNotificationMessage(subscriber, matchingDocuments);
                            } catch (Exception e) {
                                if (LOG.isErrorEnabled()) {
                                    LOG.error(logprefix + "exception while sending email to " +
                                        subscriber.getValue("email") + " with " +
                                        matchingDocuments.size() + " matching documents", e);
                                }
                            }
                        }
                    } catch (XWikiException e) {
                        if (LOG.isErrorEnabled()) {
                            LOG.error("Exception while running job for one user", e);
                        }
                    }
                }
            }
        } catch (XWikiException e) {
            // We're in a job, don't throw it
            if (LOG.isErrorEnabled()) {
                LOG.error("Exception while running job", e);
            }
        } finally {
            // give back execution context
            try {
                cleanupComponents();
            } catch (Throwable e) {
                // Don't throw anything to be sure that the finally will continue
            }
            // cleanup use database connections
            context.getWiki().getStore().cleanUp(context);
        }
    }

    /**
     * Filters updated documents against users' criteria
     *
     * @param updatedDocuments the list of updated documents
     * @param notificationCriteria the BaseObject representing the user notification criteria
     * @param subscriber the user to be notified
     * @return a filtered list of documents to be sent to the user
     */
    private List filter(List updatedDocuments,
        Object notificationCriteria, String subscriber) throws XWikiException
    {
        String spaceCriterion = "";
        // Since BaseObject.getProperty("foo") may return null we need those tests to avoid NPEs.
        if (notificationCriteria.getProperty("spaces") != null) {
            spaceCriterion = (String) notificationCriteria.getProperty("spaces").getValue();
        }
        String documentCriterion = "";
        if (notificationCriteria.getProperty("documents") != null) {
            documentCriterion = (String) notificationCriteria.getProperty("documents").getValue();
        }
        String query = "";
        if (notificationCriteria.getProperty("query") != null) {
            query = (String) notificationCriteria.getProperty("query").getValue();
        }

        List watchedDocuments = new ArrayList();
        if (spaceCriterion.length() == 0 && documentCriterion.length() == 0
            && query.length() == 0)
        {
            return new ArrayList();
        }

        List filteredDocumentList = new ArrayList();
        String[] watchedSpaces = spaceCriterion.split(",");
        String[] docArray = documentCriterion.split(",");

        for (int i = 0; i < docArray.length; i++) {
            watchedDocuments.add(docArray[i]);
        }

        if (query.length() > 0) {
            List queryDocuments =
                plugin.globalSearchDocuments(query, 0, 0, new ArrayList(), context);
            watchedDocuments.addAll(queryDocuments);
        }

        Iterator updatedDocumentsIt = updatedDocuments.iterator();
        while (updatedDocumentsIt.hasNext()) {
            String updatedDocumentName = (String) updatedDocumentsIt.next();
            Document updatedDocument =
                new Document(context.getWiki().getDocument(updatedDocumentName, context), context);
            String updatedDocumentSpace =
                updatedDocument.getWiki() + ":" + updatedDocument.getSpace();
            boolean documentAdded = false;

            if (watchedSpaces != null) {
                for (int i = 0; i < watchedSpaces.length; i++) {
                    if (updatedDocumentSpace.equals(watchedSpaces[i])) {
                        String origDatabase = context.getDatabase();
                        try {
                            context.setDatabase(updatedDocument.getWiki());
                            if (context.getWiki().getRightService()
                                .hasAccessLevel("view", subscriber, updatedDocumentName, context))
                            {
                                filteredDocumentList.add(updatedDocumentName);
                                documentAdded = true;
                                break;
                            } else {
                                if (LOG.isInfoEnabled()) {
                                    LOG.info("Matching doc " + updatedDocumentName + " for subscriber " + subscriber +
                                        " missing rights in context " + context.getDatabase());
                                }
                            }
                        } finally {
                            context.setDatabase(origDatabase);
                        }
                    }
                }
            }

            // loop over the watched documents if the current document has not
            // been included in the filteredList already
            if (!documentAdded) {
                Iterator watchedDocumentIt = watchedDocuments.iterator();
                while (watchedDocumentIt.hasNext()) {
                    String watchedDocumentName = (String) watchedDocumentIt.next();
                    if (updatedDocumentName.equals(watchedDocumentName)) {
                        String origDatabase = context.getDatabase();
                        try {
                            context.setDatabase(updatedDocument.getWiki());
                            if (context.getWiki().getRightService()
                                .hasAccessLevel("view", subscriber, updatedDocumentName, context))
                            {
                                filteredDocumentList.add(updatedDocumentName);
                                documentAdded = true;
                                break;
                            } else {
                                if (LOG.isInfoEnabled()) {
                                    LOG.info("Matching doc " + updatedDocumentName + " for subscriber " + subscriber +
                                        " missing rights in context " + context.getDatabase());
                                }
                            }
                        } finally {
                            context.setDatabase(origDatabase);
                        }
                    }
                }
            }
        }

        return filteredDocumentList;
    }

    /**
     * Retrieves all the XWiki.XWikiUsers who have requested to be notified by changes, i.e. who have an Object of class
     * WATCHLIST_CLASS attached AND who have choosen the current interval (ex:hourly).
     *
     * @return a collection of document names pointing to the XWikiUsers wishing to get notified.
     */
    protected List retrieveNotificationSubscribers() throws XWikiException
    {
        String request =
            ", BaseObject as obj, StringProperty as prop where " +
                "obj.name=doc.fullName and obj.className='"
                + WatchListPlugin.WATCHLIST_CLASS +
                "' and prop.id.id=obj.id and prop.name='interval' " +
                "and prop.value='" + interval + "')";
        return plugin.globalSearchDocuments(request, 0, 0, new ArrayList(), context);
    }

    /**
     * Retrieves the list of documents that have been updated or created in the interval.
     *
     * @return a list of updated or created documents in the interval
     */
    protected List retrieveUpdatedDocuments() throws XWikiException
    {
        DateTime dt = new DateTime();

        switch (interval) {
            case WatchListPlugin.WATCHLIST_INTERVAL_HOUR:
                // hourly
                dt = dt.minus(Hours.ONE);
                break;
            case WatchListPlugin.WATCHLIST_INTERVAL_DAY:
                // daily
                dt = dt.minus(Days.ONE);
                break;
            case WatchListPlugin.WATCHLIST_INTERVAL_WEEK:
                // weekly
                dt = dt.minus(Weeks.ONE);
                break;
            case WatchListPlugin.WATCHLIST_INTERVAL_MONTH:
                // monthly
                dt = dt.minus(Months.ONE);
                break;
        }
        List values = new ArrayList();
        values.add(dt.toDate());
        String updatedDocumentRequest = "where doc.date > ? order by doc.date desc";

        return plugin.globalSearchDocuments(updatedDocumentRequest, 0, 0, values, context);
    }

    /**
     * Sends the email notifying the subscriber that the updatedDocuments have been changed.
     *
     * @param subscriber person to notify
     * @param updatedDocuments list of updated documents
     */
    protected void sendNotificationMessage(Document subscriber,
        List updatedDocuments) throws XWikiException
    {
        // Get user email
        Object userObj = subscriber.getObject("XWiki.XWikiUsers");
        String emailAddr = (String) userObj.getProperty("email").getValue();
        if (emailAddr == null || emailAddr.length() == 0 || emailAddr.indexOf("@") < 0) {
            // Invalid email
            return;
        }

        // Prepare email template (wiki page) context
        VelocityContext vcontext = new VelocityContext();
        vcontext.put("pseudo", userObj.getProperty("first_name").getValue());
        vcontext.put("documents", updatedDocuments);
        vcontext.put("interval", new Integer(interval));
        vcontext.put("xwiki", new com.xpn.xwiki.api.XWiki(context.getWiki(), context));
        vcontext.put("util", new com.xpn.xwiki.api.Util(context.getWiki(), context));
        vcontext.put("context", new Context(context));

        // Get wiki's default language (default en)
        String language = context.getWiki().getXWikiPreference("default_language", "en", context);

        // Get mailsenderplugin
        MailSenderPlugin emailService =
            (MailSenderPlugin) context.getWiki().getPlugin(MailSenderPlugin.ID, context);
        if (emailService == null) {
            return;
        }

        // Get wiki administrator email (default : mailer@xwiki.localdomain.com)
        String sender = context.getWiki()
            .getXWikiPreference("admin_email", "mailer@xwiki.localdomain.com", context);

        // Set email template
        String mailTemplate = "";
        if (context.getWiki().exists(jobMailTemplate, context)) {
            mailTemplate = jobMailTemplate;
        } else if (context.getWiki().exists(WatchListPlugin.WATCHLIST_EMAIL_TEMPLATE, context)) {
            mailTemplate = WatchListPlugin.WATCHLIST_EMAIL_TEMPLATE;
        } else {
            mailTemplate =
                context.getMainXWiki() + ":" + WatchListPlugin.WATCHLIST_EMAIL_TEMPLATE;
        }

        // Send message from template
        emailService.sendMailFromTemplate(mailTemplate, sender, emailAddr, null, null, language,
            vcontext, context);
    }
}
