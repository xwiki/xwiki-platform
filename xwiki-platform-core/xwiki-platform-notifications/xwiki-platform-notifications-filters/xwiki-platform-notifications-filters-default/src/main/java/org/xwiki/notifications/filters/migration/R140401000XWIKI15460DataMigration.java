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
package org.xwiki.notifications.filters.migration;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.internal.DefaultNotificationFilterPreference;
import org.xwiki.notifications.filters.internal.NotificationFilterPreferenceStore;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.user.UserException;
import org.xwiki.user.UserManager;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Cleanup the notification filters preferences remaining on the main wiki from previously removed sub-wikis and users.
 * Note: this class is named {@code R131007000XWIKI1546} in branch {@code 13.10.7+}. This class also covers
 * {@code XWIKI-18397} to prevent having an almost identical migration executed again.
 *
 * @version $Id$
 * @see <a href="https://jira.xwiki.org/browse/XWIKI-15460">XWIKI-15460: Notification filter preferences are not cleaned
 *     when a wiki is deleted</a>
 * @see <a href="https://jira.xwiki.org/browse/XWIKI-18397">XWIKI-18397: Notification filter preferences are never
 *     cleaned for deleted users</a>
 * @since 14.5
 * @since 14.4.1
 * @since 13.10.7
 */
@Component
@Singleton
@Named("R140401000XWIKI15460")
public class R140401000XWIKI15460DataMigration extends AbstractHibernateDataMigration
{
    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private NotificationFilterPreferenceStore store;

    @Inject
    private DocumentReferenceResolver<String> resolver;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> documentReferenceUserReferenceResolver;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("count")
    private QueryFilter countQueryFilter;

    @Inject
    private UserManager userManager;

    @Inject
    private ConfigurationSource configurationSource;

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(140401000);
    }

    @Override
    public String getDescription()
    {
        return "Remove the notification filters preferences remaining from removed sub-wikis.";
    }

    @Override
    public boolean shouldExecute(XWikiDBVersion startupVersion)
    {
        boolean shouldExecute = super.shouldExecute(startupVersion);

        if (shouldExecute) {
            int version = startupVersion.getVersion();
            shouldExecute = !(version >= 131007000 && version < 140000000);
        }
        return shouldExecute;
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException
    {
        boolean isMainWiki = isMainWiki();

        // Stop the execution early if the configuration uses the main store and we are not upgrading the main wiki.
        // This check cannot be done in #shouldExecute because possibly missing columns are not yet added to the 
        // database.
        if (useMainStore() && !isMainWiki) {
            return;
        }

        internalHibernateMigrate(isMainWiki);
    }

    private boolean useMainStore()
    {
        return this.configurationSource.getProperty("eventstream.usemainstore", true);
    }

    private void internalHibernateMigrate(boolean isMainWiki) throws DataMigrationException
    {
        try {
            Collection<String> knownWikiIds;
            if (isMainWiki) {
                // The know wikis are only initialized for the main wiki, as they are not needed for the sub-wikis.
                knownWikiIds = this.wikiDescriptorManager.getAllIds();
            } else {
                knownWikiIds = Collections.emptyList();
            }
            Set<String> unknownWikiIds = new HashSet<>();
            // The keys are user identifiers, the values are true if the user exists on the wiki, false otherwise.
            Map<String, Boolean> usersStatus = new HashMap<>();

            int limit = 1000;
            int offset = 0;
            Set<DefaultNotificationFilterPreference> allNotificationFilterPreferences = this.store
                .getPaginatedFilterPreferences(limit, offset);

            while (!allNotificationFilterPreferences.isEmpty()) {
                for (DefaultNotificationFilterPreference filterPreference : allNotificationFilterPreferences) {
                    // Filters remaining from previously removed wikis can only be found on the main wiki.
                    if (isMainWiki) {
                        identifyRemovedWikis(knownWikiIds, unknownWikiIds, filterPreference);
                    }
                    identifyRemovedUsers(usersStatus, filterPreference);
                }
                offset += limit;
                allNotificationFilterPreferences = this.store.getPaginatedFilterPreferences(limit, offset);
            }

            for (String unknownWikiId : unknownWikiIds) {
                this.store.deleteFilterPreference(new WikiReference(unknownWikiId));
            }

            // List the users that were not found on the wiki.
            Set<String> unknownUsers = usersStatus.entrySet().stream()
                .filter(entry -> !entry.getValue())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
            for (String userReference : unknownUsers) {
                DocumentReference userDocumentReference = this.resolver.resolve(userReference);
                // Verify if the user still has some filters preferences to remove, in case they have already all been
                // cleaned up when removing the filters preferences for the unknown wikis. Without this check, calling 
                // deleteFilterPreferences could yield undesirable "no data" warning logs. 
                if (!this.store.getPreferencesOfUser(userDocumentReference).isEmpty()) {
                    this.store.deleteFilterPreferences(userDocumentReference);
                }
            }
        } catch (NotificationException e) {
            throw new DataMigrationException("Failed to retrieve the notification filters preferences.", e);
        } catch (WikiManagerException e) {
            throw new DataMigrationException("Failed to retrieve the ids of wikis of the farm.", e);
        }
    }

    private void identifyRemovedWikis(Collection<String> knownWikiIds, Set<String> unknownWikiIds,
        DefaultNotificationFilterPreference filterPreference)
    {
        filterPreference.getWikiId().ifPresent(wikiId -> {
            if (!knownWikiIds.contains(wikiId)) {
                unknownWikiIds.add(wikiId);
            }
        });
    }

    private void identifyRemovedUsers(Map<String, Boolean> usersStatus,
        DefaultNotificationFilterPreference filterPreference) throws DataMigrationException
    {
        String owner = filterPreference.getOwner();
        WikiReference currentWiki = getXWikiContext().getWikiReference();

        // Store the potential exception thrown inside computeIfAbsent, since assigning to a variable is not allowed.
        AtomicReference<Exception> exception = new AtomicReference<>();
        usersStatus.computeIfAbsent(owner, key -> {
            DocumentReference entityReference = this.resolver.resolve(key);
            WikiReference wikiReference = entityReference.getWikiReference();
            try {
                // if we're on same wiki we check user presence by using the UserManager since it's the most
                // reliable solution, and we benefit from cache.
                if (wikiReference.equals(currentWiki)) {
                    UserReference userReference =
                        this.documentReferenceUserReferenceResolver.resolve(entityReference);
                    return this.userManager.exists(userReference);
                // if we're not and the wiki doesn't exist anymore we immediately know the user doesn't exist
                } else if (!this.wikiDescriptorManager.exists(wikiReference.getName())) {
                    return false;
                // if the wiki still exist we cannot really use UserManager because other migrations might not have
                // been applied yet (see: XWIKI-20184) so we instead rely on a low-level SQL query to check existence
                // of the user
                } else {
                    String serializedName = this.entityReferenceSerializer.serialize(entityReference);
                    String statement = ", BaseObject as obj where doc.fullName = :username and "
                        + "doc.fullName = obj.name and obj.className = 'XWiki.XWikiUsers'";
                    List<Long> result = this.queryManager.createQuery(statement, Query.HQL)
                        .setWiki(wikiReference.getName())
                        .bindValue("username", serializedName)
                        .addFilter(this.countQueryFilter)
                        .setLimit(1)
                        .execute();
                    return result.get(0) > 0;

                }
            } catch (QueryException | UserException | WikiManagerException e) {
                exception.set(e);
                return null;
            }
        });

        // If the exception store has been set, propagate the exception to make the migration fail.
        if (exception.get() != null) {
            throw new DataMigrationException(
                String.format("Failed to identify if the owner of [%s] exists.", filterPreference), exception.get());
        }
    }

    private boolean isMainWiki()
    {
        return Objects.equals(this.wikiDescriptorManager.getCurrentWikiId(),
            this.wikiDescriptorManager.getMainWikiId());
    }
}
