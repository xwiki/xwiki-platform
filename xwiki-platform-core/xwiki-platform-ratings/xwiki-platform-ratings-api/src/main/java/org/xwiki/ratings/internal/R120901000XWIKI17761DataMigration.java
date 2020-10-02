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
package org.xwiki.ratings.internal;

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

    @Inject
    private RatingsManagerFactory ratingsManagerFactory;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private UserReferenceResolver<String> userReferenceResolver;

    @Inject
    private Logger logger;

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
        XWiki wiki = getXWikiContext().getWiki();

        // References of old ratings elements needed for the migration
        String xwikiSpace = "XWiki";
        DocumentReference ratingsConfigurationReference =
            new DocumentReference(wiki.getName(), xwikiSpace, "RatingsConfig");
        DocumentReference ratingsConfigXClassReference =
            new DocumentReference(wiki.getName(), xwikiSpace, "RatingsConfigClass");
        DocumentReference ratingXClassReference = new DocumentReference(wiki.getName(), xwikiSpace, "RatingsClass");

        // Check if the old ratings configuration existed
        XWikiDocument ratingsConfigurationDoc = wiki.getDocument(ratingsConfigurationReference, getXWikiContext());
        BaseObject ratingsConfig = ratingsConfigurationDoc.getXObject(ratingsConfigXClassReference);

        // if the ratings configuration does not exist, then the Ratings is no longer installed and we're not
        // supposed to migrate the data.
        if (!ratingsConfigurationDoc.isNew() && ratingsConfig != null) {

            // Retrieve information about the way ratings was configured for storing data.
            String managerHint = ratingsConfig.getStringValue("managerHint");
            try {

                // Instantiate a new RatingsManager: we didn't perform any migration yet, so if this one fails
                // for some reason, we're still safe.
                RatingsManager ratingsManager =
                    this.ratingsManagerFactory.getRatingsManager(RatingsManagerFactory.DEFAULT_APP_HINT);

                // Gather the list of pages containing a rating xobjects
                List<String> pagesWithRatingXObject =
                    getStore().executeRead(getXWikiContext(), this::getAllPagesWithRatingXObject);
                int numberOfPagesToHandle = pagesWithRatingXObject.size();

                if (!pagesWithRatingXObject.isEmpty()) {
                    logger.info("[{}] pages containing ratings xobjects have been found: those objects will be migrated"
                        + " to the new storage system and will be removed. "
                        + "The pages holding those xobjects will remain", numberOfPagesToHandle);
                }

                Set<DocumentReference> targetOfMigratedRating = new HashSet<>();
                Map<String, Pair<Integer, Integer>> statistics = new HashMap<>();
                int numberOfPageHandled = 0;
                int totalNumberOfXObjectsMigrated = 0;

                for (String serializedReference : pagesWithRatingXObject) {
                    DocumentReference reference = this.documentReferenceResolver.resolve(serializedReference);
                    XWikiDocument document = wiki.getDocument(reference, getXWikiContext());
                    boolean updatedDocument = false;

                    int numberOfXObjectsMigrated = 0;
                    int numberOfXObjecsNotMigrated = 0;
                    int numberOfXObjectHandled = 0;
                    List<BaseObject> xObjects = document.getXObjects(ratingXClassReference);
                    int numberOfXObjectsToHandle = xObjects.size();

                    for (BaseObject xObject : xObjects) {
                        boolean migratedXObject =
                            this.migrateRatingXObject(xObject, reference, managerHint, ratingsManager,
                                targetOfMigratedRating);

                        if (migratedXObject) {
                            document.removeXObject(xObject);
                            updatedDocument = true;
                            numberOfXObjectsMigrated++;
                        } else {
                            numberOfXObjecsNotMigrated++;
                        }
                        if (++numberOfXObjectHandled % BATCH_SIZE_FOR_LOG == 0) {
                            logger.info("[{}] Ratings XObjects have been handled in document [{}] on a total of [{}]. "
                                + "([{}] migrated and [{}] not migrated).", numberOfXObjectHandled,
                                serializedReference, numberOfXObjectsToHandle, numberOfXObjectsMigrated,
                                numberOfXObjecsNotMigrated);
                        }
                        totalNumberOfXObjectsMigrated += numberOfXObjectsMigrated;
                    }
                    if (numberOfXObjecsNotMigrated > 0) {
                        statistics.put(serializedReference,
                            new ImmutablePair<>(numberOfXObjectsMigrated, numberOfXObjecsNotMigrated));
                    }
                    if (updatedDocument) {
                        wiki.saveDocument(document, "Migration of Rating objects", true, getXWikiContext());
                    }

                    if (++numberOfPageHandled % BATCH_SIZE_FOR_LOG == 0) {
                        logger.info("[{}] pages have been handled for migrating their Ratings xobject on a total of "
                            + "[{}]", numberOfPageHandled, numberOfPagesToHandle);
                    }
                }
                logger.info("All pages have been handled to migrate their Ratings xobject. "
                        + "Total: [{}] pages handled and [{}] xobjects migrated.",
                    numberOfPagesToHandle, totalNumberOfXObjectsMigrated);
                if (!statistics.isEmpty()) {
                    logger.info("Some ratings xobject have not been migrated because of the ratings configuration. "
                        + "Here's the list of pages with ratings values not migrated: ");
                    for (Map.Entry<String, Pair<Integer, Integer>> entry : statistics.entrySet()) {
                        logger.info("Page: [{}]. Migrated: [{}]. Not migrated: [{}]", entry.getKey(),
                            entry.getValue().getLeft(), entry.getValue().getRight());
                    }
                }

                logger.info("Starting recomputation of average ratings on rated pages for consistency of data.");

                int numberOfTargetPageHandled = 0;
                int totalNumberOfTargetPageToHandle = targetOfMigratedRating.size();
                for (DocumentReference ratedDocumentReference : targetOfMigratedRating) {
                    ratingsManager.recomputeAverageRating(ratedDocumentReference);

                    if (++numberOfPageHandled % BATCH_SIZE_FOR_LOG == 0) {
                        logger.info("Average rating have been recomputed on [{}] pages on a total of [{}]",
                            numberOfTargetPageHandled, totalNumberOfTargetPageToHandle);
                    }
                }
                logger.info("Average ratings have been recomputed on all [{}] pages. The migration is now finished.",
                    totalNumberOfXObjectsMigrated);
            } catch (RatingsException e) {
                throw new DataMigrationException("Cannot migrate old rating xobject to new storage system.", e);
            }
        }
    }

    private boolean migrateRatingXObject(BaseObject xObject, DocumentReference ownerDocReference, String managerHint,
        RatingsManager ratingsManager, Set<DocumentReference> targetOfMigratedRatings) throws RatingsException
    {
        String author = xObject.getStringValue("author");
        Date date = xObject.getDateValue("date");
        String parent = xObject.getStringValue("parent");
        int vote = xObject.getIntValue("vote");

        DocumentReference ratedPage = this.documentReferenceResolver.resolve(parent);

        String separateHintManager = "separate";
        boolean migrateRating = (separateHintManager.equals(managerHint) && !ratedPage.equals(ownerDocReference))
            || (!separateHintManager.equals(managerHint) && ratedPage.equals(ownerDocReference));
        if (migrateRating) {
            DefaultRating rating = new DefaultRating(UUID.randomUUID().toString())
                .setManagerId(RatingsManagerFactory.DEFAULT_APP_HINT)
                .setAuthor(this.userReferenceResolver.resolve(author))
                .setReference(ratedPage)
                .setCreatedAt(date)
                .setUpdatedAt(date)
                .setScale(5)
                .setVote(vote);
            ratingsManager.saveRating(rating);
            targetOfMigratedRatings.add(ratedPage);
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
