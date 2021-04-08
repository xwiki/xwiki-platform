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
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.hibernate.Session;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.internal.event.NotificationFilterPreferenceAddOrUpdatedEvent;
import org.xwiki.notifications.filters.internal.event.NotificationFilterPreferenceDeletedEvent;
import org.xwiki.notifications.preferences.internal.UserProfileNotificationPreferenceProvider;
import org.xwiki.notifications.preferences.internal.WikiNotificationPreferenceProvider;
import org.xwiki.observation.ObservationManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.text.StringUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateStore;

/**
 * Store component to load and save notification filter preferences into the corresponding table in hibernate.
 *
 * @version $Id$
 * @since 10.8RC1
 * @since 9.11.8
 */
@Component(roles = NotificationFilterPreferenceStore.class)
@Singleton
public class NotificationFilterPreferenceStore
{
    @Inject
    private NotificationFilterPreferenceConfiguration filterPreferenceConfiguration;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private QueryManager queryManager;

    @Inject
    private ObservationManager observation;

    /**
     * Get the notification preference that corresponds to the given id and user.
     * 
     * @param user a user
     * @param filterPreferenceId a filter preference id
     * @return the corresponding preference
     * @throws NotificationException if an error occurs
     */
    public NotificationFilterPreference getFilterPreference(DocumentReference user, String filterPreferenceId)
        throws NotificationException
    {
        for (NotificationFilterPreference preference : getPreferencesOfUser(user)) {
            if (StringUtils.equals(preference.getId(), filterPreferenceId)) {
                return preference;
            }
        }
        return null;
    }

    /**
     * Get the notification preference that corresponds to the given id and wiki.
     *
     * @param wikiReference a wiki
     * @param filterPreferenceId a filter preference id
     * @return the corresponding preference
     * @throws NotificationException if an error occurs
     */
    public NotificationFilterPreference getFilterPreference(WikiReference wikiReference, String filterPreferenceId)
        throws NotificationException
    {
        for (NotificationFilterPreference preference : getPreferencesOfWiki(wikiReference)) {
            if (StringUtils.equals(preference.getId(), filterPreferenceId)) {
                return preference;
            }
        }
        return null;
    }

    /**
     * Get all the notification preferences that corresponds to the given user.
     *
     * @param user the user from which we need to extract the preference
     * @return a set of available filter preferences
     * @throws NotificationException if an error happens
     */
    public List<DefaultNotificationFilterPreference> getPreferencesOfUser(DocumentReference user)
        throws NotificationException
    {
        try {
            return this.getPreferencesOfEntity(user, UserProfileNotificationPreferenceProvider.NAME);
        } catch (QueryException e) {
            throw new NotificationException(String.format(
                "Error while loading the notification filter preferences of the user [%s].", user.toString()), e);
        }
    }

    /**
     * Get all the notification preferences that corresponds to the given user.
     *
     * @param wikiReference the wiki from which we need to extract the preferences
     * @return a set of available filter preferences
     * @throws NotificationException if an error happens
     * @since 13.3RC1
     */
    public List<DefaultNotificationFilterPreference> getPreferencesOfWiki(WikiReference wikiReference)
        throws NotificationException
    {
        try {
            return getPreferencesOfEntity(wikiReference, WikiNotificationPreferenceProvider.NAME);
        } catch (QueryException e) {
            throw new NotificationException(String.format(
                "Error while loading the notification filter preferences of the wiki [%s].", wikiReference.getName()),
                e);
        }
    }

    private List<DefaultNotificationFilterPreference> getPreferencesOfEntity(EntityReference entityReference,
        String providerHint) throws QueryException
    {
        if (entityReference == null) {
            return Collections.emptyList();
        }

        String serializedEntity = entityReferenceSerializer.serialize(entityReference);

        XWikiContext context = contextProvider.get();

        Query query = queryManager.createQuery(
            "select nfp from DefaultNotificationFilterPreference nfp where nfp.owner = :owner " + "order by nfp.id",
            Query.HQL);
        query.bindValue("owner", serializedEntity);
        if (filterPreferenceConfiguration.useMainStore()) {
            query.setWiki(context.getMainXWiki());
        }
        List<DefaultNotificationFilterPreference> results = query.execute();

        for (DefaultNotificationFilterPreference preference : results) {
            preference.setProviderHint(providerHint);
        }

        return results;
    }

    /**
     * Delete a filter preference.
     * 
     * @param user reference of the user concerned by the filter preference
     * @param filterPreferenceId name of the filter preference
     * @throws NotificationException if an error happens
     */
    public void deleteFilterPreference(DocumentReference user, String filterPreferenceId) throws NotificationException
    {
        NotificationFilterPreference preference = getFilterPreference(user, filterPreferenceId);
        this.deleteFilterPreference(preference);
    }

    /**
     * Delete a filter preference.
     *
     * @param wikiReference reference of the wiki concerned by the filter preference
     * @param filterPreferenceId name of the filter preference
     * @throws NotificationException if an error happens
     * @since 13.3RC1
     */
    public void deleteFilterPreference(WikiReference wikiReference, String filterPreferenceId)
        throws NotificationException
    {
        NotificationFilterPreference preference = getFilterPreference(wikiReference, filterPreferenceId);
        this.deleteFilterPreference(preference);
    }

    /**
     * Delete a filter preference.
     *
     * @param preference the preference to delete.
     * @throws NotificationException if an error happens
     */
    private void deleteFilterPreference(NotificationFilterPreference preference) throws NotificationException
    {
        if (preference == null) {
            return;
        }

        XWikiContext context = contextProvider.get();

        // store event in the main database
        String oriDatabase = context.getWikiId();

        if (filterPreferenceConfiguration.useMainStore()) {
            context.setWikiId(context.getMainXWiki());
        }

        context.setWikiId(context.getMainXWiki());
        XWikiHibernateStore hibernateStore = context.getWiki().getHibernateStore();
        try {
            hibernateStore.beginTransaction(context);
            Session session = hibernateStore.getSession(context);
            session.delete(preference);
            hibernateStore.endTransaction(context, true);
        } catch (XWikiException e) {
            hibernateStore.endTransaction(context, false);
        } finally {
            if (!context.getWikiId().equals(oriDatabase)) {
                context.setWikiId(oriDatabase);
            }
        }

        this.observation.notify(new NotificationFilterPreferenceDeletedEvent(), null);
    }

    /**
     * Save a collection of NotificationFilterPreferences.
     * 
     * @param user reference of the user concerned by the filter preference
     * @param filterPreferences a list of NotificationFilterPreference
     * @throws NotificationException if an error happens
     */
    public void saveFilterPreferences(DocumentReference user,
        Collection<NotificationFilterPreference> filterPreferences) throws NotificationException
    {
        this.saveFilterPreferences((EntityReference) user, filterPreferences);
    }

    /**
     * Save a collection of NotificationFilterPreferences.
     *
     * @param wikiReference reference of the wiki concerned by the filter preference
     * @param filterPreferences a list of NotificationFilterPreference
     * @throws NotificationException if an error happens
     * @since 13.3RC1
     */
    public void saveFilterPreferences(WikiReference wikiReference,
        Collection<NotificationFilterPreference> filterPreferences) throws NotificationException
    {
        this.saveFilterPreferences((EntityReference) wikiReference, filterPreferences);
    }

    /**
     * Save a collection of NotificationFilterPreferences.
     *
     * @param entityReference reference of the entity concerned by the filter preference
     * @param filterPreferences a list of NotificationFilterPreference
     * @throws NotificationException if an error happens
     */
    private void saveFilterPreferences(EntityReference entityReference,
        Collection<NotificationFilterPreference> filterPreferences) throws NotificationException
    {
        if (entityReference == null) {
            return;
        }

        String serializedEntity = entityReferenceSerializer.serialize(entityReference);

        XWikiContext context = contextProvider.get();

        // store event in the main database
        String currentWiki = context.getWikiId();

        XWikiHibernateStore hibernateStore = null;

        try {
            if (filterPreferenceConfiguration.useMainStore()) {
                // store event in the main database
                context.setWikiId(context.getMainXWiki());
            }

            hibernateStore = context.getWiki().getHibernateStore();
            hibernateStore.beginTransaction(context);
            Session session = hibernateStore.getSession(context);

            for (NotificationFilterPreference preference : filterPreferences) {
                // Hibernate mapping only describes how to save NotificationFilterPreference objects and does not
                // handle extended objects (like ScopeNotificationFilterPreference).
                // So we create a copy just in case we are not saving a basic NotificationFilterPreference object.
                DefaultNotificationFilterPreference copy = new DefaultNotificationFilterPreference(preference);
                copy.setOwner(serializedEntity);
                session.saveOrUpdate(copy);
            }

            hibernateStore.endTransaction(context, true);

            for (int i = 0; i < filterPreferences.size(); ++i) {
                this.observation.notify(new NotificationFilterPreferenceAddOrUpdatedEvent(), null);
            }
        } catch (Exception e) {
            if (hibernateStore != null) {
                hibernateStore.endTransaction(context, false);
            }
            throw new NotificationException("Failed to save the notification filter preferences.", e);
        } finally {
            if (!currentWiki.equals(context.getWikiId())) {
                context.setWikiId(currentWiki);
            }
        }
    }
}
