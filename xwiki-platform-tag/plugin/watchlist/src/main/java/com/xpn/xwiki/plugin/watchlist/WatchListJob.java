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

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.Object;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.plugin.mailsender.MailSenderPlugin;
import com.xpn.xwiki.plugin.mailsender.MailSenderPluginApi;
import com.xpn.xwiki.user.api.XWikiRightService;
import org.apache.velocity.VelocityContext;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * WatchList implementation of Quartz's Job
 *
 * This class behaves as follow when the execute method is called: 1) it selects the persons who
 * have requested a notification matching the frequency of this Job and returns Set1 2) if Set1 is
 * not void, then it selects the documents that have changed during the last period and stores them
 * in Set2 3) if Set2 is not void, then for each person Pi in Set1, it intersects the following
 * sets: Set2, matching criteria of Pi, documents that can be read by Pi 4) it sends an email to Pi
 */
public class WatchListJob implements Job
{
    protected com.xpn.xwiki.api.XWiki xwiki = null;
    protected BaseObject xjob = null;
    protected com.xpn.xwiki.api.Context xcontext = null;
    protected WatchListPluginApi notificationPlugin = null;
    protected int interval = 0;

    /**
     * Sets objects required by the Job : XWiki, XWikiContext, WatchListPlugin, etc
     *
     * @param context Context of the request
     */
    public void init(JobExecutionContext context)
    {
        JobDataMap data = context.getJobDetail().getJobDataMap();
        xwiki = (com.xpn.xwiki.api.XWiki) data.get("xwiki");
        xcontext = (com.xpn.xwiki.api.Context) data.get("context");
        notificationPlugin = (WatchListPluginApi)xwiki.getPlugin(WatchListPlugin.ID);
        xjob = (BaseObject) data.get("xjob");
        interval = Integer.parseInt(xjob.getLargeStringValue("script"));
    }

    /**
     * Method called from the scheduler
     *
     * @param context Context of the request
     * @throws JobExecutionException
     */
    public void execute(JobExecutionContext context) throws JobExecutionException
    {
        // Set required objects
        init(context);

        try {
            // Retreive notification subscribers
            Collection subscribers = retrieveNotificationSubscribers();
            if (subscribers != null && subscribers.size() > 0) {
                // Retreive updated documents
                List updatedDocuments = retrieveUpdatedDocuments();
                Iterator it = subscribers.iterator();
                while (it.hasNext()) {
                    try {
                        // Retreive WatchList Object for each subscribers
                        Document subscriber = xwiki.getDocument((String) it.next());                        
                        Object userObj = subscriber.getObject("XWiki.XWikiUsers");
                        Object notificationCriteria =
                            subscriber.getObject(WatchListPlugin.WATCHLIST_CLASS);
                        if (userObj == null || notificationCriteria == null) {
                            continue;
                        }
                        // Filter documents according to lists in the WatchList Object
                        List matchingDocuments =
                            filter(updatedDocuments, notificationCriteria, subscriber
                                .getFullName());

                        // If there are matching documents, sends the notification
                        if (matchingDocuments.size() > 0)
                            sendNotificationMessage(subscriber, matchingDocuments);
                    } catch (XWikiException e) {
                        // We're in a job, don't throw it
                    }
                }
            }
        } catch (XWikiException e) {
            // We're in a job, don't throw it   
        }
    }

    /**
     * Filters updated documents against users' criteria
     *
     * @param updatedDocuments the list of updated documents
     * @param notificationCriteria the BaseObject representing the user notification criteria
     * @param subscriber the user to be notified
     * @return a filtered list of documents to be sent to the user
     * @throws XWikiException
     */
    private List filter(List updatedDocuments,
        Object notificationCriteria, String subscriber) throws XWikiException
    {
        String spaceCriterion = (String)notificationCriteria.display("spaces", "view");
        String documentCriterion = (String)notificationCriteria.display("documents", "view");
        String query = (String)notificationCriteria.display("query", "view");

        List watchedDocuments = new ArrayList();
        if (spaceCriterion.length() == 0 && documentCriterion.length() == 0
            && query.length() == 0) {
            Iterator docIt = updatedDocuments.iterator();
            while (docIt.hasNext()) {
                watchedDocuments.add(docIt.next());
            }
        }

        List filteredDocumentList = new ArrayList();
        String[] watchedSpaces = spaceCriterion.split(",");

        String[] docArray = documentCriterion.split(",");
        for (int i = 0; i < docArray.length; i++)
            watchedDocuments.add(docArray[i]);

        if (query.length() > 0) {
            List queryDocuments = xwiki.searchDocuments(query);
            watchedDocuments.addAll(queryDocuments);
        }

        Iterator updatedDocumentsIt = updatedDocuments.iterator();
        while (updatedDocumentsIt.hasNext()) {
            String updatedDocument = (String)updatedDocumentsIt.next();
            String updatedDocumentSpace = xwiki.getDocument(updatedDocument).getSpace();
            boolean documentAdded = false;

            for (int i = 0; i < watchedSpaces.length; i++) {
                if (updatedDocumentSpace.equals(watchedSpaces[i])
                    && xwiki.hasAccessLevel("view", subscriber, updatedDocument)) {
                    filteredDocumentList.add(updatedDocument);
                    documentAdded = true;
                    break;
                }
            }

            // loop over the watched documents if the current document has not
            // been included in the filteredList already
            if (!documentAdded) {
                Iterator watchedDocumentIt = watchedDocuments.iterator();
                while (watchedDocumentIt.hasNext()) {
                    String watchedDocumentName = (String)watchedDocumentIt.next();
                    if (updatedDocument.equals(watchedDocumentName)
                        && xwiki.hasAccessLevel("view", subscriber, updatedDocument)) {
                        filteredDocumentList.add(updatedDocument);
                        break;
                    }
                }
            }

        }

        return filteredDocumentList;
    }

    /**
     * Retrieves all the XWiki.XWikiUsers who have requested to be notified by changes, i.e. who
     * have an Object of class WATCHLIST_CLASS attached AND who have choosen the current interval (ex:hourly).
     *
     * @return a collection of document names pointing to the XWikiUsers wishing to get notified.
     * @throws XWikiException
     */
    protected Collection retrieveNotificationSubscribers() throws XWikiException
    {
        Collection userDocs =
            xwiki.searchDocuments(
                ", BaseObject as obj, StringProperty as prop where obj.name=doc.fullName and obj.className='"
                    + WatchListPlugin.WATCHLIST_CLASS + "' and prop.id.id=obj.id and prop.name='interval' " +
                        "and prop.value='" + interval + "'");
        return userDocs;
    }

    /**
     * Retrieves the list of documents that have been updated or created in the interval.
     *
     * @return a list of updated or created documents in the interval
     * @throws XWikiException
     */
    protected List retrieveUpdatedDocuments() throws XWikiException
    {
        String updatedDocumentRequest = "where year(doc.date) = year(current_date()) and ";
        switch (interval) {
            case WatchListPlugin.WATCHLIST_INTERVAL_HOUR:
                // hourly
                updatedDocumentRequest += "month(doc.date) = month(current_date()) and day(doc.date) = day(current_date()) and hour(doc.date) > (hour(current_time()) - 1)";
                break;
            case WatchListPlugin.WATCHLIST_INTERVAL_DAY:
                // daily
                updatedDocumentRequest += "month(doc.date) = month(current_date()) and day(doc.date) > (day(current_date()) - 1)";
                break;
            case WatchListPlugin.WATCHLIST_INTERVAL_WEEK :
                // weekly
                updatedDocumentRequest += "month(doc.date) = month(current_date()) and day(doc.date) > (day(current_date()) - 7)";
                break;
            case WatchListPlugin.WATCHLIST_INTERVAL_MONTH:
                // monthly
                updatedDocumentRequest += "month(doc.date) > (month(current_date()) - 1)";
                break;
        }
        updatedDocumentRequest += " order by doc.date desc";
        return xwiki.searchDocuments(updatedDocumentRequest);                   
    }

    // TODO : 

    /**
     * Sends the email notifying the subscriber that the updatedDocuments have been changed.
     *
     * @param subscriber person to notify
     * @param updatedDocuments list of updated documents
     * @throws XWikiException
     */
    protected void sendNotificationMessage(Document subscriber,
        List updatedDocuments) throws XWikiException
    {
        // Get user email
        Object userObj = subscriber.getObject("XWiki.XWikiUsers");
        String emailAddr = (String)userObj.display("email", "view");
        if (emailAddr == null || emailAddr.length() == 0 || emailAddr.indexOf("@") < 0) {
            // Invalid email
            return;
        }

        // Prepare email template (wiki page) context
        VelocityContext vcontext = new VelocityContext();
        vcontext.put("pseudo", userObj.display("first_name", "view"));
        vcontext.put("documents", updatedDocuments);
        vcontext.put("interval", new Integer(interval));

        // Get wiki's default language (default en)
        String language = xwiki.getXWikiPreference("default_language", "en");

        // Get mailsenderplugin
        MailSenderPluginApi emailService =
            (MailSenderPluginApi) xwiki.getPlugin(MailSenderPlugin.ID);
        if (emailService == null) {
            return;
        }

        // Get wiki administrator email (default : mailer@xwiki.localdomain.com)
        String sender = xwiki.getXWikiPreference("admin_email", "mailer@xwiki.localdomain.com");                    

        // Send message from template
        int sendResult =
            emailService.sendMessageFromTemplate(sender, emailAddr, null,
                null, language, WatchListPlugin.WATCHLIST_EMAIL_TEMPLATE, vcontext);
    }
}
