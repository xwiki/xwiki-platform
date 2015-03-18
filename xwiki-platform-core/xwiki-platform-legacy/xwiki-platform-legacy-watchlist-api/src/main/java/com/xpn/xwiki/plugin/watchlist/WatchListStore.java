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
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.watchlist.internal.WatchListNotificationCacheListener;
import org.xwiki.watchlist.internal.api.WatchedElementType;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.Utils;

/**
 * WatchList store class. Handles user subscription storage.
 * 
 * @version $Id$
 */
@Deprecated
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
     * Logging helper object.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(WatchListStore.class);

    /**
     * XWiki Class used to store user subscriptions.
     */
    private static final String WATCHLIST_CLASS = "XWiki.WatchListClass";

    /**
     * XWiki Class used to store user.
     */
    private static final String USERS_CLASS = "XWiki.XWikiUsers";

    /**
     * The wrapped component.
     */
    private org.xwiki.watchlist.internal.api.WatchListStore store;

    /**
     * The now externalized event listener that this class used to implement.
     */
    private EventListener listener;

    /**
     * Init watchlist store. Get all the jobs present in the wiki. Create the list of subscribers.
     * 
     * @param context the XWiki context
     * @throws XWikiException if the watchlist XWiki class creation fails
     */
    public void init(XWikiContext context) throws XWikiException
    {
        this.store = Utils.getComponent(org.xwiki.watchlist.internal.api.WatchListStore.class);
        this.listener = Utils.getComponent(EventListener.class, WatchListNotificationCacheListener.LISTENER_NAME);
    }

    /**
     * Virtual init for watchlist store. Create the WatchList XWiki class.
     * 
     * @param context the XWiki context
     * @throws XWikiException if the watchlist XWiki class creation fails
     */
    public void virtualInit(XWikiContext context) throws XWikiException
    {

    }

    /**
     * @param jobId ID of the job.
     * @return subscribers for the given notification job.
     */
    public List<String> getSubscribersForJob(String jobId)
    {
        return new ArrayList<>(this.store.getSubscribers(jobId));
    }

    /**
     * @return Names of documents which contain a watchlist job object.
     */
    public List<String> getJobDocumentNames()
    {
        return new ArrayList<>(this.store.getJobDocumentNames());
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
        return new ArrayList<>(this.store.getWatchedElements(user, getWatchedElementType(type)));
    }

    /**
     * @param type the value to convert
     * @return the corresponding {@link WatchedElementType} value or null if null was passed
     */
    private WatchedElementType getWatchedElementType(ElementType type)
    {
        WatchedElementType watchedElementType = type == null ? null : WatchedElementType.valueOf(type.name());
        return watchedElementType;
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
        return this.store.isWatched(element, user, getWatchedElementType(type));
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
        return this.store.addWatchedElement(user, newWatchedElement, getWatchedElementType(type));
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
        return this.store.removeWatchedElement(user, watchedElement, getWatchedElementType(type));
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
     * Get automatic document edition watching mode based on user profile and xwiki.cfg.
     * 
     * @param user the user
     * @param context the XWiki context
     * @return the mode, if not set return the default one which is {@link AutomaticWatchMode#MAJOR}
     */
    public AutomaticWatchMode getAutomaticWatchMode(String user, XWikiContext context)
    {
        org.xwiki.watchlist.internal.api.AutomaticWatchMode watchMode = this.store.getAutomaticWatchMode(user);

        return AutomaticWatchMode.valueOf(watchMode.name());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        this.listener.onEvent(event, source, data);
    }

    @Override
    public List<Event> getEvents()
    {
        return this.listener.getEvents();
    }

    @Override
    public String getName()
    {
        return this.listener.getName();
    }
}
