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
import javax.inject.Singleton;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import org.xwiki.component.annotation.Component;
import org.xwiki.ratings.Rating;
import org.xwiki.ratings.RatingsException;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.ratings.UpdateRatingEvent;

/**
 * @version $Id$
 * @see RatingsManager
 * @see AbstractRatingsManager
 */
@Component
@Singleton
@Named("separate")
public class SeparatePageRatingsManager extends AbstractRatingsManager
{
    public static final String RATINGS_CONFIG_PARAM_PREFIX = "xwiki.ratings.separatepage.";
    public static final String RATINGS_CONFIG_FIELDNAME_SEPARATEPAGE_SPACE = "space";
    public static final String RATINGS_CONFIG_FIELDNAME_SEPARATEPAGE_RATINGS_SPACE_PER_SPACE = "ratingsSpacePerSpace";

    /**
     * The logger to LOGGER.
     */
    @Inject
    private Logger LOGGER;
  
    public SeparatePageRatingsManager()
    {
        super();
    }

    public String getRatingsSpaceName()
    {
        String ratingsSpaceName = getXWiki().Param("xwiki.ratings.separatepagemanager.spacename", "");
        ratingsSpaceName =
            getXWiki().getXWikiPreference("ratings_separatepagemanager_spacename", ratingsSpaceName, getXWikiContext());
        return getConfigParameter(RatingsManager.RATINGS_CONFIG_CLASS_FIELDNAME_STORAGE_SPACE, ratingsSpaceName);
    }

    public boolean hasRatingsSpaceForeachSpace()
    {
        String result = getXWiki().Param("xwiki.ratings.separatepagemanager.ratingsspaceforeachspace", "0");
        result = getXWiki().getXWikiPreference("ratings_separatepagemanager_ratingsspaceforeachspace", result, getXWikiContext());
        return (getConfigParameter(RatingsManager.RATINGS_CONFIG_CLASS_FIELDNAME_STORAGE_SEPARATE_SPACES, result) == "1");
    }

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
    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.ratings.RatingsManager#setRating(com.xpn.xwiki.plugin.comments.Container, String, int,
     *      com.xpn.xwiki.XWikiContext)
     */
    public Rating setRating(String documentName, String author, int vote) throws RatingsException
    {
        Rating rating = getRating(documentName, author);
        int oldVote;
        if (rating == null) {
            oldVote = 0;
            rating = new SeparatePageRating(documentName, author, vote, getXWikiContext(), this);
        } else {
            oldVote = rating.getVote();
            rating.setVote(vote);
            rating.setDate(new Date());
        }
        
        // savin rating
        rating.save();
       
        // update the average rating
        updateAverageRatings(documentName, rating, oldVote);

        // update reputation
        observationManager.notify(new UpdateRatingEvent(documentName, rating, oldVote), null);
        return rating;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.ratings.RatingsManager#getRatings(com.xpn.xwiki.plugin.comments.Container, int, int,
     *      boolean, com.xpn.xwiki.XWikiContext)
     */
    public List<Rating> getRatings(String documentName, int start, int count, boolean asc) throws RatingsException
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
                + documentName
                +
                "' and obj.name not in (select obj2.name from BaseObject as obj2, StringProperty as statusprop where obj2.className='"
                + getRatingsClassName()
                +
                "' and obj2.id=statusprop.id.id and statusprop.id.name='status' and (statusprop.value='moderated' or statusprop.value='refused') and obj.id=obj2.id) order by doc.date "
                + (asc ? "asc" : "desc");
        List<Rating> ratings = new ArrayList<Rating>();
        try {
            List<String> ratingPageNameList =
                getXWiki().getStore().searchDocumentsNames(sql, count, start, getXWikiContext());

            for (String ratingPageName : ratingPageNameList) {
                ratings.add(getRatingFromDocument(documentName, getXWiki()
                    .getDocument(ratingPageName, getXWikiContext())));
            }
        } catch (XWikiException e) {
            throw new RatingsException(e);
        }

        return ratings;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.ratings.RatingsManager#getRatings(com.xpn.xwiki.plugin.comments.Container, int, int,
     *      boolean, com.xpn.xwiki.XWikiContext)
     */
    public Rating getRating(String documentName, int id) throws RatingsException
    {
        String sql =
            ", BaseObject as obj, StringProperty as parentprop where doc.fullName=obj.name and obj.className='"
                + getRatingsClassName()
                + "' and obj.id=parentprop.id.id and parentprop.id.name='"
                + RATING_CLASS_FIELDNAME_PARENT
                + "' and parentprop.value='"
                + documentName
                +
                "' and obj.name not in (select obj2.name from BaseObject as obj2, StringProperty as statusprop where obj2.className='"
                + getRatingsClassName()
                +
                "' and obj2.id=statusprop.id.id and statusprop.id.name='status' and (statusprop.value='moderated' or statusprop.value='refused') and obj.id=obj2.id) order by doc.date desc";
        try {
            List<String> ratingPageNameList = getXWiki().getStore().searchDocumentsNames(sql, 1, id, getXWikiContext());
            if ((ratingPageNameList == null) || (ratingPageNameList.size() == 0)) {
                return null;
            } else {
                return new SeparatePageRatingsManager().getRatingFromDocument(documentName, getXWiki()
                    .getDocument(ratingPageNameList.get(0), getXWikiContext()));
            }
        } catch (XWikiException e) {
            throw new RatingsException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.ratings.RatingsManager#getRating(com.xpn.xwiki.plugin.comments.Container, String,
     *      com.xpn.xwiki.XWikiContext)
     */
    public Rating getRating(String documentName, String author) throws RatingsException
    {
        try {
            for (Rating rating : getRatings(documentName, 0, 0, false)) {
                if (author.equals(rating.getAuthor())) {
                    return rating;
                }
            }
        } catch (XWikiException e) {
            return null;
        }
        return null;
    }

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

            return new SeparatePageRating(parentDocName, doc, getXWikiContext(), this);
        } catch (XWikiException e) {
            throw new RatingsException(e);
        }
    }

    public Rating getRatingFromDocument(String documentName, XWikiDocument doc)
        throws RatingsException
    {
        return new SeparatePageRating(documentName, doc, getXWikiContext(), this);
    }
}
