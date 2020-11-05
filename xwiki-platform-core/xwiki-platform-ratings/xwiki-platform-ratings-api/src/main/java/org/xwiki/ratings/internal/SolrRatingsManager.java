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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.ratings.AverageRating;
import org.xwiki.ratings.Rating;
import org.xwiki.ratings.RatingsConfiguration;
import org.xwiki.ratings.RatingsException;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.ratings.events.CreatedRatingEvent;
import org.xwiki.ratings.events.DeletedRatingEvent;
import org.xwiki.ratings.events.UpdatedRatingEvent;
import org.xwiki.ratings.internal.averagerating.AverageRatingManager;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.SolrUtils;
import org.xwiki.user.UserReference;

/**
 * Default implementation of {@link RatingsManager} which stores Rating and AverageRating in Solr.
 *
 * @version $Id$
 * @since 12.9RC1
 */
@Component
@Named("solr")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class SolrRatingsManager implements RatingsManager
{
    private static final int AVERAGE_COMPUTATION_BATCH_SIZE = 100;

    private static final String AVERAGE_RATING_NOT_ENABLED_ERROR_MESSAGE =
        "This rating manager is not configured to store average rating.";

    @Inject
    private SolrUtils solrUtils;

    @Inject
    private Solr solr;

    @Inject
    private ObservationManager observationManager;

    @Inject
    @Named("context")
    private ComponentManager contextComponentManager;

    private AverageRatingManager averageRatingManager;

    private RatingsConfiguration ratingsConfiguration;

    private String identifier;

    /**
     * Retrieve the solr client for storing ratings based on the configuration.
     * If the configuration specifies to use a dedicated core (see {@link RatingsConfiguration#hasDedicatedCore()}),
     * then it will use a client based on the current manager identifier, else it will use the default solr core.
     *
     * @return the right solr client for storing ratings.
     * @throws SolrException in case of problem to retrieve the solr client.
     */
    private SolrClient getRatingSolrClient() throws SolrException
    {
        if (this.getRatingConfiguration().hasDedicatedCore()) {
            return this.solr.getClient(this.getIdentifier());
        } else {
            return this.solr.getClient(RatingSolrCoreInitializer.DEFAULT_RATINGS_SOLR_CORE);
        }
    }

    private AverageRatingManager getAverageRatingManager() throws RatingsException
    {
        if (this.averageRatingManager == null) {
            String averageRatingManagerHint = this.getRatingConfiguration().getAverageRatingStorageHint();
            try {
                this.averageRatingManager = this.contextComponentManager
                    .getInstance(AverageRatingManager.class, averageRatingManagerHint);
                this.averageRatingManager.setRatingsManager(this);
            } catch (ComponentLookupException e) {
                throw new RatingsException(String.format("Cannot instantiate AverageRatingManager with hint [%s]",
                    averageRatingManagerHint), e);
            }
        }
        return this.averageRatingManager;
    }

    @Override
    public String getIdentifier()
    {
        return this.identifier;
    }

    @Override
    public void setIdentifer(String identifier)
    {
        this.identifier = identifier;
    }

    @Override
    public int getScale()
    {
        return this.getRatingConfiguration().getScaleUpperBound();
    }

    @Override
    public void setRatingConfiguration(RatingsConfiguration configuration)
    {
        this.ratingsConfiguration = configuration;
    }

    @Override
    public RatingsConfiguration getRatingConfiguration()
    {
        return this.ratingsConfiguration;
    }

    private SolrQuery.ORDER getOrder(boolean asc)
    {
        return (asc) ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc;
    }

    private Rating getRatingFromSolrDocument(SolrDocument document)
    {
        String ratingId = this.solrUtils.getId(document);
        String managerId = this.solrUtils.get(RatingQueryField.MANAGER_ID.getFieldName(), document);
        EntityReference entityReference = this.solrUtils.get(RatingQueryField.ENTITY_REFERENCE.getFieldName(),
            document, EntityReference.class);
        UserReference userReference = this.solrUtils.get(RatingQueryField.USER_REFERENCE.getFieldName(), document,
            UserReference.class);
        int vote = this.solrUtils.get(RatingQueryField.VOTE.getFieldName(), document);
        Date createdAt = this.solrUtils.get(RatingQueryField.CREATED_DATE.getFieldName(), document);
        Date updatedAt = this.solrUtils.get(RatingQueryField.UPDATED_DATE.getFieldName(), document);
        int scale = this.solrUtils.get(RatingQueryField.SCALE.getFieldName(), document);

        return new DefaultRating(ratingId)
            .setReference(entityReference)
            .setAuthor(userReference)
            .setVote(vote)
            .setScaleUpperBound(scale)
            .setCreatedAt(createdAt)
            .setUpdatedAt(updatedAt)
            .setManagerId(managerId);
    }

    private List<Rating> getRatingsFromQueryResult(SolrDocumentList documents)
    {
        if (documents != null) {
            return documents.stream().map(this::getRatingFromSolrDocument).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private String mapToQuery(Map<RatingQueryField, Object> originalParameters)
    {
        Map<RatingQueryField, Object> queryParameters = new LinkedHashMap<>(originalParameters);
        queryParameters.put(RatingQueryField.MANAGER_ID, this.getIdentifier());

        StringBuilder result = new StringBuilder();
        Iterator<Map.Entry<RatingQueryField, Object>> iterator = queryParameters.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<RatingQueryField, Object> queryParameter = iterator.next();
            result.append("filter(");
            result.append(queryParameter.getKey().getFieldName());
            result.append(":");

            Object value = queryParameter.getValue();
            if (value instanceof String || value instanceof Date) {
                result.append(solrUtils.toFilterQueryString(value));
            } else if (value instanceof UserReference) {
                result.append(solrUtils.toFilterQueryString(value, UserReference.class));
            } else if (value instanceof EntityReference) {
                result.append(solrUtils.toFilterQueryString(value, EntityReference.class));
            } else if (value != null) {
                result.append(value);
            }
            result.append(")");
            if (iterator.hasNext()) {
                result.append(" AND ");
            }
        }

        return result.toString();
    }

    private SolrInputDocument getInputDocumentFromRating(Rating rating)
    {
        SolrInputDocument result = new SolrInputDocument();
        solrUtils.setId(rating.getId(), result);
        solrUtils.setString(RatingQueryField.ENTITY_REFERENCE.getFieldName(), rating.getReference(),
            EntityReference.class, result);
        solrUtils.set(RatingQueryField.CREATED_DATE.getFieldName(), rating.getCreatedAt(), result);
        solrUtils.set(RatingQueryField.UPDATED_DATE.getFieldName(), rating.getUpdatedAt(), result);
        solrUtils.setString(RatingQueryField.USER_REFERENCE.getFieldName(), rating.getAuthor(),
            UserReference.class, result);
        // set Parents to be able to easily request on them
        EntityReference parentReference = rating.getReference().getParent();
        List<EntityReference> parentReferenceList = new ArrayList<>();
        while (parentReference != null) {
            parentReferenceList.add(parentReference);
            parentReference = parentReference.getParent();
        }
        this.solrUtils.setString(RatingsManager.RatingQueryField.PARENTS_REFERENCE.getFieldName(), parentReferenceList,
            EntityReference.class, result);
        solrUtils.set(RatingQueryField.SCALE.getFieldName(), rating.getScaleUpperBound(), result);
        solrUtils.set(RatingQueryField.MANAGER_ID.getFieldName(), rating.getManagerId(), result);
        solrUtils.set(RatingQueryField.VOTE.getFieldName(), rating.getVote(), result);
        return result;
    }

    private Optional<Rating> retrieveExistingRating(EntityReference reference, UserReference voter)
        throws RatingsException
    {
        Map<RatingQueryField, Object> queryMap = new LinkedHashMap<>();
        queryMap.put(RatingQueryField.ENTITY_REFERENCE, reference);
        queryMap.put(RatingQueryField.USER_REFERENCE, voter);

        List<Rating> ratings = this.getRatings(queryMap, 0, 1, RatingQueryField.CREATED_DATE, true);
        if (ratings.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(ratings.get(0));
        }
    }

    @Override
    public Rating saveRating(EntityReference reference, UserReference user, int vote)
        throws RatingsException
    {
        // If the vote is outside the scope of the scale, we throw an exception immediately.
        if (vote < 0 || vote > this.getScale()) {
            throw new RatingsException(String.format("The vote [%s] is out of scale [%s] for [%s] rating manager.",
                vote, this.getScale(), this.getIdentifier()));
        }

        // Check if a vote for the same entity by the same user and on the same manager already exists.
        Optional<Rating> existingRating = this.retrieveExistingRating(reference, user);

        boolean storeAverage = this.getRatingConfiguration().isAverageStored();
        Event event = null;
        Rating result = null;
        Rating oldRating = null;

        // It's the first vote for the tuple entity, user, manager.
        if (!existingRating.isPresent()) {

            // We only store the vote if it's not 0 or if the configuration allows to store 0
            if (vote != 0 || this.getRatingConfiguration().isZeroStored()) {
                result = new DefaultRating(UUID.randomUUID().toString())
                    .setManagerId(this.getIdentifier())
                    .setReference(reference)
                    .setCreatedAt(new Date())
                    .setUpdatedAt(new Date())
                    .setVote(vote)
                    .setScaleUpperBound(this.getScale())
                    .setAuthor(user);

                // it's a vote creation
                event = new CreatedRatingEvent(result);
            }

        // There was already a vote with the same information
        } else {
            oldRating = existingRating.get();

            // If the vote is not 0 or if we store zero, we just modify the existing vote
            if (vote != 0) {
                result = new DefaultRating(oldRating)
                    .setUpdatedAt(new Date())
                    .setVote(vote);

                // It's an update of a vote
                event = new UpdatedRatingEvent(result, oldRating.getVote());
            // Else we remove it.
            } else if (this.ratingsConfiguration.isZeroStored()) {
                this.removeRating(oldRating.getId());
            }
        }

        // If there's a vote to store (all cases except if the vote is 0 and we don't store it)
        if (result != null) {
            SolrInputDocument solrInputDocument = this.getInputDocumentFromRating(result);
            try {
                // Store the new document in Solr
                this.getRatingSolrClient().add(solrInputDocument);
                this.getRatingSolrClient().commit();

                // Send the appropriate notification
                this.observationManager.notify(event, this.getIdentifier(), result);

                // If we store the average, we also compute the new informations for it.
                if (storeAverage) {
                    if (oldRating == null) {
                        this.getAverageRatingManager().addVote(reference, vote);
                    } else {
                        this.getAverageRatingManager().updateVote(reference, oldRating.getVote(), vote);
                    }
                }
            } catch (SolrServerException | IOException | SolrException e) {
                throw new RatingsException(
                    String.format("Error when storing rating information for entity [%s] with user [%s].",
                        reference, user), e);
            }
        }
        return result;
    }

    @Override
    public List<Rating> getRatings(Map<RatingQueryField, Object> queryParameters, int offset, int limit,
        RatingQueryField orderBy, boolean asc) throws RatingsException
    {
        SolrQuery solrQuery = new SolrQuery()
            .addFilterQuery(this.mapToQuery(queryParameters))
            .setStart(offset)
            .setRows(limit)
            .setSort(orderBy.getFieldName(), this.getOrder(asc));

        try {
            QueryResponse query = this.getRatingSolrClient().query(solrQuery);
            return this.getRatingsFromQueryResult(query.getResults());
        } catch (SolrServerException | IOException | SolrException e) {
            throw new RatingsException("Error while trying to get ratings", e);
        }
    }

    @Override
    public long countRatings(Map<RatingQueryField, Object> queryParameters) throws RatingsException
    {
        SolrQuery solrQuery = new SolrQuery()
            .addFilterQuery(this.mapToQuery(queryParameters))
            .setStart(0)
            .setRows(0);

        try {
            QueryResponse query = this.getRatingSolrClient().query(solrQuery);
            return query.getResults().getNumFound();
        } catch (SolrServerException | IOException | SolrException e) {
            throw new RatingsException("Error while trying to get count of ratings", e);
        }
    }

    @Override
    public boolean removeRating(String ratingIdentifier) throws RatingsException
    {
        Map<RatingQueryField, Object> queryMap = Collections
            .singletonMap(RatingQueryField.IDENTIFIER, ratingIdentifier);

        List<Rating> ratings = this.getRatings(queryMap, 0, 1, RatingQueryField.CREATED_DATE, true);
        if (!ratings.isEmpty()) {
            try {
                this.getRatingSolrClient().deleteById(ratingIdentifier);
                this.getRatingSolrClient().commit();
                Rating rating = ratings.get(0);
                this.observationManager.notify(new DeletedRatingEvent(rating), this.getIdentifier(), rating);
                if (this.getRatingConfiguration().isAverageStored()) {
                    this.getAverageRatingManager().removeVote(rating.getReference(), rating.getVote());
                }
                return true;
            } catch (SolrServerException | IOException | SolrException e) {
                throw new RatingsException("Error while removing rating.", e);
            }
        } else {
            return false;
        }
    }

    @Override
    public long removeRatings(EntityReference entityReference) throws RatingsException
    {
        String escapedEntityReference = this.solrUtils.toFilterQueryString(entityReference, EntityReference.class);
        String filterQuery = String.format("filter(%s:%s) AND (filter(%s:%s) OR filter(%s:%s))",
            RatingQueryField.MANAGER_ID.getFieldName(), solrUtils.toFilterQueryString(this.getIdentifier()),
            RatingQueryField.ENTITY_REFERENCE.getFieldName(), escapedEntityReference,
            RatingQueryField.PARENTS_REFERENCE.getFieldName(), escapedEntityReference);
        SolrQuery solrQuery = new SolrQuery()
            .addFilterQuery(filterQuery)
            .setStart(0)
            .setRows(0);

        long result;
        try {
            QueryResponse query = this.getRatingSolrClient().query(solrQuery);
            result = query.getResults().getNumFound();
            this.getRatingSolrClient().deleteByQuery(filterQuery);
            this.getRatingSolrClient().commit();
            if (this.getRatingConfiguration().isAverageStored()) {
                this.getAverageRatingManager().removeAverageRatings(entityReference);
            }
        } catch (SolrServerException | IOException | SolrException e) {
            throw new RatingsException("Error while trying to remove ratings", e);
        }
        return result;
    }

    @Override
    public AverageRating getAverageRating(EntityReference entityReference) throws RatingsException
    {
        if (this.getRatingConfiguration().isAverageStored()) {
            return this.getAverageRatingManager().getAverageRating(entityReference);
        } else {
            throw new RatingsException(AVERAGE_RATING_NOT_ENABLED_ERROR_MESSAGE);
        }
    }

    @Override
    public void saveRating(Rating rating) throws RatingsException
    {
        SolrInputDocument solrInputDocument = this.getInputDocumentFromRating(rating);
        try {
            this.getRatingSolrClient().add(solrInputDocument);
            this.getRatingSolrClient().commit();
        } catch (SolrServerException | IOException | SolrException e) {
            throw new RatingsException(String.format("Error when saving the given rating: [%s]", rating), e);
        }
    }

    @Override
    public AverageRating recomputeAverageRating(EntityReference entityReference) throws RatingsException
    {
        if (this.getRatingConfiguration().isAverageStored()) {
            Map<RatingQueryField, Object> queryMap = new LinkedHashMap<>();
            queryMap.put(RatingQueryField.ENTITY_REFERENCE, entityReference);

            Long sumOfVotes = 0L;
            int numberOfVotes = 0;
            List<Rating> ratings;
            int offsetIndex = 0;
            do {
                ratings = this.getRatings(queryMap, offsetIndex, AVERAGE_COMPUTATION_BATCH_SIZE,
                    RatingQueryField.CREATED_DATE, true);
                sumOfVotes += ratings.stream().map(Rating::getVote).map(Long::valueOf).reduce(0L, Long::sum);
                numberOfVotes += ratings.size();
                offsetIndex += AVERAGE_COMPUTATION_BATCH_SIZE;
            } while (!ratings.isEmpty());

            float newAverage = Float.valueOf(sumOfVotes) / numberOfVotes;
            return this.getAverageRatingManager().resetAverageRating(entityReference, newAverage, numberOfVotes);
        } else {
            throw new RatingsException(AVERAGE_RATING_NOT_ENABLED_ERROR_MESSAGE);
        }
    }
}
