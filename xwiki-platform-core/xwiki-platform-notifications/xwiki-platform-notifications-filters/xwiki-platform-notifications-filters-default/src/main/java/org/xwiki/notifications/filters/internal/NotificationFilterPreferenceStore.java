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
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.text.StringUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.activitystream.impl.ActivityStreamConfiguration;
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
    private ActivityStreamConfiguration activityStreamConfiguration;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private Provider<XWikiContext> contextProvider;

    /**
     * Get the notification preference that corresponds to the given id and user.
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
     * Get all the notification preferences that corresponds to the given user.
     *
     * @param user the user from which we need to extract the preference
     * @return a set of available filter preferences
     * @throws NotificationException if an error happens
     */
    public List<DefaultNotificationFilterPreference> getPreferencesOfUser(DocumentReference user)
            throws NotificationException
    {
        if (user == null) {
            return Collections.emptyList();
        }

        List<DefaultNotificationFilterPreference> results;

        String serializedUser = entityReferenceSerializer.serialize(user);

        XWikiContext context = contextProvider.get();

        // Search in the main database
        String oriDatabase = context.getWikiId();
        try {
            context.setWikiId(context.getMainXWiki());
            results =
                    context.getWiki().getStore().search(
                            "select nfp from DefaultNotificationFilterPreference nfp where nfp.owner = ? "
                                    + "order by nfp.id",
                            1000, 0, Collections.singletonList(serializedUser), context);

            for (DefaultNotificationFilterPreference preference : results) {
                preference.setProviderHint("userProfile");
            }
        } catch (XWikiException e) {
            throw new NotificationException("error", e);
        } finally {
            context.setWikiId(oriDatabase);
        }

        return results;
    }

    /**
     * Delete a filter preference.
     * @param user reference of the user concerned by the filter preference
     * @param filterPreferenceId name of the filter preference
     * @throws NotificationException if an error happens
     */
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

    /**
     * Save a collection of NotificationFilterPreferences.
     * @param user reference of the user concerned by the filter preference
     * @param filterPreferences a list of NotificationFilterPreference
     * @throws NotificationException if an error happens
     */
    public void saveFilterPreferences(DocumentReference user,
            Collection<NotificationFilterPreference> filterPreferences) throws NotificationException
    {
        if (user == null) {
            return;
        }

        String serializedUser = entityReferenceSerializer.serialize(user);

        XWikiContext context = contextProvider.get();

        // store event in the main database
        String currentWiki = context.getWikiId();

        XWikiHibernateStore hibernateStore = null;

        try {
            if (activityStreamConfiguration.useMainStore()) {
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
                copy.setOwner(serializedUser);
                session.saveOrUpdate(copy);
            }

            hibernateStore.endTransaction(context, true);
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
