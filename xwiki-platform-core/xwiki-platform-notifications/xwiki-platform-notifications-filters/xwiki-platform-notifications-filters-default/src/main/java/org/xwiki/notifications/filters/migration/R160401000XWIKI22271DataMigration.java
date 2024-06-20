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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.notifications.filters.internal.DefaultNotificationFilterPreference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Ensure to clean up filters related to deleted wikis.
 *
 * @version $Id$
 * @since 16.5.0
 * @since 16.4.1
 */
@Component
@Named("R160401000XWIKI22271")
@Singleton
public class R160401000XWIKI22271DataMigration extends AbstractHibernateDataMigration
{
    // We use a nested query here because the columns are all nullable and apparently any query
    // such as not length(nfp.wiki) > 0 doesn't return result if the column data is null
    // because length(nfp.wiki) returns null when the data is null...
    // So easiest way to not have deal with null values (which are interpreted differently in different DBs)
    // was to use nested query.
    private static final String SEARCH_FILTERS_STATEMENT = "select nfp "
        + "from DefaultNotificationFilterPreference nfp "
        + "where nfp.id not in ("
        + "select nfp2.id from DefaultNotificationFilterPreference nfp2 "
        + "where nfp2.page like :wikiPrefix or "
        + "nfp2.pageOnly like :wikiPrefix or "
        + "nfp2.user like :wikiPrefix or "
        + "nfp2.wiki = :wikiId"
        + ")";

    private static final String DELETE_FILTER_STATEMENT = "delete from DefaultNotificationFilterPreference "
        + "where internalId in (:filterIds)";

    private static final int BATCH_SIZE = 100;

    @Inject
    private QueryManager queryManager;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private Logger logger;

    @Override
    public String getDescription()
    {
        return "Ensure that filters related to deleted wiki are clean up.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(160401000);
    }

    @Override
    public boolean shouldExecute(XWikiDBVersion startupVersion)
    {
        // We only execute the migration on main wiki: we cannot have filters related to another subwiki in a subwiki
        // DB.
        boolean shouldExecute = super.shouldExecute(startupVersion)
            && this.wikiDescriptorManager.isMainWiki(this.wikiDescriptorManager.getCurrentWikiId());

        if (shouldExecute) {
            int version = startupVersion.getVersion();
            // The migration is backported in 16.4.1 so any DB between 16.4.1 and 16.5.0 don't need to execute it again.
            shouldExecute = !(version >= 160401000 && version < 160500000);
        }
        return shouldExecute;
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        String currentWikiId = getXWikiContext().getWikiId();
        Set<Long> idsToCleanup = new HashSet<>();
        int offset = 0;
        List<DefaultNotificationFilterPreference> results = null;
        try {
            Collection<String> allIds = this.wikiDescriptorManager.getAllIds();
            do {
                results = this.queryManager
                    .createQuery(SEARCH_FILTERS_STATEMENT, Query.HQL)
                    // Same escaping logic as documented in EscapeLikeParametersQuery
                    // Note that we can't rely on using a DefaultQueryParameter here because of the nested query...
                    // See XWIKI-22279
                    .bindValue("wikiPrefix", currentWikiId.replaceAll("([%_!])", "!$1") + ":%")
                    .bindValue("wikiId", currentWikiId)
                    .setOffset(offset)
                    .setLimit(BATCH_SIZE)
                    .execute();

                if (!results.isEmpty()) {
                    this.logger.info("Performing analysis of [{}] filters to find those related to removed wikis",
                        results.size());

                    results.forEach(filter -> {
                        if (!isFilterAboutExistingWiki(filter, allIds)) {
                            idsToCleanup.add(filter.getInternalId());
                        }
                    });
                }
                offset += results.size();
            } while (!results.isEmpty());
        } catch (QueryException e) {
            throw new DataMigrationException("Error while performing query for finding filters", e);
        } catch (WikiManagerException e) {
            throw new DataMigrationException("Error when searching for wiki ids", e);
        }

        if (!idsToCleanup.isEmpty()) {
            this.logger.info("Removing [{}] filters related to deleted wikis", idsToCleanup.size());
            try {
                getStore().executeWrite(getXWikiContext(), session -> {
                    session
                        .createQuery(DELETE_FILTER_STATEMENT)
                        .setParameter("filterIds", idsToCleanup).executeUpdate();

                    return null;
                });
            } catch (XWikiException e) {
                throw new DataMigrationException("Error when performing the request to clean up filters.", e);
            }
        } else {
            this.logger.info("No filter has been found related to deleted wikis.");
        }
    }

    private boolean isFilterAboutExistingWiki(DefaultNotificationFilterPreference filterPreference,
        Collection<String> wikiIds)
    {
        if (filterPreference.getWikiId().isPresent()) {
            return wikiIds.contains(filterPreference.getWikiId().get());
        } else {
            return false;
        }
    }
}
