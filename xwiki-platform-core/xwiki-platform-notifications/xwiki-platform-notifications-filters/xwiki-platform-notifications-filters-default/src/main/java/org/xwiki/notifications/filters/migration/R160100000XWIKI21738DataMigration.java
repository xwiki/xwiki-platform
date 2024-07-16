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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.internal.DefaultNotificationFilterPreference;
import org.xwiki.notifications.filters.internal.NotificationFilterPreferenceStore;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.stability.Unstable;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migrate filters to put them in the same DB than where the users are located.
 *
 * @version $Id$
 * @since 16.1.0RC1
 */
@Component
@Named("R160100000XWIKI21738")
@Singleton
@Unstable
public class R160100000XWIKI21738DataMigration extends AbstractHibernateDataMigration
{
    private static final int BATCH_SIZE = 100;

    private static final String SEARCH_FILTERS_STATEMENT = "select nfp "
        + "from DefaultNotificationFilterPreference nfp "
        + "where nfp.owner not like :ownerLike and nfp.owner <> :mainWiki "
        + "order by nfp.owner, nfp.internalId";

    private static final String DELETE_FILTER_STATEMENT = "delete from DefaultNotificationFilterPreference nfp "
        + "where nfp.internalId IN (:listIds)";

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private QueryManager queryManager;

    @Inject
    private NotificationFilterPreferenceStore filterPreferenceStore;

    @Inject
    @Named("relative")
    private EntityReferenceResolver<String> entityReferenceResolver;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private Logger logger;

    @Override
    public String getDescription()
    {
        return "Migrate filters in the same DB than their owners.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(160100000);
    }

    @Override
    public boolean shouldExecute(XWikiDBVersion startupVersion)
    {
        return super.shouldExecute(startupVersion)
            && this.wikiDescriptorManager.isMainWiki(this.wikiDescriptorManager.getCurrentWikiId());
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        // This migration only needs to be performed on main wiki: if we have filters on local wikis it's because
        // local filters was enabled and we don't support switching mainstore / localstore.
        // Migration steps:
        //   - Retrieve filters from main wiki where owner belongs to a subwiki
        //   - Store the filters on the subwiki DB using standard storage component
        //   - Remove the filters from main DB

        String mainWikiId = this.wikiDescriptorManager.getMainWikiId();

        List<DefaultNotificationFilterPreference> filters = null;
        List<DefaultNotificationFilterPreference> previousFilters;
        do {
            try {
                previousFilters = filters;
                filters = this.queryManager.createQuery(SEARCH_FILTERS_STATEMENT, Query.HQL)
                    .bindValue("ownerLike")
                    .literal(mainWikiId)
                    .literal(":")
                    .anyChars()
                    .query()
                    .bindValue("mainWiki", this.entityReferenceSerializer.serialize(new WikiReference(mainWikiId)))
                    .setLimit(BATCH_SIZE)
                    .execute();

                if (!filters.isEmpty() && (previousFilters == null || !previousFilters.equals(filters))) {
                    this.logger.info("Found [{}] filters to migrate...", filters.size());
                    this.migrateFilters(filters);
                }
            } catch (QueryException e) {
                throw new DataMigrationException("Error when trying to retrieve filters to move", e);
            }
        // We use previous filters as a security measure to ensure we won't loop forever if for some reason filters
        // are not properly deleted.
        } while (!filters.isEmpty() && (previousFilters == null || !previousFilters.equals(filters)));

        if (previousFilters != null && previousFilters.equals(filters)) {
            throw new DataMigrationException("Error while performing the migration: filters are not properly deleted.");
        }
        this.logger.info("No more filters found to migrate.");
    }

    private void migrateFilters(List<DefaultNotificationFilterPreference> filters)
        throws XWikiException, DataMigrationException
    {
        List<Long> internalIds = new ArrayList<>();
        Map<EntityReference, List<NotificationFilterPreference>> filtersToStore = new HashMap<>();

        for (DefaultNotificationFilterPreference filter : filters) {
            EntityReference entityReference = this.getOwnerEntityReference(filter);
            EntityReference wikiReference = entityReference.extractReference(EntityType.WIKI);
            try {
                if (this.wikiDescriptorManager.exists(wikiReference.getName())) {
                    List<NotificationFilterPreference> filterPreferenceList;
                    if (filtersToStore.containsKey(entityReference)) {
                        filterPreferenceList = filtersToStore.get(entityReference);
                    } else {
                        filterPreferenceList = new ArrayList<>();
                        filtersToStore.put(entityReference, filterPreferenceList);
                    }
                    // We clone filter to ensure using new ids when storing.
                    filterPreferenceList.add(new DefaultNotificationFilterPreference(filter, false));
                } else {
                    this.logger.warn("Owner [{}] of some filter preferences belongs to a wiki that does not long exist"
                        + ", preferences will be removed.", entityReference);
                }
            } catch (WikiManagerException e) {
                throw new DataMigrationException(
                    String.format("Error when checking existence of wiki [%s]", wikiReference), e);
            }
            internalIds.add(filter.getInternalId());
        }

        this.logger.info("Migrating filters for [{}] entities", filtersToStore.size());

        for (Map.Entry<EntityReference, List<NotificationFilterPreference>> entry
            : filtersToStore.entrySet()) {
            EntityReference entityReference = entry.getKey();
            List<NotificationFilterPreference> notificationFilterPreferenceList = entry.getValue();
            try {
                if (entityReference.getType() == EntityType.DOCUMENT) {
                    this.filterPreferenceStore.saveFilterPreferences(new DocumentReference(entityReference),
                        notificationFilterPreferenceList);
                } else {
                    this.filterPreferenceStore.saveFilterPreferences(new WikiReference(entityReference.getName()),
                        notificationFilterPreferenceList);
                }
            } catch (NotificationException e) {
                throw new DataMigrationException(
                    String.format("Error when trying to save filters to migrate for [%s]", entityReference), e);
            }
        }
        if (!internalIds.isEmpty()) {
            getStore().executeWrite(getXWikiContext(), session ->
                session.createQuery(DELETE_FILTER_STATEMENT).setParameter("listIds", internalIds)
                    .executeUpdate());
        }
    }

    private EntityReference getOwnerEntityReference(DefaultNotificationFilterPreference filterPreference)
    {
        // if the owner is a document it's always a full reference containing the wiki part
        String owner = filterPreference.getOwner();
        EntityReference reference = this.entityReferenceResolver.resolve(owner, EntityType.DOCUMENT);
        if (reference.extractReference(EntityType.WIKI) == null) {
            reference = this.entityReferenceResolver.resolve(owner, EntityType.WIKI);
        }
        return reference;
    }
}
