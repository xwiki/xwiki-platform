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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.ratings.Rating;
import org.xwiki.ratings.RatingsException;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.ratings.UpdateRatingEvent;
import org.xwiki.ratings.UpdatingRatingEvent;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * @version $Id$
 * @see RatingsManager
 * @see AbstractRatingsManager
 * @since 6.4M3
 */
@Component
@Singleton
@Named("separate")
public class SeparatePageRatingsManager extends AbstractRatingsManager
{
    public static final String SEPARATERATINGS_CONFIG_PARAM_PREFIX = "xwiki.ratings.separatepage.";

    public static final String SEPARATERATINGS_CONFIG_FIELDNAME_SEPARATEPAGE_SPACE = "space";

    public static final String SEPARATERATINGS_CONFIG_FIELDNAME_SEPARATEPAGE_RATINGS_SPACE_PER_SPACE =
        "ratingsSpacePerSpace";

    @Inject
    private Logger logger;

    @Inject
    @Named("user/current")
    protected DocumentReferenceResolver<String> userReferenceResolver;

    /**
     * SeparatePageRatingsManager constructor.
     */
    public SeparatePageRatingsManager()
    {
        super();
    }

    /**
     * Gets the ratings space name.
     * 
     * @param documentRef reference to the document with which the ratings are associated
     * @return the ratings space name
     */
    public String getRatingsSpaceName(DocumentReference documentRef)
    {
        String ratingsSpaceName = getXWiki().Param("xwiki.ratings.separatepagemanager.spacename", "");
        ratingsSpaceName =
            getXWiki().getXWikiPreference("ratings_separatepagemanager_spacename", ratingsSpaceName, getXWikiContext());
        return getRatingsConfiguration().getConfigurationParameter(documentRef,
            RatingsManager.RATINGS_CONFIG_CLASS_FIELDNAME_STORAGE_SPACE, ratingsSpaceName);
    }

    /**
     * Gets whether to associate a different space for every space which is ratable.
     * 
     * @param documentRef reference to the document with which the ratings are associated
     * @return whether to associate a different space for every space which is ratable
     */
    public boolean hasRatingsSpaceForeachSpace(DocumentReference documentRef)
    {
        String result = getXWiki().Param("xwiki.ratings.separatepagemanager.ratingsspaceforeachspace", "0");
        result = getXWiki().getXWikiPreference("ratings_separatepagemanager_ratingsspaceforeachspace", result,
            getXWikiContext());
        return (getRatingsConfiguration().getConfigurationParameter(documentRef,
            RatingsManager.RATINGS_CONFIG_CLASS_FIELDNAME_STORAGE_SEPARATE_SPACES, result).equals("1"));
    }

    /**
     * Saves the rating.
     * 
     * @param rating a Rating object
     * @throws RatingsException when an error occurs while saving the rating
     */
    protected void saveRating(Rating rating) throws RatingsException
    {
        try {
            rating.save();
        } catch (XWikiException e) {
            throw new RatingsException(e);
        }
    }

    @Override
    public Rating setRating(DocumentReference documentRef, DocumentReference author, int vote) throws RatingsException
    {
        Rating rating = getRating(documentRef, author);
        int oldVote;
        if (rating == null) {
            oldVote = 0;
            rating = new SeparatePageRating(documentRef, author, vote, getXWikiContext(), this);
        } else {
            oldVote = rating.getVote();
            rating.setVote(vote);
            rating.setDate(new Date());
        }

        // Indicate that we start modifying the rating
        this.observationManager.notify(new UpdatingRatingEvent(documentRef, rating, oldVote), null);

        boolean updateFailed = true;
        try {
            // saving rating
            rating.save();

            // update the average rating
            updateAverageRatings(documentRef, rating, oldVote);

            updateFailed = false;
        } finally {
            if (updateFailed) {
                // Indicate that the we start modifying the rating
                this.observationManager.notify(new UpdatingRatingEvent(documentRef, rating, oldVote), null);
            } else {
                // Indicate that we finished updating the rating
                this.observationManager.notify(new UpdateRatingEvent(documentRef, rating, oldVote), null);
            }
        }

        return rating;
    }

    @Override
    public List<Rating> getRatings(DocumentReference documentRef, int start, int count, boolean asc)
        throws RatingsException
    {
        if (logger.isDebugEnabled()) {
            logger.debug("Calling separate page manager code for ratings");
        }

        String sql =
            ", BaseObject as obj, StringProperty as parentprop where doc.fullName=obj.name and obj.className=?1"
                + " and obj.id=parentprop.id.id and parentprop.id.name=?2 and parentprop.value=?3"
                + " and obj.name not in ("
                + "select obj2.name from BaseObject as obj2, StringProperty as statusprop where obj2.className=?4"
                + " and obj2.id=statusprop.id.id and statusprop.id.name=?5 and "
                + "(statusprop.value=?6 or statusprop.value= ?7) and obj.id=obj2.id"
                + ") order by doc.date "
                + (asc ? "asc" : "desc");

        List<?> params = new ArrayList<>(Arrays.asList(
            getRatingsClassName(),
            RATING_CLASS_FIELDNAME_PARENT,
            entityReferenceSerializer.serialize(documentRef),
            getRatingsClassName(),
            "status",
            "moderated",
            "refused"
        ));

        List<Rating> ratings = new ArrayList<>();
        try {
            List<DocumentReference> ratingPageReferenceList = getXWikiContext().getWiki().getStore()
                    .searchDocumentReferences(sql, count, start, params, getXWikiContext());

            for (DocumentReference ratingPageReference : ratingPageReferenceList) {
                ratings.add(
                    getRatingFromDocument(documentRef, getXWiki().getDocument(ratingPageReference, getXWikiContext())));
            }
        } catch (XWikiException e) {
            throw new RatingsException(e);
        }

        return ratings;
    }

    @Override
    public List<Rating> getRatings(UserReference userReference, int start, int count, boolean asc)
        throws RatingsException
    {
        List<Rating> ratings = new ArrayList<>();

        DocumentReference userDoc = this.userReferenceSerializer.serialize(userReference);
        if (logger.isDebugEnabled()) {
            logger.debug("Calling separate page manager code for ratings");
        }

        // This query performs the following:
        //   - It retrieves both the doc.fullName containing the rating, and the parent property contaning the reference
        //     to the targeted document (and the date to allow ordering on it with distinct)
        //   - It selects only documents containing a RatingClass object whose author is the author reference
        //   - It avoids selecting document translations to avoid duplications in result (and ratings objects shouldn't
        //     be in translated pages)
        //   - It filters out results whom status is refused or moderated
        String statement = "select distinct doc.fullName, parentProp.value, doc.date "
            + "from XWikiDocument doc, BaseObject as obj, StringProperty as authorProp, StringProperty as parentProp "
            + "where doc.fullName=obj.name and doc.translation=0 and obj.className=:ratingClassName "
            + "and obj.id=authorProp.id.id and authorProp.id.name=:authorPropertyName and authorProp.value=:authorValue"
            + " and obj.id=parentProp.id.id and parentProp.id.name=:parentPropertyName"
            + " and obj.name not in ("
            + "select obj2.name from BaseObject as obj2, StringProperty as statusprop "
            + "where obj.id=obj2.id and obj2.className=:ratingClassName and obj2.id=statusprop.id.id and "
            + "statusprop.id.name=:statusPropertyName and "
            + "(statusprop.value=:statusModerated or statusprop.value=:statusRefused)"
            + ") order by doc.date " + (asc ? "asc" : "desc");

        try {
            Query query = this.queryManager.createQuery(statement, Query.HQL);
            List<Object[]> queryResult = query.bindValue("ratingClassName", getRatingsClassName())
                .bindValue("authorPropertyName", RATING_CLASS_FIELDNAME_AUTHOR)
                .bindValue("authorValue", this.entityReferenceSerializer.serialize(userDoc))
                .bindValue("parentPropertyName", RATING_CLASS_FIELDNAME_PARENT)
                .bindValue("statusPropertyName", "status")
                .bindValue("statusModerated", "moderated")
                .bindValue("statusRefused", "refused")
                .setLimit(count)
                .setOffset(start)
                .execute();

            for (Object[] result : queryResult) {
                DocumentReference ratingDocReference = this.documentReferenceResolver.resolve((String) result[0]);
                DocumentReference ratedDocReference = this.documentReferenceResolver.resolve((String) result[1]);
                ratings.add(
                    getRatingFromDocument(ratedDocReference,
                        getXWiki().getDocument(ratingDocReference, getXWikiContext())));
            }
        } catch (QueryException | XWikiException e) {
            throw new RatingsException(String.format("Error while retrieving ratings for user [%s].", userDoc), e);
        }

        return ratings;
    }

    @Override
    public Rating getRating(DocumentReference documentRef, int id) throws RatingsException
    {
        String sql =
            ", BaseObject as obj, StringProperty as parentprop where doc.fullName=obj.name and obj.className=?1"
                + " and obj.id=parentprop.id.id and parentprop.id.name=?2 and parentprop.value=?3"
                + " and obj.name not in (select obj2.name from BaseObject as obj2, StringProperty as statusprop where obj2.className=?4"
                + " and obj2.id=statusprop.id.id and statusprop.id.name=?5 and (statusprop.value=?6 or statusprop.value=?7) and obj.id=obj2.id) order by doc.date desc";

        List<?> params = new ArrayList<>(Arrays.asList(getRatingsClassName(), RATING_CLASS_FIELDNAME_PARENT,
            entityReferenceSerializer.serialize(documentRef), getRatingsClassName(), "status", "moderated", "refused"));

        try {
            List<DocumentReference> ratingPageReferenceList =
                getXWikiContext().getWiki().getStore().searchDocumentReferences(sql, 1, id, params, getXWikiContext());
            if (CollectionUtils.isEmpty(ratingPageReferenceList)) {
                return null;
            } else {
                return new SeparatePageRatingsManager().getRatingFromDocument(documentRef,
                    getXWiki().getDocument(ratingPageReferenceList.get(0), getXWikiContext()));
            }
        } catch (XWikiException e) {
            throw new RatingsException(e);
        }
    }

    @Override
    public Rating getRating(DocumentReference documentRef, DocumentReference author) throws RatingsException
    {
        try {
            for (Rating rating : getRatings(documentRef, 0, 0, false)) {
                if (author.equals(rating.getAuthor())) {
                    return rating;
                }
            }
        } catch (XWikiException e) {
            return null;
        }
        return null;
    }

    @Override
    public Rating getRating(String ratingId) throws RatingsException
    {
        try {
            int i1 = StringUtils.indexOf(ratingId, '.');
            if (i1 == -1) {
                throw new RatingsException(RatingsException.MODULE_PLUGIN_RATINGS,
                    RatingsException.ERROR_RATINGS_INVALID_RATING_ID, "Invalid rating ID, cannot parse rating id");
            }

            XWikiDocument doc = getXWiki().getDocument(ratingId, getXWikiContext());
            if (doc.isNew()) {
                throw new RatingsException(RatingsException.MODULE_PLUGIN_RATINGS,
                    RatingsException.ERROR_RATINGS_INVALID_RATING_ID, "Invalid rating ID, rating does not exist");
            }

            BaseObject object = doc.getObject(getRatingsClassName());
            if (object == null) {
                throw new RatingsException(RatingsException.MODULE_PLUGIN_RATINGS,
                    RatingsException.ERROR_RATINGS_INVALID_RATING_ID, "Invalid rating ID, rating does not exist");
            }

            String parentDocName = object.getStringValue(RATING_CLASS_FIELDNAME_PARENT);
            XWikiDocument parentDoc = getXWikiContext().getWiki().getDocument(parentDocName, getXWikiContext());

            return new SeparatePageRating(parentDoc.getDocumentReference(), doc, getXWikiContext(), this);
        } catch (XWikiException e) {
            throw new RatingsException(e);
        }
    }

    /**
     * Gets a SeparatePageRating instance from a document
     * 
     * @param documentRef the reference of the document which the ratings are for
     * @param doc the document which the ratings are for
     * @return a SeparatePageRating
     * @throws RatingsException when an error occurs while fetching the rating
     */
    public Rating getRatingFromDocument(DocumentReference documentRef, XWikiDocument doc) throws RatingsException
    {
        return new SeparatePageRating(documentRef, doc, getXWikiContext(), this);
    }

    /**
     * Generate a unique DocumentReference that aims to contain the rating object of a rated document.
     *
     * @param ratedDocumentReference reference to the document with which the rating is associated
     * @return a reference to the document in which the rating is stored
     */
    public DocumentReference getRatingDocumentReference(DocumentReference ratedDocumentReference)
    {
        XWikiContext context = getXWikiContext();
        String ratingsSpace = this.getRatingsSpaceName(ratedDocumentReference);
        String pageSuffix = "R";

        boolean hasRatingsSpaceForeachSpace = this.hasRatingsSpaceForeachSpace(ratedDocumentReference);

        SpaceReference spaceReference = ratedDocumentReference.getLastSpaceReference();
        spaceReference.replaceParent(spaceReference.getWikiReference(), context.getWikiReference());

        if (hasRatingsSpaceForeachSpace) {
            spaceReference = new SpaceReference(spaceReference.getName() + ratingsSpace, spaceReference.getParent());

            return getUniquePageName(spaceReference, ratedDocumentReference.getName(), pageSuffix);
        } else if (ratingsSpace == null) {
            return getUniquePageName(spaceReference, ratedDocumentReference.getName() + pageSuffix, "");
        } else {
            SpaceReference ratingSpaceReference = new SpaceReference(context.getWikiId(), ratingsSpace);
            return getUniquePageName(ratingSpaceReference,
                ratedDocumentReference.getLastSpaceReference().getName() + "_" + ratedDocumentReference.getName(),
                pageSuffix);
        }
    }

    /**
     * Gets a unique page name.
     *
     * @param spaceReference the reference of the space in which the document should be
     * @param name the name of the document
     * @param postfix post fix to add to the document name
     * @return the unique document name
     */
    private DocumentReference getUniquePageName(SpaceReference spaceReference, String name, String postfix)
    {
        XWikiContext context = getXWikiContext();
        String originalPageName = context.getWiki().clearName(name, context);
        DocumentReference documentReference = new DocumentReference(originalPageName, spaceReference);
        int i = 1;
        if (context.getWiki().exists(documentReference, context)) {
            do {
                String pageName = originalPageName + postfix + i;
                documentReference = new DocumentReference(pageName, spaceReference);
                i++;
            } while (context.getWiki().exists(documentReference, context));
        }
        return documentReference;
    }
}
