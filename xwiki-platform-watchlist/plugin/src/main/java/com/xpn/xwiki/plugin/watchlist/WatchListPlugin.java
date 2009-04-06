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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.*;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.plugin.lucene.LucenePlugin;
import com.xpn.xwiki.plugin.lucene.SearchResult;
import com.xpn.xwiki.plugin.scheduler.SchedulerPlugin;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Plugin that offers WatchList features to XWiki. These feature allow users to build lists of pages and spaces they
 * want to follow. At a frequency choosen by the user XWiki will send an email notification to him with a list of the
 * elements that has been modified since the last notification.
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
            // Main wiki
            initWatchListClass(context);
            initWatchlistJobs(context);
            sanitizeWatchlists(context);
        } catch (XWikiException e) {
            log.error("init", e);
            e.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#getPluginApi(XWikiPluginInterface, XWikiContext)
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
        boolean needsUpdate = false;

        try {
            doc = context.getWiki().getDocument(WATCHLIST_CLASS, context);
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

        if (StringUtils.isBlank(doc.getAuthor())) {
            needsUpdate = true;
            doc.setAuthor("XWiki.Admin");
        }
        if (StringUtils.isBlank(doc.getCreator())) {
            needsUpdate = true;
            doc.setCreator("XWiki.Admin");
        }
        if (StringUtils.isBlank(doc.getParent())) {
            needsUpdate = true;
            doc.setParent("XWiki.XWikiClasses");
        }
        if (StringUtils.isBlank(doc.getContent())) {
            needsUpdate = true;
            doc.setContent("1 XWiki Watchlist Notification Rule Class");
        }

        if (needsUpdate) {
            context.getWiki().saveDocument(doc, "", true, context);
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
        boolean needsUpdate = false;
        String jobClass = "com.xpn.xwiki.plugin.watchlist.WatchListJob";
        BaseObject job = null;

        String docName = WATCHLIST_EMAIL_JOB_COMMON_NAME + interval;
        try {
            doc = context.getWiki().getDocument(docName, context);

            job = doc.getObject(SchedulerPlugin.XWIKI_JOB_CLASS);
            if (job == null) {
                needsUpdate = true;
                int index = doc.createNewObject(SchedulerPlugin.XWIKI_JOB_CLASS, context);
                job = doc.getObject(SchedulerPlugin.XWIKI_JOB_CLASS, index);
                job.setStringValue("jobName", name);
                job.setStringValue("jobClass", jobClass);
                job.setStringValue("cron", cron);
                job.setLargeStringValue("script", Integer.toString(interval));
                job.setLargeStringValue("jobDescription", description);
                job.setStringValue("contextUser", "XWiki.Admin");
                job.setStringValue("contextLang", "en");
                job.setStringValue("contextDatabase", "xwiki");
            }

            BaseObject rights = doc.getObject("XWiki.XWikiRights");
            if (rights == null) {
                needsUpdate = true;
                int index = doc.createNewObject("XWiki.XWikiRights", context);
                rights = doc.getObject("XWiki.XWikiRights", index);
                rights.setStringValue("groups", "XWiki.XWikiAdminGroup");
                rights.setStringValue("levels", "edit,delete");
                rights.setIntValue("allow", 1);
            }

            if (StringUtils.isBlank(doc.getAuthor())) {
                needsUpdate = true;
                doc.setAuthor("XWiki.Admin");
            }
            if (StringUtils.isBlank(doc.getCreator())) {
                needsUpdate = true;
                doc.setCreator("XWiki.Admin");
            }
            if (StringUtils.isBlank(doc.getParent())) {
                needsUpdate = true;
                doc.setParent("XWiki.XWikiClasses");
            }
            if (StringUtils.isBlank(doc.getContent())) {
                needsUpdate = true;
                doc.setContent("#includeInContext(\"XWiki.SchedulerJobSheet\")");
            }

            if (needsUpdate) {
                context.getWiki().saveDocument(doc, "", true, context);
                ((SchedulerPlugin) context.getWiki().getPlugin("scheduler", context)).scheduleJob(job, context);
            }
        } catch (Exception e) {
            log.error("Cannot initialize WatchListJob", e);
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
     * Is the watchedDocument in user WatchList ?
     *
     * @param user XWiki User
     * @param watchedElement XWiki Document name
     * @param context Context of the request
     * @return True if the page is watched by user
     */
    public boolean isWatched(String user, String watchedElement, XWikiContext context)
        throws XWikiException
    {
        return this.getWatchedDocuments(user, context).contains(watchedElement) |
            this.getWatchedSpaces(user, context).contains(watchedElement);
    }

    /**
     * Creates a WatchList XWiki Object in the user's profile's page
     *
     * @param user XWiki User
     * @param context Context of the request
     * @return the watchlist object that has been created
     * @throws XWikiException if the document cannot be saved
     */
    public BaseObject createWatchListObject(String user, XWikiContext context) throws XWikiException
    {
        XWikiDocument userDocument = context.getWiki().getDocument(user, context);
        int nb = userDocument.createNewObject(WATCHLIST_CLASS, context);
        BaseObject wObj = userDocument.getObject(WATCHLIST_CLASS, nb);
        wObj.set("interval", "1", context);
        context.getWiki()
            .saveDocument(userDocument, context.getMessageTool().get("watchlist.create.object"),
                true, context);
        return wObj;
    }

    /**
     * Gets the WatchList XWiki Object from user's profile's page
     *
     * @param user XWiki User
     * @param context Context of the request
     * @return the WatchList XWiki BaseObject
     * @throws XWikiException if BaseObject creation fails
     */
    public BaseObject getWatchListObject(String user, XWikiContext context)
        throws XWikiException
    {
        XWikiDocument userDocument = context.getWiki().getDocument(user, context);
        BaseObject obj = userDocument.getObject(WATCHLIST_CLASS);
        if (obj == null) {
            obj = this.createWatchListObject(user, context);
        }
        return obj;
    }

    /**
     * Sets a largeString property in the user's WatchList Object, then saves the user's profile
     *
     * @param user XWiki User
     * @param prop Property name (documents,spaces,query)
     * @param value Property value (list of documents,list of pages,hql query)
     * @param context Context of the request
     * @throws XWikiException if the user's profile cannot be saved
     */
    public void setWatchListLargeStringProperty(String user, String prop, String value,
        XWikiContext context) throws XWikiException
    {
        XWikiDocument userDocument = context.getWiki().getDocument(user, context);
        userDocument.setLargeStringValue(WATCHLIST_CLASS, prop, value);
        userDocument.isMinorEdit();
        context.getWiki().saveDocument(userDocument,
            context.getMessageTool().get("watchlist.save.object"), true, context);
    }

    /**
     * Get the list of documents watched by user
     *
     * @param user XWiki User
     * @param context Context of the request
     * @return List of watched documents
     * @throws XWikiException if the WatchList Object cannot be retreived
     */
    public List getWatchedDocuments(String user, XWikiContext context) throws XWikiException
    {
        BaseObject watchListObject = this.getWatchListObject(user, context);
        String watchedItems = watchListObject.getLargeStringValue("documents").trim();
        return Arrays.asList(watchedItems.split(","));
    }

    /**
     * Get the list of spaces watched by user
     *
     * @param user XWiki User
     * @param context Context of the request
     * @return List of watched space
     * @throws XWikiException if the WatchList Object cannot be retreived
     */
    public List getWatchedSpaces(String user, XWikiContext context) throws XWikiException
    {
        BaseObject watchListObject = this.getWatchListObject(user, context);
        String watchedItems = watchListObject.getLargeStringValue("spaces").trim();
        return Arrays.asList(watchedItems.split(","));
    }

    /**
     * Add the specified element (document or space) to the corresponding list in the user's WatchList
     *
     * @param user XWikiUser
     * @param newWatchedElement The name of the element to add (document of space)
     * @param isSpace True if the element is a space, false if it's a document
     * @param context Context of the request
     * @return True if the element was'nt already in list
     * @throws XWikiException if the modification hasn't been saved
     */
    public boolean addWatchedElement(String user, String newWatchedElement,
        boolean isSpace, XWikiContext context) throws XWikiException
    {
        newWatchedElement = context.getDatabase() + ":" + newWatchedElement;

        if (this.isWatched(user, newWatchedElement, context)) {
            return false;
        }

        String prop = isSpace ? "spaces" : "documents";
        List watchedItems = isSpace ?
            this.getWatchedSpaces(user, context) :
            this.getWatchedDocuments(user, context);

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
        this.setWatchListLargeStringProperty(user, prop, updatedWatchedElements.toString(),
            context);
        return true;
    }

    /**
     * Remove the specified element (document or space) from the corresponding list in the user's WatchList
     *
     * @param user XWiki User
     * @param watchedElement The name of the element to remove (document or space)
     * @param isSpace True if the element is a space, false if it's a document
     * @param context Context of the request
     * @return True if the element was in list and has been removed, false if the element was'nt in the list
     * @throws XWikiException If the WatchList Object cannot be retreived or if the user's profile cannot be saved
     */
    public boolean removeWatchedElement(String user, String watchedElement,
        boolean isSpace, XWikiContext context) throws XWikiException
    {
        if (!watchedElement.contains(":")) {
            watchedElement = context.getDatabase() + ":" + watchedElement;
        }

        if (!this.isWatched(user, watchedElement, context)) {
            return false;
        }

        String prop = isSpace ? "spaces" : "documents";
        List watchedItems = isSpace ?
            this.getWatchedSpaces(user, context) :
            this.getWatchedDocuments(user, context);

        StringBuffer updatedWatchedElements = new StringBuffer();
        for (int i = 0; i < watchedItems.size(); i++) {
            if (!watchedItems.get(i).equals(watchedElement)) {
                if (i > 0) {
                    updatedWatchedElements.append(",");
                }
                updatedWatchedElements.append(watchedItems.get(i));
            }
        }
        this.setWatchListLargeStringProperty(user, prop, updatedWatchedElements.toString(),
            context);
        return true;
    }

    /**
     * Transforms a list of wiki:space in a lucene query
     */
    private String getLuceneQuery(String[] docs, String replaceString)
    {
        List query = new ArrayList();

        for (int i = 0; i < docs.length; i++) {
            if ((docs[i] != null) && (!docs[i].trim().equals(""))) {
                query.add(docs[i].replaceAll("(.*):(.*)", replaceString));
            }
        }

        return StringUtils.join(query, " OR ");
    }

    /**
     * Transforms a list of wiki:space in a lucene query
     */
    private String getLuceneQuery(String documents, String spaces)
    {
        String watchedDocuments = getLuceneQuery(documents.split(","), "(wiki:$1 AND fullname:$2)");
        String watchedSpaces = getLuceneQuery(spaces.trim().split(","), "(wiki:$1 AND web:$2)");
        String request = null;
        if (!StringUtils.isBlank(watchedSpaces) && !StringUtils.isBlank(watchedDocuments)) {
            request = "(" + watchedSpaces + " OR " + watchedDocuments + ") AND type:wikipage";
        } else if (StringUtils.isBlank(watchedSpaces) && !StringUtils.isBlank(watchedDocuments)) {
            request = "(" + watchedDocuments + ") AND type:wikipage";
        } else if (!StringUtils.isBlank(watchedSpaces) && StringUtils.isBlank(watchedDocuments)) {
            request = "(" + watchedSpaces + ") AND type:wikipage";
        }
        return request;
    }

    /**
     * Get the list of the elements watched by user ordered by last modification date, descending
     *
     * @param user XWiki User
     * @param context Context of the request
     * @return The list of the watched elements ordered by last modification date, descending
     * @throws XWikiException If the search request fails
     */
    public List getWatchListWhatsNew(String user, XWikiContext context) throws Exception
    {
        List<String> results = new ArrayList<String>();
        BaseObject watchListObject = this.getWatchListObject(user, context);

        if (context.getWiki().isVirtualMode()) {
            // In virtual mode RSS feeds are generated from a Lucene query
            String documents = watchListObject.getLargeStringValue("documents").trim();
            String spaces = watchListObject.getLargeStringValue("spaces");
            String request = getLuceneQuery(documents, spaces);

            if (!StringUtils.isBlank(request)) {
                String language = StringUtils.isBlank(context.getLanguage()) ? "default" : context.getLanguage();
                LucenePlugin lucene = (LucenePlugin) context.getWiki().getPlugin("lucene", context);
                List<SearchResult> luceneResults =
                    lucene.getSearchResults(request, "-date", null, language, context).getResults(0, 20);

                for (SearchResult result : luceneResults) {
                    results.add(result.getWiki() + ":" + result.getFullName());
                }
            }
        } else {
            // In simple mode RSS feeds are generated from a HSQL query
            String watchedDocuments =
                watchListObject.getLargeStringValue("documents").trim().replaceFirst("^,", "")
                    .replaceAll("[^\\.,:]+:", "").replaceAll(",", "','");
            String watchedSpaces =
                watchListObject.getLargeStringValue("spaces").trim().replaceFirst("^,", "")
                    .replaceAll("[^\\.,:]+:", "").replaceAll(",", "','");
            String request = "select doc.fullName from XWikiDocument as doc where doc.space in ('" +
                watchedSpaces + "') or doc.fullName in ('" + watchedDocuments + "') " +
                "order by doc.date desc";
            results.addAll(context.getWiki().getStore().<String>search(request, 20, 0, context));
        }
        return results;
    }

    /**
     * Search documents on all the wikis by passing HQL where clause values as parameters.
     *
     * @param request the HQL where clause.
     * @param values the where clause values that replace the question marks (?)
     * @return a list of document names prefixed with the wiki they come from ex : xwiki:Main.WebHome
     */
    protected List globalSearchDocuments(String request, int nb, int start, List values,
        XWikiContext context)
    {
        String initialDb =
            !context.getDatabase().equals("") ? context.getDatabase() :
                context.getMainXWiki();
        List wikiServers = Collections.EMPTY_LIST;
        List results = new ArrayList();

        if (context.getWiki().isVirtualMode()) {
            try {
                wikiServers = context.getWiki().getVirtualWikiList();
                if (!wikiServers.contains(context.getMainXWiki())) {
                    wikiServers.add(context.getMainXWiki());
                }
            } catch (Exception e) {
                getLogger().error("error getting list of wiki servers!", e);
            }
        } else {
            wikiServers = new ArrayList();
            wikiServers.add(context.getMainXWiki());
        }

        if (log.isInfoEnabled()) {
            log.info("WatchList checking subscribers in wikis: " + StringUtils.join(wikiServers, ","));
        }

        try {
            for (Iterator iter = wikiServers.iterator(); iter.hasNext();) {
                String wiki = (String) iter.next();
                String wikiPrefix = wiki + ":";
                context.setDatabase(wiki);
                try {
                    List upDocsInWiki = context.getWiki().getStore()
                        .searchDocumentsNames(request, 0, 0, values, context);
                    Iterator it = upDocsInWiki.iterator();
                    while (it.hasNext()) {
                        results.add(wikiPrefix + it.next());
                    }
                } catch (Exception e) {
                    getLogger().error("error getting list of documents in the wiki : " + wiki, e);
                }
            }
        } finally {
            context.setDatabase(initialDb);
        }
        return results;
    }

    /**
     * Loop over all the watchlists stored in all the wikis. Verify if the database prefix is present on all the
     * watchlist items, if not adds MainWiki as prefix.
     */
    protected void sanitizeWatchlists(XWikiContext context)
    {
        String request = ", BaseObject as obj where obj.name=doc.fullName and obj.className='"
            + WatchListPlugin.WATCHLIST_CLASS + "'";
        List subscribers =
            globalSearchDocuments(request, 0, 0, new ArrayList(), context);
        Iterator it = subscribers.iterator();
        while (it.hasNext()) {
            String user = (String) it.next();
            try {

                XWikiDocument userDocument =
                    context.getWiki().getDocument(user, context);
                BaseObject wobj = userDocument.getObject(WatchListPlugin.WATCHLIST_CLASS);
                String docs = wobj.getLargeStringValue("documents").trim();
                String spaces = wobj.getLargeStringValue("spaces").trim();
                boolean update = false;

                // Add db prefixes to document names stored in the watchlist object
                Pattern p = Pattern.compile("(^|,)([^\\.,:]+)(\\.)([^\\.,]+)");
                Matcher m = p.matcher(docs);
                if (m.find()) {
                    String newdocs = docs.replaceAll("(^|,)([^\\.,:]+)(\\.)([^\\.,]+)",
                        "$1" + context.getMainXWiki() + ":$2$3$4");
                    wobj.setLargeStringValue("documents", newdocs);
                    getLogger().info("Sanitizing watchlist documents for user : " + user);
                    update = true;
                }

                // Add db prefixes to space names stored in the watchlist object
                p = Pattern.compile("(^|,)([^:,]+)(?=(,|$))");
                m = p.matcher(spaces);
                if (m.find()) {
                    String newspaces = spaces.replaceAll("(^|,)([^:,]+)(?=(,|$))",
                        "$1" + context.getMainXWiki() + ":$2");
                    wobj.setLargeStringValue("spaces", newspaces);
                    getLogger().info("Sanitizing watchlist spaces for user : " + user);
                    update = true;
                }

                if (update) {
                    context.getWiki().saveDocument(userDocument, "", true, context);
                }
            } catch (Exception e) {
                getLogger().error("Exception while sanitizing watchlist for user : " + user);
                e.printStackTrace();
            }
        }
    }
}
