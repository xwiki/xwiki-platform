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
package org.xwiki.notifications.filters.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.text.StringUtils;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.XWikiHibernateStore;

/**
 * Default implementation for {@link ModelBridge}.
 *
 * @version $Id$
 * @since 9.7RC1
 */
@Component
@Singleton
public class DefaultModelBridge implements ModelBridge
{
    private static final SpaceReference NOTIFICATION_CODE_SPACE = new SpaceReference("Code",
            new SpaceReference("Notifications",
                    new SpaceReference("XWiki", new WikiReference("xwiki"))
            )
    );

    private static final DocumentReference NOTIFICATION_FILTER_PREFERENCE_CLASS = new DocumentReference(
            "NotificationFilterPreferenceClass", NOTIFICATION_CODE_SPACE
    );

    private static final DocumentReference TOGGLEABLE_FILTER_PREFERENCE_CLASS = new DocumentReference(
            "ToggleableFilterPreferenceClass", NOTIFICATION_CODE_SPACE
    );

    private static final String FIELD_FILTER_NAME = "filterName";

    private static final String FIELD_IS_ENABLED = "isEnabled";

    private static final String FIELD_IS_ACTIVE = "isActive";

    private static final String FILTER_PREFERENCE_NAME = "filterPreferenceName";

    private static final String FIELD_APPLICATIONS = "applications";

    private static final String FIELD_EVENT_TYPES = "eventTypes";

    private static final String FIELD_PAGES = "pages";

    private static final String FIELD_SPACES = "spaces";

    private static final String FIELD_WIKIS = "wikis";

    private static final String FIELD_USERS = "users";

    private static final String FIELD_FILTER_TYPE = "filterType";

    private static final String FIELD_FILTER_FORMATS = "filterFormats";

    private static final String FIELD_STARTING_DATE = "startingDate";

    public static final String ALL_EVENT_TYPES = "allEventTypes";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private Logger logger;

    private List<NotificationFilterPreference> getInternalFilterPreferences(DocumentReference user)
            throws NotificationException
    {
        if (user == null) {
            return Collections.emptyList();
        }

        List<NotificationFilterPreference> results;

        String serializedUser = entityReferenceSerializer.serialize(user);

        XWikiContext context = contextProvider.get();

        // Search in the main database
        String oriDatabase = context.getWikiId();
        try {
            context.setWikiId(context.getMainXWiki());
            results =
                    context.getWiki().getStore().search(
                            "select nfp from NotificationFilterPreference nfp where nfp.owner = ?",
                            1000, 0, Arrays.asList(serializedUser), context);
        } catch (XWikiException e) {
            throw new NotificationException("error", e);
        } finally {
            context.setWikiId(oriDatabase);
        }

        return results;
    }

    @Override
    public Set<NotificationFilterPreference> getFilterPreferences(DocumentReference user)
            throws NotificationException
    {
        return new HashSet<>(getInternalFilterPreferences(user));
    }

    @Override
    public Map<String, Boolean> getToggeableFilterActivations(DocumentReference user) throws NotificationException
    {
        XWikiContext context = contextProvider.get();
        XWiki xwiki = context.getWiki();

        final DocumentReference notificationPreferencesScopeClass
                = TOGGLEABLE_FILTER_PREFERENCE_CLASS.setWikiReference(user.getWikiReference());

        Map<String, Boolean> filterStatus = new HashMap<>();

        try {
            XWikiDocument doc = xwiki.getDocument(user, context);
            for (NotificationFilter filter : componentManager.<NotificationFilter>getInstanceList(
                    NotificationFilter.class)) {
                if (filter instanceof ToggleableNotificationFilter) {
                    ToggleableNotificationFilter toggleableFilter = (ToggleableNotificationFilter) filter;
                    boolean status = toggleableFilter.isEnabledByDefault();
                    BaseObject obj = doc.getXObject(notificationPreferencesScopeClass, FIELD_FILTER_NAME,
                            filter.getName());
                    if (obj != null) {
                        status = obj.getIntValue(FIELD_IS_ENABLED, status ? 1 : 0) != 0;
                    }
                    filterStatus.put(filter.getName(), status);
                }
            }
        } catch (Exception e) {
            throw new NotificationException(
                    String.format("Failed to get the toggleable filters preferences for the user [%s].",
                            user), e);
        }

        return filterStatus;
    }

    @Override
    public void deleteFilterPreference(DocumentReference user, String filterPreferenceId) throws NotificationException
    {
        NotificationFilterPreference preference = getFilterPreference(user, filterPreferenceId);
        if (preference == null) {
            return;
        }

        XWikiContext context = contextProvider.get();

        // store event in the main database
        String oriDatabase = context.getWikiId();
        context.setWikiId(context.getMainXWiki());
        XWikiHibernateStore mainHibernateStore = context.getWiki().getHibernateStore();
        try {
            mainHibernateStore.beginTransaction(context);
            Session session = mainHibernateStore.getSession(context);
            session.delete(preference);
            mainHibernateStore.endTransaction(context, true);
        } catch (XWikiException e) {
            mainHibernateStore.endTransaction(context, false);
        } finally {
            context.setWikiId(oriDatabase);
        }
    }

    private NotificationFilterPreference getFilterPreference(DocumentReference user, String filterPreferenceId)
            throws NotificationException
    {
        for (NotificationFilterPreference preference : getInternalFilterPreferences(user)) {
            if (StringUtils.equals(preference.getId(), filterPreferenceId)) {
                return preference;
            }
        }
        return null;
    }

    @Override
    public void setFilterPreferenceEnabled(DocumentReference user, String filterPreferenceId, boolean enabled)
            throws NotificationException
    {
        NotificationFilterPreference preference = getFilterPreference(user, filterPreferenceId);
        if (preference != null && enabled != preference.isEnabled()) {
            preference.setEnabled(enabled);
            saveFilterPreferences(user, Collections.singletonList(preference));
        }
    }

    @Override
    public void saveFilterPreferences(DocumentReference user,
            Collection<NotificationFilterPreference> filterPreferences) throws NotificationException
    {
        if (user == null) {
            return;
        }

        String serializedUser = entityReferenceSerializer.serialize(user);

        XWikiContext context = contextProvider.get();

        // store event in the main database
        String oriDatabase = context.getWikiId();
        context.setWikiId(context.getMainXWiki());
        XWikiHibernateStore mainHibernateStore = context.getWiki().getHibernateStore();
        try {
            mainHibernateStore.beginTransaction(context);
            Session session = mainHibernateStore.getSession(context);
            for (NotificationFilterPreference preference : filterPreferences) {
                preference.setOwner(serializedUser);
                // Hibernate mapping only describes how to save NotificationFilterPreference objects and does not
                // handle extended objects (like ScopeNotificationFilterPreference).
                // So we create a copy just in case we are not saving a basic NotificationFilterPreference object.
                session.saveOrUpdate(new NotificationFilterPreference(preference));
            }
            mainHibernateStore.endTransaction(context, true);
        } catch (Exception e) {
            mainHibernateStore.endTransaction(context, false);
            throw new NotificationException("Failed to save the notification filter preferences.", e);
        } finally {
            context.setWikiId(oriDatabase);
        }
    }

    @Override
    public void setStartDateForUser(DocumentReference user, Date startDate) throws NotificationException
    {
        Set<NotificationFilterPreference> preferences = getFilterPreferences(user);

        if (preferences.isEmpty()) {
            return;
        }

        for (NotificationFilterPreference preference : preferences) {
            preference.setStartingDate(startDate);
        }

        saveFilterPreferences(user, preferences);
    }
}
