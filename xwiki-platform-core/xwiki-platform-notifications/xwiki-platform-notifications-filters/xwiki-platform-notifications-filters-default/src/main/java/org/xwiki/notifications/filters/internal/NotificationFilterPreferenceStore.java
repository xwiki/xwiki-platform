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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
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
    private static final String FILTER_PREFIX = "NFP_";
    private static final String ID = "id";

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private QueryManager queryManager;

    @Inject
    private ObservationManager observation;

    /**
     * Retrieve the notification preference that corresponds to the given id and wiki.
     *
     * @param wikiReference the wiki for which to retrieve a notification preference
     * @param filterPreferenceId a filter preference id
     * @return the corresponding preference or {@link Optional#empty()} if none can be found
     * @throws NotificationException if an error occurs
     * @since 16.3.0RC1
     */
    public Optional<NotificationFilterPreference> getFilterPreference(String filterPreferenceId,
        WikiReference wikiReference) throws NotificationException
    {
        Optional<NotificationFilterPreference> result = Optional.empty();
        DefaultNotificationFilterPreference filterPreference = configureContextWrapper(wikiReference, () -> {
            Query query;
            try {
                query = this.queryManager.createQuery(
                    "select nfp from DefaultNotificationFilterPreference nfp where nfp.id = :id",
                    Query.HQL);
                query.setLimit(1);
                query.bindValue(ID, filterPreferenceId);

                List<DefaultNotificationFilterPreference> results = query.execute();
                if (!results.isEmpty()) {
                    return results.get(0);
                }
            } catch (QueryException e) {
                throw new NotificationException(
                    String.format("Error while retrieving notification with id [%s]", filterPreferenceId), e);
            }
            return null;
        });
        if (filterPreference != null) {
            result = Optional.of(filterPreference);
        }

        return result;
    }

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
            throw new NotificationException(
                String.format("Error while loading the notification filter preferences of the wiki [%s].",
                    wikiReference.getName()),
                e);
        }
    }

    /**
     * @param limit the maximum number of results to return
     * @param offset the offset of the first result to return
     * @return all the notification filter preferences
     * @since 14.5
     * @since 14.4.1
     * @since 13.10.7
     */
    public Set<DefaultNotificationFilterPreference> getPaginatedFilterPreferences(int limit, int offset)
        throws NotificationException
    {
        return configureContextWrapper(null, () -> {
            try {
                List<DefaultNotificationFilterPreference> list = this.queryManager
                    .createQuery("select nfp from DefaultNotificationFilterPreference nfp order by nfp.internalId",
                        Query.HQL)
                    .setLimit(limit).setOffset(offset).execute();
                // We return DefaultNotificationFilterPreference instead of NotificationFilterPreference because we
                // need to have access to the owner of the notification filter preferences.
                return new HashSet<>(list);
            } catch (QueryException e) {
                String message =
                    String.format("Error while loading all the notification filter preferences on wiki [%s].",
                        this.contextProvider.get().getWikiId());
                throw new NotificationException(message, e);
            }
        });
    }

    private List<DefaultNotificationFilterPreference> getPreferencesOfEntity(EntityReference entityReference,
        String providerHint) throws QueryException
    {
        if (entityReference == null) {
            return Collections.emptyList();
        }
        WikiReference wikiReference = (WikiReference) entityReference.extractReference(EntityType.WIKI);
        return configureContextWrapper(wikiReference, () -> {
            String serializedEntity = this.entityReferenceSerializer.serialize(entityReference);

            Query query = this.queryManager.createQuery(
                "select nfp from DefaultNotificationFilterPreference nfp where nfp.owner = :owner order by nfp.id",
                Query.HQL);
            query.bindValue("owner", serializedEntity);

            List<DefaultNotificationFilterPreference> results = query.execute();

            for (DefaultNotificationFilterPreference preference : results) {
                preference.setProviderHint(providerHint);
            }

            return results;
        });
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
        deleteFilterPreferences(user, Set.of(filterPreferenceId));
    }

    /**
     * Delete filter preferences.
     *
     * @param user reference of the user concerned by the filter preference
     * @param filterPreferenceIds name of the filter preferences
     * @throws NotificationException if an error happens
     * @since 16.0.0RC1
     * @since 15.10.2
     */
    public void deleteFilterPreferences(DocumentReference user, Set<String> filterPreferenceIds)
        throws NotificationException
    {
        Set<Long> filterPreferenceInternalIds = new HashSet<>();
        for (String filterPreferenceId : filterPreferenceIds) {
            filterPreferenceInternalIds.add(getInternalIdFromId(filterPreferenceId));
        }

        this.deleteFilterPreferences(user.getWikiReference(), filterPreferenceInternalIds);
        this.observation.notify(new NotificationFilterPreferenceDeletedEvent(), user, filterPreferenceIds);
    }

    /**
     * Delete all to notification preferences where the given user is the owner.
     *
     * @param user the document reference of a user
     * @throws NotificationException in case of error during the hibernate operations
     * @since 14.5
     * @since 14.4.1
     * @since 13.10.7
     */
    public void deleteFilterPreferences(DocumentReference user) throws NotificationException
    {
        configureContextWrapper(user.getWikiReference(), () -> {
            XWikiContext context = this.contextProvider.get();

            XWikiHibernateStore hibernateStore = context.getWiki().getHibernateStore();

            String serializedUser = this.entityReferenceSerializer.serialize(user);

            try {
                hibernateStore.executeWrite(context, session -> {
                    session
                        .createQuery(
                            "delete from DefaultNotificationFilterPreference where owner = :user or user = :user")
                        .setParameter("user", serializedUser).executeUpdate();

                    return null;
                });                
            } catch (XWikiException e) {
                throw new NotificationException(
                    String.format("Failed to delete the notification preferences for user [%s]", user), e);
            }

            return null;
        });
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
        this.deleteFilterPreferences(wikiReference, Set.of(getInternalIdFromId(filterPreferenceId)));
        this.observation.notify(new NotificationFilterPreferenceDeletedEvent(), wikiReference, filterPreferenceId);
    }

    private long getInternalIdFromId(String filterPreferenceId) throws NotificationException
    {
        if (StringUtils.startsWith(filterPreferenceId, FILTER_PREFIX)) {
            return Long.parseLong(filterPreferenceId.substring(FILTER_PREFIX.length()));
        } else {
            throw new NotificationException(String.format("Cannot guess internal id of preference with id [%s].",
                filterPreferenceId));
        }
    }

    /**
     * Delete all the filter preferences from a wiki.
     *
     * @param wikiReference the reference of a wiki
     * @throws NotificationException in case of error during the hibernate operations
     * @since 14.5
     * @since 14.4.1
     * @since 13.10.7
     */
    public void deleteFilterPreference(WikiReference wikiReference) throws NotificationException
    {
        configureContextWrapper(wikiReference, () -> {
            XWikiContext context = this.contextProvider.get();

            XWikiHibernateStore hibernateStore = context.getWiki().getHibernateStore();

            try {
                hibernateStore.executeWrite(context, session -> {
                    session
                        .createQuery("delete from DefaultNotificationFilterPreference "
                            + "where page like :wikiPrefix "
                            + "or pageOnly like :wikiPrefix "
                            + "or user like :wikiPrefix "
                            + "or wiki = :wikiId")
                        .setParameter("wikiPrefix", wikiReference.getName() + ":%")
                        .setParameter("wikiId", wikiReference.getName()).executeUpdate();

                    return null;
                });
            } catch (XWikiException e) {
                throw new NotificationException(String
                    .format("Failed to delete the notification preferences for wiki [%s]", wikiReference.getName()), e);
            }

            return null;
        });
    }

    /**
     * Delete a filter preference.
     */
    private void deleteFilterPreferences(WikiReference wikiReference, Set<Long> internalFilterPreferenceIds)
        throws NotificationException
    {
        configureContextWrapper(wikiReference, () -> {
            XWikiContext context = this.contextProvider.get();

            XWikiHibernateStore hibernateStore = context.getWiki().getHibernateStore();

            try {
                hibernateStore.executeWrite(context, session ->
                    session.createQuery("delete from DefaultNotificationFilterPreference where internalId in (:id)")
                    .setParameter(ID, internalFilterPreferenceIds)
                    .executeUpdate());
            } catch (XWikiException e) {
                throw new NotificationException(
                    String.format("Failed to delete the notification preferences [%s]", internalFilterPreferenceIds),
                    e);
            }
            return null;
        });
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
        if (entityReference != null) {
            WikiReference wikiReference = (WikiReference) entityReference.extractReference(EntityType.WIKI);
            configureContextWrapper(wikiReference, () -> {
                String serializedEntity = this.entityReferenceSerializer.serialize(entityReference);

                XWikiContext context = this.contextProvider.get();

                XWikiHibernateStore hibernateStore = context.getWiki().getHibernateStore();

                try {
                    List<DefaultNotificationFilterPreference> preferencesToSend =
                        new ArrayList<>(filterPreferences.size());
                    hibernateStore.executeWrite(context, session -> {
                        for (NotificationFilterPreference preference : filterPreferences) {
                            // Hibernate mapping only describes how to save NotificationFilterPreference objects and
                            // does not
                            // handle extended objects (like ScopeNotificationFilterPreference).
                            // So we create a copy just in case we are not saving a basic NotificationFilterPreference
                            // object.
                            DefaultNotificationFilterPreference copy =
                                new DefaultNotificationFilterPreference(preference);
                            copy.setOwner(serializedEntity);
                            session.saveOrUpdate(copy);
                            preferencesToSend.add(copy);
                        }

                        return null;
                    });

                    // Notify listeners about the update
                    for (DefaultNotificationFilterPreference filterPreference : preferencesToSend) {
                        this.observation.notify(new NotificationFilterPreferenceAddOrUpdatedEvent(), filterPreference,
                            entityReference);
                    }
                } catch (Exception e) {
                    throw new NotificationException("Failed to save the notification filter preferences.", e);
                }

                return null;
            });
        }
    }

    /**
     * Set the right wikiId in the context according to the configuration.
     *
     * @param supplier the supplier to execute in the configured context
     * @param <T> the type of the result of the supplier
     * @param <E> the type of exception thrown by the supplier
     * @return the result of the supplier
     * @throws E in case of error during the execution of the supplier
     */
    private <T, E extends Throwable> T configureContextWrapper(WikiReference wikiReference, SupplierErr<T, E> supplier)
        throws E
    {
        XWikiContext context = this.contextProvider.get();
        WikiReference currentWiki = context.getWikiReference();
        context.setWikiReference(wikiReference);
        try {
            return supplier.get();
        } finally {
            context.setWikiReference(currentWiki);
        }
    }

    @FunctionalInterface
    private interface SupplierErr<T, E extends Throwable>
    {
        T get() throws E;
    }
}
