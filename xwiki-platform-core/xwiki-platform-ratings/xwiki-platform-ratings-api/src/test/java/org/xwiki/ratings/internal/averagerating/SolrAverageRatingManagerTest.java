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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.ratings.AverageRating;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.ratings.internal.averagerating.AverageRatingManager.AverageRatingQueryField;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrUtils;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SolrAverageRatingManager}.
 *
 * @version $Id$
 * @since 12.9RC1
 */
@ComponentTest
class SolrAverageRatingManagerTest
{
    @InjectMockComponents
    private SolrAverageRatingManager averageRatingManager;

    @MockComponent
    private SolrUtils solrUtils;

    @MockComponent
    private Solr solr;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    private RatingsManager ratingsManager;

    @Mock
    private SolrClient solrClient;

    @Mock
    private SolrDocumentList documentList;

    @BeforeEach
    void setup()
    {
        when(this.solrUtils.toCompleteFilterQueryString(any()))
            .then(invocationOnMock -> invocationOnMock.getArgument(0).toString().replaceAll(":", "\\\\:"));
        when(this.solrUtils.toCompleteFilterQueryString(any(), any()))
            .then(invocationOnMock -> invocationOnMock.getArgument(0).toString().replaceAll(":", "\\\\:"));
        when(this.solrUtils.getId(any()))
            .then(invocationOnMock -> ((SolrDocument) invocationOnMock.getArgument(0)).get("id"));
        when(this.solrUtils.get(any(), any()))
            .then(invocationOnMock ->
                ((SolrDocument) invocationOnMock.getArgument(1)).get(invocationOnMock.getArgument(0)));
        doAnswer(invocationOnMock -> {
            String fieldName = invocationOnMock.getArgument(0);
            Object fieldValue = invocationOnMock.getArgument(1);
            SolrInputDocument inputDocument = invocationOnMock.getArgument(2);
            inputDocument.setField(fieldName, fieldValue);
            return null;
        }).when(this.solrUtils).set(any(), any(Object.class), any());
        doAnswer(invocationOnMock -> {
            Object fieldValue = invocationOnMock.getArgument(0);
            SolrInputDocument inputDocument = invocationOnMock.getArgument(1);
            inputDocument.setField("id", fieldValue);
            return null;
        }).when(this.solrUtils).setId(any(), any());
        doAnswer(invocationOnMock -> {
            String fieldName = invocationOnMock.getArgument(0);
            Object fieldValue = invocationOnMock.getArgument(1);
            Type type = invocationOnMock.getArgument(2);
            SolrInputDocument inputDocument = invocationOnMock.getArgument(3);
            inputDocument.setField(fieldName, fieldValue);
            return null;
        }).when(this.solrUtils).setString(any(), any(Object.class), any(), any());
        this.averageRatingManager.setRatingsManager(this.ratingsManager);
    }

    private QueryResponse prepareSolrClientQueryWhenStatement(SolrClient solrClient, SolrQuery expectedQuery)
        throws Exception
    {
        QueryResponse response = mock(QueryResponse.class);
        when(solrClient.query(any())).then(invocationOnMock -> {
            SolrQuery givenQuery = invocationOnMock.getArgument(0);
            assertEquals(expectedQuery.getQuery(), givenQuery.getQuery());
            assertArrayEquals(expectedQuery.getFilterQueries(), givenQuery.getFilterQueries());
            assertEquals(expectedQuery.getRows(), givenQuery.getRows());
            assertEquals(expectedQuery.getStart(), givenQuery.getStart());
            assertEquals(expectedQuery.getSorts(), givenQuery.getSorts());
            return response;
        });
        return response;
    }

    @Test
    void getAverageRatingExisting() throws Exception
    {
        when(this.solr.getClient(AverageRatingSolrCoreInitializer.DEFAULT_AVERAGE_RATING_SOLR_CORE))
            .thenReturn(this.solrClient);

        String managerId = "averageId2";
        when(this.ratingsManager.getIdentifier()).thenReturn(managerId);
        EntityReference reference = mock(EntityReference.class);
        when(reference.toString()).thenReturn("xwiki:Something");

        String filterQuery = "filter(managerId:averageId2) AND filter(reference:xwiki\\:Something)";
        SolrQuery solrQuery = new SolrQuery().addFilterQuery(filterQuery)
            .setStart(0)
            .setRows(1)
            .setSort("updatedAt", SolrQuery.ORDER.asc);
        QueryResponse response = prepareSolrClientQueryWhenStatement(this.solrClient, solrQuery);
        when(response.getResults()).thenReturn(this.documentList);
        when(this.documentList.isEmpty()).thenReturn(false);

        Map<String, Object> fieldMap = new HashMap<>();
        fieldMap.put("id", "average1");
        fieldMap.put(AverageRatingQueryField.AVERAGE_VOTE.getFieldName(), 2.341f);
        fieldMap.put(AverageRatingQueryField.TOTAL_VOTE.getFieldName(), 242);
        fieldMap.put(AverageRatingQueryField.ENTITY_REFERENCE.getFieldName(), "xwiki:Something");
        fieldMap.put(AverageRatingQueryField.MANAGER_ID.getFieldName(), managerId);
        fieldMap.put(AverageRatingQueryField.UPDATED_AT.getFieldName(), new Date(42));
        fieldMap.put(AverageRatingQueryField.SCALE.getFieldName(), 12);
        SolrDocument solrDocument = new SolrDocument(fieldMap);
        when(this.solrUtils.get(AverageRatingQueryField.ENTITY_REFERENCE.getFieldName(), solrDocument,
            EntityReference.class)).thenReturn(reference);
        when(this.documentList.get(0)).thenReturn(solrDocument);

        AverageRating averageRating = this.averageRatingManager.getAverageRating(reference);

        AverageRating expectedRating = new DefaultAverageRating("average1")
            .setAverageVote(2.341f)
            .setTotalVote(242)
            .setUpdatedAt(new Date(42))
            .setManagerId(managerId)
            .setScaleUpperBound(12)
            .setReference(reference);
        assertEquals(expectedRating, averageRating);
    }

    @Test
    void getAverageRatingNotExisting() throws Exception
    {
        when(this.solr.getClient(AverageRatingSolrCoreInitializer.DEFAULT_AVERAGE_RATING_SOLR_CORE))
            .thenReturn(this.solrClient);

        String managerId = "averageId1";
        when(this.ratingsManager.getIdentifier()).thenReturn(managerId);
        when(this.ratingsManager.getScale()).thenReturn(7);
        EntityReference reference = mock(EntityReference.class);
        when(reference.toString()).thenReturn("xwiki:FooBarBar");

        String filterQuery = "filter(managerId:averageId1) AND filter(reference:xwiki\\:FooBarBar)";
        SolrQuery solrQuery = new SolrQuery().addFilterQuery(filterQuery)
            .setStart(0)
            .setRows(1)
            .setSort("updatedAt", SolrQuery.ORDER.asc);
        QueryResponse response = prepareSolrClientQueryWhenStatement(this.solrClient, solrQuery);
        when(response.getResults()).thenReturn(this.documentList);
        when(this.documentList.isEmpty()).thenReturn(true);
        AverageRating averageRating = this.averageRatingManager.getAverageRating(reference);

        AverageRating expectedRating = new DefaultAverageRating(averageRating.getId())
            .setAverageVote(0)
            .setTotalVote(0)
            .setUpdatedAt(averageRating.getUpdatedAt())
            .setManagerId(managerId)
            .setScaleUpperBound(7)
            .setReference(reference);
        assertEquals(expectedRating, averageRating);
    }

    @Test
    void saveAverageRating() throws Exception
    {
        String managerId = "myManager";
        int scale = 12;
        EntityReference reference = mock(EntityReference.class);
        when(reference.toString()).thenReturn("wiki:foobar");

        SolrClient averageSolrClient = mock(SolrClient.class);
        when(this.solr.getClient(AverageRatingSolrCoreInitializer.DEFAULT_AVERAGE_RATING_SOLR_CORE))
            .thenReturn(averageSolrClient);

        String filterQuery = "filter(managerId:saveRank3) AND filter(reference:wiki\\:foobar)";
        SolrQuery averageQuery = new SolrQuery().addFilterQuery(filterQuery)
            .setStart(0)
            .setRows(1)
            .setSort("updatedAt", SolrQuery.ORDER.asc);
        QueryResponse averageResponse = prepareSolrClientQueryWhenStatement(averageSolrClient, averageQuery);
        SolrDocumentList averageDocumentList = mock(SolrDocumentList.class);
        when(averageResponse.getResults()).thenReturn(averageDocumentList);
        when(averageDocumentList.isEmpty()).thenReturn(false);

        Map<String, Object> fieldMap = new HashMap<>();
        fieldMap.put("id", "average1");
        fieldMap.put(AverageRatingQueryField.AVERAGE_VOTE.getFieldName(), 6.0f);
        fieldMap.put(AverageRatingQueryField.TOTAL_VOTE.getFieldName(), 2);
        fieldMap.put(AverageRatingQueryField.ENTITY_REFERENCE.getFieldName(), "wiki:foobar");
        fieldMap.put(AverageRatingQueryField.MANAGER_ID.getFieldName(), managerId);
        fieldMap.put(AverageRatingQueryField.UPDATED_AT.getFieldName(), new Date(42));
        fieldMap.put(AverageRatingQueryField.SCALE.getFieldName(), scale);
        SolrDocument solrDocument = new SolrDocument(fieldMap);
        when(this.solrUtils.get(AverageRatingQueryField.ENTITY_REFERENCE.getFieldName(), solrDocument,
            EntityReference.class)).thenReturn(reference);
        when(averageDocumentList.get(0)).thenReturn(solrDocument);

        DefaultAverageRating expectedModifiedAverage = new DefaultAverageRating("average1")
            .setAverageVote(5.5f) // ((6 * 2) - (3 + 2)) / 2 -> 11 / 2 -> 5.5
            .setTotalVote(2)
            .setManagerId(managerId)
            .setReference(reference)
            .setScaleUpperBound(scale);
        SolrInputDocument expectedAverageInputDocument = new SolrInputDocument();
        expectedAverageInputDocument.setField("id", "average1");
        expectedAverageInputDocument.setField(AverageRatingQueryField.ENTITY_REFERENCE.getFieldName(), "wiki:foobar");
        expectedAverageInputDocument.setField(AverageRatingQueryField.UPDATED_AT.getFieldName(), new Date(0));
        expectedAverageInputDocument.setField(AverageRatingQueryField.TOTAL_VOTE.getFieldName(), 2L);
        expectedAverageInputDocument.setField(AverageRatingQueryField.SCALE.getFieldName(), scale);
        expectedAverageInputDocument.setField(AverageRatingQueryField.MANAGER_ID.getFieldName(), managerId);
        expectedAverageInputDocument.setField(AverageRatingQueryField.AVERAGE_VOTE.getFieldName(), 5.5);

        when(averageSolrClient.add(any(SolrInputDocument.class))).then(invocationOnMock -> {
            SolrInputDocument obtainedInputDocument = invocationOnMock.getArgument(0);
            Date updatedAt = (Date) obtainedInputDocument.getFieldValue("updatedAt");
            expectedAverageInputDocument.setField(AverageRatingQueryField.UPDATED_AT.getFieldName(), updatedAt);
            expectedModifiedAverage.setUpdatedAt(updatedAt);
            // We rely on the toString method since there's no proper equals method
            assertEquals(expectedAverageInputDocument.toString(), obtainedInputDocument.toString());
            return null;
        });

        this.averageRatingManager.saveAverageRating(new DefaultAverageRating("average1")
            .setReference(reference)
            .setAverageVote(5.5f)
            .setManagerId(managerId)
            .setScaleUpperBound(scale)
            .setTotalVote(2)
            .setUpdatedAt(new Date(42)));

        verify(averageSolrClient).add(any(SolrInputDocument.class));
        verify(averageSolrClient).commit();
    }

    @Test
    void moveAverageRatings() throws Exception
    {
        String managerId = "moveRatingsManagerId";
        when(this.ratingsManager.getIdentifier()).thenReturn(managerId);
        when(this.solr.getClient(AverageRatingSolrCoreInitializer.DEFAULT_AVERAGE_RATING_SOLR_CORE))
            .thenReturn(this.solrClient);

        EntityReference oldReference = mock(EntityReference.class);
        EntityReference newReference = mock(EntityReference.class);
        when(oldReference.toString()).thenReturn("document:My.Old.Doc");

        String filterQuery = String.format("filter(%s:%s) AND (filter(%s:%s) OR filter(%s:%s))",
            AverageRatingQueryField.MANAGER_ID.getFieldName(), managerId,
            AverageRatingQueryField.ENTITY_REFERENCE.getFieldName(), "document\\:My.Old.Doc",
            AverageRatingQueryField.PARENTS.getFieldName(), "document\\:My.Old.Doc");

        SolrQuery expectedQuery1 = new SolrQuery()
            .addFilterQuery(filterQuery)
            .setRows(100)
            .setStart(0)
            .setSort(AverageRatingQueryField.UPDATED_AT.getFieldName(), SolrQuery.ORDER.asc);

        SolrDocument rating1 = mock(SolrDocument.class);
        SolrDocument rating2 = mock(SolrDocument.class);
        SolrDocument rating3 = mock(SolrDocument.class);
        SolrDocument rating4 = mock(SolrDocument.class);

        // rating1 have the appropriate reference, but not the appropriate parent
        when(rating1.get("id")).thenReturn("rating1");
        when(this.solrUtils.get(AverageRatingQueryField.ENTITY_REFERENCE.getFieldName(), rating1,
            EntityReference.class))
            .thenReturn(oldReference);
        when(this.solrUtils.getCollection(AverageRatingQueryField.PARENTS.getFieldName(), rating1,
            EntityReference.class))
            .thenReturn(Collections.emptyList());

        // rating2 have not the appropriate reference but the appropriate parent
        when(rating2.get("id")).thenReturn("rating2");
        when(this.solrUtils.get(AverageRatingQueryField.ENTITY_REFERENCE.getFieldName(), rating2, EntityReference.class))
            .thenReturn(mock(EntityReference.class));
        when(this.solrUtils.getCollection(AverageRatingQueryField.PARENTS.getFieldName(), rating2,
            EntityReference.class))
            .thenReturn(Collections.singletonList(oldReference));

        // rating3 have the appropriate reference and also contain the appropriate parent
        when(rating3.get("id")).thenReturn("rating3");
        when(this.solrUtils.get(AverageRatingQueryField.ENTITY_REFERENCE.getFieldName(), rating3, EntityReference.class))
            .thenReturn(oldReference);
        when(this.solrUtils.getCollection(AverageRatingQueryField.PARENTS.getFieldName(), rating3,
            EntityReference.class))
            .thenReturn(Arrays.asList(mock(EntityReference.class), oldReference, mock(EntityReference.class)));

        // rating4 only contain the appropriate parent
        when(rating4.get("id")).thenReturn("rating4");
        when(this.solrUtils.get(AverageRatingQueryField.ENTITY_REFERENCE.getFieldName(), rating4, EntityReference.class))
            .thenReturn(mock(EntityReference.class));
        when(this.solrUtils.getCollection(AverageRatingQueryField.PARENTS.getFieldName(), rating4,
            EntityReference.class))
            .thenReturn(Arrays.asList(mock(EntityReference.class), mock(EntityReference.class), oldReference));

        when(this.documentList.iterator())
            .thenReturn(Arrays.asList(rating1, rating2, rating3, rating4).iterator());

        SolrQuery expectedQuery2 = new SolrQuery()
            .addFilterQuery(filterQuery)
            .setRows(100)
            .setStart(100)
            .setSort(AverageRatingQueryField.UPDATED_AT.getFieldName(), SolrQuery.ORDER.asc);

        QueryResponse response1 = mock(QueryResponse.class);
        QueryResponse response2 = mock(QueryResponse.class);

        AtomicInteger queryCounter = new AtomicInteger(0);
        when(solrClient.query(any())).then(invocationOnMock -> {

            SolrQuery givenQuery = invocationOnMock.getArgument(0);
            QueryResponse result = null;
            if (queryCounter.get() == 0) {
                assertEquals(expectedQuery1.getQuery(), givenQuery.getQuery());
                assertArrayEquals(expectedQuery1.getFilterQueries(), givenQuery.getFilterQueries());
                assertEquals(expectedQuery1.getRows(), givenQuery.getRows());
                assertEquals(expectedQuery1.getStart(), givenQuery.getStart());
                assertEquals(expectedQuery1.getSorts(), givenQuery.getSorts());
                result = response1;
            } else if (queryCounter.get() == 1) {
                assertEquals(expectedQuery2.getQuery(), givenQuery.getQuery());
                assertArrayEquals(expectedQuery2.getFilterQueries(), givenQuery.getFilterQueries());
                assertEquals(expectedQuery2.getRows(), givenQuery.getRows());
                assertEquals(expectedQuery2.getStart(), givenQuery.getStart());
                assertEquals(expectedQuery2.getSorts(), givenQuery.getSorts());
                result = response2;
            } else {
                fail("Too many requests performed.");
            }
            queryCounter.getAndIncrement();
            return result;
        });
        when(response1.getResults()).thenReturn(this.documentList);
        when(response2.getResults()).thenReturn(new SolrDocumentList());

        doAnswer(invocationOnMock -> {
            String modifier = invocationOnMock.getArgument(0);
            String fieldName = invocationOnMock.getArgument(1);
            Object fieldValue = invocationOnMock.getArgument(2);
            // we normally only invoke it with entity reference.
            assertEquals(EntityReference.class, invocationOnMock.getArgument(3));
            SolrInputDocument solrInputDocument = invocationOnMock.getArgument(4);
            solrInputDocument.setField(fieldName, Collections.singletonMap(modifier, fieldValue));
            return null;
        }).when(this.solrUtils).setAtomic(any(), any(), any(), any(), any());

        // expected committed documents
        SolrInputDocument solrInputDocument1 = new SolrInputDocument();
        solrInputDocument1.setField("id", "rating1");
        solrInputDocument1.setField(AverageRatingQueryField.ENTITY_REFERENCE.getFieldName(),
            Collections.singletonMap(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, newReference));

        SolrInputDocument solrInputDocument2 = new SolrInputDocument();
        solrInputDocument2.setField("id", "rating2");
        solrInputDocument2.setField(AverageRatingQueryField.PARENTS.getFieldName(),
            Collections.singletonMap(SolrUtils.ATOMIC_UPDATE_MODIFIER_REMOVE, oldReference));
        solrInputDocument2.setField(AverageRatingQueryField.PARENTS.getFieldName(),
            Collections.singletonMap(SolrUtils.ATOMIC_UPDATE_MODIFIER_ADD, newReference));

        SolrInputDocument solrInputDocument3 = new SolrInputDocument();
        solrInputDocument3.setField("id", "rating3");
        solrInputDocument3.setField(AverageRatingQueryField.ENTITY_REFERENCE.getFieldName(),
            Collections.singletonMap(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, newReference));
        solrInputDocument3.setField(AverageRatingQueryField.PARENTS.getFieldName(),
            Collections.singletonMap(SolrUtils.ATOMIC_UPDATE_MODIFIER_REMOVE, oldReference));
        solrInputDocument3.setField(AverageRatingQueryField.PARENTS.getFieldName(),
            Collections.singletonMap(SolrUtils.ATOMIC_UPDATE_MODIFIER_ADD, newReference));

        SolrInputDocument solrInputDocument4 = new SolrInputDocument();
        solrInputDocument4.setField("id", "rating4");
        solrInputDocument4.setField(AverageRatingQueryField.PARENTS.getFieldName(),
            Collections.singletonMap(SolrUtils.ATOMIC_UPDATE_MODIFIER_REMOVE, oldReference));
        solrInputDocument4.setField(AverageRatingQueryField.PARENTS.getFieldName(),
            Collections.singletonMap(SolrUtils.ATOMIC_UPDATE_MODIFIER_ADD, newReference));

        List<SolrInputDocument> expectedAddDocuments = new ArrayList<>(Arrays.asList(
            solrInputDocument1, solrInputDocument2, solrInputDocument3, solrInputDocument4));

        when(this.solrClient.add(any(SolrInputDocument.class))).then(invocationOnMock -> {
            SolrInputDocument solrInputDocument = invocationOnMock.getArgument(0);
            SolrInputDocument expectedSolrInputDocument = expectedAddDocuments.remove(0);

            // There's no proper equals method for SolrInputDocument, so we're comparing the toString
            assertEquals(expectedSolrInputDocument.toString(), solrInputDocument.toString());
            return null;
        });

        this.averageRatingManager.moveAverageRatings(oldReference, newReference);
        verify(this.solrClient, times(4)).add(any(SolrInputDocument.class));
        verify(this.solrClient).commit();
    }
}
