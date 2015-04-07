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
import org.xwiki.ratings.RatingsException;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.ratings.UpdateRatingEvent;

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
    private Logger LOGGER;

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
     * @param documentRef reference to the document with which the ratings are associated
     * @return the ratings space name
     */
    public String getRatingsSpaceName(DocumentReference documentRef)
    {
        String ratingsSpaceName = getXWiki().Param("xwiki.ratings.separatepagemanager.spacename", "");
        ratingsSpaceName =
            getXWiki().getXWikiPreference("ratings_separatepagemanager_spacename", ratingsSpaceName, getXWikiContext());
        return getConfigParameter(documentRef, RatingsManager.RATINGS_CONFIG_CLASS_FIELDNAME_STORAGE_SPACE,
            ratingsSpaceName);
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
        result =
            getXWiki().getXWikiPreference("ratings_separatepagemanager_ratingsspaceforeachspace", result,
                getXWikiContext());
        return (getConfigParameter(documentRef, RatingsManager.RATINGS_CONFIG_CLASS_FIELDNAME_STORAGE_SEPARATE_SPACES,
            result) == "1");
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
            // Force content dirty to false, so that the content update date is not changed when saving the document.
            // This should not be handled there, since it is not the responsibility of this plugin to decide if
            // the content has actually been changed or not since current revision, but the implementation of
            // this in XWiki core is wrong. See http://jira.xwiki.org/jira/XWIKI-2800 for more details.
            // There is a draw-back to doing this, being that if the document content is being changed before
            // the document is rated, the contentUpdateDate will not be modified. Although possible, this is very
            // unlikely to happen, or to be a use case. The default rating application will use an asynchronous service
            // to
            // note a document, which service will only set the rating, so the behavior will be correct.
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

        // saving rating
        rating.save();

        // update the average rating
        updateAverageRatings(documentRef, rating, oldVote);

        // update reputation
        observationManager.notify(new UpdateRatingEvent(documentRef, rating, oldVote), null);
        return rating;
    }

    @Override
    public List<Rating> getRatings(DocumentReference documentRef, int start, int count, boolean asc)
        throws RatingsException
    {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Calling separate page manager code for ratings");
        }

        String sql =
            ", BaseObject as obj, StringProperty as parentprop where doc.fullName=obj.name and obj.className='"
                + getRatingsClassName()
                + "' and obj.id=parentprop.id.id and parentprop.id.name='"
                + RATING_CLASS_FIELDNAME_PARENT
                + "' and parentprop.value='"
                + entityReferenceSerializer.serialize(documentRef)
                + "' and obj.name not in (select obj2.name from BaseObject as obj2, StringProperty as statusprop where obj2.className='"
                + getRatingsClassName()
                + "' and obj2.id=statusprop.id.id and statusprop.id.name='status' and (statusprop.value='moderated' or statusprop.value='refused') and obj.id=obj2.id) order by doc.date "
                + (asc ? "asc" : "desc");
        List<Rating> ratings = new ArrayList<Rating>();
        try {
            List<DocumentReference> ratingPageReferenceList =
                getXWiki().getStore().searchDocumentReferences(sql, count, start, getXWikiContext());

            for (DocumentReference ratingPageReference : ratingPageReferenceList) {
                ratings.add(getRatingFromDocument(documentRef,
                    getXWiki().getDocument(ratingPageReference, getXWikiContext())));
            }
        } catch (XWikiException e) {
            throw new RatingsException(e);
        }

        return ratings;
    }

    @Override
    public Rating getRating(DocumentReference documentRef, int id) throws RatingsException
    {
        String sql =
            ", BaseObject as obj, StringProperty as parentprop where doc.fullName=obj.name and obj.className='"
                + getRatingsClassName()
                + "' and obj.id=parentprop.id.id and parentprop.id.name='"
                + RATING_CLASS_FIELDNAME_PARENT
                + "' and parentprop.value='"
                + entityReferenceSerializer.serialize(documentRef)
                + "' and obj.name not in (select obj2.name from BaseObject as obj2, StringProperty as statusprop where obj2.className='"
                + getRatingsClassName()
                + "' and obj2.id=statusprop.id.id and statusprop.id.name='status' and (statusprop.value='moderated' or statusprop.value='refused') and obj.id=obj2.id) order by doc.date desc";
        try {
            List<DocumentReference> ratingPageReferenceList =
                getXWiki().getStore().searchDocumentReferences(sql, 1, id, getXWikiContext());
            if ((ratingPageReferenceList == null) || (ratingPageReferenceList.size() == 0)) {
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
     * @param documentRef the reference of the document which the ratings are for
     * @param doc the document which the ratings are for
     * @return a SeparatePageRating
     * @throws RatingsException when an error occurs while fetching the rating
     */
    public Rating getRatingFromDocument(DocumentReference documentRef, XWikiDocument doc) throws RatingsException
    {
        return new SeparatePageRating(documentRef, doc, getXWikiContext(), this);
    }
}
