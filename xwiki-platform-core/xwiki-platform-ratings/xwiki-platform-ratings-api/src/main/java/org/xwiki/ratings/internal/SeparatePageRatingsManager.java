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
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.ratings.Rating;
import org.xwiki.ratings.RatingsConfiguration;
import org.xwiki.ratings.RatingsException;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.ratings.UpdateRatingEvent;
import org.xwiki.ratings.UpdatingRatingEvent;

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
    private RatingsConfiguration ratingsConfiguration;

    @Inject
    @Named("user/current")
    protected DocumentReferenceResolver<String> userReferenceResolver;

    @Inject
    @Named("compactwiki")
    protected EntityReferenceSerializer<String> entityReferenceSerializer;

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
     * @param documentReference reference to the document with which the ratings are associated
     * @return the ratings space name
     */
    public String getRatingsSpaceName(DocumentReference documentReference)
    {
        String ratingsSpaceName = getXWiki().Param("xwiki.ratings.separatepagemanager.spacename", "");
        ratingsSpaceName =
            getXWiki().getXWikiPreference("ratings_separatepagemanager_spacename", ratingsSpaceName, getXWikiContext());
        return ratingsConfiguration.getConfigurationParameter(documentReference,
            RatingsManager.RATINGS_CONFIG_CLASS_FIELDNAME_STORAGE_SPACE, ratingsSpaceName);
    }

    /**
     * Gets whether to associate a different space for every space which is ratable.
     * 
     * @param documentReference reference to the document with which the ratings are associated
     * @return whether to associate a different space for every space which is ratable
     */
    public boolean hasRatingsSpaceForeachSpace(DocumentReference documentReference)
    {
        String result = getXWiki().Param("xwiki.ratings.separatepagemanager.ratingsspaceforeachspace", "0");
        result =
            getXWiki().getXWikiPreference("ratings_separatepagemanager_ratingsspaceforeachspace", result,
                getXWikiContext());
        return (ratingsConfiguration.getConfigurationParameter(documentReference,
            RatingsManager.RATINGS_CONFIG_CLASS_FIELDNAME_STORAGE_SEPARATE_SPACES, result) == "1");
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
    public Rating setRating(DocumentReference documentReference, DocumentReference author, int vote) throws RatingsException
    {
        Rating rating = getRating(documentReference, author);
        int oldVote;
        if (rating == null) {
            oldVote = 0;
            rating = new SeparatePageRating(documentReference, author, vote, getXWikiContext(), this);
        } else {
            oldVote = rating.getVote();
            rating.setVote(vote);
            rating.setDate(new Date());
        }

        // Indicate that we start modifying the rating
        this.observationManager.notify(new UpdatingRatingEvent(documentReference, rating, oldVote), null);

        boolean updateFailed = true;
        try {
            // saving rating
            rating.save();

            // update the average rating
            updateAverageRatings(documentReference, rating, oldVote);

            updateFailed = false;
        } finally {
            if (updateFailed) {
                // Indicate that the we start modifying the rating
                this.observationManager.notify(new UpdatingRatingEvent(documentReference, rating, oldVote), null);
            } else {
                // Indicate that we finished updating the rating
                this.observationManager.notify(new UpdateRatingEvent(documentReference, rating, oldVote), null);
            }
        }

        return rating;
    }

    @Override
    public List<Rating> getRatings(DocumentReference documentReference, int start, int count, boolean asc)
        throws RatingsException
    {
        if (logger.isDebugEnabled()) {
            logger.debug("Calling separate page manager code for ratings");
        }

        String sql =
            ", BaseObject as obj, StringProperty as parentprop where doc.fullName=obj.name and obj.className='"
                + getRatingsClassName()
                + "' and obj.id=parentprop.id.id and parentprop.id.name='"
                + RATING_CLASS_FIELDNAME_PARENT
                + "' and parentprop.value='"
                + entityReferenceSerializer.serialize(documentReference)
                + "' and obj.name not in (select obj2.name from BaseObject as obj2, StringProperty as statusprop where obj2.className='"
                + getRatingsClassName()
                + "' and obj2.id=statusprop.id.id and statusprop.id.name='status' and (statusprop.value='moderated' or statusprop.value='refused') and obj.id=obj2.id) order by doc.date "
                + (asc ? "asc" : "desc");
        List<Rating> ratings = new ArrayList<Rating>();
        try {
            List<DocumentReference> ratingPageReferenceList =
                getXWiki().getStore().searchDocumentReferences(sql, count, start, getXWikiContext());

            for (DocumentReference ratingPageReference : ratingPageReferenceList) {
                ratings.add(getRatingFromDocument(documentReference,
                    getXWiki().getDocument(ratingPageReference, getXWikiContext())));
            }
        } catch (XWikiException e) {
            throw new RatingsException(e);
        }

        return ratings;
    }

    @Override
    public Rating getRating(DocumentReference documentReference, int id) throws RatingsException
    {
        String sql =
            ", BaseObject as obj, StringProperty as parentprop where doc.fullName=obj.name and obj.className='"
                + getRatingsClassName()
                + "' and obj.id=parentprop.id.id and parentprop.id.name='"
                + RATING_CLASS_FIELDNAME_PARENT
                + "' and parentprop.value='"
                + entityReferenceSerializer.serialize(documentReference)
                + "' and obj.name not in (select obj2.name from BaseObject as obj2, StringProperty as statusprop where obj2.className='"
                + getRatingsClassName()
                + "' and obj2.id=statusprop.id.id and statusprop.id.name='status' and (statusprop.value='moderated' or statusprop.value='refused') and obj.id=obj2.id) order by doc.date desc";
        try {
            List<DocumentReference> ratingPageReferenceList =
                getXWiki().getStore().searchDocumentReferences(sql, 1, id, getXWikiContext());
            if ((ratingPageReferenceList == null) || (ratingPageReferenceList.size() == 0)) {
                return null;
            } else {
                return new SeparatePageRatingsManager().getRatingFromDocument(documentReference,
                    getXWiki().getDocument(ratingPageReferenceList.get(0), getXWikiContext()));
            }
        } catch (XWikiException e) {
            throw new RatingsException(e);
        }
    }

    @Override
    public Rating getRating(DocumentReference documentReference, DocumentReference author) throws RatingsException
    {
        try {
            for (Rating rating : getRatings(documentReference, 0, 0, false)) {
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
            int i1 = ratingId.indexOf(".");
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
     * @param documentReference the reference of the document which the ratings are for
     * @param doc the document which the ratings are for
     * @return a SeparatePageRating
     * @throws RatingsException when an error occurs while fetching the rating
     */
    public Rating getRatingFromDocument(DocumentReference documentReference, XWikiDocument doc) throws RatingsException
    {
        return new SeparatePageRating(documentReference, doc, getXWikiContext(), this);
    }
}
