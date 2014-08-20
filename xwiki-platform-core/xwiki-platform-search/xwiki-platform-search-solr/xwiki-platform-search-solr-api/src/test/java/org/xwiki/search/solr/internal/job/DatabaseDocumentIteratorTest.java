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
package org.xwiki.search.solr.internal.job;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Unit tests for {@link DatabaseDocumentIterator}.
 * 
 * @version $Id$
 * @since 5.4.5
 */
public class DatabaseDocumentIteratorTest
{
    @Rule
    public MockitoComponentMockingRule<DocumentIterator<String>> mocker =
        new MockitoComponentMockingRule<DocumentIterator<String>>(DatabaseDocumentIterator.class);

    @Before
    public void configure() throws Exception
    {
        // We explicitly leave the list of wikis unsorted.
        Collection<String> wikiIds = Arrays.asList("chess", "tennis");

        WikiDescriptorManager wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        when(wikiDescriptorManager.getAllIds()).thenReturn(wikiIds);
    }

    @Test
    public void iterateAllWikis() throws Exception
    {
        Query emptyQuery = mock(Query.class);
        when(emptyQuery.execute()).thenReturn(Collections.emptyList());

        Query chessQuery = mock(Query.class);
        when(chessQuery.setOffset(0)).thenReturn(chessQuery);
        when(chessQuery.setOffset(100)).thenReturn(emptyQuery);
        when(chessQuery.execute()).thenReturn(Arrays.<Object> asList(
            new Object[] {"Blog", "WebHome", "", "3.2"},
            new Object[] {"Main", "Welcome", "en", "1.1"},
            new Object[] {"XWiki", "Syntax", "fr", "2.5"}));

        Query tennisQuery = mock(Query.class);
        when(tennisQuery.setOffset(0)).thenReturn(tennisQuery);
        when(tennisQuery.setOffset(100)).thenReturn(emptyQuery);
        when(tennisQuery.execute()).thenReturn(Arrays.<Object> asList(
            new Object[] {"Main", "Welcome", "en", "2.1"},
            new Object[] {"XWiki", "Syntax", "fr", "1.3"}));

        Query query = mock(Query.class);
        when(query.setLimit(anyInt())).thenReturn(query);
        when(query.getNamedParameters()).thenReturn(Collections.<String, Object> emptyMap());
        when(query.setWiki("chess")).thenReturn(chessQuery);
        when(query.setWiki("tennis")).thenReturn(tennisQuery);

        Query chessCountQuery = mock(Query.class);
        when(chessCountQuery.execute()).thenReturn(Collections.<Object> singletonList(3L));

        Query tennisCountQuery = mock(Query.class);
        when(tennisCountQuery.execute()).thenReturn(Collections.<Object> singletonList(2L));

        Query countQuery = mock(Query.class);
        when(countQuery.addFilter(mocker.<QueryFilter> getInstance(QueryFilter.class, "count"))).thenReturn(countQuery);
        when(countQuery.setWiki("chess")).thenReturn(chessCountQuery);
        when(countQuery.setWiki("tennis")).thenReturn(tennisCountQuery);

        QueryManager queryManager = mocker.getInstance(QueryManager.class);
        when(queryManager.createQuery("select doc.space, doc.name, doc.language, doc.version from XWikiDocument doc"
            + " order by doc.space, doc.name, doc.language", Query.HQL)).thenReturn(query);
        when(queryManager.createQuery("", Query.HQL)).thenReturn(countQuery);

        DocumentIterator<String> iterator = mocker.getComponentUnderTest();

        assertEquals(5L, iterator.size());

        List<Pair<DocumentReference, String>> actualResults = new ArrayList<Pair<DocumentReference, String>>();
        while (iterator.hasNext()) {
            actualResults.add(iterator.next());
        }

        List<Pair<DocumentReference, String>> expectedResults = new ArrayList<Pair<DocumentReference, String>>();
        expectedResults.add(new ImmutablePair<DocumentReference, String>(new DocumentReference("chess", "Blog",
            "WebHome"), "3.2"));
        expectedResults.add(new ImmutablePair<DocumentReference, String>(new DocumentReference("chess", "Main",
            "Welcome", Locale.ENGLISH), "1.1"));
        expectedResults.add(new ImmutablePair<DocumentReference, String>(new DocumentReference("chess", "XWiki",
            "Syntax", Locale.FRENCH), "2.5"));
        expectedResults.add(new ImmutablePair<DocumentReference, String>(new DocumentReference("tennis", "Main",
            "Welcome", Locale.ENGLISH), "2.1"));
        expectedResults.add(new ImmutablePair<DocumentReference, String>(new DocumentReference("tennis", "XWiki",
            "Syntax", Locale.FRENCH), "1.3"));

        assertEquals(expectedResults, actualResults);
    }

    @Test
    public void iterateOneWiki() throws Exception
    {
        DocumentReference rootReference = new DocumentReference("gang", "XWiki", "Syntax");

        Query emptyQuery = mock(Query.class);
        when(emptyQuery.execute()).thenReturn(Collections.emptyList());

        Query query = mock(Query.class);
        when(query.setLimit(anyInt())).thenReturn(query);
        when(query.setWiki(rootReference.getWikiReference().getName())).thenReturn(query);
        when(query.setOffset(0)).thenReturn(query);
        when(query.setOffset(100)).thenReturn(emptyQuery);
        when(query.execute()).thenReturn(Collections.<Object> singletonList(
            new Object[]{rootReference.getLastSpaceReference().getName(), rootReference.getName(), "de", "3.1"}));

        Map<String, Object> namedParameters = new HashMap<String, Object>();
        namedParameters.put("space", rootReference.getLastSpaceReference().getName());
        namedParameters.put("name", rootReference.getName());
        when(query.getNamedParameters()).thenReturn(namedParameters);

        Query countQuery = mock(Query.class);
        when(countQuery.addFilter(mocker.<QueryFilter> getInstance(QueryFilter.class, "count"))).thenReturn(countQuery);

        QueryManager queryManager = mocker.getInstance(QueryManager.class);
        String whereClause = " where doc.space = :space and doc.name = :name";
        when(queryManager.createQuery("select doc.space, doc.name, doc.language, doc.version from XWikiDocument doc"
            + whereClause + " order by doc.space, doc.name, doc.language", Query.HQL)).thenReturn(query);
        when(queryManager.createQuery(whereClause, Query.HQL)).thenReturn(countQuery);

        DocumentIterator<String> iterator = mocker.getComponentUnderTest();
        iterator.setRootReference(rootReference);

        List<Pair<DocumentReference, String>> actualResults = new ArrayList<Pair<DocumentReference, String>>();
        while (iterator.hasNext()) {
            actualResults.add(iterator.next());
        }

        List<Pair<DocumentReference, String>> expectedResults = new ArrayList<Pair<DocumentReference, String>>();
        expectedResults.add(new ImmutablePair<DocumentReference, String>(new DocumentReference(rootReference,
            Locale.GERMAN), "3.1"));

        assertEquals(expectedResults, actualResults);

        verify(query).bindValue("space", rootReference.getLastSpaceReference().getName());
        verify(query).bindValue("name", rootReference.getName());

        verify(countQuery).bindValue("space", rootReference.getLastSpaceReference().getName());
        verify(countQuery).bindValue("name", rootReference.getName());
    }
}
