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

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.ratings.Rating;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.SolrUtils;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SolrRatingsManager}.
 *
 * @version $Id$
 */
@ComponentTest
public class SolrRatingsManagerTest
{
    @InjectMockComponents
    private SolrRatingsManager solrRatingsManager;

    @MockComponent
    @Named("document")
    protected UserReferenceSerializer<DocumentReference> userReferenceSerializer;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    private SolrUtils solrUtils;

    @MockComponent
    private Solr solr;

    @Mock
    private SolrClient solrClient;

    DocumentReference ratedPageReference;
    DocumentReference fooDocumentReference;
    DocumentReference barDocumentReference;

    @Mock
    UserReference fooUserReference;

    @BeforeEach
    void beforeEach() throws SolrException
    {
        when(this.solr.getClient(RatingCoreSolrInitializer.NAME)).thenReturn(solrClient);
        when(solrUtils.get(any(), any())).then(invocationOnMock -> {
            String parameterName = invocationOnMock.getArgument(0);
            SolrDocument solrDocument = invocationOnMock.getArgument(1);
            return solrDocument.get(parameterName);
        });
        when(solrUtils.getId(any())).then(invocationOnMock -> {
            SolrDocument solrDocument = invocationOnMock.getArgument(0);
            return solrDocument.get("id");
        });

        ratedPageReference = new DocumentReference("xwiki", "Foo", "RatedPage");
        when(this.entityReferenceSerializer.serialize(ratedPageReference)).thenReturn("xwiki:Foo.RatedPage");
        when(this.documentReferenceResolver.resolve("xwiki:Foo.RatedPage")).thenReturn(ratedPageReference);

        fooDocumentReference = new DocumentReference("xwiki", "XWiki", "Foo");
        when(this.entityReferenceSerializer.serialize(fooDocumentReference)).thenReturn("xwiki:XWiki.Foo");
        when(this.documentReferenceResolver.resolve("xwiki:XWiki.Foo")).thenReturn(fooDocumentReference);

        barDocumentReference = new DocumentReference("xwiki", "XWiki", "Bar");
        when(this.entityReferenceSerializer.serialize(barDocumentReference)).thenReturn("xwiki:XWiki.Bar");
        when(this.documentReferenceResolver.resolve("xwiki:XWiki.Bar")).thenReturn(barDocumentReference);

        when(this.solrUtils.toFilterQueryString(any())).then(invocationOnMock -> {
            String value = invocationOnMock.getArgument(0);
            return value.replaceAll("\\:", "\\\\:");
        });

        when(this.userReferenceSerializer.serialize(fooUserReference)).thenReturn(fooDocumentReference);
    }

    @Test
    void getRatingsFromDocumentReference() throws Exception
    {
        int start = 12;
        int count = 20;

        QueryResponse queryResponse = mock(QueryResponse.class);
        SolrQuery solrQuery = new SolrQuery()
            .addFilterQuery("filter(parent:xwiki\\:Foo.RatedPage)")
            .setStart(start)
            .setSort("id", SolrQuery.ORDER.asc)
            .setRows(count);
        when(this.solrClient.query(any())).then(invocationOnMock -> {
            SolrQuery query = invocationOnMock.getArgument(0);
            if (solrQuery.toQueryString().equals(query.toQueryString())) {
                return queryResponse;
            }
            return null;
        });

        SolrDocumentList solrDocumentList = new SolrDocumentList();
        Map<String, Object> document = new HashMap<>();
        document.put("id", "id-doc-1");
        document.put("parent", "xwiki:Foo.RatedPage");
        document.put("ratingId", 1);
        document.put("date", Date.valueOf("2020-05-01"));
        document.put("author", "xwiki:XWiki.Bar");
        document.put("vote", 4);
        solrDocumentList.add(new SolrDocument(document));

        document = new HashMap<>();
        document.put("id", "id-doc-2");
        document.put("parent", "xwiki:Foo.RatedPage");
        document.put("ratingId", 12);
        document.put("date", Date.valueOf("2018-12-26"));
        document.put("author", "xwiki:XWiki.Foo");
        document.put("vote", 1);
        solrDocumentList.add(new SolrDocument(document));

        document = new HashMap<>();
        document.put("id", "id-doc-3");
        document.put("parent", "xwiki:Foo.RatedPage");
        document.put("ratingId", 31);
        document.put("date", Date.valueOf("2020-07-21"));
        document.put("author", "xwiki:XWiki.Bar");
        document.put("vote", 3);
        solrDocumentList.add(new SolrDocument(document));

        when(queryResponse.getResults()).thenReturn(solrDocumentList);

        DocumentReference fooDocumentReference = new DocumentReference("xwiki", "XWiki", "Foo");
        DocumentReference barDocumentReference = new DocumentReference("xwiki", "XWiki", "Bar");
        when(this.documentReferenceResolver.resolve("xwiki:XWiki.Foo")).thenReturn(fooDocumentReference);
        when(this.documentReferenceResolver.resolve("xwiki:XWiki.Bar")).thenReturn(barDocumentReference);

        List<Rating> expectedRatings = new ArrayList<>();
        SolrRating rating = new SolrRating(ratedPageReference, "id-doc-1", 1);
        rating.setDate(Date.valueOf("2020-05-01"));
        rating.setVote(4);
        rating.setAuthor(barDocumentReference);
        expectedRatings.add(rating);

        rating = new SolrRating(ratedPageReference, "id-doc-2", 12);
        rating.setDate(Date.valueOf("2018-12-26"));
        rating.setVote(1);
        rating.setAuthor(fooDocumentReference);
        expectedRatings.add(rating);

        rating = new SolrRating(ratedPageReference, "id-doc-3", 31);
        rating.setDate(Date.valueOf("2020-07-21"));
        rating.setVote(3);
        rating.setAuthor(barDocumentReference);
        expectedRatings.add(rating);

        assertEquals(expectedRatings, this.solrRatingsManager.getRatings(ratedPageReference, start, count, true));
        verify(this.solrClient).query(any());
    }

    @Test
    void getRatingsFromDocumentReferenceWithCount0() throws Exception
    {
        int start = 0;
        int count = 0;

        QueryResponse queryResponse = mock(QueryResponse.class);
        SolrQuery solrQuery = new SolrQuery()
            .addFilterQuery("filter(parent:xwiki\\:Foo.RatedPage)")
            .setStart(start)
            .setSort("id", SolrQuery.ORDER.asc);
        when(this.solrClient.query(any())).then(invocationOnMock -> {
            SolrQuery query = invocationOnMock.getArgument(0);
            if (solrQuery.toQueryString().equals(query.toQueryString())) {
                return queryResponse;
            }
            return null;
        });

        SolrDocumentList solrDocumentList = new SolrDocumentList();
        Map<String, Object> document = new HashMap<>();
        document.put("id", "id-doc-1");
        document.put("parent", "xwiki:Foo.RatedPage");
        document.put("ratingId", 1);
        document.put("date", Date.valueOf("2020-05-01"));
        document.put("author", "xwiki:XWiki.Bar");
        document.put("vote", 4);
        solrDocumentList.add(new SolrDocument(document));

        document = new HashMap<>();
        document.put("id", "id-doc-2");
        document.put("parent", "xwiki:Foo.RatedPage");
        document.put("ratingId", 12);
        document.put("date", Date.valueOf("2018-12-26"));
        document.put("author", "xwiki:XWiki.Foo");
        document.put("vote", 1);
        solrDocumentList.add(new SolrDocument(document));

        document = new HashMap<>();
        document.put("id", "id-doc-3");
        document.put("parent", "xwiki:Foo.RatedPage");
        document.put("ratingId", 31);
        document.put("date", Date.valueOf("2020-07-21"));
        document.put("author", "xwiki:XWiki.Bar");
        document.put("vote", 3);
        solrDocumentList.add(new SolrDocument(document));

        when(queryResponse.getResults()).thenReturn(solrDocumentList);

        DocumentReference fooDocumentReference = new DocumentReference("xwiki", "XWiki", "Foo");
        DocumentReference barDocumentReference = new DocumentReference("xwiki", "XWiki", "Bar");
        when(this.documentReferenceResolver.resolve("xwiki:XWiki.Foo")).thenReturn(fooDocumentReference);
        when(this.documentReferenceResolver.resolve("xwiki:XWiki.Bar")).thenReturn(barDocumentReference);

        List<Rating> expectedRatings = new ArrayList<>();
        SolrRating rating = new SolrRating(ratedPageReference, "id-doc-1", 1);
        rating.setDate(Date.valueOf("2020-05-01"));
        rating.setVote(4);
        rating.setAuthor(barDocumentReference);
        expectedRatings.add(rating);

        rating = new SolrRating(ratedPageReference, "id-doc-2", 12);
        rating.setDate(Date.valueOf("2018-12-26"));
        rating.setVote(1);
        rating.setAuthor(fooDocumentReference);
        expectedRatings.add(rating);

        rating = new SolrRating(ratedPageReference, "id-doc-3", 31);
        rating.setDate(Date.valueOf("2020-07-21"));
        rating.setVote(3);
        rating.setAuthor(barDocumentReference);
        expectedRatings.add(rating);

        assertEquals(expectedRatings, this.solrRatingsManager.getRatings(ratedPageReference, start, count, true));
        verify(this.solrClient).query(any());
    }

    @Test
    void getRatingsFromUserReference() throws Exception
    {
        int start = 48;
        int count = 2;

        QueryResponse queryResponse = mock(QueryResponse.class);
        SolrQuery solrQuery = new SolrQuery()
            .addFilterQuery("filter(author:xwiki\\:XWiki.Foo)")
            .setStart(start)
            .setSort("id", SolrQuery.ORDER.desc)
            .setRows(count);

        when(this.solrClient.query(any())).then(invocationOnMock -> {
            SolrQuery query = invocationOnMock.getArgument(0);
            if (solrQuery.toQueryString().equals(query.toQueryString())) {
                return queryResponse;
            }
            return null;
        });

        SolrDocumentList solrDocumentList = new SolrDocumentList();
        Map<String, Object> document = new HashMap<>();
        document.put("id", "id-doc-1");
        document.put("parent", "xwiki:Foo.RatedPage");
        document.put("ratingId", 1);
        document.put("date", Date.valueOf("2020-05-01"));
        document.put("author", "xwiki:XWiki.Foo");
        document.put("vote", 4);
        solrDocumentList.add(new SolrDocument(document));

        document = new HashMap<>();
        document.put("id", "id-doc-2");
        document.put("parent", "xwiki:XWiki.Bar");
        document.put("ratingId", 12);
        document.put("date", Date.valueOf("2018-12-26"));
        document.put("author", "xwiki:XWiki.Foo");
        document.put("vote", 1);
        solrDocumentList.add(new SolrDocument(document));

        when(queryResponse.getResults()).thenReturn(solrDocumentList);

        List<Rating> expectedRatings = new ArrayList<>();
        SolrRating rating = new SolrRating(ratedPageReference, "id-doc-1", 1);
        rating.setDate(Date.valueOf("2020-05-01"));
        rating.setVote(4);
        rating.setAuthor(fooDocumentReference);
        expectedRatings.add(rating);

        rating = new SolrRating(barDocumentReference, "id-doc-2", 12);
        rating.setDate(Date.valueOf("2018-12-26"));
        rating.setVote(1);
        rating.setAuthor(fooDocumentReference);
        expectedRatings.add(rating);

        assertEquals(expectedRatings, this.solrRatingsManager.getRatings(fooUserReference, start, count, false));
        verify(this.solrClient).query(any());
    }

    @Test
    void getRatingGlobalId() throws Exception
    {
        QueryResponse queryResponse = mock(QueryResponse.class);
        SolrQuery solrQuery = new SolrQuery()
            .addFilterQuery("filter(id:myId)")
            .setStart(0)
            .setSort("id", SolrQuery.ORDER.asc)
            .setRows(1);

        when(this.solrClient.query(any())).then(invocationOnMock -> {
            SolrQuery query = invocationOnMock.getArgument(0);
            if (solrQuery.toQueryString().equals(query.toQueryString())) {
                return queryResponse;
            }
            return null;
        });

        SolrDocumentList solrDocumentList = new SolrDocumentList();
        Map<String, Object> document = new HashMap<>();
        document.put("id", "myId");
        document.put("parent", "xwiki:Foo.RatedPage");
        document.put("ratingId", 1);
        document.put("date", Date.valueOf("2020-05-01"));
        document.put("author", "xwiki:XWiki.Foo");
        document.put("vote", 4);
        solrDocumentList.add(new SolrDocument(document));

        when(queryResponse.getResults()).thenReturn(solrDocumentList);

        SolrRating rating = new SolrRating(ratedPageReference, "myId", 1);
        rating.setDate(Date.valueOf("2020-05-01"));
        rating.setVote(4);
        rating.setAuthor(fooDocumentReference);

        assertEquals(rating, this.solrRatingsManager.getRating("myId"));
        verify(this.solrClient).query(any());
    }

    @Test
    void getRatingDocumentAndLocalId() throws Exception
    {
        QueryResponse queryResponse = mock(QueryResponse.class);
        SolrQuery solrQuery = new SolrQuery()
            .addFilterQuery("filter(parent:xwiki\\:XWiki.Foo) AND filter(ratingId:12)")
            .setStart(0)
            .setSort("id", SolrQuery.ORDER.asc)
            .setRows(1);

        when(this.solrClient.query(any())).then(invocationOnMock -> {
            SolrQuery query = invocationOnMock.getArgument(0);
            if (solrQuery.toQueryString().equals(query.toQueryString())) {
                return queryResponse;
            }
            return null;
        });

        SolrDocumentList solrDocumentList = new SolrDocumentList();
        Map<String, Object> document = new HashMap<>();
        document.put("id", "myId-12");
        document.put("parent", "xwiki:XWiki.Foo");
        document.put("ratingId", 12);
        document.put("date", Date.valueOf("2020-05-01"));
        document.put("author", "xwiki:XWiki.Bar");
        document.put("vote", 0);
        solrDocumentList.add(new SolrDocument(document));

        when(queryResponse.getResults()).thenReturn(solrDocumentList);

        SolrRating rating = new SolrRating(fooDocumentReference, "myId-12", 12);
        rating.setDate(Date.valueOf("2020-05-01"));
        rating.setVote(0);
        rating.setAuthor(barDocumentReference);

        assertEquals(rating, this.solrRatingsManager.getRating(fooDocumentReference, 12));
        verify(this.solrClient).query(any());
    }

    @Test
    void getRatingDocumentAndAuthor() throws Exception
    {
        QueryResponse queryResponse = mock(QueryResponse.class);
        SolrQuery solrQuery = new SolrQuery()
            .addFilterQuery("filter(parent:xwiki\\:XWiki.Foo) AND filter(author:xwiki\\:XWiki.Bar)")
            .setStart(0)
            .setSort("id", SolrQuery.ORDER.asc)
            .setRows(1);

        when(this.solrClient.query(any())).then(invocationOnMock -> {
            SolrQuery query = invocationOnMock.getArgument(0);
            if (solrQuery.toQueryString().equals(query.toQueryString())) {
                return queryResponse;
            }
            return null;
        });

        SolrDocumentList solrDocumentList = new SolrDocumentList();
        Map<String, Object> document = new HashMap<>();
        document.put("id", "myId-12");
        document.put("parent", "xwiki:XWiki.Foo");
        document.put("ratingId", 12);
        document.put("date", Date.valueOf("2020-05-01"));
        document.put("author", "xwiki:XWiki.Bar");
        document.put("vote", 0);
        solrDocumentList.add(new SolrDocument(document));

        when(queryResponse.getResults()).thenReturn(solrDocumentList);

        SolrRating rating = new SolrRating(fooDocumentReference, "myId-12", 12);
        rating.setDate(Date.valueOf("2020-05-01"));
        rating.setVote(0);
        rating.setAuthor(barDocumentReference);

        assertEquals(rating, this.solrRatingsManager.getRating(fooDocumentReference, barDocumentReference));
        verify(this.solrClient).query(any());
    }

    /**
     * Test setting a rating to a document that has never been rated.
     */
    @Test
    void setRatingNewAuthorNewDoc() throws Exception
    {
        SolrInputDocument expectedInputDocument = new SolrInputDocument();
        doAnswer(invocationOnMock -> {
            String fieldName = invocationOnMock.getArgument(0);
            Object fieldValue = invocationOnMock.getArgument(1);
            SolrInputDocument inputDocument = invocationOnMock.getArgument(2);
            if (fieldName.equals("date")) {
                assertNotNull(fieldValue);
                assertTrue(fieldValue instanceof java.util.Date);
                expectedInputDocument.setField("date", fieldValue);
            }
            inputDocument.setField(fieldName, fieldValue);
            return null;
        }).when(solrUtils).set(any(String.class), any(Object.class), any(SolrInputDocument.class));

        doAnswer(invocationOnMock -> {
            Object fieldValue = invocationOnMock.getArgument(0);
            SolrInputDocument inputDocument = invocationOnMock.getArgument(1);
            inputDocument.setField("id", fieldValue);
            return null;
        }).when(solrUtils).setId(any(), any());


        expectedInputDocument.setField("id", "xwiki:Foo.RatedPage_0");
        expectedInputDocument.setField("ratingId", 0);
        expectedInputDocument.setField("parent", "xwiki:Foo.RatedPage");
        expectedInputDocument.setField("author", "xwiki:XWiki.Bar");
        expectedInputDocument.setField("vote", 3);

        doAnswer(invocationOnMock -> {
            SolrInputDocument inputDoc = invocationOnMock.getArgument(0);
            assertEquals(expectedInputDocument.toString(), inputDoc.toString());
            return null;
        }).when(this.solrClient).add(any(SolrInputDocument.class));

        QueryResponse queryResponse = mock(QueryResponse.class);
        SolrQuery solrQuery = new SolrQuery()
            .addFilterQuery("filter(id:xwiki\\:Foo.RatedPage_0)")
            .setStart(0)
            .setSort("id", SolrQuery.ORDER.asc)
            .setRows(1);

        when(this.solrClient.query(any())).then(invocationOnMock -> {
            SolrQuery query = invocationOnMock.getArgument(0);
            if (solrQuery.toQueryString().equals(query.toQueryString())) {
                return queryResponse;
            }
            return mock(QueryResponse.class);
        });

        SolrDocumentList solrDocumentList = new SolrDocumentList();
        Map<String, Object> document = new HashMap<>();
        document.put("id", "xwiki:Foo.RatedPage_0");
        document.put("parent", "xwiki:Foo.RatedPage");
        document.put("ratingId", 0);
        document.put("date", Date.valueOf("2020-05-01"));
        document.put("author", "xwiki:XWiki.Bar");
        document.put("vote", 3);
        solrDocumentList.add(new SolrDocument(document));

        when(queryResponse.getResults()).thenReturn(solrDocumentList);

        SolrRating rating = new SolrRating(ratedPageReference, "xwiki:Foo.RatedPage_0", 0);
        rating.setDate(Date.valueOf("2020-05-01"));
        rating.setVote(3);
        rating.setAuthor(barDocumentReference);

        assertEquals(rating, this.solrRatingsManager.setRating(ratedPageReference, barDocumentReference, 3));

        // 3 times:
        //  - first: to check if the author already voted for the doc
        //  - second: to retrieve the ratingId if anyone already voted for the doc
        //  - third: to retrieve the actual rating performed after the vote is committed.
        verify(this.solrClient, times(3)).query(any());
        verify(this.solrClient).add(any(SolrInputDocument.class));
        verify(this.solrClient).commit();
    }

    /**
     * Test setting a rating to a document that has already been rated
     */
    @Test
    void setRatingNewAuthorDoc() throws Exception
    {
        SolrInputDocument expectedInputDocument = new SolrInputDocument();
        doAnswer(invocationOnMock -> {
            String fieldName = invocationOnMock.getArgument(0);
            Object fieldValue = invocationOnMock.getArgument(1);
            SolrInputDocument inputDocument = invocationOnMock.getArgument(2);
            if (fieldName.equals("date")) {
                assertNotNull(fieldValue);
                assertTrue(fieldValue instanceof java.util.Date);
                expectedInputDocument.setField("date", fieldValue);
            }
            inputDocument.setField(fieldName, fieldValue);
            return null;
        }).when(solrUtils).set(any(String.class), any(Object.class), any(SolrInputDocument.class));

        QueryResponse ratingIdQueryResponse = mock(QueryResponse.class);
        SolrQuery ratingIdSolrQuery = new SolrQuery()
            .addFilterQuery("filter(parent:xwiki\\:Foo.RatedPage)")
            .setStart(0)
            .setSort("id", SolrQuery.ORDER.desc)
            .setRows(1);

        SolrDocumentList solrDocumentList = new SolrDocumentList();
        Map<String, Object> document = new HashMap<>();
        document.put("id", "xwiki:Foo.RatedPage_42");
        document.put("parent", "xwiki:Foo.RatedPage");
        document.put("ratingId", 42);
        document.put("date", Date.valueOf("2020-05-01"));
        document.put("author", "xwiki:XWiki.Foo");
        document.put("vote", 1);
        solrDocumentList.add(new SolrDocument(document));

        when(ratingIdQueryResponse.getResults()).thenReturn(solrDocumentList);

        doAnswer(invocationOnMock -> {
            Object fieldValue = invocationOnMock.getArgument(0);
            SolrInputDocument inputDocument = invocationOnMock.getArgument(1);
            inputDocument.setField("id", fieldValue);
            return null;
        }).when(solrUtils).setId(any(), any());


        expectedInputDocument.setField("id", "xwiki:Foo.RatedPage_43");
        expectedInputDocument.setField("ratingId", 43);
        expectedInputDocument.setField("parent", "xwiki:Foo.RatedPage");
        expectedInputDocument.setField("author", "xwiki:XWiki.Bar");
        expectedInputDocument.setField("vote", 5);

        doAnswer(invocationOnMock -> {
            SolrInputDocument inputDoc = invocationOnMock.getArgument(0);
            assertEquals(expectedInputDocument.toString(), inputDoc.toString());
            return null;
        }).when(this.solrClient).add(any(SolrInputDocument.class));

        QueryResponse finalQueryResponse = mock(QueryResponse.class);
        SolrQuery finalSolrQuery = new SolrQuery()
            .addFilterQuery("filter(id:xwiki\\:Foo.RatedPage_43)")
            .setStart(0)
            .setSort("id", SolrQuery.ORDER.asc)
            .setRows(1);

        when(this.solrClient.query(any())).then(invocationOnMock -> {
            SolrQuery query = invocationOnMock.getArgument(0);
            if (ratingIdSolrQuery.toQueryString().equals(query.toQueryString())) {
                return ratingIdQueryResponse;
            } else if (finalSolrQuery.toQueryString().equals(query.toQueryString())) {
                return finalQueryResponse;
            }
            return mock(QueryResponse.class);
        });

        solrDocumentList = new SolrDocumentList();
        document = new HashMap<>();
        document.put("id", "xwiki:Foo.RatedPage_43");
        document.put("parent", "xwiki:Foo.RatedPage");
        document.put("ratingId", 43);
        document.put("date", Date.valueOf("2020-05-01"));
        document.put("author", "xwiki:XWiki.Bar");
        document.put("vote", 5);
        solrDocumentList.add(new SolrDocument(document));

        when(finalQueryResponse.getResults()).thenReturn(solrDocumentList);

        SolrRating rating = new SolrRating(ratedPageReference, "xwiki:Foo.RatedPage_43", 43);
        rating.setDate(Date.valueOf("2020-05-01"));
        rating.setVote(5);
        rating.setAuthor(barDocumentReference);

        assertEquals(rating, this.solrRatingsManager.setRating(ratedPageReference, barDocumentReference, 5));

        // 3 times:
        //  - first: to check if the author already voted for the doc
        //  - second: to retrieve the ratingId if anyone already voted for the doc
        //  - third: to retrieve the actual rating performed after the vote is committed.
        verify(this.solrClient, times(3)).query(any());
        verify(this.solrClient).add(any(SolrInputDocument.class));
        verify(this.solrClient).commit();
    }

    /**
     * Test setting a rating to a document that the author already rated
     */
    @Test
    void setRatingAuthorDoc() throws Exception
    {
        SolrInputDocument expectedInputDocument = new SolrInputDocument();
        doAnswer(invocationOnMock -> {
            String fieldName = invocationOnMock.getArgument(0);
            Object fieldValue = invocationOnMock.getArgument(1);
            SolrInputDocument inputDocument = invocationOnMock.getArgument(2);
            if (fieldName.equals("date")) {
                assertNotNull(fieldValue);
                assertTrue(fieldValue instanceof java.util.Date);
                expectedInputDocument.setField("date", fieldValue);
            }
            inputDocument.setField(fieldName, fieldValue);
            return null;
        }).when(solrUtils).set(any(String.class), any(Object.class), any(SolrInputDocument.class));

        QueryResponse originalRatingQueryResponse = mock(QueryResponse.class);
        SolrQuery originalRatingSolrQuery = new SolrQuery()
            .addFilterQuery("filter(parent:xwiki\\:Foo.RatedPage) AND filter(author:xwiki\\:XWiki.Bar)")
            .setStart(0)
            .setSort("id", SolrQuery.ORDER.asc)
            .setRows(1);

        SolrDocumentList solrDocumentList = new SolrDocumentList();
        Map<String, Object> document = new HashMap<>();
        document.put("id", "xwiki:Foo.RatedPage_42");
        document.put("parent", "xwiki:Foo.RatedPage");
        document.put("ratingId", 42);
        document.put("date", Date.valueOf("2020-05-01"));
        document.put("author", "xwiki:XWiki.Bar");
        document.put("vote", 2);
        solrDocumentList.add(new SolrDocument(document));

        when(originalRatingQueryResponse.getResults()).thenReturn(solrDocumentList);

        doAnswer(invocationOnMock -> {
            Object fieldValue = invocationOnMock.getArgument(0);
            SolrInputDocument inputDocument = invocationOnMock.getArgument(1);
            inputDocument.setField("id", fieldValue);
            return null;
        }).when(solrUtils).setId(any(), any());


        expectedInputDocument.setField("id", "xwiki:Foo.RatedPage_42");
        expectedInputDocument.setField("ratingId", 42);
        expectedInputDocument.setField("parent", "xwiki:Foo.RatedPage");
        expectedInputDocument.setField("author", "xwiki:XWiki.Bar");
        expectedInputDocument.setField("vote", 1);

        doAnswer(invocationOnMock -> {
            SolrInputDocument inputDoc = invocationOnMock.getArgument(0);
            assertEquals(expectedInputDocument.toString(), inputDoc.toString());
            return null;
        }).when(this.solrClient).add(any(SolrInputDocument.class));

        QueryResponse finalQueryResponse = mock(QueryResponse.class);
        SolrQuery finalSolrQuery = new SolrQuery()
            .addFilterQuery("filter(id:xwiki\\:Foo.RatedPage_42)")
            .setStart(0)
            .setSort("id", SolrQuery.ORDER.asc)
            .setRows(1);

        when(this.solrClient.query(any())).then(invocationOnMock -> {
            SolrQuery query = invocationOnMock.getArgument(0);
            if (originalRatingSolrQuery.toQueryString().equals(query.toQueryString())) {
                return originalRatingQueryResponse;
            } else if (finalSolrQuery.toQueryString().equals(query.toQueryString())) {
                return finalQueryResponse;
            }
            return mock(QueryResponse.class);
        });

        solrDocumentList = new SolrDocumentList();
        document = new HashMap<>();
        document.put("id", "xwiki:Foo.RatedPage_42");
        document.put("parent", "xwiki:Foo.RatedPage");
        document.put("ratingId", 42);
        document.put("date", Date.valueOf("2020-05-01"));
        document.put("author", "xwiki:XWiki.Bar");
        document.put("vote", 1);
        solrDocumentList.add(new SolrDocument(document));

        when(finalQueryResponse.getResults()).thenReturn(solrDocumentList);

        SolrRating rating = new SolrRating(ratedPageReference, "xwiki:Foo.RatedPage_42", 42);
        rating.setDate(Date.valueOf("2020-05-01"));
        rating.setVote(1);
        rating.setAuthor(barDocumentReference);

        assertEquals(rating, this.solrRatingsManager.setRating(ratedPageReference, barDocumentReference, 1));

        // 2 times:
        //  - first: to check if the author already voted for the doc
        //  - second: to retrieve the actual rating performed after the vote is committed.
        verify(this.solrClient, times(2)).query(any());
        verify(this.solrClient).add(any(SolrInputDocument.class));
        verify(this.solrClient).commit();
    }

    @Test
    void removeRating() throws Exception
    {
        Rating rating = mock(Rating.class);
        when(rating.getGlobalRatingId()).thenReturn("globalRatingId");
        UpdateResponse updateResponse = mock(UpdateResponse.class);
        when(this.solrClient.deleteById("globalRatingId")).thenReturn(updateResponse);
        when(updateResponse.getStatus()).thenReturn(200);
        assertTrue(this.solrRatingsManager.removeRating(rating));
        verify(this.solrClient).deleteById("globalRatingId");

        when(updateResponse.getStatus()).thenReturn(400);
        assertFalse(this.solrRatingsManager.removeRating(rating));
        verify(this.solrClient, times(2)).deleteById("globalRatingId");
    }
}
