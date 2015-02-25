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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.objects.classes.StaticListClass;

/**
 * WatchList store class. Handles user subscription storage.
 * 
 * @version $Id$
 */
@SuppressWarnings("serial")
public class WatchListStore implements EventListener
{
    /**
     * Character used to separated elements in Watchlist lists (pages, spaces, etc).
     * 
     * @deprecated: since 7.0M2. Elements are now stored individually.
     */
    @Deprecated
    public static final String WATCHLIST_ELEMENT_SEP = ",";

    /**
     * Character used to separated wiki and space in XWiki model.
     */
    public static final String WIKI_SPACE_SEP = ":";

    /**
     * Character used to separated space and page in XWiki model.
     */
    public static final String SPACE_PAGE_SEP = ".";

    /**
     * Character used to separated values in XProperties lists.
     */
    public static final String PIPE_SEP = "|";

    /**
     * Space of the scheduler application.
     */
    public static final String SCHEDULER_SPACE = "Scheduler";

    /**
     * List of elements that can be watched.
     */
    public enum ElementType
    {
        /**
         * Wiki.
         */
        WIKI,
        /**
         * Space.
         */
        SPACE,
        /**
         * Document.
         */
        DOCUMENT,
        /**
         * User.
         */
        USER
    }

    /**
     * The name of the listener.
     */
    private static final String LISTENER_NAME = "watchliststore";

    /**
     * The events to match.
     */
    private static final List<Event> LISTENER_EVENTS = new ArrayList<Event>()
    {
        {
            add(new DocumentCreatedEvent());
            add(new DocumentUpdatedEvent());
            add(new DocumentDeletedEvent());
        }
    };

    /**
     * Logging helper object.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(WatchListStore.class);

    /**
     * XWiki Class used to store user subscriptions.
     */
    private static final String WATCHLIST_CLASS = "XWiki.WatchListClass";

    /**
     * Property of the watchlist class used to store the notification interval preference.
     */
    private static final String WATCHLIST_CLASS_INTERVAL_PROP = "interval";

    /**
     * Property of the watchlist class used to store the list of wikis to watch.
     */
    private static final String WATCHLIST_CLASS_WIKIS_PROP = "wikis";

    /**
     * Property of the watchlist class used to store the list of spaces to watch.
     */
    private static final String WATCHLIST_CLASS_SPACES_PROP = "spaces";

    /**
     * Property of the watchlist class used to store the list of documents to watch.
     */
    private static final String WATCHLIST_CLASS_DOCUMENTS_PROP = "documents";

    /**
     * Property of the watchlist class used to store the list of users to watch.
     */
    private static final String WATCHLIST_CLASS_USERS_PROP = "users";

    /**
     * Property of the watchlist class used to indicate what should be automatically watched.
     */
    private static final String WATCHLIST_CLASS_AUTOMATICWATCH = "automaticwatch";

    /**
     * XWiki Class used to store user.
     */
    private static final String USERS_CLASS = "XWiki.XWikiUsers";

    /**
     * Watchlist jobs document names in the wiki.
     */
    private List<String> jobDocumentNames;

    /**
     * List of subscribers in the wiki farm.
     */
    private Map<String, List<String>> subscribers = new HashMap<String, List<String>>();

    /**
     * Create or update the watchlist class properties.
     * 
     * @param watchListClass document in which the class must be created
     * @param context the XWiki context
     * @return true if the class properties have been created or modified
     * @throws XWikiException when retrieving of watchlist jobs in the wiki fails
     */
    private boolean initWatchListClassProperties(XWikiDocument watchListClass, XWikiContext context)
        throws XWikiException
    {
        boolean needsUpdate = false;
        BaseClass bclass = watchListClass.getXClass();
        bclass.setName(WATCHLIST_CLASS);

        needsUpdate |= bclass.addStaticListField(WATCHLIST_CLASS_INTERVAL_PROP, "Email notifications interval", "");

        // Check that the interval property contains all the available jobs
        StaticListClass intervalClass = (StaticListClass) bclass.get(WATCHLIST_CLASS_INTERVAL_PROP);
        List<String> intervalValues = intervalClass.getList(context);

        // Look for missing or outdated jobs in the interval list
        Collections.sort(jobDocumentNames);
        if (!ListUtils.isEqualList(intervalValues, jobDocumentNames)) {
            needsUpdate = true;
            intervalClass.setValues(StringUtils.join(jobDocumentNames, PIPE_SEP));
        }

        // Create storage properties
        needsUpdate |= this.addDBListField(bclass, WATCHLIST_CLASS_WIKIS_PROP, "Wiki list");
        needsUpdate |= this.addDBListField(bclass, WATCHLIST_CLASS_SPACES_PROP, "Space list");
        needsUpdate |= this.addDBListField(bclass, WATCHLIST_CLASS_DOCUMENTS_PROP, "Document list");
        needsUpdate |= this.addDBListField(bclass, WATCHLIST_CLASS_USERS_PROP, "User list");

        needsUpdate |=
            bclass.addStaticListField(WATCHLIST_CLASS_AUTOMATICWATCH, "Automatic watching",
                "default|" + StringUtils.join(AutomaticWatchMode.values(), PIPE_SEP));

        return needsUpdate;
    }

    /**
     * @param bclass the class to add to
     * @param name the name of the property to add
     * @param prettyName the pretty name of the property to add
     * @return true if the property was added; false otherwise
     */
    private boolean addDBListField(BaseClass bclass, String name, String prettyName)
    {
        boolean needsUpdate = false;

        needsUpdate = bclass.addDBListField(name, prettyName, 80, true, null);
        if (needsUpdate) {
            // Set the input display type in order to easily debug from the object editor.
            DBListClass justAddedProperty = (DBListClass) bclass.get(name);
            justAddedProperty.setDisplayType(ListClass.DISPLAYTYPE_INPUT);
        }

        return needsUpdate;
    }

    /**
     * Create or update the watchlist class documents fields (content, title, etc).
     *
     * @param doc the watchlist class document.
     * @return true if the class properties have been created or modified.
     */
    public boolean initWatchListClassDocumentFields(XWikiDocument doc)
    {
        boolean needsUpdate = false;

        if (doc.getCreatorReference() == null) {
            needsUpdate = true;
            doc.setCreator(WatchListPlugin.DEFAULT_DOC_AUTHOR);
        }
        if (doc.getAuthorReference() == null) {
            needsUpdate = true;
            doc.setAuthor(doc.getCreator());
        }
        if (StringUtils.isBlank(doc.getParent())) {
            needsUpdate = true;
            doc.setParent("XWiki.XWikiClasses");
        }
        if (StringUtils.isBlank(doc.getTitle())) {
            needsUpdate = true;
            doc.setTitle("XWiki WatchList Notification Rules Class");
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

        return needsUpdate;
    }

    /**
     * Creates the WatchList xwiki class.
     * 
     * @param context Context of the request
     * @throws XWikiException if class fields cannot be created
     */
    private void initWatchListClass(XWikiContext context) throws XWikiException
    {
        XWikiDocument doc;
        boolean needsUpdate = false;

        try {
            doc = context.getWiki().getDocument(WATCHLIST_CLASS, context);
        } catch (Exception e) {
            doc = new XWikiDocument();
            String[] spaceAndName = StringUtils.split(WATCHLIST_CLASS, SPACE_PAGE_SEP);
            doc.setSpace(spaceAndName[0]);
            doc.setName(spaceAndName[1]);
            needsUpdate = true;
        }

        needsUpdate |= initWatchListClassProperties(doc, context);
        needsUpdate |= initWatchListClassDocumentFields(doc);

        if (needsUpdate) {
            context.getWiki().saveDocument(doc, "", true, context);
        }
    }

    /**
     * Retrieves all the users with a WatchList object in their profile.
     * 
     * @param jobName name of the job to init the cache for
     * @param context the XWiki context
     */
    private void initSubscribersCache(String jobName, XWikiContext context)
    {
        // init subscribers cache
        List<Object> queryParams = new ArrayList<Object>();
        queryParams.add(WATCHLIST_CLASS);
        queryParams.add(jobName);
        queryParams.add(USERS_CLASS);

        List<String> subscribersForJob =
            globalSearchDocuments(", BaseObject as obj, StringProperty as prop, BaseObject as userobj where"
                + " doc.fullName=obj.name and obj.className=? and obj.id=prop.id.id and prop.value=?"
                + " and doc.fullName=userobj.name and userobj.className=?", 0, 0, queryParams, context);
        subscribers.put(jobName, subscribersForJob);
    }

    /**
     * Destroy subscribers cache for the given job.
     * 
     * @param jobName name of the job for which the cache must be destroyed
     * @param context the XWiki context
     */
    private void destroySubscribersCache(String jobName, XWikiContext context)
    {
        // init subscribers cache
        subscribers.remove(jobName);
    }

    /**
     * Init watchlist store. Get all the jobs present in the wiki. Create the list of subscribers.
     * 
     * @param context the XWiki context
     * @throws XWikiException if the watchlist XWiki class creation fails
     */
    public void init(XWikiContext context) throws XWikiException
    {
        try {
            final Query q = context.getWiki().getStore().getQueryManager().getNamedQuery("getWatchlistJobDocuments");
            this.jobDocumentNames = (List<String>) (List) q.execute();
        } catch (QueryException e) {
            throw new XWikiException(0, 0, "Failed to run query for watchlist jobs.", e);
        }

        initWatchListClass(context);

        for (String jobDocumentName : jobDocumentNames) {
            initSubscribersCache(jobDocumentName, context);
        }
    }

    /**
     * Virtual init for watchlist store. Create the WatchList XWiki class.
     * 
     * @param context the XWiki context
     * @throws XWikiException if the watchlist XWiki class creation fails
     */
    public void virtualInit(XWikiContext context) throws XWikiException
    {
        // Create the watchlist class if needed
        initWatchListClass(context);
    }

    /**
     * @param jobId ID of the job.
     * @return subscribers for the given notification job.
     */
    public List<String> getSubscribersForJob(String jobId)
    {
        List<String> result = subscribers.get(jobId);

        if (result == null) {
            return new ArrayList<String>();
        } else {
            return result;
        }
    }

    /**
     * Register a new subscriber for the given job.
     * 
     * @param jobId ID of the job
     * @param user subscriber to add
     */
    private void addSubscriberForJob(String jobId, String user)
    {
        List<String> subForJob = subscribers.get(jobId);

        if (subForJob != null && !subForJob.contains(user)) {
            subForJob.add(user);
        }
    }

    /**
     * Remove a subscriber for the given job.
     * 
     * @param jobId ID of the job
     * @param user subscriber to remove
     */
    private void removeSubscriberForJob(String jobId, String user)
    {
        List<String> subForJob = subscribers.get(jobId);

        if (subForJob != null && subForJob.contains(user)) {
            subForJob.remove(user);
        }
    }

    /**
     * @return Names of documents which contain a watchlist job object.
     */
    public List<String> getJobDocumentNames()
    {
        return jobDocumentNames;
    }

    /**
     * Get watched elements for the given element type and user.
     * 
     * @param user user to match
     * @param type element type to match
     * @param context the XWiki context
     * @return matching elements
     * @throws XWikiException if retrieval of elements fails
     */
    public List<String> getWatchedElements(String user, ElementType type, XWikiContext context) throws XWikiException
    {
        BaseObject watchListObject = getWatchListObject(user, context);
        List<String> watchedItems = watchListObject.getListValue(getWatchListClassPropertyForType(type));

        return watchedItems;
    }

    /**
     * Is the element watched by the given user.
     * 
     * @param element the element to look for
     * @param user user to check
     * @param type type of the element
     * @param context the XWiki context
     * @return true if the element is watched by the user, false otherwise
     * @throws XWikiException if the retrieval of watched elements fails
     */
    public boolean isWatched(String element, String user, ElementType type, XWikiContext context) throws XWikiException
    {
        // TODO: Can this be optimized by a direct "exists" query on the list item? Would it e better than what we
        // currently have with the document cache? It would also need to be performed on the user's wiki/database, not
        // the current one.
        return getWatchedElements(user, type, context).contains(element);
    }

    /**
     * Get the name of the XClass property the given type is stored in.
     * 
     * @param type type to retrieve
     * @return the name of the XClass property
     */
    private String getWatchListClassPropertyForType(ElementType type)
    {
        if (ElementType.WIKI.equals(type)) {
            return WATCHLIST_CLASS_WIKIS_PROP;
        } else if (ElementType.SPACE.equals(type)) {
            return WATCHLIST_CLASS_SPACES_PROP;
        } else if (ElementType.DOCUMENT.equals(type)) {
            return WATCHLIST_CLASS_DOCUMENTS_PROP;
        } else if (ElementType.USER.equals(type)) {
            return WATCHLIST_CLASS_USERS_PROP;
        } else {
            return StringUtils.EMPTY;
        }
    }

    /**
     * Add the specified element (document or space) to the corresponding list in the user's WatchList.
     * 
     * @param user the reference to the user
     * @param newWatchedElement The name of the element to add (document, space, wiki, user)
     * @param type type of the element to remove
     * @param context Context of the request
     * @return true if the element wasn't already in watched list or false otherwise
     * @throws XWikiException if the modification hasn't been saved
     */
    public boolean addWatchedElement(String user, String newWatchedElement, ElementType type, XWikiContext context)
        throws XWikiException
    {
        String elementToWatch = newWatchedElement;

        if (!ElementType.WIKI.equals(type) && !newWatchedElement.contains(WIKI_SPACE_SEP)) {
            elementToWatch = context.getWikiId() + WIKI_SPACE_SEP + newWatchedElement;
        }

        if (isWatched(elementToWatch, user, type, context)) {
            return false;
        }

        // Copy the list of watched elements because it could be unmodifiable.
        List<String> watchedElements = new ArrayList<String>(getWatchedElements(user, type, context));

        watchedElements.add(elementToWatch);

        setWatchListElementsProperty(user, type, watchedElements, context);
        return true;
    }

    /**
     * Remove the specified element (document or space) from the corresponding list in the user's WatchList.
     * 
     * @param user XWiki User
     * @param watchedElement The name of the element to remove (document or space)
     * @param type type of the element to remove
     * @param context Context of the request
     * @return True if the element was in list and has been removed, false if the element was'nt in the list
     * @throws XWikiException If the WatchList Object cannot be retrieved or if the user's profile cannot be saved
     */
    public boolean removeWatchedElement(String user, String watchedElement, ElementType type, XWikiContext context)
        throws XWikiException
    {
        String elementToRemove = watchedElement;

        if (!ElementType.WIKI.equals(type) && !watchedElement.contains(WIKI_SPACE_SEP)) {
            elementToRemove = context.getWikiId() + WIKI_SPACE_SEP + watchedElement;
        }

        if (!this.isWatched(elementToRemove, user, type, context)) {
            return false;
        }

        List<String> watchedElements = getWatchedElements(user, type, context);
        watchedElements.remove(elementToRemove);

        this.setWatchListElementsProperty(user, type, watchedElements, context);
        return true;
    }

    /**
     * Creates a WatchList XWiki Object in the user's profile's page.
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
        context.getWiki().saveDocument(userDocument, context.getMessageTool().get("watchlist.create.object"), true,
            context);
        return wObj;
    }

    /**
     * Gets the WatchList XWiki Object from user's profile's page.
     * 
     * @param user XWiki User
     * @param context Context of the request
     * @return the WatchList XWiki BaseObject
     * @throws XWikiException if BaseObject creation fails or if user does not exists
     */
    public BaseObject getWatchListObject(String user, XWikiContext context) throws XWikiException
    {
        XWikiDocument userDocument = context.getWiki().getDocument(user, context);
        if (userDocument.isNew() || userDocument.getObject(USERS_CLASS) == null) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN, "User ["
                + user + "] does not exists");
        }

        BaseObject obj = userDocument.getObject(WATCHLIST_CLASS);
        if (obj == null) {
            obj = createWatchListObject(user, context);
        }

        return obj;
    }

    /**
     * Sets a largeString property in the user's WatchList Object, then saves the user's profile.
     * 
     * @param user XWiki User
     * @param type Elements type
     * @param elements List of elements to store
     * @param context Context of the request
     * @throws XWikiException if the user's profile cannot be saved
     */
    private void setWatchListElementsProperty(String user, ElementType type, List<String> elements,
        XWikiContext context) throws XWikiException
    {
        XWikiDocument userDocument = context.getWiki().getDocument(user, context);
        userDocument.setDBStringListValue(WATCHLIST_CLASS, getWatchListClassPropertyForType(type), elements);
        context.getWiki().saveDocument(userDocument, context.getMessageTool().get("watchlist.save.object"), true,
            context);
    }

    /**
     * Search documents on all the wikis by passing HQL where clause values as parameters.
     * 
     * @param request The HQL where clause.
     * @param nb Number of results to retrieve
     * @param start Offset to use in the search query
     * @param values The where clause values that replaces the question marks (?)
     * @param context The XWiki context
     * @return a list of document names prefixed with the wiki they come from ex : xwiki:Main.WebHome
     */
    public List<String> globalSearchDocuments(String request, int nb, int start, List<Object> values,
        XWikiContext context)
    {
        List<String> wikiServers = new ArrayList<String>();
        List<String> results = new ArrayList<String>();

        try {
            wikiServers = context.getWiki().getVirtualWikisDatabaseNames(context);
        } catch (Exception e) {
            LOGGER.error("error getting list of wiki servers", e);
        }

        String oriDatabase = context.getWikiId();

        try {
            for (String wiki : wikiServers) {
                String wikiPrefix = wiki + WIKI_SPACE_SEP;
                context.setWikiId(wiki);
                try {
                    List<String> upDocsInWiki =
                        context.getWiki().getStore().searchDocumentsNames(request, 0, 0, values, context);
                    Iterator<String> it = upDocsInWiki.iterator();
                    while (it.hasNext()) {
                        results.add(wikiPrefix + it.next());
                    }
                } catch (Exception e) {
                    LOGGER.error("error getting list of documents in the wiki : " + wiki, e);
                }
            }
        } finally {
            context.setWikiId(oriDatabase);
        }

        return results;
    }

    /**
     * Manage events affecting watchlist job objects.
     * 
     * @param originalDoc document version before the event occurred
     * @param currentDoc document version after event occurred
     * @param context the XWiki context
     */
    private void watchListJobObjectsEventHandler(XWikiDocument originalDoc, XWikiDocument currentDoc,
        XWikiContext context)
    {
        boolean reinitWatchListClass = false;

        BaseObject originalJob = originalDoc.getObject(WatchListJobManager.WATCHLIST_JOB_CLASS);
        BaseObject currentJob = currentDoc.getObject(WatchListJobManager.WATCHLIST_JOB_CLASS);

        if (originalJob != null && currentJob == null) {
            if (jobDocumentNames.contains(originalDoc.getFullName())) {
                int index = jobDocumentNames.indexOf(originalDoc.getFullName());
                jobDocumentNames.remove(index);
                destroySubscribersCache(originalDoc.getFullName(), context);
                reinitWatchListClass = true;
            }
        }

        if (originalJob == null && currentJob != null) {
            jobDocumentNames.add(currentDoc.getFullName());
            initSubscribersCache(currentDoc.getFullName(), context);
            reinitWatchListClass = true;
        }

        if (reinitWatchListClass) {
            try {
                initWatchListClass(context);
            } catch (XWikiException e) {
                // Do nothing
            }
        }
    }

    /**
     * Manage events affecting watchlist objects.
     * 
     * @param originalDoc document version before the event occurred
     * @param currentDoc document version after event occurred
     * @param context the XWiki context
     */
    private void watchListObjectsEventHandler(XWikiDocument originalDoc, XWikiDocument currentDoc, XWikiContext context)
    {
        String wiki = context.getWikiId();
        BaseObject originalWatchListObj = originalDoc.getObject(WATCHLIST_CLASS);
        BaseObject currentWatchListObj = currentDoc.getObject(WATCHLIST_CLASS);

        if (originalWatchListObj != null) {
            // Existing subscriber

            String oriInterval = originalWatchListObj.getStringValue(WATCHLIST_CLASS_INTERVAL_PROP);

            // If a subscriber has been deleted, remove it from our cache and exit
            if (currentWatchListObj == null) {
                removeSubscriberForJob(oriInterval, wiki + WIKI_SPACE_SEP + originalDoc.getFullName());
                return;
            }

            // If the subscription object has been deleted, remove the subscriber from our cache and exit
            if (originalWatchListObj != null && currentDoc.getObject(WATCHLIST_CLASS) == null) {
                removeSubscriberForJob(oriInterval, wiki + WIKI_SPACE_SEP + originalDoc.getFullName());
                return;
            }

            // Modification of the interval
            String newInterval = currentWatchListObj.getStringValue(WATCHLIST_CLASS_INTERVAL_PROP);

            if (!newInterval.equals(oriInterval)) {
                removeSubscriberForJob(oriInterval, wiki + WIKI_SPACE_SEP + originalDoc.getFullName());
                addSubscriberForJob(newInterval, wiki + WIKI_SPACE_SEP + currentDoc.getFullName());
            }
        }

        if ((originalWatchListObj == null || originalDoc == null) && currentWatchListObj != null) {
            // New subscriber
            String newInterval = currentWatchListObj.getStringValue(WATCHLIST_CLASS_INTERVAL_PROP);

            addSubscriberForJob(newInterval, wiki + WIKI_SPACE_SEP + currentDoc.getFullName());
        }
    }

    /**
     * Get automatic document edition watching mode based on user profile and xwiki.cfg.
     * 
     * @param user the user
     * @param context the XWiki context
     * @return the mode, if not set return the default one which is {@link AutomaticWatchMode#MAJOR}
     */
    public AutomaticWatchMode getAutomaticWatchMode(String user, XWikiContext context)
    {
        AutomaticWatchMode mode = null;

        try {
            BaseObject watchObject = getWatchListObject(user, context);

            String value = watchObject.getStringValue(WATCHLIST_CLASS_AUTOMATICWATCH);

            if (value != null && !value.equals("default")) {
                mode = AutomaticWatchMode.valueOf(value);
            }
        } catch (Exception e) {
            // Failed for some reason, now try getting it from xwiki.cfg
        }

        if (mode == null) {
            String value = context.getWiki().Param("xwiki.plugin.watchlist.automaticwatch");

            if (value != null) {
                try {
                    mode = AutomaticWatchMode.valueOf(value.toUpperCase());
                } catch (Exception e) {
                    LOGGER.warn("Invalid configuration in xwiki.plugin.watchlist.automaticwatch", e);
                }
            }
        }

        return mode != null ? mode : AutomaticWatchMode.MAJOR;
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument currentDoc = (XWikiDocument) source;
        XWikiDocument originalDoc = currentDoc.getOriginalDocument();
        XWikiContext context = (XWikiContext) data;

        watchListJobObjectsEventHandler(originalDoc, currentDoc, context);
        watchListObjectsEventHandler(originalDoc, currentDoc, context);
    }

    @Override
    public List<Event> getEvents()
    {
        return LISTENER_EVENTS;
    }

    @Override
    public String getName()
    {
        return LISTENER_NAME;
    }
}
