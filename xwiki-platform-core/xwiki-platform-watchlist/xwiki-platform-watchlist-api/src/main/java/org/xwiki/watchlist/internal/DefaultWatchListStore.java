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
package org.xwiki.watchlist.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.watchlist.internal.api.AutomaticWatchMode;
import org.xwiki.watchlist.internal.api.WatchListStore;
import org.xwiki.watchlist.internal.api.WatchedElementType;
import org.xwiki.watchlist.internal.documents.WatchListClassDocumentInitializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * WatchList store class. Handles user subscription storage.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class DefaultWatchListStore implements WatchListStore
{
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
     * XWiki Class used to store user.
     */
    public static final String USERS_CLASS = "XWiki.XWikiUsers";

    /**
     * Logging helper object.
     */
    @Inject
    private Logger logger;

    /**
     * Context provider.
     */
    @Inject
    private Provider<XWikiContext> contextProvider;

    /**
     * Used to read cached notification related information.
     */
    @Inject
    private Provider<WatchListNotificationCache> notificationCache;

    /**
     * Used to resolve translations.
     */
    @Inject
    private ContextualLocalizationManager localization;

    @Override
    public Collection<String> getWatchedElements(String user, WatchedElementType type) throws XWikiException
    {
        BaseObject watchListObject = getWatchListObject(user);
        List<String> watchedItems = watchListObject.getListValue(getWatchListClassPropertyForType(type));

        return watchedItems;
    }

    /**
     * Gets the WatchList XWiki Object from user's profile's page.
     * 
     * @param user XWiki User
     * @return the WatchList XWiki BaseObject
     * @throws XWikiException if BaseObject creation fails or if user does not exists
     */
    public BaseObject getWatchListObject(String user) throws XWikiException
    {
        XWikiContext context = contextProvider.get();

        XWikiDocument userDocument = context.getWiki().getDocument(user, context);
        if (userDocument.isNew() || userDocument.getObject(USERS_CLASS) == null) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN, "User ["
                + user + "] does not exists");
        }

        BaseObject obj = userDocument.getObject(WatchListClassDocumentInitializer.DOCUMENT_FULL_NAME);
        if (obj == null) {
            obj = createWatchListObject(user, context);
        }

        return obj;
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

        BaseObject object = userDocument.newObject(WatchListClassDocumentInitializer.DOCUMENT_FULL_NAME, context);
        context.getWiki().saveDocument(userDocument, this.localization.getTranslationPlain("watchlist.create.object"),
            true, context);

        return object;
    }

    /**
     * Get the name of the XClass property the given type is stored in.
     * 
     * @param type type to retrieve
     * @return the name of the XClass property
     */
    private String getWatchListClassPropertyForType(WatchedElementType type)
    {
        String result = StringUtils.EMPTY;

        switch (type) {
            case WIKI:
                result = WatchListClassDocumentInitializer.WIKIS_PROPERTY;
                break;
            case SPACE:
                result = WatchListClassDocumentInitializer.SPACES_PROPERTY;
                break;
            case DOCUMENT:
                result = WatchListClassDocumentInitializer.DOCUMENTS_PROPERTY;
                break;
            case USER:
                result = WatchListClassDocumentInitializer.USERS_PROPERTY;
                break;
            default:
                break;
        }

        return result;
    }

    @Override
    public boolean isWatched(String element, String user, WatchedElementType type) throws XWikiException
    {
        // TODO: Can this be optimized by a direct "exists" query on the list item? Would it e better than what we
        // currently have with the document cache? If we try a query, it would also need to be performed on the user's
        // wiki/database, not the current one.
        return getWatchedElements(user, type).contains(element);
    }

    @Override
    public boolean addWatchedElement(String user, String newWatchedElement, WatchedElementType type)
        throws XWikiException
    {
        XWikiContext context = contextProvider.get();
        String elementToWatch = newWatchedElement;

        if (!WatchedElementType.WIKI.equals(type) && !newWatchedElement.contains(WIKI_SPACE_SEP)) {
            elementToWatch = context.getWikiId() + WIKI_SPACE_SEP + newWatchedElement;
        }

        if (isWatched(elementToWatch, user, type)) {
            return false;
        }

        // Copy the list of watched elements because it could be unmodifiable.
        List<String> watchedElements = new ArrayList<String>(getWatchedElements(user, type));

        watchedElements.add(elementToWatch);

        setWatchListElementsProperty(user, type, watchedElements);
        return true;
    }

    /**
     * Sets a DBList property in the user's WatchList Object, then saves the user's profile.
     * 
     * @param user the user whose watchlist to set
     * @param type the element type as defined by {@link WatchedElementType}
     * @param elements the elements to store
     * @throws XWikiException if the user's profile cannot be saved
     */
    private void setWatchListElementsProperty(String user, WatchedElementType type, Collection<String> elements)
        throws XWikiException
    {
        XWikiContext context = contextProvider.get();
        XWikiDocument userDocument = context.getWiki().getDocument(user, context);

        List<String> elementsList = new ArrayList<String>(elements);
        userDocument.setDBStringListValue(WatchListClassDocumentInitializer.DOCUMENT_FULL_NAME,
            getWatchListClassPropertyForType(type), elementsList);

        context.getWiki().saveDocument(userDocument, localization.getTranslationPlain("watchlist.save.object"), true,
            context);
    }

    @Override
    public boolean removeWatchedElement(String user, String watchedElement, WatchedElementType type)
        throws XWikiException
    {
        XWikiContext context = contextProvider.get();
        String elementToRemove = watchedElement;

        if (!WatchedElementType.WIKI.equals(type) && !watchedElement.contains(WIKI_SPACE_SEP)) {
            elementToRemove = context.getWikiId() + WIKI_SPACE_SEP + watchedElement;
        }

        if (!this.isWatched(elementToRemove, user, type)) {
            return false;
        }

        Collection<String> watchedElements = getWatchedElements(user, type);
        watchedElements.remove(elementToRemove);

        this.setWatchListElementsProperty(user, type, watchedElements);
        return true;
    }

    @Override
    public AutomaticWatchMode getAutomaticWatchMode(String user)
    {
        XWikiContext context = contextProvider.get();
        AutomaticWatchMode mode = null;

        try {
            BaseObject watchObject = getWatchListObject(user);

            String value = watchObject.getStringValue(WatchListClassDocumentInitializer.AUTOMATICWATCH_PROPERTY);

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
                    logger.warn("Invalid configuration in xwiki.plugin.watchlist.automaticwatch", e);
                }
            }
        }

        return mode != null ? mode : AutomaticWatchMode.MAJOR;
    }

    @Override
    public Collection<String> getJobDocumentNames()
    {
        return notificationCache.get().getJobDocumentNames();
    }

    @Override
    public Collection<String> getSubscribers(String intervalId)
    {
        return notificationCache.get().getSubscribers(intervalId);
    }

}
