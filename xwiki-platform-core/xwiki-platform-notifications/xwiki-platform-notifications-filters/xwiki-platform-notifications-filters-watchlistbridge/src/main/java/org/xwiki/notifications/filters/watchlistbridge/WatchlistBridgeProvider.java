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
package org.xwiki.notifications.filters.watchlistbridge;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterPreferenceProvider;
import org.xwiki.notifications.filters.NotificationFilterProperty;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.internal.DefaultNotificationFilterPreference;
import org.xwiki.notifications.filters.internal.scope.ScopeNotificationFilter;
import org.xwiki.notifications.filters.watch.WatchedEntitiesConfiguration;

import com.google.common.collect.Sets;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;

/**
 * Bridge to return watchlist preferences as NotificationFilterPreference, and to delete some of them.
 *
 * @version $Id$
 * @since 9.8RC1
 */
@Component
@Singleton
@Named(WatchlistBridgeProvider.PROVIDER_HINT)
public class WatchlistBridgeProvider implements NotificationFilterPreferenceProvider
{
    /**
     * Hint of the provider.
     */
    public static final String PROVIDER_HINT = "watchlistbridge";

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
    private Logger logger;

    @Override
    public Set<NotificationFilterPreference> getFilterPreferences(DocumentReference user)
            throws NotificationException
    {
        if (!configuration.isEnabled()) {
            return Collections.emptySet();
        }

        XWikiContext context = contextProvider.get();
        XWiki xwiki = context.getWiki();

        Set<NotificationFilterPreference> results = new HashSet<>();
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

        return results;
    }

    private void getValues(BaseObject obj, String fieldName, NotificationFilterProperty property,
            Set<NotificationFilterPreference> results)
    {
        List<String> values = obj.getListValue(fieldName);
        if (values != null && !values.isEmpty()) {
            for (String value : values) {
                DefaultNotificationFilterPreference pref = createDefaultNotificationFilterPreference(
                        String.format(WATCHLIST_FILTER_PREFERENCES_NAME, property.name(), sha256Hex(value)));
                Map<NotificationFilterProperty, List<String>> preferenceProperties = new HashMap<>();
                preferenceProperties.put(property, Collections.singletonList(value));
                pref.setPreferenceProperties(preferenceProperties);
                results.add(pref);
            }
        }
    }

    private DefaultNotificationFilterPreference createDefaultNotificationFilterPreference(String name)
    {
        DefaultNotificationFilterPreference pref = new DefaultNotificationFilterPreference(name);
        pref.setEnabled(true);
        pref.setNotificationFormats(Sets.newHashSet(NotificationFormat.values()));
        pref.setProviderHint(PROVIDER_HINT);
        pref.setFilterName(ScopeNotificationFilter.FILTER_NAME);
        pref.setFilterType(NotificationFilterType.INCLUSIVE);
        pref.setPreferenceProperties(new HashMap<>());
        return pref;
    }

    @Override
    public void saveFilterPreferences(Set<NotificationFilterPreference> filterPreferences)
            throws NotificationException
    {
        // We do not want to create any new preference for the watchlist application, since we want to migrate
        // smoothly to the new notification mechanism
    }

    @Override
    public void deleteFilterPreference(String filterPreferenceName) throws NotificationException
    {
        if (!configuration.isEnabled()) {
            return;
        }

        String[] parts = filterPreferenceName.split("_");
        if (parts.length != 3 || !"watchlist".equals(parts[0])) {
            return;
        }
        String type = extractType(parts);
        String fieldName = extractFieldName(type);

        XWikiContext context = contextProvider.get();
        XWiki xwiki = context.getWiki();
        DocumentReference user = context.getUserReference();

        try {
            XWikiDocument document = xwiki.getDocument(user, context);
            List<BaseObject> objects = document.getXObjects(
                    CLASS_REFERENCE.appendParent(context.getUserReference().getWikiReference()));
            if (objects == null) {
                return;
            }

            boolean found = false;
            for (BaseObject obj : objects) {
                if (obj == null) {
                    continue;
                }
                found |= removeValueFromObject(filterPreferenceName, type, fieldName, obj);
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
    public void setFilterPreferenceEnabled(String filterPreferenceName, boolean enabled)
            throws NotificationException
    {
        // Watchlist preferences are always enabled
    }

    @Override
    public void setStartDateForUser(DocumentReference user, Date startDate) throws NotificationException
    {
        // Unavailable
    }
}
