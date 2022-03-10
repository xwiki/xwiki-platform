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
package org.xwiki.notifications.filters.watchlistbridge.internal;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.collections4.SetUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterPreferenceProvider;
import org.xwiki.notifications.filters.NotificationFilterProperty;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.internal.DefaultNotificationFilterPreference;
import org.xwiki.notifications.filters.internal.scope.ScopeNotificationFilter;
import org.xwiki.notifications.filters.watch.WatchedEntitiesConfiguration;
import org.xwiki.notifications.preferences.internal.cache.UnboundedEntityCacheManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.ListProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.ListClass;

import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;

/**
 * Bridge to return watchlist preferences as NotificationFilterPreference, and to delete some of them.
 *
 * @version $Id$
 * @since 9.8RC1
 * @deprecated Since 11.10.11, 12.6.5, 12.10, this should not be used and new implementation
 *              should not be provided here.
 */
@Component
@Singleton
@Named(WatchlistBridgeProvider.PROVIDER_HINT)
// TODO: migrate watchlist objects to filters instead of dynamically converting them every time
// (see: https://jira.xwiki.org/browse/XWIKI-17243)
@Deprecated
public class WatchlistBridgeProvider implements NotificationFilterPreferenceProvider, Initializable
{
    /**
     * Hint of the provider.
     */
    public static final String PROVIDER_HINT = "watchlistbridge";

    private static final String PREFERENCEFILTERCACHE_NAME = "WatchlistFilterPreferences";

    private static final LocalDocumentReference CLASS_REFERENCE = new LocalDocumentReference("XWiki",
        "WatchListClass");

    private static final String FIELD_WIKIS = "wikis";

    private static final String FIELD_SPACES = "spaces";

    private static final String FIELD_DOCUMENTS = "documents";

    private static final String WATCHLIST_FILTER_PREFERENCES_NAME = "watchlist_%s_%s";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private WatchedEntitiesConfiguration configuration;

    @Inject
    private UnboundedEntityCacheManager cacheManager;

    @Inject
    private Logger logger;

    private Map<EntityReference, Set<NotificationFilterPreference>> preferenceCache;

    @Override
    public void initialize() throws InitializationException
    {
        this.preferenceCache = this.cacheManager.createCache(PREFERENCEFILTERCACHE_NAME, true);
    }

    @Override
    public Set<NotificationFilterPreference> getFilterPreferences(DocumentReference user)
    {
        if (!configuration.isEnabled()) {
            return Collections.emptySet();
        }

        Set<NotificationFilterPreference> results = this.preferenceCache.get(user);
        if (results != null) {
            return results;
        }

        XWikiContext context = contextProvider.get();
        XWiki xwiki = context.getWiki();

        results = new HashSet<>();
        try {
            XWikiDocument document = xwiki.getDocument(user, context);
            List<BaseObject> objects = document.getXObjects(CLASS_REFERENCE.appendParent(user.getWikiReference()));
            if (objects == null) {
                return Collections.emptySet();
            }

            for (BaseObject obj : objects) {
                if (obj == null) {
                    continue;
                }
                getValues(obj, FIELD_WIKIS, NotificationFilterProperty.WIKI, results);
                getValues(obj, FIELD_SPACES, NotificationFilterProperty.SPACE, results);
                getValues(obj, FIELD_DOCUMENTS, NotificationFilterProperty.PAGE, results);
            }
        } catch (XWikiException e) {
            logger.error("Failed to read the preferences of the watchlist for the user {}.", user, e);
        }

        this.preferenceCache.put(user, results);

        return results;
    }

    private void getValues(BaseObject obj, String fieldName, NotificationFilterProperty property,
        Set<NotificationFilterPreference> results)
    {
        List<String> values;
        PropertyInterface objProperty = obj.safeget(fieldName);
        // Support both pre and post 7.0 type of watchlist objects
        if (objProperty instanceof ListProperty) {
            values = ((ListProperty) objProperty).getList();
        } else if (objProperty instanceof LargeStringProperty) {
            values = ListClass.getListFromString(((LargeStringProperty) objProperty).getValue(), ",", false);
        } else {
            values = Collections.emptyList();
        }

        if (values != null && !values.isEmpty()) {
            for (String value : values) {
                DefaultNotificationFilterPreference pref = createNotificationFilterPreference(
                        String.format(WATCHLIST_FILTER_PREFERENCES_NAME, property.name(), sha256Hex(value)));
                switch (property) {
                    case PAGE:
                        pref.setPageOnly(value);
                        break;
                    case SPACE:
                        pref.setPage(value);
                        break;
                    case WIKI:
                        pref.setWiki(value);
                        break;
                    default:
                        break;
                }
                results.add(pref);
            }
        }
    }

    private DefaultNotificationFilterPreference createNotificationFilterPreference(String id)
    {
        DefaultNotificationFilterPreference pref = new DefaultNotificationFilterPreference();
        pref.setId(id);
        pref.setEnabled(true);
        pref.setNotificationFormats(SetUtils.hashSet(NotificationFormat.values()));
        pref.setProviderHint(PROVIDER_HINT);
        pref.setFilterName(ScopeNotificationFilter.FILTER_NAME);
        pref.setFilterType(NotificationFilterType.INCLUSIVE);
        return pref;
    }

    @Override
    public void saveFilterPreferences(DocumentReference user, Set<NotificationFilterPreference> filterPreferences)
    {
        // We do not want to create any new preference for the watchlist application, since we want to migrate
        // smoothly to the new notification mechanism
    }

    @Override
    public void deleteFilterPreference(DocumentReference user, String filterPreferenceId)
    {
        if (!configuration.isEnabled()) {
            return;
        }

        String[] parts = filterPreferenceId.split("_");
        if (parts.length != 3 || !"watchlist".equals(parts[0])) {
            return;
        }
        String type = extractType(parts);
        String fieldName = extractFieldName(type);

        XWikiContext context = contextProvider.get();
        XWiki xwiki = context.getWiki();

        try {
            XWikiDocument document = xwiki.getDocument(user, context);
            List<BaseObject> objects = document.getXObjects(
                CLASS_REFERENCE.appendParent(user.getWikiReference()));
            if (objects == null) {
                return;
            }

            boolean found = false;
            for (BaseObject obj : objects) {
                if (obj == null) {
                    continue;
                }
                found |= removeValueFromObject(filterPreferenceId, type, fieldName, obj);
            }

            if (found) {
                xwiki.saveDocument(document, "Remove a watchlist preference.", context);
            }
        } catch (XWikiException e) {
            logger.error("Failed to delete a preference of the watchlist for the user {}.", user, e);
        }
    }

    private boolean removeValueFromObject(String filterPreferenceName, String type, String fieldName, BaseObject obj)
    {
        boolean found = false;
        List<String> values = obj.getListValue(fieldName);
        if (values != null && !values.isEmpty()) {
            Iterator<String> iterator = values.iterator();
            while (iterator.hasNext()) {
                String currentValue = iterator.next();
                String currentFilterPreferenceName = String.format(WATCHLIST_FILTER_PREFERENCES_NAME,
                    type, sha256Hex(currentValue));
                if (currentFilterPreferenceName.equals(filterPreferenceName)) {
                    iterator.remove();
                    found = true;
                }
            }
        }
        return found;
    }

    private String extractType(String[] part)
    {
        return part[1];
    }

    private String extractFieldName(String type)
    {
        return NotificationFilterProperty.WIKI.name().equals(type) ? FIELD_WIKIS
            : NotificationFilterProperty.SPACE.name().equals(type) ? FIELD_SPACES : FIELD_DOCUMENTS;
    }

    @Override
    public void setFilterPreferenceEnabled(DocumentReference user, String filterPreferenceName, boolean enabled)
    {
        // Watchlist preferences are always enabled
    }

    @Override
    public void setStartDateForUser(DocumentReference user, Date startDate)
    {
        // Unavailable
    }
}
