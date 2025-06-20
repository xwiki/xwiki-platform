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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.ratings.AverageRating;
import org.xwiki.ratings.Rating;
import org.xwiki.ratings.RatingsConfiguration;
import org.xwiki.ratings.RatingsException;
import org.xwiki.ratings.RatingsManager.RatingQueryField;
import org.xwiki.ratings.events.CreatedRatingEvent;
import org.xwiki.ratings.events.DeletedRatingEvent;
import org.xwiki.ratings.events.UpdatedRatingEvent;
import org.xwiki.ratings.internal.averagerating.AverageRatingManager;
import org.xwiki.ratings.internal.averagerating.DefaultAverageRating;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrUtils;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.user.UserReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SolrRatingsManager}.
 *
 * @version $Id$
 * @since 12.9RC1
 */
@ComponentTest
public class SolrRatingsManagerTest
{
    @InjectMockComponents
    private SolrRatingsManager manager;

    @MockComponent
    private SolrUtils solrUtils;

    @MockComponent
    private Solr solr;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private RatingsConfiguration configuration;

    @MockComponent
    private AverageRatingManager averageRatingManager;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @Mock
    private SolrClient solrClient;

    @Mock
    private SolrDocumentList documentList;

    @BeforeComponent
    void beforeComponent(MockitoComponentManager componentManager) throws Exception
    {
        componentManager.registerComponent(ComponentManager.class, "context", componentManager);
    }

    @BeforeEach
    void setup(MockitoComponentManager componentManager) throws Exception
    {
        this.manager.setRatingConfiguration(configuration);
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
        when(this.configuration.getAverageRatingStorageHint()).thenReturn("averageHint");
        componentManager.registerComponent(AverageRatingManager.class, "averageHint", this.averageRatingManager);
        when(this.documentAccessBridge.exists(any(DocumentReference.class))).thenReturn(true);
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
    void countRatings() throws Exception
    {
        UserReference userReference = mock(UserReference.class);
        EntityReference reference = mock(EntityReference.class);
        Map<RatingQueryField, Object> queryParameters = new LinkedHashMap<>();
        queryParameters.put(RatingQueryField.ENTITY_REFERENCE, reference);
        queryParameters.put(RatingQueryField.USER_REFERENCE, userReference);
        queryParameters.put(RatingQueryField.SCALE, 12);

        String managerId = "managerTest";
        this.manager.setIdentifier(managerId);
        when(this.configuration.hasDedicatedCore()).thenReturn(true);
        when(this.solr.getClient(managerId)).thenReturn(this.solrClient);

        when(reference.toString()).thenReturn("block:toto");
        when(userReference.toString()).thenReturn("user:Foobar");
        String query = "filter(reference:block\\:toto) AND filter(author:user\\:Foobar) "
            + "AND filter(scale:12) AND filter(managerId:managerTest)";
        SolrQuery expectedQuery = new SolrQuery().addFilterQuery(query).setStart(0).setRows(0);
        QueryResponse response = prepareSolrClientQueryWhenStatement(this.solrClient, expectedQuery);
        when(response.getResults()).thenReturn(this.documentList);
        when(this.documentList.getNumFound()).thenReturn(455L);

        assertEquals(455L, this.manager.countRatings(queryParameters));
    }

    @Test
    void getRatings() throws Exception
    {
        UserReference userReference = mock(UserReference.class);
        Map<RatingQueryField, Object> queryParameters = new LinkedHashMap<>();
        queryParameters.put(RatingQueryField.USER_REFERENCE, userReference);
        queryParameters.put(RatingQueryField.SCALE, "6");

        String managerId = "otherId";
        this.manager.setIdentifier(managerId);
        when(this.configuration.hasDedicatedCore()).thenReturn(false);
        when(this.solr.getClient(RatingSolrCoreInitializer.DEFAULT_RATINGS_SOLR_CORE)).thenReturn(this.solrClient);

        when(userReference.toString()).thenReturn("user:barfoo");
        String query = "filter(author:user\\:barfoo) AND filter(scale:6) AND filter(managerId:otherId)";

        int offset = 12;
        int limit = 42;
        String orderField = RatingQueryField.USER_REFERENCE.getFieldName();
        boolean asc = false;
        SolrQuery expectedQuery = new SolrQuery().addFilterQuery(query)
            .setStart(offset)
            .setRows(limit)
            .addSort(orderField, SolrQuery.ORDER.desc);
        QueryResponse response = prepareSolrClientQueryWhenStatement(this.solrClient, expectedQuery);
        when(response.getResults()).thenReturn(this.documentList);

        Map<String, Object> documentResult = new HashMap<>();
        documentResult.put("id", "result1");
        documentResult.put(RatingQueryField.MANAGER_ID.getFieldName(), "otherId");
        documentResult.put(RatingQueryField.CREATED_DATE.getFieldName(), new Date(1));
        documentResult.put(RatingQueryField.UPDATED_DATE.getFieldName(), new Date(1111));
        documentResult.put(RatingQueryField.VOTE.getFieldName(), 8);
        documentResult.put(RatingQueryField.SCALE.getFieldName(), 10);
        documentResult.put(RatingQueryField.ENTITY_REFERENCE.getFieldName(), "attachment:Foo");
        EntityReference reference1 = mock(EntityReference.class);
        documentResult.put(RatingQueryField.USER_REFERENCE.getFieldName(), "user:barfoo");
        SolrDocument result1 = new SolrDocument(documentResult);
        when(this.solrUtils.get(RatingQueryField.USER_REFERENCE.getFieldName(), result1, UserReference.class))
            .thenReturn(userReference);
        when(this.solrUtils.get(RatingQueryField.ENTITY_REFERENCE.getFieldName(), result1, EntityReference.class))
            .thenReturn(reference1);

        documentResult = new HashMap<>();
        documentResult.put("id", "result2");
        documentResult.put(RatingQueryField.MANAGER_ID.getFieldName(), "otherId");
        documentResult.put(RatingQueryField.CREATED_DATE.getFieldName(), new Date(2));
        documentResult.put(RatingQueryField.UPDATED_DATE.getFieldName(), new Date(2222));
        documentResult.put(RatingQueryField.VOTE.getFieldName(), 1);
        documentResult.put(RatingQueryField.SCALE.getFieldName(), 10);
        documentResult.put(RatingQueryField.ENTITY_REFERENCE.getFieldName(), "attachment:Bar");
        EntityReference reference2 = mock(EntityReference.class);
        documentResult.put(RatingQueryField.USER_REFERENCE.getFieldName(), "user:barfoo");
        SolrDocument result2 = new SolrDocument(documentResult);
        when(this.solrUtils.get(RatingQueryField.USER_REFERENCE.getFieldName(), result2, UserReference.class))
            .thenReturn(userReference);
        when(this.solrUtils.get(RatingQueryField.ENTITY_REFERENCE.getFieldName(), result2, EntityReference.class))
            .thenReturn(reference2);

        documentResult = new HashMap<>();
        documentResult.put("id", "result3");
        documentResult.put(RatingQueryField.MANAGER_ID.getFieldName(), "otherId");
        documentResult.put(RatingQueryField.CREATED_DATE.getFieldName(), new Date(3));
        documentResult.put(RatingQueryField.UPDATED_DATE.getFieldName(), new Date(3333));
        documentResult.put(RatingQueryField.VOTE.getFieldName(), 3);
        documentResult.put(RatingQueryField.SCALE.getFieldName(), 10);
        documentResult.put(RatingQueryField.ENTITY_REFERENCE.getFieldName(), "attachment:Baz");
        EntityReference reference3 = mock(EntityReference.class);
        documentResult.put(RatingQueryField.USER_REFERENCE.getFieldName(), "user:barfoo");
        SolrDocument result3 = new SolrDocument(documentResult);
        when(this.solrUtils.get(RatingQueryField.USER_REFERENCE.getFieldName(), result3, UserReference.class))
            .thenReturn(userReference);
        when(this.solrUtils.get(RatingQueryField.ENTITY_REFERENCE.getFieldName(), result3, EntityReference.class))
            .thenReturn(reference3);

        when(this.documentList.stream()).thenReturn(Stream.of(result1, result2, result3));

        List<Rating> expectedRatings = Arrays.asList(
            new DefaultRating("result1")
                .setManagerId("otherId")
                .setCreatedAt(new Date(1))
                .setUpdatedAt(new Date(1111))
                .setVote(8)
                .setReference(reference1)
                .setAuthor(userReference)
                .setScaleUpperBound(10),

            new DefaultRating("result2")
                .setManagerId("otherId")
                .setCreatedAt(new Date(2))
                .setUpdatedAt(new Date(2222))
                .setVote(1)
                .setReference(reference2)
                .setAuthor(userReference)
                .setScaleUpperBound(10),

            new DefaultRating("result3")
                .setManagerId("otherId")
                .setCreatedAt(new Date(3))
                .setUpdatedAt(new Date(3333))
                .setVote(3)
                .setReference(reference3)
                .setAuthor(userReference)
                .setScaleUpperBound(10)
        );
        assertEquals(expectedRatings,
            this.manager.getRatings(queryParameters, offset, limit, RatingQueryField.USER_REFERENCE, asc));
    }

    @Test
    void getAverageRating() throws Exception
    {
        String managerId = "averageId2";
        this.manager.setIdentifier(managerId);
        EntityReference reference = new EntityReference("xwiki:Something", EntityType.PAGE);
        AverageRating expectedAverageRating = new DefaultAverageRating("average1")
            .setAverageVote(2.341f)
            .setTotalVote(242)
            .setUpdatedAt(new Date(42))
            .setManagerId(managerId)
            .setScaleUpperBound(12)
            .setReference(reference);
        when(this.averageRatingManager.getAverageRating(reference)).thenReturn(expectedAverageRating);
        when(this.configuration.isAverageStored()).thenReturn(true);

        AverageRating averageRating = this.manager.getAverageRating(reference);
        assertEquals(expectedAverageRating, averageRating);
    }

    @Test
    void removeRatingNotExisting() throws Exception
    {
        String ratingingId = "ratinging389";
        String managerId = "removeRating1";
        this.manager.setIdentifier(managerId);
        when(this.configuration.hasDedicatedCore()).thenReturn(false);
        when(this.solr.getClient(RatingSolrCoreInitializer.DEFAULT_RATINGS_SOLR_CORE)).thenReturn(this.solrClient);

        String query = "filter(id:ratinging389) AND filter(managerId:removeRating1)";
        SolrQuery expectedQuery = new SolrQuery()
            .addFilterQuery(query)
            .setStart(0)
            .setRows(1)
            .setSort(RatingQueryField.CREATED_DATE.getFieldName(), SolrQuery.ORDER.asc);
        QueryResponse response = prepareSolrClientQueryWhenStatement(this.solrClient, expectedQuery);
        when(response.getResults()).thenReturn(this.documentList);
        assertFalse(this.manager.removeRating(ratingingId));
        verify(this.solrClient, never()).deleteById(any(String.class));
    }

    @Test
    void removeRatingExisting() throws Exception
    {
        String ratingingId = "ratinging429";
        String managerId = "removeRating2";
        this.manager.setIdentifier(managerId);
        when(this.configuration.hasDedicatedCore()).thenReturn(false);
        when(this.solr.getClient(RatingSolrCoreInitializer.DEFAULT_RATINGS_SOLR_CORE)).thenReturn(this.solrClient);

        String query = "filter(id:ratinging429) AND filter(managerId:removeRating2)";
        SolrQuery expectedQuery = new SolrQuery()
            .addFilterQuery(query)
            .setStart(0)
            .setRows(1)
            .setSort(RatingQueryField.CREATED_DATE.getFieldName(), SolrQuery.ORDER.asc);
        QueryResponse response = prepareSolrClientQueryWhenStatement(this.solrClient, expectedQuery);
        when(response.getResults()).thenReturn(this.documentList);
        when(this.documentList.isEmpty()).thenReturn(false);

        Map<String, Object> documentResult = new HashMap<>();
        documentResult.put("id", ratingingId);
        documentResult.put(RatingQueryField.MANAGER_ID.getFieldName(), managerId);
        documentResult.put(RatingQueryField.CREATED_DATE.getFieldName(), new Date(1));
        documentResult.put(RatingQueryField.UPDATED_DATE.getFieldName(), new Date(1111));
        documentResult.put(RatingQueryField.VOTE.getFieldName(), 8);
        documentResult.put(RatingQueryField.SCALE.getFieldName(), 10);
        documentResult.put(RatingQueryField.ENTITY_REFERENCE.getFieldName(), "attachment:Foo");
        EntityReference reference1 = mock(EntityReference.class);
        documentResult.put(RatingQueryField.USER_REFERENCE.getFieldName(), "user:barfoo");
        UserReference userReference = mock(UserReference.class);
        SolrDocument result1 = new SolrDocument(documentResult);
        when(this.documentList.stream()).thenReturn(Collections.singletonList(result1).stream());
        when(this.solrUtils.get(RatingQueryField.USER_REFERENCE.getFieldName(), result1, UserReference.class))
            .thenReturn(userReference);
        when(this.solrUtils.get(RatingQueryField.ENTITY_REFERENCE.getFieldName(), result1, EntityReference.class))
            .thenReturn(reference1);

        Rating rating = new DefaultRating(ratingingId)
            .setReference(reference1)
            .setManagerId(managerId)
            .setCreatedAt(new Date(1))
            .setUpdatedAt(new Date(1111))
            .setAuthor(userReference)
            .setScaleUpperBound(10)
            .setVote(8);

        // Average rating handling
        when(this.configuration.isAverageStored()).thenReturn(true);

        assertTrue(this.manager.removeRating(ratingingId));
        verify(this.solrClient).deleteById(ratingingId);
        verify(this.solrClient).commit();
        verify(this.observationManager).notify(any(DeletedRatingEvent.class), eq(managerId), eq(rating));
        verify(this.configuration).isAverageStored();
        verify(this.averageRatingManager).removeVote(reference1, 8);
    }

    @Test
    void saveRatingOutScale()
    {
        when(this.configuration.getScaleUpperBound()).thenReturn(5);
        this.manager.setIdentifier("saveRating1");
        RatingsException exception = assertThrows(RatingsException.class, () -> {
            this.manager.saveRating(new EntityReference("test", EntityType.PAGE), mock(UserReference.class), -1);
        });
        assertEquals("The vote [-1] is out of scale [5] for [saveRating1] rating manager.", exception.getMessage());

        exception = assertThrows(RatingsException.class, () -> {
            this.manager.saveRating(new EntityReference("test", EntityType.PAGE), mock(UserReference.class), 8);
        });
        assertEquals("The vote [8] is out of scale [5] for [saveRating1] rating manager.", exception.getMessage());
    }

    @Test
    void saveRatingZeroNotExisting() throws Exception
    {
        String managerId = "saveRating2";
        this.manager.setIdentifier(managerId);
        int scale = 10;
        when(this.configuration.getScaleUpperBound()).thenReturn(scale);
        DocumentReference reference = new DocumentReference("wiki", "Space", "Page");
        UserReference userReference = mock(UserReference.class);
        when(userReference.toString()).thenReturn("user:Toto");

        String filterQuery = "filter(reference:wiki\\:Space.Page) AND filter(author:user\\:Toto) "
            + "AND filter(managerId:saveRating2)";
        SolrQuery expectedQuery = new SolrQuery()
            .addFilterQuery(filterQuery)
            .setStart(0)
            .setRows(1)
            .addSort(RatingQueryField.CREATED_DATE.getFieldName(), SolrQuery.ORDER.asc);
        QueryResponse response = prepareSolrClientQueryWhenStatement(this.solrClient, expectedQuery);

        // We don't mock stream behaviour, so that the returned result is empty.
        when(response.getResults()).thenReturn(this.documentList);

        when(this.configuration.hasDedicatedCore()).thenReturn(false);
        when(this.solr.getClient(RatingSolrCoreInitializer.DEFAULT_RATINGS_SOLR_CORE)).thenReturn(this.solrClient);

        // Check if we don't store 0
        when(this.configuration.isZeroStored()).thenReturn(false);
        assertNull(this.manager.saveRating(reference, userReference, 0));

        // Check if we store 0
        when(this.configuration.isZeroStored()).thenReturn(true);
        // Handle Average rating
        when(this.configuration.isAverageStored()).thenReturn(true);

        DefaultRating expectedRating = new DefaultRating("")
            .setManagerId(managerId)
            .setReference(reference)
            .setCreatedAt(new Date())
            .setUpdatedAt(new Date())
            .setVote(0)
            .setScaleUpperBound(scale)
            .setAuthor(userReference);

        SolrInputDocument expectedInputDocument = new SolrInputDocument();
        expectedInputDocument.setField("id", "");
        expectedInputDocument.setField(RatingQueryField.ENTITY_REFERENCE.getFieldName(), "wiki:Space.Page");
        expectedInputDocument.setField(RatingQueryField.CREATED_DATE.getFieldName(), new Date());
        expectedInputDocument.setField(RatingQueryField.UPDATED_DATE.getFieldName(), new Date());
        expectedInputDocument.setField(RatingQueryField.USER_REFERENCE.getFieldName(), "user:Toto");
        expectedInputDocument.setField(RatingQueryField.SCALE.getFieldName(), scale);
        expectedInputDocument.setField(RatingQueryField.MANAGER_ID.getFieldName(), managerId);
        expectedInputDocument.setField(RatingQueryField.VOTE.getFieldName(), 0);

        when(this.solrClient.add(any(SolrInputDocument.class))).then(invocationOnMock -> {
            SolrInputDocument obtainedInputDocument = invocationOnMock.getArgument(0);
            Date updatedAt = (Date) obtainedInputDocument.getFieldValue(RatingQueryField.UPDATED_DATE.getFieldName());
            Date createdAt = (Date) obtainedInputDocument.getFieldValue(RatingQueryField.CREATED_DATE.getFieldName());
            String id = (String) obtainedInputDocument.getFieldValue("id");
            expectedInputDocument.setField(RatingQueryField.CREATED_DATE.getFieldName(), createdAt);
            expectedInputDocument.setField(RatingQueryField.UPDATED_DATE.getFieldName(), updatedAt);
            expectedInputDocument.setField("id", id);

            expectedRating
                .setId(id)
                .setCreatedAt(createdAt)
                .setUpdatedAt(updatedAt);
            // We rely on the toString method since there's no proper equals method
            assertEquals(expectedInputDocument.toString(), obtainedInputDocument.toString());
            return null;
        });

        assertEquals(expectedRating, this.manager.saveRating(reference, userReference, 0));
        verify(this.solrClient).add(any(SolrInputDocument.class));
        verify(this.solrClient).commit();
        verify(this.observationManager).notify(any(CreatedRatingEvent.class), eq(managerId), eq(expectedRating));
        verify(this.averageRatingManager).addVote(reference, 0);
    }

    @Test
    void saveRatingExisting() throws Exception
    {
        String managerId = "saveRating3";
        this.manager.setIdentifier(managerId);
        int scale = 8;
        int newVote = 2;
        int oldVote = 3;
        when(this.configuration.getScaleUpperBound()).thenReturn(scale);
        DocumentReference reference = new DocumentReference("wiki", "Space", "Page");
        UserReference userReference = mock(UserReference.class);
        when(userReference.toString()).thenReturn("user:Toto");

        String filterQuery = "filter(reference:wiki\\:Space.Page) AND filter(author:user\\:Toto) "
            + "AND filter(managerId:saveRating3)";
        SolrQuery expectedQuery = new SolrQuery()
            .addFilterQuery(filterQuery)
            .setStart(0)
            .setRows(1)
            .addSort(RatingQueryField.CREATED_DATE.getFieldName(), SolrQuery.ORDER.asc);
        QueryResponse response = prepareSolrClientQueryWhenStatement(this.solrClient, expectedQuery);

        // We don't mock stream behaviour, so that the returned result is empty.
        when(response.getResults()).thenReturn(this.documentList);

        Map<String, Object> fieldMap = new HashMap<>();
        fieldMap.put("id", "myRating");
        fieldMap.put(RatingQueryField.VOTE.getFieldName(), oldVote);
        fieldMap.put(RatingQueryField.CREATED_DATE.getFieldName(), new Date(422));
        fieldMap.put(RatingQueryField.UPDATED_DATE.getFieldName(), new Date(422));
        fieldMap.put(RatingQueryField.USER_REFERENCE.getFieldName(), "user:Toto");
        fieldMap.put(RatingQueryField.ENTITY_REFERENCE.getFieldName(), "wiki:Space.Page");
        fieldMap.put(RatingQueryField.SCALE.getFieldName(), scale);
        fieldMap.put(RatingQueryField.MANAGER_ID.getFieldName(), managerId);

        SolrDocument solrDocument = new SolrDocument(fieldMap);
        when(this.solrUtils.get(RatingQueryField.ENTITY_REFERENCE.getFieldName(), solrDocument, EntityReference.class))
            .thenReturn(reference);
        when(this.solrUtils.get(RatingQueryField.USER_REFERENCE.getFieldName(), solrDocument, UserReference.class))
            .thenReturn(userReference);
        when(this.documentList.stream()).thenReturn(Collections.singletonList(solrDocument).stream());

        when(this.configuration.hasDedicatedCore()).thenReturn(false);
        when(this.solr.getClient(RatingSolrCoreInitializer.DEFAULT_RATINGS_SOLR_CORE)).thenReturn(this.solrClient);

        when(this.configuration.isZeroStored()).thenReturn(false);

        // Handle Average rating
        when(this.configuration.isAverageStored()).thenReturn(true);

        DefaultRating expectedRating = new DefaultRating("myRating")
            .setManagerId(managerId)
            .setReference(reference)
            .setCreatedAt(new Date(422))
            .setUpdatedAt(new Date())
            .setVote(newVote)
            .setScaleUpperBound(scale)
            .setAuthor(userReference);

        SolrInputDocument expectedInputDocument = new SolrInputDocument();
        expectedInputDocument.setField("id", "myRating");
        expectedInputDocument.setField(RatingQueryField.ENTITY_REFERENCE.getFieldName(), "wiki:Space.Page");
        expectedInputDocument.setField(RatingQueryField.CREATED_DATE.getFieldName(), new Date(422));
        expectedInputDocument.setField(RatingQueryField.UPDATED_DATE.getFieldName(), new Date());
        expectedInputDocument.setField(RatingQueryField.USER_REFERENCE.getFieldName(), "user:Toto");
        expectedInputDocument.setField(RatingQueryField.SCALE.getFieldName(), scale);
        expectedInputDocument.setField(RatingQueryField.MANAGER_ID.getFieldName(), managerId);
        expectedInputDocument.setField(RatingQueryField.VOTE.getFieldName(), newVote);

        when(this.solrClient.add(any(SolrInputDocument.class))).then(invocationOnMock -> {
            SolrInputDocument obtainedInputDocument = invocationOnMock.getArgument(0);
            Date updatedAt = (Date) obtainedInputDocument.getFieldValue(RatingQueryField.UPDATED_DATE.getFieldName());
            expectedInputDocument.setField(RatingQueryField.UPDATED_DATE.getFieldName(), updatedAt);

            expectedRating
                .setUpdatedAt(updatedAt);
            // We rely on the toString method since there's no proper equals method
            assertEquals(expectedInputDocument.toString(), obtainedInputDocument.toString());
            return null;
        });

        assertEquals(expectedRating, this.manager.saveRating(reference, userReference, newVote));
        verify(this.solrClient).add(any(SolrInputDocument.class));
        verify(this.solrClient).commit();
        verify(this.observationManager).notify(new UpdatedRatingEvent(expectedRating, oldVote), managerId,
            expectedRating);
        verify(this.averageRatingManager).updateVote(reference, oldVote, newVote);
    }

    @Test
    void saveRatingExistingToZero() throws Exception
    {
        String managerId = "saveRating4";
        this.manager.setIdentifier(managerId);
        int scale = 8;
        int newVote = 0;
        int oldVote = 3;
        when(this.configuration.getScaleUpperBound()).thenReturn(scale);
        DocumentReference reference = new DocumentReference("wiki", "Space", "Page");
        UserReference userReference = mock(UserReference.class);
        when(userReference.toString()).thenReturn("user:Toto");

        String filterQuery = "filter(reference:wiki\\:Space.Page) AND filter(author:user\\:Toto) "
            + "AND filter(managerId:saveRating4)";
        SolrQuery firstExpectedQuery = new SolrQuery()
            .addFilterQuery(filterQuery)
            .setStart(0)
            .setRows(1)
            .addSort(RatingQueryField.CREATED_DATE.getFieldName(), SolrQuery.ORDER.asc);

        Map<String, Object> fieldMap = new HashMap<>();
        fieldMap.put("id", "myRating");
        fieldMap.put(RatingQueryField.VOTE.getFieldName(), oldVote);
        fieldMap.put(RatingQueryField.CREATED_DATE.getFieldName(), new Date(422));
        fieldMap.put(RatingQueryField.UPDATED_DATE.getFieldName(), new Date(422));
        fieldMap.put(RatingQueryField.USER_REFERENCE.getFieldName(), "user:Toto");
        fieldMap.put(RatingQueryField.ENTITY_REFERENCE.getFieldName(), "wiki:Space.Page");
        fieldMap.put(RatingQueryField.SCALE.getFieldName(), scale);
        fieldMap.put(RatingQueryField.MANAGER_ID.getFieldName(), managerId);

        SolrDocument solrDocument = new SolrDocument(fieldMap);
        when(this.solrUtils.get(RatingQueryField.ENTITY_REFERENCE.getFieldName(), solrDocument, EntityReference.class))
            .thenReturn(reference);
        when(this.solrUtils.get(RatingQueryField.USER_REFERENCE.getFieldName(), solrDocument, UserReference.class))
            .thenReturn(userReference);
        when(this.documentList.stream())
            .thenReturn(Stream.of(solrDocument))
            .thenReturn(Stream.of(solrDocument))
            .thenReturn(Stream.of(solrDocument))
            .thenReturn(Stream.of(solrDocument));
        // Those are used for deletion.
        when(this.documentList.isEmpty()).thenReturn(false);
        when(this.documentList.get(0)).thenReturn(solrDocument);

        when(this.configuration.hasDedicatedCore()).thenReturn(false);
        when(this.solr.getClient(RatingSolrCoreInitializer.DEFAULT_RATINGS_SOLR_CORE)).thenReturn(this.solrClient);

        // Handle Average rating
        when(this.configuration.isAverageStored()).thenReturn(true);

        DefaultRating oldRating = new DefaultRating("myRating")
            .setManagerId(managerId)
            .setReference(reference)
            .setCreatedAt(new Date(422))
            .setUpdatedAt(new Date(422))
            .setVote(oldVote)
            .setScaleUpperBound(scale)
            .setAuthor(userReference);

        filterQuery = "filter(id:myRating) AND filter(managerId:saveRating4)";
        SolrQuery secondExpectedQuery = new SolrQuery()
            .addFilterQuery(filterQuery)
            .setStart(0)
            .setRows(1)
            .addSort(RatingQueryField.CREATED_DATE.getFieldName(), SolrQuery.ORDER.asc);

        QueryResponse response = mock(QueryResponse.class);
        when(solrClient.query(any())).then(invocationOnMock -> {
            SolrQuery givenQuery = invocationOnMock.getArgument(0);
            SolrQuery checkExpectedQuery = firstExpectedQuery;

            assertEquals(checkExpectedQuery.getQuery(), givenQuery.getQuery());
            assertArrayEquals(checkExpectedQuery.getFilterQueries(), givenQuery.getFilterQueries());
            assertEquals(checkExpectedQuery.getRows(), givenQuery.getRows());
            assertEquals(checkExpectedQuery.getStart(), givenQuery.getStart());
            assertEquals(checkExpectedQuery.getSorts(), givenQuery.getSorts());
            return response;
        }).then(invocationOnMock -> {
            SolrQuery givenQuery = invocationOnMock.getArgument(0);
            SolrQuery checkExpectedQuery = secondExpectedQuery;

            assertEquals(checkExpectedQuery.getQuery(), givenQuery.getQuery());
            assertArrayEquals(checkExpectedQuery.getFilterQueries(), givenQuery.getFilterQueries());
            assertEquals(checkExpectedQuery.getRows(), givenQuery.getRows());
            assertEquals(checkExpectedQuery.getStart(), givenQuery.getStart());
            assertEquals(checkExpectedQuery.getSorts(), givenQuery.getSorts());
            return response;
        }).then(invocationOnMock -> {
            SolrQuery givenQuery = invocationOnMock.getArgument(0);
            SolrQuery checkExpectedQuery = firstExpectedQuery;

            assertEquals(checkExpectedQuery.getQuery(), givenQuery.getQuery());
            assertArrayEquals(checkExpectedQuery.getFilterQueries(), givenQuery.getFilterQueries());
            assertEquals(checkExpectedQuery.getRows(), givenQuery.getRows());
            assertEquals(checkExpectedQuery.getStart(), givenQuery.getStart());
            assertEquals(checkExpectedQuery.getSorts(), givenQuery.getSorts());
            return response;
        }).then(invocationOnMock -> {
            SolrQuery givenQuery = invocationOnMock.getArgument(0);
            SolrQuery checkExpectedQuery = secondExpectedQuery;

            assertEquals(checkExpectedQuery.getQuery(), givenQuery.getQuery());
            assertArrayEquals(checkExpectedQuery.getFilterQueries(), givenQuery.getFilterQueries());
            assertEquals(checkExpectedQuery.getRows(), givenQuery.getRows());
            assertEquals(checkExpectedQuery.getStart(), givenQuery.getStart());
            assertEquals(checkExpectedQuery.getSorts(), givenQuery.getSorts());
            return response;
        });
        when(response.getResults()).thenReturn(this.documentList);

        when(this.configuration.isZeroStored()).thenReturn(false);
        assertNull(this.manager.saveRating(reference, userReference, newVote));
        verify(this.solrClient, never()).add(any(SolrInputDocument.class));
        verify(this.solrClient).deleteById("myRating");
        verify(this.solrClient).commit();
        verify(this.observationManager).notify(any(DeletedRatingEvent.class), eq(managerId), eq(oldRating));
        verify(this.averageRatingManager).removeVote(reference, oldVote);

        when(this.configuration.isZeroStored()).thenReturn(true);
        DefaultRating expectedRating = new DefaultRating("myRating")
            .setManagerId(managerId)
            .setReference(reference)
            .setCreatedAt(new Date(422))
            .setVote(0)
            .setScaleUpperBound(scale)
            .setAuthor(userReference);
        Rating rating = this.manager.saveRating(reference, userReference, newVote);
        assertNotNull(rating);
        expectedRating.setUpdatedAt(rating.getUpdatedAt());
        assertEquals(expectedRating, rating);
        verify(this.solrClient).add(any(SolrInputDocument.class));
    }

    @Test
    void saveRatingWrongEntityType() throws Exception
    {
        when(this.configuration.getScaleUpperBound()).thenReturn(2);
        EntityReference reference = new SpaceReference("xwiki", "Space");
        UserReference userReference = mock(UserReference.class);
        RatingsException exception =
            assertThrows(RatingsException.class, () -> this.manager.saveRating(reference, userReference, 1));
        assertEquals("The reference [Space xwiki:Space] is not an existing page.", exception.getMessage());
        verify(this.documentAccessBridge, never()).exists(any(DocumentReference.class));
    }

    @Test
    void saveRatingNoneExistingPage() throws Exception
    {
        when(this.configuration.getScaleUpperBound()).thenReturn(2);
        DocumentReference reference = new DocumentReference("xwiki", "Space", "Page");
        when(this.documentAccessBridge.exists(reference)).thenReturn(false);
        UserReference userReference = mock(UserReference.class);
        RatingsException exception =
            assertThrows(RatingsException.class, () -> this.manager.saveRating(reference, userReference, 1));
        assertEquals("The reference [xwiki:Space.Page] is not an existing page.", exception.getMessage());
        verify(this.documentAccessBridge).exists(reference);
    }

    @Test
    void saveRating() throws Exception
    {
        String ratingId = "myId";
        String managerId = "managerId";
        EntityReference entityReference = mock(EntityReference.class);
        when(entityReference.toString()).thenReturn("wiki:foobar");
        UserReference userReference = mock(UserReference.class);
        when(userReference.toString()).thenReturn("user:Toto");
        int scale = 8;
        int vote = 3;

        DefaultRating inputRating = new DefaultRating(ratingId)
            .setManagerId(managerId)
            .setReference(entityReference)
            .setCreatedAt(new Date(48))
            .setUpdatedAt(new Date(54))
            .setVote(vote)
            .setScaleUpperBound(scale)
            .setAuthor(userReference);

        SolrInputDocument expectedInputDocument = new SolrInputDocument();
        expectedInputDocument.setField("id", ratingId);
        expectedInputDocument.setField(RatingQueryField.ENTITY_REFERENCE.getFieldName(), "wiki:foobar");
        expectedInputDocument.setField(RatingQueryField.CREATED_DATE.getFieldName(), new Date(48));
        expectedInputDocument.setField(RatingQueryField.UPDATED_DATE.getFieldName(), new Date(54));
        expectedInputDocument.setField(RatingQueryField.USER_REFERENCE.getFieldName(), "user:Toto");
        expectedInputDocument.setField(RatingQueryField.SCALE.getFieldName(), scale);
        expectedInputDocument.setField(RatingQueryField.MANAGER_ID.getFieldName(), managerId);
        expectedInputDocument.setField(RatingQueryField.VOTE.getFieldName(), vote);

        when(this.configuration.hasDedicatedCore()).thenReturn(false);
        when(this.solr.getClient(RatingSolrCoreInitializer.DEFAULT_RATINGS_SOLR_CORE)).thenReturn(this.solrClient);

        when(this.solrClient.add(any(SolrInputDocument.class))).then(invocationOnMock -> {
            // allow to check that add retrieve the right argument: there's no equals method in SolrInputDocument
            assertEquals(expectedInputDocument.toString(), invocationOnMock.getArgument(0).toString());
            return null;
        });
        this.manager.saveRating(inputRating);
        verify(this.solrClient).add(any(SolrInputDocument.class));
        verify(this.solrClient).commit();
    }

    @Test
    void recomputeAverageRatingNotStored()
    {
        when(this.configuration.isAverageStored()).thenReturn(false);
        RatingsException ratingsException = assertThrows(RatingsException.class, () -> {
            this.manager.recomputeAverageRating(mock(EntityReference.class));
        });
        assertEquals("This rating manager is not configured to store average rating.", ratingsException.getMessage());
    }

    @Test
    void recomputeAverageRating() throws Exception
    {
        when(this.configuration.isAverageStored()).thenReturn(true);
        when(this.solr.getClient(RatingSolrCoreInitializer.DEFAULT_RATINGS_SOLR_CORE)).thenReturn(this.solrClient);

        EntityReference inputReference = mock(EntityReference.class);
        when(inputReference.toString()).thenReturn("document:Input.Reference");
        String managerId = "myManager";

        this.manager.setIdentifier(managerId);

        String filterQuery = String.format("filter(%s:%s) AND filter(%s:%s)",
            RatingQueryField.ENTITY_REFERENCE.getFieldName(), "document\\:Input.Reference",
            RatingQueryField.MANAGER_ID.getFieldName(), managerId);

        SolrQuery expectedQuery1 = new SolrQuery()
            .addFilterQuery(filterQuery)
            .setRows(100)
            .setStart(0)
            .setSort(RatingQueryField.CREATED_DATE.getFieldName(), SolrQuery.ORDER.asc);

        SolrDocument rating1 = mock(SolrDocument.class);
        SolrDocument rating2 = mock(SolrDocument.class);
        SolrDocument rating3 = mock(SolrDocument.class);
        SolrDocument rating4 = mock(SolrDocument.class);
        when(rating1.get(RatingQueryField.VOTE.getFieldName())).thenReturn(4);
        when(rating1.get(RatingQueryField.SCALE.getFieldName())).thenReturn(5);
        when(rating2.get(RatingQueryField.VOTE.getFieldName())).thenReturn(1);
        when(rating2.get(RatingQueryField.SCALE.getFieldName())).thenReturn(5);
        when(rating3.get(RatingQueryField.VOTE.getFieldName())).thenReturn(0);
        when(rating3.get(RatingQueryField.SCALE.getFieldName())).thenReturn(5);
        when(rating4.get(RatingQueryField.VOTE.getFieldName())).thenReturn(2);
        when(rating4.get(RatingQueryField.SCALE.getFieldName())).thenReturn(5);
        when(this.documentList.stream())
            .thenReturn(Arrays.asList(rating1, rating2, rating3, rating4).stream());

        SolrQuery expectedQuery2 = new SolrQuery()
            .addFilterQuery(filterQuery)
            .setRows(100)
            .setStart(100)
            .setSort(RatingQueryField.CREATED_DATE.getFieldName(), SolrQuery.ORDER.asc);

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

        AverageRating averageRating = mock(AverageRating.class);
        when(this.averageRatingManager.resetAverageRating(inputReference, 1.75f, 4)).thenReturn(averageRating);
        assertEquals(averageRating, this.manager.recomputeAverageRating(inputReference));
    }

    @Test
    void recomputeAverageRatings_noVote() throws Exception
    {
        when(this.configuration.isAverageStored()).thenReturn(true);
        when(this.solr.getClient(RatingSolrCoreInitializer.DEFAULT_RATINGS_SOLR_CORE)).thenReturn(this.solrClient);
        EntityReference inputReference = mock(EntityReference.class);
        when(inputReference.toString()).thenReturn("document:Input.Reference");
        String managerId = "myManager";

        this.manager.setIdentifier(managerId);

        String filterQuery = String.format("filter(%s:%s) AND filter(%s:%s)",
            RatingQueryField.ENTITY_REFERENCE.getFieldName(), "document\\:Input.Reference",
            RatingQueryField.MANAGER_ID.getFieldName(), managerId);

        SolrQuery expectedQuery = new SolrQuery()
            .addFilterQuery(filterQuery)
            .setRows(100)
            .setStart(0)
            .setSort(RatingQueryField.CREATED_DATE.getFieldName(), SolrQuery.ORDER.asc);

        QueryResponse response1 = mock(QueryResponse.class);
        when(solrClient.query(any())).then(invocationOnMock -> {
            SolrQuery givenQuery = invocationOnMock.getArgument(0);
            QueryResponse result = null;
            assertEquals(expectedQuery.getQuery(), givenQuery.getQuery());
            assertArrayEquals(expectedQuery.getFilterQueries(), givenQuery.getFilterQueries());
            assertEquals(expectedQuery.getRows(), givenQuery.getRows());
            assertEquals(expectedQuery.getStart(), givenQuery.getStart());
            assertEquals(expectedQuery.getSorts(), givenQuery.getSorts());
            result = response1;
            return result;
        });
        // empty response
        when(response1.getResults()).thenReturn(new SolrDocumentList());

        AverageRating averageRating = mock(AverageRating.class);
        when(this.averageRatingManager.resetAverageRating(inputReference, 0, 0)).thenReturn(averageRating);
        assertEquals(averageRating, this.manager.recomputeAverageRating(inputReference));
    }

    @Test
    void removeRatings() throws Exception
    {
        String managerId = "myRatingManager";
        this.manager.setIdentifier(managerId);
        when(this.solr.getClient(RatingSolrCoreInitializer.DEFAULT_RATINGS_SOLR_CORE)).thenReturn(this.solrClient);

        EntityReference entityReference = mock(EntityReference.class);
        when(entityReference.toString()).thenReturn("document:My.Doc");
        String filterQuery = String.format("filter(%s:%s) AND (filter(%s:%s) OR filter(%s:%s))",
            RatingQueryField.MANAGER_ID.getFieldName(), managerId,
            RatingQueryField.ENTITY_REFERENCE.getFieldName(), "document\\:My.Doc",
            RatingQueryField.PARENTS_REFERENCE.getFieldName(), "document\\:My.Doc");

        SolrQuery expectedQuery = new SolrQuery()
            .addFilterQuery(filterQuery)
            .setRows(0)
            .setStart(0);
        QueryResponse response = prepareSolrClientQueryWhenStatement(this.solrClient, expectedQuery);
        when(response.getResults()).thenReturn(this.documentList);
        when(this.documentList.getNumFound()).thenReturn(42L);
        when(this.configuration.isAverageStored()).thenReturn(true);

        assertEquals(42L, this.manager.removeRatings(entityReference));
        verify(this.solrClient).deleteByQuery(filterQuery);
        verify(this.solrClient).commit();
        verify(this.averageRatingManager).removeAverageRatings(entityReference);
    }

    @Test
    void moveRatings() throws Exception
    {
        String managerId = "moveRatingsManagerId";
        this.manager.setIdentifier(managerId);
        when(this.solr.getClient(RatingSolrCoreInitializer.DEFAULT_RATINGS_SOLR_CORE)).thenReturn(this.solrClient);

        EntityReference oldReference = mock(EntityReference.class);
        EntityReference newReference = mock(EntityReference.class);
        when(oldReference.toString()).thenReturn("document:My.Old.Doc");

        String filterQuery = String.format("filter(%s:%s) AND (filter(%s:%s) OR filter(%s:%s))",
            RatingQueryField.MANAGER_ID.getFieldName(), managerId,
            RatingQueryField.ENTITY_REFERENCE.getFieldName(), "document\\:My.Old.Doc",
            RatingQueryField.PARENTS_REFERENCE.getFieldName(), "document\\:My.Old.Doc");

        SolrQuery expectedQuery1 = new SolrQuery()
            .addFilterQuery(filterQuery)
            .setRows(100)
            .setStart(0)
            .setSort(RatingQueryField.CREATED_DATE.getFieldName(), SolrQuery.ORDER.asc);

        SolrDocument rating1 = mock(SolrDocument.class);
        SolrDocument rating2 = mock(SolrDocument.class);
        SolrDocument rating3 = mock(SolrDocument.class);
        SolrDocument rating4 = mock(SolrDocument.class);

        // rating1 have the appropriate reference, but not the appropriate parent
        when(rating1.get(RatingQueryField.IDENTIFIER.getFieldName())).thenReturn("rating1");
        when(this.solrUtils.get(RatingQueryField.ENTITY_REFERENCE.getFieldName(), rating1, EntityReference.class))
            .thenReturn(oldReference);
        when(this.solrUtils.getCollection(RatingQueryField.PARENTS_REFERENCE.getFieldName(), rating1,
            EntityReference.class))
            .thenReturn(Collections.emptyList());

        // rating2 have not the appropriate reference but the appropriate parent
        when(rating2.get(RatingQueryField.IDENTIFIER.getFieldName())).thenReturn("rating2");
        when(this.solrUtils.get(RatingQueryField.ENTITY_REFERENCE.getFieldName(), rating2, EntityReference.class))
            .thenReturn(mock(EntityReference.class));
        when(this.solrUtils.getCollection(RatingQueryField.PARENTS_REFERENCE.getFieldName(), rating2,
            EntityReference.class))
            .thenReturn(Collections.singletonList(oldReference));

        // rating3 have the appropriate reference and also contain the appropriate parent
        when(rating3.get(RatingQueryField.IDENTIFIER.getFieldName())).thenReturn("rating3");
        when(this.solrUtils.get(RatingQueryField.ENTITY_REFERENCE.getFieldName(), rating3, EntityReference.class))
            .thenReturn(oldReference);
        when(this.solrUtils.getCollection(RatingQueryField.PARENTS_REFERENCE.getFieldName(), rating3,
            EntityReference.class))
            .thenReturn(Arrays.asList(mock(EntityReference.class), oldReference, mock(EntityReference.class)));

        // rating4 only contain the appropriate parent
        when(rating4.get(RatingQueryField.IDENTIFIER.getFieldName())).thenReturn("rating4");
        when(this.solrUtils.get(RatingQueryField.ENTITY_REFERENCE.getFieldName(), rating4, EntityReference.class))
            .thenReturn(mock(EntityReference.class));
        when(this.solrUtils.getCollection(RatingQueryField.PARENTS_REFERENCE.getFieldName(), rating4,
            EntityReference.class))
            .thenReturn(Arrays.asList(mock(EntityReference.class), mock(EntityReference.class), oldReference));

        when(this.documentList.iterator())
            .thenReturn(Arrays.asList(rating1, rating2, rating3, rating4).iterator());

        SolrQuery expectedQuery2 = new SolrQuery()
            .addFilterQuery(filterQuery)
            .setRows(100)
            .setStart(100)
            .setSort(RatingQueryField.CREATED_DATE.getFieldName(), SolrQuery.ORDER.asc);

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
        solrInputDocument1.setField(RatingQueryField.ENTITY_REFERENCE.getFieldName(),
            Collections.singletonMap(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, newReference));

        SolrInputDocument solrInputDocument2 = new SolrInputDocument();
        solrInputDocument2.setField("id", "rating2");
        solrInputDocument2.setField(RatingQueryField.PARENTS_REFERENCE.getFieldName(),
            Collections.singletonMap(SolrUtils.ATOMIC_UPDATE_MODIFIER_REMOVE, oldReference));
        solrInputDocument2.setField(RatingQueryField.PARENTS_REFERENCE.getFieldName(),
            Collections.singletonMap(SolrUtils.ATOMIC_UPDATE_MODIFIER_ADD, newReference));

        SolrInputDocument solrInputDocument3 = new SolrInputDocument();
        solrInputDocument3.setField("id", "rating3");
        solrInputDocument3.setField(RatingQueryField.ENTITY_REFERENCE.getFieldName(),
            Collections.singletonMap(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, newReference));
        solrInputDocument3.setField(RatingQueryField.PARENTS_REFERENCE.getFieldName(),
            Collections.singletonMap(SolrUtils.ATOMIC_UPDATE_MODIFIER_REMOVE, oldReference));
        solrInputDocument3.setField(RatingQueryField.PARENTS_REFERENCE.getFieldName(),
            Collections.singletonMap(SolrUtils.ATOMIC_UPDATE_MODIFIER_ADD, newReference));

        SolrInputDocument solrInputDocument4 = new SolrInputDocument();
        solrInputDocument4.setField("id", "rating4");
        solrInputDocument4.setField(RatingQueryField.PARENTS_REFERENCE.getFieldName(),
            Collections.singletonMap(SolrUtils.ATOMIC_UPDATE_MODIFIER_REMOVE, oldReference));
        solrInputDocument4.setField(RatingQueryField.PARENTS_REFERENCE.getFieldName(),
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

        when(this.configuration.isAverageStored()).thenReturn(true);
        this.manager.moveRatings(oldReference, newReference);
        verify(this.solrClient, times(4)).add(any(SolrInputDocument.class));
        verify(this.solrClient).commit();
        verify(this.averageRatingManager).moveAverageRatings(oldReference, newReference);
    }
}
