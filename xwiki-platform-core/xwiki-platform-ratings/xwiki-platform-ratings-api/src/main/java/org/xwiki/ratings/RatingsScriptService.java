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
package org.xwiki.ratings;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Document;

import java.util.List;
import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.ratings.ConfiguredProvider;
import org.xwiki.script.service.ScriptService;

/**
 * @version $Id$
 */
@Component
@Singleton
@Named("ratings")
public class RatingsScriptService implements ScriptService
{
    @Inject
    private Execution execution;
    
    @Inject
    private ConfiguredProvider<RatingsManager> ratingsManagerProvider;

    @Inject
    private Provider<ReputationAlgorithm> reputationAlgorithmProvider;
 
    protected static List<RatingApi> wrapRatings(List<Rating> ratings)
    {
        if (ratings == null) {
            return null;
        }

        List<RatingApi> ratingsResult = new ArrayList<RatingApi>();
        for (Rating rating : ratings) {
            ratingsResult.add(new RatingApi(rating));
        }
        return ratingsResult;
    }

    /**
     * <p>
     * Retrieve the XWiki context from the current execution context
     * </p>
     * 
     * @return The XWiki context.
     * @throws RuntimeException If there was an error retrieving the context.
     */
    protected XWikiContext getXWikiContext()
    {
        return (XWikiContext) execution.getContext().getProperty("xwikicontext");
    }
    
    
    public RatingApi setRating(Document doc, String author, int vote)
    {
        // TODO protect this with programming rights
        // and add a setRating(docName), not protected but for which the author is retrieved from getXWikiContext().
        try {
            String documentName = doc.getFullName();
            return new RatingApi(ratingsManagerProvider.get(documentName).setRating(documentName, author, vote));
        } catch (Throwable e) {
            getXWikiContext().put("exception", e);
            return null;
        }
    }

    public RatingApi getRating(Document doc, String author)
    {
        try {
            String documentName = doc.getFullName();
            Rating rating = ratingsManagerProvider.get(documentName).getRating(documentName, author);
            if (rating == null) {
                return null;
            }
            return new RatingApi(rating);
        } catch (Throwable e) {
            getXWikiContext().put("exception", e);
            return null;
        }
    }

    public List<RatingApi> getRatings(Document doc, int start, int count)
    {
        return getRatings(doc, start, count, true);
    }

    public List<RatingApi> getRatings(Document doc, int start, int count, boolean asc)
    {
        try {
            String documentName = doc.getFullName();
            return wrapRatings(ratingsManagerProvider.get(documentName).getRatings(documentName, start, count, asc));
        } catch (Exception e) {
            getXWikiContext().put("exception", e);
            return null;
        }
    }

    public AverageRatingApi getAverageRating(Document doc, String method)
    {
        try {
            String documentName = doc.getFullName();
            return new AverageRatingApi(ratingsManagerProvider.get(documentName).getAverageRating(documentName, method));
        } catch (Throwable e) {
            getXWikiContext().put("exception", e);
            return null;
        }
    }

    public AverageRatingApi getAverageRating(Document doc)
    {
        try {
            String documentName = doc.getFullName();
            return new AverageRatingApi(ratingsManagerProvider.get(documentName).getAverageRating(documentName));
        } catch (Throwable e) {
            getXWikiContext().put("exception", e);
            return null;
        }
    }

    public AverageRatingApi getAverageRating(String fromsql, String wheresql, String method)
    {
        try {
            return new AverageRatingApi(ratingsManagerProvider.get(RatingsManager.RATINGS_CONFIG_GLOBAL_PAGE).getAverageRatingFromQuery(fromsql, wheresql, method));
        } catch (Throwable e) {
            getXWikiContext().put("exception", e);
            return null;
        }
    }

    public AverageRatingApi getAverageRating(String fromsql, String wheresql)
    {
        try {
            return new AverageRatingApi(ratingsManagerProvider.get(RatingsManager.RATINGS_CONFIG_GLOBAL_PAGE).getAverageRatingFromQuery(fromsql, wheresql));
        } catch (Throwable e) {
            getXWikiContext().put("exception", e);
            return null;
        }
    }

    public AverageRatingApi getUserReputation(String username)
    {
        try {
            return new AverageRatingApi(ratingsManagerProvider.get(RatingsManager.RATINGS_CONFIG_GLOBAL_PAGE).getUserReputation(username));
        } catch (Throwable e) {
            getXWikiContext().put("exception", e);
            return null;
        }
    }
}
