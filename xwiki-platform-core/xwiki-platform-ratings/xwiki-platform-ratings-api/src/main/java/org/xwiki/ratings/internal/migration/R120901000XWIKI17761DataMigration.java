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
package org.xwiki.ratings.internal.migration;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrQuery;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.ratings.RatingsException;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.ratings.RatingsManagerFactory;
import org.xwiki.ratings.internal.DefaultRating;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.XWikiSolrCore;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration of old Ratings XObjects to Solr store.
 *
 * This migration performs the following operations:
 *   1. it queries in database all the references of document containings a Rating xobjects
 *   2. it iterates over all those documents and all the rating xobjects in them, check if the voted document matches
 *      the configuration (i.e. if the Rating App was configured to use separate documents, then the xobjects to
 *      consider should not be located in the same page as the voted page), and migrate them if its the case and store
 *      the reference of the voted document
 *   3. iterates over all voted documents for which we migrated ratings, and compute back the average rating for them:
 *      this is done to ensure consistency of the average rating data.
 *   4. iterates over Like records and store them in the core.
 *
 * @version $Id$
 * @since 12.9RC1
 */
@Component
@Named("R120901000XWIKI17761")
@Singleton
public class R120901000XWIKI17761DataMigration extends AbstractHibernateDataMigration
{
    private static final int BATCH_SIZE_FOR_LOG = 100;

    private class GlobalMigrationStatistics
    {
        private final int numberOfPagesToHandle;
        private int numberOfPageHandled;
        private int totalNumberOfXObjectsMigrated;
        private final Map<String, Pair<Integer, Integer>> pagesWithNotMigratedRatings = new HashMap<>();

        GlobalMigrationStatistics(int numberOfPagesToHandle)
        {
            this.numberOfPagesToHandle = numberOfPagesToHandle;
        }

        void saveNotMigratedRatings(String serializedReference, int migrated, int notMigrated)
        {
            this.pagesWithNotMigratedRatings.put(serializedReference, new ImmutablePair<>(migrated, notMigrated));
        }

        void displayPageLogMigration()
        {
            if (++numberOfPageHandled % BATCH_SIZE_FOR_LOG == 0) {
                logger.info("[{}] pages have been handled for migrating their Ratings xobject on a total of "
                    + "[{}]", numberOfPageHandled, numberOfPagesToHandle);
            }
        }

        void displayAfterXObjectMigrationLog()
        {
            logger.info("All pages have been handled to migrate their Ratings xobject. "
                    + "Total: [{}] pages handled and [{}] xobjects migrated.",
                numberOfPagesToHandle, totalNumberOfXObjectsMigrated);
            if (!this.pagesWithNotMigratedRatings.isEmpty()) {
                logger.info("Some ratings xobject have not been migrated because of the ratings configuration. "
                    + "Here's the list of pages with ratings values not migrated: ");
                for (Map.Entry<String, Pair<Integer, Integer>> entry : pagesWithNotMigratedRatings.entrySet()) {
                    logger.info("Page: [{}]. Migrated: [{}]. Not migrated: [{}]", entry.getKey(),
                        entry.getValue().getLeft(), entry.getValue().getRight());
                }
            }
        }
    }

    private class XObjectMigrationStatistics
    {
        private final String serializedReference;
        private final int numberOfXObjectsToHandle;
        private int numberOfXObjectsMigrated;
        private int numberOfXObjecsNotMigrated;
        private int numberOfXObjectHandled;

        XObjectMigrationStatistics(String serializedReference, int numberOfXObjectsToHandle)
        {
            this.serializedReference = serializedReference;
            this.numberOfXObjectsToHandle = numberOfXObjectsToHandle;
        }

        void displayLog()
        {
            if (++numberOfXObjectHandled % BATCH_SIZE_FOR_LOG == 0) {
                logger.info("[{}] Ratings XObjects have been handled in document [{}] on a total of [{}]. "
                        + "([{}] migrated and [{}] not migrated).", numberOfXObjectHandled,
                    serializedReference, numberOfXObjectsToHandle, numberOfXObjectsMigrated,
                    numberOfXObjecsNotMigrated);
            }
        }

        void handleNotMigrated()
        {
            if (this.numberOfXObjecsNotMigrated > 0) {
                globalMigrationStatistics
                    .saveNotMigratedRatings(this.serializedReference, this.numberOfXObjectsMigrated,
                        this.numberOfXObjecsNotMigrated);
            }
        }
    }

    private class AverageMigrationStatistics
    {
        private int numberOfTargetPageHandled;
        private final int totalNumberOfTargetPageToHandle;

        AverageMigrationStatistics()
        {
            this.totalNumberOfTargetPageToHandle = targetOfMigratedRating.size();
        }

        void displayLog()
        {
            if (++numberOfTargetPageHandled % BATCH_SIZE_FOR_LOG == 0) {
                logger.info("Average rating have been recomputed on [{}] pages on a total of [{}]",
                    numberOfTargetPageHandled, totalNumberOfTargetPageToHandle);
            }
        }

        void displayLogAfterMigration()
        {
            logger.info("Average ratings have been recomputed on all [{}] pages.", totalNumberOfTargetPageToHandle);
        }
    }

    /**
     * Used to migrate all data from like solr core.
     */
    private static final String LIKE_SOLR_CORE = "like";

    @Inject
    private RatingsManagerFactory ratingsManagerFactory;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private UserReferenceResolver<String> userReferenceResolver;

    @Inject
    private Solr solr;

    @Inject
    private SolrDocumentMigration120900000 solrDocumentMigration120900000;

    @Inject
    private Logger logger;

    private XWiki wiki;
    private DocumentReference ratingXClassReference;
    private String ratingManagerHint;
    private RatingsManager ratingsManager;
    private Set<DocumentReference> targetOfMigratedRating;
    private GlobalMigrationStatistics globalMigrationStatistics;

    @Override
    public String getDescription()
    {
        return "Move old Ratings XObject to the default Solr rating store.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(120901000);
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        this.wiki = getXWikiContext().getWiki();

        // References of old ratings elements needed for the migration
        String xwikiSpace = "XWiki";
        String wikiName = getXWikiContext().getWikiId();
        DocumentReference ratingsConfigurationReference =
            new DocumentReference(wikiName, xwikiSpace, "RatingsConfig");
        DocumentReference ratingsConfigXClassReference =
            new DocumentReference(wikiName, xwikiSpace, "RatingsConfigClass");
        this.ratingXClassReference = new DocumentReference(wikiName, xwikiSpace, "RatingsClass");

        // Check if the old ratings configuration existed
        XWikiDocument ratingsConfigurationDoc = wiki.getDocument(ratingsConfigurationReference, getXWikiContext());
        BaseObject ratingsConfig = ratingsConfigurationDoc.getXObject(ratingsConfigXClassReference);

        // if the ratings configuration does not exist, then the Ratings is no longer installed and we're not
        // supposed to migrate the data.
        if (!ratingsConfigurationDoc.isNew() && ratingsConfig != null) {

            // Retrieve information about the way ratings was configured for storing data.
            this.ratingManagerHint = ratingsConfig.getStringValue("managerHint");
            try {

                // Instantiate a new RatingsManager: we didn't perform any migration yet, so if this one fails
                // for some reason, we're still safe.
                this.ratingsManager =
                    this.ratingsManagerFactory.getRatingsManager(RatingsManagerFactory.DEFAULT_APP_HINT);

                // Gather the list of pages containing a rating xobjects
                List<String> pagesWithRatingXObject =
                    getStore().executeRead(getXWikiContext(), this::getAllPagesWithRatingXObject);
                this.globalMigrationStatistics = new GlobalMigrationStatistics(pagesWithRatingXObject.size());

                if (!pagesWithRatingXObject.isEmpty()) {
                    logger.info("[{}] pages containing ratings xobjects have been found: those objects will be migrated"
                        + " to the new storage system and will be removed. "
                        + "The pages holding those xobjects will remain",
                        this.globalMigrationStatistics.numberOfPagesToHandle);
                }
                this.targetOfMigratedRating = new HashSet<>();

                for (String serializedReference : pagesWithRatingXObject) {
                    this.handlePage(serializedReference);
                }
                this.globalMigrationStatistics.displayAfterXObjectMigrationLog();

                this.handleAverageMigration();
                this.handleLikeMigration();
            } catch (RatingsException e) {
                throw new DataMigrationException("Cannot migrate old rating xobject to new storage system.", e);
            } catch (SolrException e) {
                throw new DataMigrationException("Error while migrating Like informations.", e);
            }
        }
    }

    private void handleLikeMigration() throws SolrException
    {
        // We only migrate like once, if we are performing the migration on main wiki.
        if (this.getXWikiContext().isMainWiki()) {
            // Move of old Likes and migrate them
            // (note that we copy them in the actual ratings core in order to migrate them)
            XWikiSolrCore likeCore = this.solr.getCore(LIKE_SOLR_CORE);
            // likeClient could be null here if for some reason the core was never initialized or if it has been
            // removed.
            // In case of a Remote Solr server then the client won't be null, so we check that we can perform a
            // request to it.
            if (likeCore != null) {
                try {
                    SolrQuery solrQuery = new SolrQuery("*")
                        .setStart(0)
                        .setRows(1);
                    likeCore.getClient().query(solrQuery);
                // we catch Throwable here since Solr create Runtime exceptions in case of client issues.
                } catch (Throwable e) {
                    // in case of exception we consider that the client is null.
                    likeCore = null;
                    // and we log a debug message in case it's another issue
                    this.logger.debug("Solr test connection for Like migration did not went well.", e);
                }
            }
            // TODO: Check in case it's a remote Solr core and it has not been created, it could be not null but
            //  still not exist.
            if (likeCore != null) {
                this.logger.info("Starting migration of Likes information to the Ratings Solr Core.");
                this.solrDocumentMigration120900000
                    .migrateAllDocumentsFrom1207000000(likeCore, 1, LIKE_SOLR_CORE);
            }

            logger.info("The migration is now finished.");
        }
    }

    private void handleAverageMigration() throws RatingsException
    {
        logger.info("Starting recomputation of average ratings on rated pages for consistency of data.");

        AverageMigrationStatistics averageMigrationStatistics = new AverageMigrationStatistics();
        for (DocumentReference ratedDocumentReference : targetOfMigratedRating) {
            ratingsManager.recomputeAverageRating(ratedDocumentReference);

            averageMigrationStatistics.displayLog();
        }
        averageMigrationStatistics.displayLogAfterMigration();
    }

    private void handlePage(String serializedReference) throws XWikiException, RatingsException
    {
        DocumentReference reference = this.documentReferenceResolver.resolve(serializedReference);
        XWikiDocument document = this.wiki.getDocument(reference, getXWikiContext());
        boolean updatedDocument = false;


        List<BaseObject> xObjects = document.getXObjects(this.ratingXClassReference);
        XObjectMigrationStatistics xObjectMigrationStatistics =
            new XObjectMigrationStatistics(serializedReference, xObjects.size());

        for (BaseObject xObject : xObjects) {
            updatedDocument = this.handleXObject(xObject, document, xObjectMigrationStatistics);
        }
        xObjectMigrationStatistics.handleNotMigrated();
        if (updatedDocument) {
            wiki.saveDocument(document, "Migration of Rating objects", true, getXWikiContext());
        }

        this.globalMigrationStatistics.displayPageLogMigration();
    }

    private boolean handleXObject(BaseObject xObject, XWikiDocument ownerDocument,
        XObjectMigrationStatistics xObjectMigrationStatistics) throws RatingsException
    {
        boolean result = false;
        boolean migratedXObject = this.migrateRatingXObject(xObject, ownerDocument.getDocumentReference());

        if (migratedXObject) {
            ownerDocument.removeXObject(xObject);
            result = true;
            xObjectMigrationStatistics.numberOfXObjectsMigrated++;
        } else {
            xObjectMigrationStatistics.numberOfXObjecsNotMigrated++;
        }
        xObjectMigrationStatistics.displayLog();
        this.globalMigrationStatistics.totalNumberOfXObjectsMigrated +=
            xObjectMigrationStatistics.numberOfXObjectsMigrated;
        return result;
    }

    private boolean migrateRatingXObject(BaseObject xObject, DocumentReference ownerDocReference)
        throws RatingsException
    {
        String author = xObject.getStringValue("author");
        Date date = xObject.getDateValue("date");
        String parent = xObject.getStringValue("parent");
        int vote = xObject.getIntValue("vote");

        DocumentReference ratedPage = this.documentReferenceResolver.resolve(parent);

        boolean migrateRating =
            ("separate".equals(this.ratingManagerHint) && !ratedPage.equals(ownerDocReference))
            || ("default".equals(this.ratingManagerHint) && ratedPage.equals(ownerDocReference));
        if (migrateRating) {
            DefaultRating rating = new DefaultRating(UUID.randomUUID().toString())
                .setManagerId(RatingsManagerFactory.DEFAULT_APP_HINT)
                .setAuthor(this.userReferenceResolver.resolve(author))
                .setReference(ratedPage)
                .setCreatedAt(date)
                .setUpdatedAt(date)
                // We explicitely don't retrieve scale from config here since the previous ratings
                // were done with a scale of 5.
                .setScaleUpperBound(5)
                .setVote(vote);
            this.ratingsManager.saveRating(rating);
            this.targetOfMigratedRating.add(ratedPage);
        }
        return migrateRating;
    }

    private List<String> getAllPagesWithRatingXObject(Session session) throws HibernateException, XWikiException
    {
        Query<String> query = session.createQuery("select distinct obj.name from BaseObject obj"
                + " where obj.className = 'XWiki.RatingsClass'",
            String.class);

        return query.list();
    }
}
