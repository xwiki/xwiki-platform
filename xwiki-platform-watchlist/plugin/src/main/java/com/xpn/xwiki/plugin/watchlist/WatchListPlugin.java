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
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.plugin.mailsender.MailSenderPlugin;
import com.xpn.xwiki.plugin.scheduler.SchedulerPlugin;
import com.xpn.xwiki.plugin.scheduler.SchedulerPluginApi;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Plugin that offers WatchList features to XWiki. These feature allow users to build lists of pages
 * and spaces they want to follow. At a frequency choosen by the user XWiki will send an email
 * notification to him with a list of the elements that has been modified since the last
 * notification.
 *
 * @version $Id: $
 */
public class WatchListPlugin extends XWikiDefaultPlugin implements XWikiPluginInterface
{
    public static String WATCHLIST_EMAIL_TEMPLATE = "XWiki.WatchListMessage";

    public static String WATCHLIST_EMAIL_JOB_COMMON_NAME = "Scheduler.WatchListJob";

    public static String WATCHLIST_CLASS = "XWiki.WatchListClass";

    public static final int WATCHLIST_INTERVAL_NEVER = 0;

    public static final int WATCHLIST_INTERVAL_HOUR = 1;

    public static final int WATCHLIST_INTERVAL_DAY = 2;

    public static final int WATCHLIST_INTERVAL_WEEK = 3;

    public static final int WATCHLIST_INTERVAL_MONTH = 4;

    public static final String ID = "watchlist";

    private static final Log log = LogFactory.getLog(WatchListPlugin.class);

    /**
     * {@inheritDoc}
     *
     * @see XWikiDefaultPlugin#XWikiDefaultPlugin(String,String,com.xpn.xwiki.XWikiContext)
     */
    public WatchListPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#getName()
     */
    public String getName()
    {
        return ID;
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#virtualInit(XWikiContext)
     */
    public void virtualInit(XWikiContext context)
    {
        super.virtualInit(context);
        try {
            initWatchListClass(context);
            initEmailTemplate(context);
            initWatchlistJobs(context);
        } catch (XWikiException e) {
            log.error("virtualInit", e);
            e.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#init(XWikiContext)
     */
    public void init(XWikiContext context)
    {
        super.init(context);
        try {
            initWatchListClass(context);
            initEmailTemplate(context);
            initWatchlistJobs(context);
        } catch (XWikiException e) {
            log.error("virtualInit", e);
            e.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#getPluginApi(XWikiPluginInterface,
     *XWikiContext)
     */
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new WatchListPluginApi((WatchListPlugin) plugin, context);
    }

    /**
     * @return log Log interface
     */
    protected Log getLogger()
    {
        return log;
    }

    /**
     * Creates the WatchList xwiki class
     *
     * @param context Context of the request
     * @return the WatchList XWiki Class
     * @throws XWikiException if class fields cannot be created
     */
    protected BaseClass initWatchListClass(XWikiContext context) throws XWikiException
    {
        XWikiDocument doc;
        XWiki xwiki = context.getWiki();
        boolean needsUpdate = false;

        try {
            doc = xwiki.getDocument(WATCHLIST_CLASS, context);
        } catch (Exception e) {
            doc = new XWikiDocument();
            String[] spaceAndName = WATCHLIST_CLASS.split(".");
            doc.setSpace(spaceAndName[0]);
            doc.setName(spaceAndName[1]);
            needsUpdate = true;
        }

        BaseClass bclass = doc.getxWikiClass();
        bclass.setName(WATCHLIST_CLASS);
        needsUpdate |=
            bclass.addStaticListField("interval", "Email notifications interval", +
                WATCHLIST_INTERVAL_NEVER + "=never|" +
                WATCHLIST_INTERVAL_HOUR + "=hourly|" +
                WATCHLIST_INTERVAL_DAY + "=daily|" +
                WATCHLIST_INTERVAL_WEEK + "=weekly|" +
                WATCHLIST_INTERVAL_MONTH + "=monthly");
        needsUpdate |=
            bclass.addTextAreaField("spaces", "Space list, separated by commas", 40, 5);
        needsUpdate |=
            bclass.addTextAreaField("documents", "Document list, separated by commas", 40, 5);
        needsUpdate |= bclass.addTextAreaField("query", "Query (HQL)", 40, 5);

        String content = doc.getContent();
        if ((content == null) || (content.equals(""))) {
            needsUpdate = true;
            doc.setContent("1 NotificationRule");
        }

        if (needsUpdate) {
            xwiki.saveDocument(doc, context);
        }
        return bclass;
    }

    /**
     * Creates a WatchList job in the XWiki Scheduler application (XWiki Object)
     *
     * @param interval WatchList interval (used in job name)
     * @param name Job name
     * @param description Job description
     * @param cron CRON expression (see quartz CRON expressions)
     * @param context Context of the request
     */
    protected void initWatchListJob(int interval, String name, String description, String cron,
        XWikiContext context) throws XWikiException
    {
        XWikiDocument doc;
        XWiki xwiki = context.getWiki();
        boolean needsUpdate = false;
        String jobClass = "com.xpn.xwiki.plugin.watchlist.WatchListJob";

        String docName = WATCHLIST_EMAIL_JOB_COMMON_NAME + interval;
        try {
            doc = xwiki.getDocument(docName, context);
            BaseObject obj = doc.getObject(SchedulerPlugin.XWIKI_JOB_CLASS);
            if (obj == null) {
                needsUpdate = true;
            }
        } catch (Exception e) {
            doc = new XWikiDocument();
            String[] spaceAndName = docName.split(".");
            doc.setSpace(spaceAndName[0]);
            doc.setName(spaceAndName[1]);
            needsUpdate = true;
        }

        if (needsUpdate) {
            int index = doc.createNewObject(SchedulerPlugin.XWIKI_JOB_CLASS, context);
            BaseObject job = doc.getObject(SchedulerPlugin.XWIKI_JOB_CLASS, index);
            job.setStringValue("jobName", name);
            job.setStringValue("jobClass", jobClass);
            job.setStringValue("cron", cron);
            job.setLargeStringValue("script", Integer.toString(interval));
            job.setLargeStringValue("jobDescription", description);

            // set the needed context params
            // TODO create a watchlist application that holds those jobs as documents
            job.setStringValue("contextUser", "XWiki.Admin");
            job.setStringValue("contextLang","en");
            job.setStringValue("contextDatabase","xwiki");

            doc.setContent("#includeInContext('XWiki.SchedulerJobSheet')");
            doc.setAuthor("XWiki.Admin");
            doc.setCreator("XWiki.Admin");
            index = doc.createNewObject("XWiki.XWikiRights", context);
            BaseObject rights = doc.getObject("XWiki.XWikiRights", index);
            rights.setStringValue("groups", "XWiki.XWikiAdminGroup");
            rights.setStringValue("levels", "edit,delete");
            rights.setIntValue("allow", 1);
            xwiki.saveDocument(doc, context);
            ((SchedulerPlugin) context.getWiki().getPlugin("scheduler", context))
                .scheduleJob(job, context);
        }
    }

    /**
     * Creates the 4 WatchList jobs (hourly,daily,weekly,monthly)
     *
     * @param context Context of the request
     * @throws XWikiException When a job creation fails
     */
    protected void initWatchlistJobs(XWikiContext context) throws XWikiException
    {
        // Every hour at 00
        initWatchListJob(WATCHLIST_INTERVAL_HOUR, "WatchList hourly notifications",
            "WatchList hourly email watchlist job", "0 0 * * * ?", context);
        // Every day at 00:00
        initWatchListJob(WATCHLIST_INTERVAL_DAY, "WatchList daily notifications",
            "WatchList daily email watchlist job", "0 0 0 * * ?", context);
        // Every monday at 00:00
        initWatchListJob(WATCHLIST_INTERVAL_WEEK, "WatchList weekly notifications",
            "WatchList weekly email watchlist job", "0 0 0 ? * SUN", context);
        // First day of every month at 00:00
        initWatchListJob(WATCHLIST_INTERVAL_MONTH, "WatchList monthly notifications",
            "WatchList monthly email watchlist job", "0 0 0 ? * L", context);
    }

    /**
     * Creates the email notification template (XWiki Object)
     */
    protected void initEmailTemplate(XWikiContext context) throws XWikiException
    {
        XWikiDocument doc;
        XWiki xwiki = context.getWiki();
        boolean needsUpdate = false;

        try {
            doc = xwiki.getDocument(WATCHLIST_EMAIL_TEMPLATE, context);
            BaseObject obj = doc.getObject(MailSenderPlugin.EMAIL_XWIKI_CLASS_NAME);
            if (obj == null) {
                needsUpdate = true;
            }
        } catch (Exception e) {
            doc = new XWikiDocument();
            String[] spaceAndName = WATCHLIST_EMAIL_TEMPLATE.split(".");
            doc.setSpace(spaceAndName[0]);
            doc.setName(spaceAndName[1]);
            needsUpdate = true;
        }

        if (needsUpdate) {

            int index = doc.createNewObject(MailSenderPlugin.EMAIL_XWIKI_CLASS_NAME, context);
            BaseObject mail = doc.getObject(MailSenderPlugin.EMAIL_XWIKI_CLASS_NAME, index);
            mail.setStringValue("language", "en");
            mail.setStringValue("subject",
                "XWiki : Watchlist Updates, #set($format=\"dd/MM/yyyy\")#if ($interval == 1)" +
                    "#set($format=\"${format} hh:mm\")#end$xwiki.formatDate($xwiki.date, $format)");
            mail.setLargeStringValue("text", "Hello $pseudo,\n" +
                "This message is sent by XWiki. Here are the documents in your watchlist that have been created or updated since the last notification :\n" +
                "\n" +
                "#foreach ($udocname in $documents)\n" +
                "#set ($udoc = $xwiki.getDocument($udocname))$udoc.getDisplayTitle() : ${udoc.getExternalURL()}\n" +
                "#end");
            mail.setLargeStringValue("html", "<b>Hello $pseudo,</b><br/>\n" +
                "<i>This message is sent by XWiki. Here are the documents in your watchlist that have been created or updated since the last notification :</i><br/>\n" +
                "<br/>\n" +
                "<table width=\"100%\">\n" +
                "<tr style=\"background-color:#EFEFEF;font-weight:bold;\">\n" +
                "<td>Name</td><td>Space</td><td>Modified by</td><td>Comment</td><td>Modified on</td>\n" +
                "</tr>\n" +
                "#foreach ($udocname in $documents)\n" +
                "#set ($udoc = $xwiki.getDocument($udocname))\n" +
                "#if ($velocityCount % 2 == 0)\n" +
                "  #set ($color = \"#E5F0FE\")\n" +
                "#else\n" +
                "  #set ($color = \"#FFF\")\n" +
                "#end\n" +
                "<tr style=\"background-color:${color};\">\n" +
                "<td><a href=\"${udoc.getExternalURL()}\">$udoc.getDisplayTitle()</a></td>\n" +
                "<td>$udoc.web</td>\n" +
                "<td>$xwiki.getLocalUserName($udoc.author, false)</td>\n" +
                "<td>$udoc.getComment()</td>\n" +
                "<td>$xwiki.formatDate($udoc.date)</td>\n" +
                "</tr>\n" +
                "#end\n" +
                "</table>");
            String content = doc.getContent();
            if ((content == null) || (content.equals(""))) {
                doc.setContent("1 Notification message");
            }
            xwiki.saveDocument(doc, context);
        }
    }

    /**
     * Is the watchedDocument in localUser WatchList ?
     *
     * @param localUser XWiki User
     * @param watchedElement XWiki Document name
     * @param context Context of the request
     * @return True if the page is watched by user
     */
    public boolean isWatched(String localUser, String watchedElement, XWikiContext context)
        throws XWikiException
    {
        return this.getWatchedDocuments(localUser, context).contains(watchedElement) |
            this.getWatchedSpaces(localUser, context).contains(watchedElement);
    }

    /**
     * Creates a WatchList XWiki Object in the localUser's profile's page
     *
     * @param localUser XWiki User
     * @param context Context of the request
     * @throws XWikiException if the document cannot be saved
     */
    public void createWatchListObject(String localUser, XWikiContext context) throws XWikiException
    {
        XWiki wiki = context.getWiki();
        XWikiDocument userDocument = wiki.getDocument(localUser, context);
        int nb = userDocument.createNewObject(WATCHLIST_CLASS, context);
        BaseObject wObj = userDocument.getObject(WATCHLIST_CLASS, nb);
        wObj.set("interval", "1", context);
        wiki.saveDocument(userDocument, context.getMessageTool().get("watchlist.create.object"),
            true, context);
    }

    /**
     * Gets the WatchList XWiki Object from localUser's profile's page
     *
     * @param localUser XWiki User
     * @param context Context of the request
     * @return the WatchList XWiki BaseObject
     * @throws XWikiException if BaseObject creation fails
     */
    private BaseObject getWatchListObject(String localUser, XWikiContext context)
        throws XWikiException
    {
        XWikiDocument userDocument = context.getWiki().getDocument(localUser, context);
        if (userDocument.getObjectNumbers(WATCHLIST_CLASS) == 0) {
            this.createWatchListObject(localUser, context);
            return this.getWatchListObject(localUser, context);
        }
        return userDocument.getObject(WATCHLIST_CLASS);
    }

    /**
     * Sets a largeString property in the localUser's WatchList Object, then saves the localUser's
     * profile
     *
     * @param localUser XWiki User
     * @param prop Property name (documents,spaces,query)
     * @param value Property value (list of documents,list of pages,hql query)
     * @param context Context of the request
     * @throws XWikiException if the user's profile cannot be saved
     */
    private void setWatchListLargeStringProperty(String localUser, String prop, String value,
        XWikiContext context) throws XWikiException
    {
        XWikiDocument userDocument = context.getWiki().getDocument(localUser, context);
        userDocument.setLargeStringValue(WATCHLIST_CLASS, prop, value);
        userDocument.isMinorEdit();
        context.getWiki().saveDocument(userDocument,
            context.getMessageTool().get("watchlist.save.object"), true, context);
    }

    /**
     * Get the list of documents watched by localUser
     *
     * @param localUser XWiki User
     * @param context Context of the request
     * @return List of watched documents
     * @throws XWikiException if the WatchList Object cannot be retreived
     */
    public List getWatchedDocuments(String localUser, XWikiContext context) throws XWikiException
    {
        BaseObject watchListObject = this.getWatchListObject(localUser, context);
        String watchedItems = watchListObject.getLargeStringValue("documents").trim();
        return Arrays.asList(watchedItems.split(","));
    }

    /**
     * Get the list of spaces watched by localUser
     *
     * @param localUser XWiki User
     * @param context Context of the request
     * @return List of watched space
     * @throws XWikiException if the WatchList Object cannot be retreived
     */
    public List getWatchedSpaces(String localUser, XWikiContext context) throws XWikiException
    {
        BaseObject watchListObject = this.getWatchListObject(localUser, context);
        String watchedItems = watchListObject.getLargeStringValue("spaces").trim();
        return Arrays.asList(watchedItems.split(","));
    }

    /**
     * Add the specified element (document or space) to the corresponding list in the localUser's
     * WatchList
     *
     * @param localUser XWikiUser
     * @param newWatchedElement The name of the element to add (document of space)
     * @param isSpace True if the element is a space, false if it's a document
     * @param context Context of the request
     * @return True if the element was'nt already in list
     * @throws XWikiException if the modification hasn't been saved
     */
    public boolean addWatchedElement(String localUser, String newWatchedElement,
        boolean isSpace, XWikiContext context) throws XWikiException
    {
        if (this.isWatched(localUser, newWatchedElement, context)) {
            return false;
        }

        String prop = isSpace ? "spaces" : "documents";
        List watchedItems = isSpace ?
            this.getWatchedSpaces(localUser, context) :
            this.getWatchedDocuments(localUser, context);

        StringBuffer updatedWatchedElements = new StringBuffer();
        for (int i = 0; i < watchedItems.size(); i++) {
            if (i > 0) {
                updatedWatchedElements.append(",");
            }
            updatedWatchedElements.append(watchedItems.get(i));
        }
        if (watchedItems.size() > 0) {
            updatedWatchedElements.append(",");
        }
        updatedWatchedElements.append(newWatchedElement);
        this.setWatchListLargeStringProperty(localUser, prop, updatedWatchedElements.toString(),
            context);
        return true;
    }

    /**
     * Remove the specified element (document or space) from the corresponding list in the
     * localUser's WatchList
     *
     * @param localUser XWiki User
     * @param watchedElement The name of the element to remove (document or space)
     * @param isSpace True if the element is a space, false if it's a document
     * @param context Context of the request
     * @return True if the element was in list and has been removed, false if the element was'nt in
     *         the list
     * @throws XWikiException If the WatchList Object cannot be retreived or if the localUser's
     * profile cannot be saved
     */
    public boolean removeWatchedElement(String localUser, String watchedElement,
        boolean isSpace, XWikiContext context) throws XWikiException
    {
        if (!this.isWatched(localUser, watchedElement, context)) {
            return false;
        }

        String prop = isSpace ? "spaces" : "documents";
        List watchedItems = isSpace ?
            this.getWatchedSpaces(localUser, context) :
            this.getWatchedDocuments(localUser, context);

        StringBuffer updatedWatchedElements = new StringBuffer();
        for (int i = 0; i < watchedItems.size(); i++) {
            if (!watchedItems.get(i).equals(watchedElement)) {
                if (i > 0) {
                    updatedWatchedElements.append(",");
                }
                updatedWatchedElements.append(watchedItems.get(i));
            }
        }
        this.setWatchListLargeStringProperty(localUser, prop, updatedWatchedElements.toString(),
            context);
        return true;
    }

    /**
     * Get the list of the elements watched by localUser ordered by last modification date,
     * descending
     *
     * @param localUser XWiki User
     * @param context Context of the request
     * @return The list of the watched elements ordered by last modification date, descending
     * @throws XWikiException If the search request fails
     */
    public List getWatchListWhatsNew(String localUser, XWikiContext context) throws XWikiException
    {
        BaseObject watchListObject = this.getWatchListObject(localUser, context);
        String watchedDocuments =
            watchListObject.getLargeStringValue("documents").trim().replaceFirst("^,", "")
                .replaceAll(",", "','");
        String watchedSpaces =
            watchListObject.getLargeStringValue("spaces").trim().replaceFirst("^,", "")
                .replaceAll(",", "','");
        String request = "select doc.fullName from XWikiDocument as doc where doc.web in ('" +
            watchedSpaces + "') or doc.fullName in ('" + watchedDocuments + "') " +
            "order by doc.date desc";
        return context.getWiki().getStore().search(request, 20, 0, context);
    }
}
