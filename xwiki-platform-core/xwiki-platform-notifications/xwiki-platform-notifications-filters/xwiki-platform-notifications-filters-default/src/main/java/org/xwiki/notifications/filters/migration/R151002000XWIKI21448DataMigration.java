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

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.LRUCacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.stability.Unstable;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration for cleaning up remaining notification filters that concern deleted documents.
 *
 * @version $Id$
 * @since 16.0.0RC1
 * @since 15.10.2
 */
@Component
@Named("R151002000XWIKI21448")
@Singleton
@Unstable
public class R151002000XWIKI21448DataMigration extends AbstractHibernateDataMigration
{
    private static final int BATCH_SIZE = 100;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private ConfigurationSource configurationSource;

    @Inject
    private QueryManager queryManager;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    @Named("count")
    private QueryFilter countQueryFilter;

    @Inject
    private Logger logger;

    @Override
    public String getDescription()
    {
        return "Clean up remaining notifications filters that concerns deleted documents";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(151002000);
    }


    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        boolean isMainWiki = Objects.equals(this.wikiDescriptorManager.getCurrentWikiId(),
            this.wikiDescriptorManager.getMainWikiId());

        // Stop the execution early if the configuration uses the main store, and we are not upgrading the main wiki.
        // This check cannot be done in #shouldExecute because possibly missing columns are not yet added to the
        // database.
        if (useMainStore() && !isMainWiki) {
            return;
        }

        internalHibernateMigrate();
    }

    private boolean useMainStore()
    {
        return this.configurationSource.getProperty("eventstream.usemainstore", true);
    }

    private void internalHibernateMigrate() throws DataMigrationException
    {
        // Cache of boolean: true if the documents exists, false if it's missing.
        Cache<Boolean> documentStatus;
        try {
            documentStatus = this.cacheManager.createNewLocalCache(
                new LRUCacheConfiguration("migration.R151002000XWIKI21448.documentStatus"));
        } catch (CacheException e) {
            throw new DataMigrationException("Cannot create local cache for performing the migration", e);
        }

        // pageOnly property is not nullable and oracle is not happy when checking if it contains empty string
        String statement = "select distinct nfp.pageOnly "
            + "from DefaultNotificationFilterPreference nfp "
            + "where length(nfp.pageOnly) > 0";

        String deletionStatement = "delete from DefaultNotificationFilterPreference "
            + "where pageOnly IN (:missingDocuments)";

        try {
            List<String> queryResult;
            String latestExistingDocument = null;

            // The loop is handled this way:
            // We fetch only the pageOnly reference of the filters (with the group by directive) ordered by pageOnly
            // we analyze each reference and we always remember the latest existing reference seen
            // before looping again we perform deletion of the filters, so next query won't return those
            // a new query is performed by only getting the results > the latest existing reference seen, so that we
            // can safely ignore previously analyzed results
            // we finish the loop when the returns number of result to analyze is strictly < the size of the batch
            do {
                String fetchFilterStatement = statement;

                if (!StringUtils.isEmpty(latestExistingDocument)) {
                    fetchFilterStatement += " AND nfp.pageOnly > :latestExistingDocument";
                }

                fetchFilterStatement += " order by nfp.pageOnly";

                Query query = this.queryManager.createQuery(fetchFilterStatement, Query.HQL);
                if (!StringUtils.isEmpty(latestExistingDocument)) {
                    query = query.bindValue("latestExistingDocument", latestExistingDocument);
                }

                queryResult = query.setLimit(BATCH_SIZE)
                    .execute();

                this.logger.info("Performing filters analysis for [{}] documents",
                    queryResult.size());

                Set<String> deletedDocuments = new HashSet<>();
                for (String serializedDocument : queryResult) {
                    if (!isDocumentExisting(serializedDocument, documentStatus)) {
                        deletedDocuments.add(serializedDocument);
                    } else {
                        latestExistingDocument = serializedDocument;
                    }
                }

                this.logger.info("[{}] missing documents found, performing clean up of filters...",
                    deletedDocuments.size());

                if (!deletedDocuments.isEmpty()) {
                    getXWikiContext().getWiki().getHibernateStore().executeWrite(getXWikiContext(),
                        session -> session.createQuery(deletionStatement)
                            .setParameter("missingDocuments", deletedDocuments)
                            .executeUpdate());
                }

            } while (queryResult.size() == BATCH_SIZE);

        } catch (QueryException e) {
            throw new DataMigrationException("Error when performing the query to access filters", e);
        } catch (XWikiException e) {
            throw new DataMigrationException("Error when performing the query to clean up filters", e);
        } finally {
            // We don't need the cache anymore
            documentStatus.dispose();
        }
    }

    private boolean isDocumentExisting(String serializedDocument, Cache<Boolean> documentStatus)
        throws DataMigrationException
    {
        Boolean status = documentStatus.get(serializedDocument);

        if (status == null) {
            DocumentReference documentReference = this.documentReferenceResolver.resolve(serializedDocument);

            try {
                if (this.wikiDescriptorManager.exists(documentReference.getWikiReference().getName())) {
                    String compactSerialization = this.entityReferenceSerializer.serialize(documentReference);
                    String statement = "where doc.fullName = :docName";
                    List<Long> result = this.queryManager.createQuery(statement, Query.XWQL)
                        .bindValue("docName", compactSerialization).setLimit(1)
                        .setWiki(documentReference.getWikiReference().getName()).addFilter(this.countQueryFilter)
                        .execute();

                    status = result.get(0) > 0;
                } else {
                    status = false;
                }

                documentStatus.set(serializedDocument, status);
            } catch (Exception e) {
                throw new DataMigrationException(
                    String.format("Error when trying to check if document [%s] exists", serializedDocument), e);
            }
        }

        return status;
    }
}
