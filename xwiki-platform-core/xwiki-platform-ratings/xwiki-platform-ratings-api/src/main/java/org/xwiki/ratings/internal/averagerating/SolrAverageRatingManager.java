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
package org.xwiki.ratings.internal.averagerating;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

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
import org.xwiki.model.reference.EntityReference;
import org.xwiki.ratings.AverageRating;
import org.xwiki.ratings.RatingsException;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.SolrUtils;

/**
 * Implementation of {@link AverageRatingManager} that stores the average rating in Solr.
 *
 * @version $Id$
 * @since 12.9RC1
 */
@Component
@Named("solr")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class SolrAverageRatingManager extends AbstractAverageRatingManager
{
    private static final int BULK_OPERATIONS_BATCH_SIZE = 100;

    private static final String FILTER_REFERENCE_OR_PARENTS = "filter(%s:%s) AND (filter(%s:%s) OR filter(%s:%s))";

    @Inject
    private SolrUtils solrUtils;

    @Inject
    private Solr solr;

    private SolrClient getAverageRatingSolrClient() throws SolrException
    {
        return this.solr.getClient(AverageRatingSolrCoreInitializer.DEFAULT_AVERAGE_RATING_SOLR_CORE);
    }

    private SolrQuery.ORDER getOrder(boolean asc)
    {
        return (asc) ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc;
    }

    private SolrInputDocument getInputDocumentFromAverageRating(AverageRating averageRating)
    {
        SolrInputDocument result = new SolrInputDocument();
        solrUtils.setId(averageRating.getId(), result);
        solrUtils.setString(AverageRatingQueryField.ENTITY_REFERENCE.getFieldName(),
            averageRating.getReference(), EntityReference.class, result);
        EntityReference parent = averageRating.getReference();
        List<EntityReference> parentReferences = new ArrayList<>();
        while (parent != null) {
            parentReferences.add(parent);
            parent = parent.getParent();
        }
        solrUtils.setString(AverageRatingQueryField.PARENTS.getFieldName(), parentReferences, EntityReference.class,
            result);
        solrUtils.set(AverageRatingQueryField.UPDATED_AT.getFieldName(), averageRating.getUpdatedAt(), result);
        solrUtils.set(AverageRatingQueryField.TOTAL_VOTE.getFieldName(),
            averageRating.getNbVotes(), result);
        solrUtils.set(AverageRatingQueryField.SCALE.getFieldName(), averageRating.getScaleUpperBound(), result);
        solrUtils.set(AverageRatingQueryField.MANAGER_ID.getFieldName(), averageRating.getManagerId(), result);
        solrUtils.set(AverageRatingQueryField.AVERAGE_VOTE.getFieldName(), averageRating.getAverageVote(), result);
        return result;
    }

    @Override
    public AverageRating getAverageRating(EntityReference entityReference) throws RatingsException
    {
        SolrQuery solrQuery = new SolrQuery()
            .addFilterQuery(String.format("filter(%s:%s) AND filter(%s:%s)",
                AverageRatingQueryField.MANAGER_ID.getFieldName(),
                this.solrUtils.toCompleteFilterQueryString(this.getIdentifier()),
                AverageRatingQueryField.ENTITY_REFERENCE.getFieldName(),
                this.solrUtils.toCompleteFilterQueryString(entityReference, EntityReference.class)))
            .setStart(0)
            .setRows(1)
            .setSort(AverageRatingQueryField.UPDATED_AT.getFieldName(), this.getOrder(true));

        try {
            QueryResponse query = this.getAverageRatingSolrClient().query(solrQuery);
            AverageRating result;
            if (!query.getResults().isEmpty()) {
                SolrDocument solrDocument = query.getResults().get(0);

                result = new DefaultAverageRating(solrUtils.getId(solrDocument))
                    .setManagerId(solrUtils.get(AverageRatingQueryField.MANAGER_ID.getFieldName(), solrDocument))
                    .setAverageVote(solrUtils.get(AverageRatingQueryField.AVERAGE_VOTE.getFieldName(), solrDocument))
                    .setReference(entityReference)
                    .setTotalVote(solrUtils.get(AverageRatingQueryField.TOTAL_VOTE.getFieldName(),
                        solrDocument))
                    .setScaleUpperBound(solrUtils.get(AverageRatingQueryField.SCALE.getFieldName(), solrDocument))
                    .setUpdatedAt(solrUtils.get(AverageRatingQueryField.UPDATED_AT.getFieldName(), solrDocument));
            } else {
                return this.createAverageRating(entityReference, UUID.randomUUID().toString());
            }
            return result;
        } catch (SolrServerException | IOException | SolrException e) {
            throw new RatingsException("Error while trying to get average rating value.", e);
        }
    }

    @Override
    public long removeAverageRatings(EntityReference entityReference) throws RatingsException
    {
        String escapedEntityReference =
            this.solrUtils.toCompleteFilterQueryString(entityReference, EntityReference.class);
        String filterQuery = String.format(FILTER_REFERENCE_OR_PARENTS,
            AverageRatingQueryField.MANAGER_ID.getFieldName(),
            this.solrUtils.toCompleteFilterQueryString(this.getIdentifier()),
            AverageRatingQueryField.ENTITY_REFERENCE.getFieldName(), escapedEntityReference,
            AverageRatingQueryField.PARENTS.getFieldName(), escapedEntityReference);
        SolrQuery solrQuery = new SolrQuery()
            .addFilterQuery(filterQuery)
            .setStart(0)
            .setRows(0);

        long result;
        try {
            QueryResponse query = this.getAverageRatingSolrClient().query(solrQuery);
            result = query.getResults().getNumFound();
            this.getAverageRatingSolrClient().deleteByQuery(filterQuery);
            this.getAverageRatingSolrClient().commit();
        } catch (SolrServerException | IOException | SolrException e) {
            throw new RatingsException("Error while trying to remove ratings", e);
        }
        return result;
    }

    @Override
    public long moveAverageRatings(EntityReference oldReference, EntityReference newReference)
        throws RatingsException
    {
        String escapedEntityReference = this.solrUtils.toCompleteFilterQueryString(oldReference, EntityReference.class);
        String filterQuery = String.format(FILTER_REFERENCE_OR_PARENTS,
            AverageRatingQueryField.MANAGER_ID.getFieldName(),
            this.solrUtils.toCompleteFilterQueryString(this.getIdentifier()),
            AverageRatingQueryField.ENTITY_REFERENCE.getFieldName(), escapedEntityReference,
            AverageRatingQueryField.PARENTS.getFieldName(), escapedEntityReference);
        int offset = 0;
        SolrDocumentList rawRatings;
        long result = 0;
        do {
            SolrQuery solrQuery = new SolrQuery()
                .addFilterQuery(filterQuery)
                .setStart(offset)
                .setRows(BULK_OPERATIONS_BATCH_SIZE)
                .setSort(AverageRatingQueryField.UPDATED_AT.getFieldName(), this.getOrder(true));

            try {
                QueryResponse queryResponse = this.getAverageRatingSolrClient().query(solrQuery);
                rawRatings = queryResponse.getResults();

                offset += BULK_OPERATIONS_BATCH_SIZE;
                for (SolrDocument rawRating : rawRatings) {
                    SolrInputDocument solrInputDocument = new SolrInputDocument();
                    this.solrUtils.setId(this.solrUtils.getId(rawRating), solrInputDocument);

                    EntityReference ratingReference = this.solrUtils.get(
                        RatingsManager.RatingQueryField.ENTITY_REFERENCE.getFieldName(), rawRating,
                        EntityReference.class);

                    if (oldReference.equals(ratingReference)) {
                        this.solrUtils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET,
                            RatingsManager.RatingQueryField.ENTITY_REFERENCE.getFieldName(), newReference,
                            EntityReference.class, solrInputDocument);
                    }

                    Collection<EntityReference> parentReferences = this.solrUtils
                        .getCollection(RatingsManager.RatingQueryField.PARENTS_REFERENCE.getFieldName(), rawRating,
                            EntityReference.class);
                    if (parentReferences.contains(oldReference)) {
                        this.solrUtils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_REMOVE,
                            RatingsManager.RatingQueryField.PARENTS_REFERENCE
                                .getFieldName(), oldReference, EntityReference.class, solrInputDocument);
                        this.solrUtils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_ADD,
                            RatingsManager.RatingQueryField.PARENTS_REFERENCE
                                .getFieldName(), newReference, EntityReference.class, solrInputDocument);
                    }
                    this.getAverageRatingSolrClient().add(solrInputDocument);
                    result++;

                }
                if (!rawRatings.isEmpty()) {
                    this.getAverageRatingSolrClient().commit();
                }
            } catch (SolrException | IOException | SolrServerException e) {
                throw new RatingsException("Error while trying to update average rating reference", e);
            }
        } while (!rawRatings.isEmpty());
        return result;
    }

    @Override
    protected void saveAverageRating(AverageRating averageRating) throws RatingsException
    {
        try {
            this.getAverageRatingSolrClient().add(this.getInputDocumentFromAverageRating(averageRating));
            this.getAverageRatingSolrClient().commit();
        } catch (SolrException | SolrServerException | IOException e) {
            throw new RatingsException(String.format("Error when trying to save average rating [%s]", averageRating),
                e);
        }
    }
}
