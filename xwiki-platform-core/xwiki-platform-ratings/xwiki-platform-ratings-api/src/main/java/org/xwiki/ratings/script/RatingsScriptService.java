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
package org.xwiki.ratings.script;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.ratings.AverageRatingApi;
import org.xwiki.ratings.ConfiguredProvider;
import org.xwiki.ratings.Rating;
import org.xwiki.ratings.RatingsConfiguration;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.script.service.ScriptService;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Document;

/**
 * Script service offering access to the ratings API.
 * 
 * @version $Id$
 * @since 6.4M3
 */
@Component
@Singleton
@Named("ratings")
public class RatingsScriptService implements ScriptService
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private ConfiguredProvider<RatingsManager> ratingsManagerProvider;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> referenceDocumentReferenceResolver;

    @Inject
    @Named("user/current")
    private DocumentReferenceResolver<String> userReferenceResolver;

    @Inject
    private RatingsConfiguration ratingsConfiguration;

    /**
     * Retrieve the XWiki context from the current execution context.
     * 
     * @return the XWiki context
     * @throws RuntimeException If there was an error retrieving the context
     */
    private XWikiContext getXWikiContext()
    {
        return this.xcontextProvider.get();
    }

    /**
     * Store a caught exception in the context.
     * 
     * @param e the exception to store, can be null to clear the previously stored exception
     */
    private void setError(Throwable e)
    {
        getXWikiContext().put("exception", e);
    }

    /**
     * Retrieve the global configuration document.
     *
     * @return an absolute reference to the global configuration document
     */
    private DocumentReference getGlobalConfig()
    {
        return this.referenceDocumentReferenceResolver.resolve(RatingsManager.RATINGS_CONFIG_GLOBAL_REFERENCE);
    }

    /**
     * Wrap rating objects.
     * 
     * @param ratings a list of rating object
     * @return list of object wrapped with the RatingAPI
     */
    private static List<RatingApi> wrapRatings(List<Rating> ratings)
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
     * Set a new rating.
     * 
     * @param doc the document which is being rated
     * @param author the author giving the rating
     * @param vote the number of stars given (from 1 to 5)
     * @return the new rating object
     * @deprecated use {@link #setRating(DocumentReference, DocumentReference, int)} instead
     */
    @Deprecated
    public RatingApi setRating(Document doc, String author, int vote)
    {
        DocumentReference documentRef = doc.getDocumentReference();
        DocumentReference authorRef = this.userReferenceResolver.resolve(author);
        return setRating(documentRef, authorRef, vote);
    }

    /**
     * Set a new rating.
     * 
     * @param document the document which is being rated
     * @param author the author giving the rating
     * @param vote the number of stars given (from 1 to 5)
     * @return the new rating object
     */
    public RatingApi setRating(DocumentReference document, DocumentReference author, int vote)
    {
        // TODO protect this with programming rights
        // and add a setRating(docName), not protected but for which the author is retrieved from getXWikiContext().
        setError(null);

        try {
            return new RatingApi(this.ratingsManagerProvider.get(document).setRating(document, author, vote));
        } catch (Throwable e) {
            setError(e);
            return null;
        }
    }

    /**
     * Retrieve a specific rating.
     * 
     * @param doc the document to which the rating belongs to
     * @param author the user that gave the rating
     * @return a rating object
     * @deprecated use {@link #getRating(DocumentReference, DocumentReference)} instead
     */
    @Deprecated
    public RatingApi getRating(Document doc, String author)
    {
        DocumentReference documentRef = doc.getDocumentReference();
        DocumentReference authorRef = this.userReferenceResolver.resolve(author);
        return getRating(documentRef, authorRef);
    }

    /**
     * Retrieve a specific rating.
     * 
     * @param document the document to which the rating belongs to
     * @param author the user that gave the rating
     * @return a rating object
     */
    public RatingApi getRating(DocumentReference document, DocumentReference author)
    {
        setError(null);

        try {
            Rating rating = this.ratingsManagerProvider.get(document).getRating(document, author);
            if (rating == null) {
                return null;
            }
            return new RatingApi(rating);
        } catch (Throwable e) {
            setError(e);
            return null;
        }
    }

    /**
     * Get a list of ratings.
     * 
     * @param doc the document to which the ratings belong to
     * @param start the offset from which to start
     * @param count number of ratings to return
     * @return a list of rating objects
     * @deprecated use {@link #getRatings(DocumentReference, int, int)} instead
     */
    @Deprecated
    public List<RatingApi> getRatings(Document doc, int start, int count)
    {
        return getRatings(doc.getDocumentReference(), start, count);
    }

    /**
     * Get a list of ratings.
     * 
     * @param document the document to which the ratings belong to
     * @param start the offset from which to start
     * @param count number of ratings to return
     * @return a list of rating objects
     */
    public List<RatingApi> getRatings(DocumentReference document, int start, int count)
    {
        return getRatings(document, start, count, true);
    }

    /**
     * Get a sorted list of ratings.
     * 
     * @param doc the document to which the ratings belong to
     * @param start the offset from which to start
     * @param count number of ratings to return
     * @param asc in ascending order or not
     * @return a list of rating objects
     * @deprecated use {@link #getRatings(DocumentReference, int, int, boolean)} instead
     */
    @Deprecated
    public List<RatingApi> getRatings(Document doc, int start, int count, boolean asc)
    {
        return getRatings(doc.getDocumentReference(), start, count, asc);
    }

    /**
     * Get a sorted list of ratings.
     * 
     * @param document the document to which the ratings belong to
     * @param start the offset from which to start
     * @param count number of ratings to return
     * @param asc in ascending order or not
     * @return a list of rating objects
     */
    public List<RatingApi> getRatings(DocumentReference document, int start, int count, boolean asc)
    {
        setError(null);

        try {
            return wrapRatings(this.ratingsManagerProvider.get(document).getRatings(document, start, count, asc));
        } catch (Exception e) {
            setError(e);
            return null;
        }
    }

    /**
     * Get average rating.
     * 
     * @param doc the document to which the average rating belongs to
     * @param method the method of calculating the average
     * @return a average rating API object
     * @deprecated use {@link #getAverageRating(DocumentReference, String)} instead
     */
    @Deprecated
    public AverageRatingApi getAverageRating(Document doc, String method)
    {
        return getAverageRating(doc.getDocumentReference(), method);
    }

    /**
     * Get average rating.
     * 
     * @param document the document to which the average rating belongs to
     * @param method the method of calculating the average
     * @return a average rating API object
     */
    public AverageRatingApi getAverageRating(DocumentReference document, String method)
    {
        setError(null);

        try {
            return new AverageRatingApi(this.ratingsManagerProvider.get(document).getAverageRating(document, method));
        } catch (Throwable e) {
            setError(e);
            return null;
        }
    }

    /**
     * Get average rating.
     * 
     * @param doc the document to which the average rating belongs to
     * @return a average rating API object
     * @deprecated use {@link #getAverageRating(DocumentReference)} instead
     */
    @Deprecated
    public AverageRatingApi getAverageRating(Document doc)
    {
        return getAverageRating(doc.getDocumentReference());
    }

    /**
     * Get average rating.
     * 
     * @param document the document to which the average rating belongs to
     * @return a average rating API object
     */
    public AverageRatingApi getAverageRating(DocumentReference document)
    {
        setError(null);

        try {
            return new AverageRatingApi(this.ratingsManagerProvider.get(document).getAverageRating(document));
        } catch (Throwable e) {
            setError(e);
            return null;
        }
    }

    /**
     * Get average rating from query.
     * 
     * @param fromsql the from clause of the query
     * @param wheresql the where clause of the query
     * @param method the method of calculating the average
     * @return a average rating API object
     */
    public AverageRatingApi getAverageRating(String fromsql, String wheresql, String method)
    {
        setError(null);

        try {
            return new AverageRatingApi(this.ratingsManagerProvider.get(getGlobalConfig()).getAverageRatingFromQuery(
                fromsql, wheresql, method));
        } catch (Throwable e) {
            setError(e);
            return null;
        }
    }

    /**
     * Get average rating from query.
     * 
     * @param fromsql the from clause of the query
     * @param wheresql the where clause of the query
     * @return a average rating API object
     */
    public AverageRatingApi getAverageRating(String fromsql, String wheresql)
    {
        setError(null);

        try {
            return new AverageRatingApi(this.ratingsManagerProvider.get(getGlobalConfig()).getAverageRatingFromQuery(
                fromsql, wheresql));
        } catch (Throwable e) {
            setError(e);
            return null;
        }
    }

    /**
     * Get a user's reputation.
     * 
     * @param username the user document
     * @return a average rating API object
     */
    public AverageRatingApi getUserReputation(String username)
    {
        DocumentReference userRef = this.userReferenceResolver.resolve(username);
        return getUserReputation(userRef);
    }

    /**
     * Get a user's reputation.
     * 
     * @param username the user document
     * @return a average rating API object
     */
    public AverageRatingApi getUserReputation(DocumentReference username)
    {
        setError(null);

        try {
            return new AverageRatingApi(this.ratingsManagerProvider.get(getGlobalConfig()).getUserReputation(username));
        } catch (Throwable e) {
            setError(e);
            return null;
        }
    }
    
    /**
     * Get configuration document.
      *
      * @param documentReference the documentReference for which to return the configuration document
      * @return the configuration document
      * @since 8.2.1
      */
    public Document getConfigurationDocument(DocumentReference documentReference)
    {
        return ratingsConfiguration.getConfigurationDocument(documentReference).newDocument(getXWikiContext());
    }
}
