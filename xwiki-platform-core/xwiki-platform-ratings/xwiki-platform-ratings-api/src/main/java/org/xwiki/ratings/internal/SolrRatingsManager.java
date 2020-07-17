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

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.ratings.Rating;
import org.xwiki.ratings.RatingsException;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.SolrUtils;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;

/**
 * A dedicated RatingManager that uses Solr to store the ratings.
 *
 * Note that only the actual ratings are stored in Solr, the average ratings are still stored directly in the documents.
 *
 * @version $Id$
 * @since 12.7RC1
 */
@Component
@Named("solr")
@Singleton
public class SolrRatingsManager extends AbstractRatingsManager
{
    /**
     * Specific field on Solr for storing Rating local ID.
     */
    public static final String RATING_ID_FIELDNAME = "ratingId";

    private static final String ID_FIELDNAME = "id";

    private static final String DOUBLE_FILTER_QUERY = "filter(%s:%s) AND filter(%s:%s)";

    @Inject
    @Named("document")
    protected UserReferenceSerializer<DocumentReference> userReferenceSerializer;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private SolrUtils solrUtils;

    @Inject
    private Solr solr;

    private SolrClient getSolrClient() throws SolrException
    {
        return this.solr.getClient(RatingCoreSolrInitializer.NAME);
    }

    @Override
    public String getRatingsClassName()
    {
        return null;
    }

    private SolrQuery.ORDER getOrder(boolean asc)
    {
        return (asc) ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc;
    }

    private Rating getRatingFromSolrDocument(SolrDocument document)
    {
        String serializedDocumentReference = solrUtils.get(RatingsManager.RATING_CLASS_FIELDNAME_PARENT, document);
        int ratingId = solrUtils.get(RATING_ID_FIELDNAME, document);
        String globalRatingId = solrUtils.getId(document);
        Date ratingDate = solrUtils.get(RatingsManager.RATING_CLASS_FIELDNAME_DATE, document);
        int vote = solrUtils.get(RatingsManager.RATING_CLASS_FIELDNAME_VOTE, document);
        String serializedAuthorReference = solrUtils.get(RatingsManager.RATING_CLASS_FIELDNAME_AUTHOR, document);

        DocumentReference documentReference = this.documentReferenceResolver.resolve(serializedDocumentReference);
        DocumentReference authorReference = this.documentReferenceResolver.resolve(serializedAuthorReference);
        SolrRating result = new SolrRating(documentReference, globalRatingId, ratingId);
        result.setAuthor(authorReference);
        result.setDate(ratingDate);
        result.setVote(vote);
        return result;
    }

    private List<Rating> getRatingsFromQueryResult(SolrDocumentList documents)
    {
        if (documents != null) {
            return documents.stream().map(this::getRatingFromSolrDocument).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private List<Rating> getRatings(String filterQuery, int start, int count, boolean asc)
        throws RatingsException
    {
        SolrQuery solrQuery = new SolrQuery()
            .addFilterQuery(filterQuery)
            .setStart(start)
            .setSort(ID_FIELDNAME, getOrder(asc));
        if (count > 0) {
            solrQuery = solrQuery.setRows(count);
        }
        try {
            QueryResponse queryResponse = getSolrClient().query(solrQuery);
            return this.getRatingsFromQueryResult(queryResponse.getResults());
        } catch (SolrServerException | IOException | SolrException e) {
            throw new RatingsException("Error while getting ratings", e);
        }
    }

    private List<Rating> getRatings(String field, String value, int start, int count, boolean asc)
        throws RatingsException
    {
        String filterQuery = String.format("filter(%s:%s)", field, this.solrUtils.toFilterQueryString(value));
        return getRatings(filterQuery, start, count, asc);
    }

    @Override
    public List<Rating> getRatings(DocumentReference documentRef, int start, int count, boolean asc)
        throws RatingsException
    {
        return this.getRatings(RatingsManager.RATING_CLASS_FIELDNAME_PARENT,
            this.entityReferenceSerializer.serialize(documentRef), start, count, asc);
    }

    @Override
    public List<Rating> getRatings(UserReference userReference, int start, int count, boolean asc)
        throws RatingsException
    {
        return this.getRatings(RatingsManager.RATING_CLASS_FIELDNAME_AUTHOR,
            this.entityReferenceSerializer.serialize(this.userReferenceSerializer.serialize(userReference)),
            start, count, asc);
    }

    @Override
    public Rating getRating(String ratingId) throws RatingsException
    {
        List<Rating> ratings = this.getRatings(ID_FIELDNAME, ratingId, 0, 1, true);
        return ratings.isEmpty() ? null : ratings.get(0);
    }

    @Override
    public Rating getRating(DocumentReference documentRef, int id) throws RatingsException
    {
        String filterQuery = String.format(DOUBLE_FILTER_QUERY,
            RatingsManager.RATING_CLASS_FIELDNAME_PARENT,
            this.solrUtils.toFilterQueryString(this.entityReferenceSerializer.serialize(documentRef)),
            RATING_ID_FIELDNAME, id);

        List<Rating> ratings = this.getRatings(filterQuery, 0, 1, true);
        return ratings.isEmpty() ? null : ratings.get(0);
    }

    @Override
    public Rating getRating(DocumentReference documentRef, DocumentReference user) throws RatingsException
    {
        String filterQuery = String.format(DOUBLE_FILTER_QUERY,
            RatingsManager.RATING_CLASS_FIELDNAME_PARENT,
            this.solrUtils.toFilterQueryString(this.entityReferenceSerializer.serialize(documentRef)),
            RatingsManager.RATING_CLASS_FIELDNAME_AUTHOR,
            this.solrUtils.toFilterQueryString(this.entityReferenceSerializer.serialize(user)));

        List<Rating> ratings = this.getRatings(filterQuery, 0, 1, true);
        return ratings.isEmpty() ? null : ratings.get(0);
    }

    @Override
    public Rating setRating(DocumentReference documentRef, DocumentReference author, int vote)
        throws RatingsException
    {
        Rating rating = getRating(documentRef, author);
        int ratingNumber = 0;
        String serializedDocumentRef = this.entityReferenceSerializer.serialize(documentRef);
        String serializedAuthor = this.entityReferenceSerializer.serialize(author);
        String globalId;
        if (rating != null) {
            globalId = rating.getGlobalRatingId();
            ratingNumber = Integer.parseInt(rating.getRatingId());
        } else {
            List<Rating> ratings = getRatings(documentRef, 0, 1, false);

            if (!ratings.isEmpty()) {
                ratingNumber = Integer.parseInt(ratings.get(0).getRatingId()) + 1;
            }

            globalId = String.format("%s_%s", serializedDocumentRef, ratingNumber);
        }

        SolrInputDocument solrInputDocument = new SolrInputDocument();
        solrUtils.setId(globalId, solrInputDocument);
        solrUtils.set(RATING_ID_FIELDNAME, ratingNumber, solrInputDocument);
        solrUtils.set(RatingsManager.RATING_CLASS_FIELDNAME_PARENT, serializedDocumentRef, solrInputDocument);
        solrUtils.set(RatingsManager.RATING_CLASS_FIELDNAME_AUTHOR, serializedAuthor, solrInputDocument);
        solrUtils.set(RatingsManager.RATING_CLASS_FIELDNAME_VOTE, vote, solrInputDocument);
        solrUtils.set(RatingsManager.RATING_CLASS_FIELDNAME_DATE, new Date(), solrInputDocument);

        try {
            getSolrClient().add(solrInputDocument);
            getSolrClient().commit();
        } catch (SolrServerException | IOException | SolrException e) {
            throw new RatingsException("Error while inserting a new rating", e);
        }
        return getRating(globalId);
    }

    @Override
    public boolean removeRating(Rating rating) throws RatingsException
    {
        try {
            UpdateResponse updateResponse = getSolrClient().deleteById(rating.getGlobalRatingId());
            return (updateResponse.getStatus() > 0 && updateResponse.getStatus() < 300);
        } catch (SolrServerException | IOException | SolrException e) {
            throw new RatingsException("Error while removing a rating", e);
        }
    }
}
