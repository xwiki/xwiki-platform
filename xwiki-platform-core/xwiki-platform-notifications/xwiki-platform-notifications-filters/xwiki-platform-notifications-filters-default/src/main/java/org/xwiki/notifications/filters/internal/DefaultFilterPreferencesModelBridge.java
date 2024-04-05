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

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.internal.scope.ScopeNotificationFilter;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Default implementation for {@link FilterPreferencesModelBridge}.
 *
 * @version $Id$
 * @since 9.7RC1
 */
@Component
@Singleton
public class DefaultFilterPreferencesModelBridge implements FilterPreferencesModelBridge
{
    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private NotificationFilterPreferenceStore notificationFilterPreferenceStore;

    private List<DefaultNotificationFilterPreference> getInternalFilterPreferences(DocumentReference user)
            throws NotificationException
    {
        return notificationFilterPreferenceStore.getPreferencesOfUser(user);
    }

    @Override
    public Set<NotificationFilterPreference> getFilterPreferences(DocumentReference user)
            throws NotificationException
    {
        return new HashSet<>(getInternalFilterPreferences(user));
    }

    @Override
    public Set<NotificationFilterPreference> getFilterPreferences(WikiReference wikiReference)
        throws NotificationException
    {
        return new HashSet<>(this.notificationFilterPreferenceStore.getPreferencesOfWiki(wikiReference));
    }

    @Override
    public Map<String, ToggleableNotificationFilterActivation> getToggleableFilterActivations(DocumentReference user)
        throws NotificationException
    {
        XWikiContext context = contextProvider.get();
        WikiReference currentWiki = context.getWikiReference();
        context.setWikiReference(user.getWikiReference());
        XWiki xwiki = context.getWiki();

        Map<String, ToggleableNotificationFilterActivation> filterStatus = new HashMap<>();

        try {
            XWikiDocument doc = xwiki.getDocument(user, context);
            for (NotificationFilter filter : componentManager.<NotificationFilter>getInstanceList(
                NotificationFilter.class)) {
                if (filter instanceof ToggleableNotificationFilter toggleableFilter) {
                    boolean status = toggleableFilter.isEnabledByDefault();
                    BaseObject obj = doc.getXObject(ToggleableFilterPreferenceDocumentInitializer.XCLASS,
                        ToggleableFilterPreferenceDocumentInitializer.FIELD_FILTER_NAME,
                            filter.getName(), false);
                    int objNumber = -1;
                    if (obj != null) {
                        status = obj.getIntValue(ToggleableFilterPreferenceDocumentInitializer.FIELD_IS_ENABLED,
                            status ? 1 : 0) != 0;
                        objNumber = obj.getNumber();
                    }
                    ToggleableNotificationFilterActivation filterActivation =
                        new ToggleableNotificationFilterActivation(filter.getName(), status, user, objNumber);
                    filterStatus.put(filter.getName(), filterActivation);
                }
            }
        } catch (Exception e) {
            throw new NotificationException(
                    String.format("Failed to get the toggleable filters preferences for the user [%s].",
                            user), e);
        } finally {
            context.setWikiReference(currentWiki);
        }

        return filterStatus;
    }

    @Override
    public void deleteFilterPreference(DocumentReference user, String filterPreferenceId) throws NotificationException
    {
        notificationFilterPreferenceStore.deleteFilterPreference(user, filterPreferenceId);
    }

    @Override
    public void deleteFilterPreferences(DocumentReference user, Set<String> filterPreferenceIds)
        throws NotificationException
    {
        notificationFilterPreferenceStore.deleteFilterPreferences(user, filterPreferenceIds);
    }

    @Override
    public void deleteFilterPreferences(DocumentReference user) throws NotificationException
    {
        this.notificationFilterPreferenceStore.deleteFilterPreferences(user);
    }

    @Override
    public void deleteFilterPreference(WikiReference wikiReference, String filterPreferenceId)
        throws NotificationException
    {
        notificationFilterPreferenceStore.deleteFilterPreference(wikiReference, filterPreferenceId);
    }

    @Override
    public void deleteFilterPreferences(WikiReference wikiReference) throws NotificationException
    {
        this.notificationFilterPreferenceStore.deleteFilterPreference(wikiReference);
    }

    @Override
    public void setFilterPreferenceEnabled(DocumentReference user, String filterPreferenceId, boolean enabled)
            throws NotificationException
    {
        NotificationFilterPreference preference = notificationFilterPreferenceStore.getFilterPreference(user,
                filterPreferenceId);
        if (preference != null && enabled != preference.isEnabled()) {
            preference.setEnabled(enabled);
            saveFilterPreferences(user, Collections.singletonList(preference));
        }
    }

    @Override
    public void setFilterPreferenceEnabled(WikiReference wikiReference, String filterPreferenceId, boolean enabled)
        throws NotificationException
    {
        NotificationFilterPreference preference = notificationFilterPreferenceStore.getFilterPreference(wikiReference,
            filterPreferenceId);
        if (preference != null && enabled != preference.isEnabled()) {
            preference.setEnabled(enabled);
            saveFilterPreferences(wikiReference, Collections.singletonList(preference));
        }
    }

    @Override
    public void saveFilterPreferences(DocumentReference user,
            Collection<NotificationFilterPreference> filterPreferences) throws NotificationException
    {
        notificationFilterPreferenceStore.saveFilterPreferences(user, filterPreferences);
    }

    @Override
    public void saveFilterPreferences(WikiReference wikiReference,
        Collection<NotificationFilterPreference> filterPreferences) throws NotificationException
    {
        notificationFilterPreferenceStore.saveFilterPreferences(wikiReference, filterPreferences);
    }

    @Override
    public void setStartDateForUser(DocumentReference user, Date startDate) throws NotificationException
    {
        Set<NotificationFilterPreference> preferences = getFilterPreferences(user);

        if (preferences.isEmpty()) {
            return;
        }

        for (NotificationFilterPreference preference : preferences) {
            if (preference instanceof DefaultNotificationFilterPreference) {
                ((DefaultNotificationFilterPreference) preference).setStartingDate(startDate);
            }
        }

        saveFilterPreferences(user, preferences);
    }

    @Override
    public void createScopeFilterPreference(DocumentReference user,
            NotificationFilterType type, Set<NotificationFormat> formats,
            List<String> eventTypes, EntityReference reference) throws NotificationException
    {
        saveFilterPreferences(user,
            Collections.singletonList(getScopeFilterPreference(type, formats, eventTypes, reference)));
    }

    @Override
    public void createScopeFilterPreference(WikiReference wikiReference,
        NotificationFilterType type, Set<NotificationFormat> formats,
        List<String> eventTypes, EntityReference reference) throws NotificationException
    {
        saveFilterPreferences(wikiReference,
            Collections.singletonList(getScopeFilterPreference(type, formats, eventTypes, reference)));
    }

    private NotificationFilterPreference getScopeFilterPreference(NotificationFilterType type,
        Set<NotificationFormat> formats,
        List<String> eventTypes, EntityReference reference) throws NotificationException
    {
        DefaultNotificationFilterPreference preference = new DefaultNotificationFilterPreference();
        preference.setFilterType(type);
        preference.setNotificationFormats(formats);
        if (eventTypes != null && !eventTypes.isEmpty()) {
            preference.setEventTypes(new HashSet<>(eventTypes));
        }
        preference.setEnabled(true);
        preference.setActive(false);
        preference.setFilterName(ScopeNotificationFilter.FILTER_NAME);
        preference.setStartingDate(new Date());

        switch (reference.getType()) {
            case WIKI:
                preference.setWiki(reference.getName());
                break;
            case SPACE:
                preference.setPage(entityReferenceSerializer.serialize(reference));
                break;
            case DOCUMENT:
                preference.setPageOnly(entityReferenceSerializer.serialize(reference));
                break;
            default:
                break;
        }
        return preference;
    }
}
