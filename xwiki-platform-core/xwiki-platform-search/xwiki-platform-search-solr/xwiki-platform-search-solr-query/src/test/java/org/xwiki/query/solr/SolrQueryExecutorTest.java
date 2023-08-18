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
package org.xwiki.query.solr;

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.SolrParams;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.component.internal.ContextComponentManagerProvider;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.QueryExecutor;
import org.xwiki.query.QueryManager;
import org.xwiki.query.internal.DefaultQuery;
import org.xwiki.query.internal.DefaultQueryExecutorManager;
import org.xwiki.query.internal.DefaultQueryManager;
import org.xwiki.query.solr.internal.SolrQueryExecutor;
import org.xwiki.search.solr.internal.api.SolrInstance;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcoreRule;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Basic test for the {@link SolrQueryExecutor}.
 * 
 * @version $Id$
 */
@ComponentList({DefaultQueryManager.class, DefaultQueryExecutorManager.class, ContextComponentManagerProvider.class})
@ReferenceComponentList
public class SolrQueryExecutorTest
{
    private static final String ITERABLE_PARAM_NAME = "multiParam";

    private static final String[] ITERABLE_PARAM_EXPECTED = { "value1", "value2" };

    private static final Iterable<String> ITERABLE_PARAM_VALUE = Arrays.asList(ITERABLE_PARAM_EXPECTED);

    private static final String INT_ARR_PARAM_NAME = "intArrayParam";

    private static final String[] INT_ARR_PARAM_EXPECTED = { "-42", "4711" };

    private static final int[] INT_ARR_PARAM_VALUE = { -42, 4711 };

    private static final String STR_ARR_PARAM_NAME = "stringArrayParam";

    private static final String[] STR_ARR_PARAM_EXPECTED = { "valueA", "valueB" };

    private static final String[] STR_ARR_PARAM_VALUE = STR_ARR_PARAM_EXPECTED;

    private static final String SINGLE_PARAM_NAME = "singleParam";

    private static final Object SINGLE_PARAM_VALUE = new Object();

    private static final Object SINGLE_PARAM_EXPECTED = SINGLE_PARAM_VALUE.toString();

    public final MockitoComponentMockingRule<QueryExecutor> componentManager =
        new MockitoComponentMockingRule<QueryExecutor>(SolrQueryExecutor.class);

    @Rule
    public final MockitoOldcoreRule oldCore = new MockitoOldcoreRule(this.componentManager);

    private SolrInstance solr;

    @Before
    public void configure() throws Exception
    {
        this.solr = this.componentManager.registerMockComponent(SolrInstance.class);
    }

    @Test
    public void testExecutorRegistration() throws Exception
    {
        QueryManager queryManager = this.componentManager.getInstance(QueryManager.class);

        Assert.assertTrue(queryManager.getLanguages().contains(SolrQueryExecutor.SOLR));
    }

    @Test
    public void testMultiValuedQueryArgs() throws Exception
    {
        when(solr.query(any(SolrQuery.class))).then(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                SolrQuery solrQuery = (SolrQuery) invocation.getArguments()[0];

                Assert.assertArrayEquals(ITERABLE_PARAM_EXPECTED, solrQuery.getParams(ITERABLE_PARAM_NAME));
                Assert.assertArrayEquals(INT_ARR_PARAM_EXPECTED, solrQuery.getParams(INT_ARR_PARAM_NAME));
                Assert.assertArrayEquals(STR_ARR_PARAM_EXPECTED, solrQuery.getParams(STR_ARR_PARAM_NAME));
                Assert.assertEquals(SINGLE_PARAM_EXPECTED, solrQuery.get(SINGLE_PARAM_NAME));

                // Check that the default list of supported locales is taken from the wiki configuration.
                Assert.assertEquals("en,fr,de", solrQuery.get("xwiki.supportedLocales"));

                QueryResponse r = mock(QueryResponse.class);
                when(r.getResults()).thenReturn(new SolrDocumentList());
                return r;
            }
        });

        DefaultQuery query = new DefaultQuery("TestQuery", null);
        query.bindValue(ITERABLE_PARAM_NAME, ITERABLE_PARAM_VALUE);
        query.bindValue(INT_ARR_PARAM_NAME, INT_ARR_PARAM_VALUE);
        query.bindValue(STR_ARR_PARAM_NAME, STR_ARR_PARAM_VALUE);
        query.bindValue(SINGLE_PARAM_NAME, SINGLE_PARAM_VALUE);

        // The default list of supported locales should be taken from the wiki configuration.
        XWikiContext xcontext = this.oldCore.getXWikiContext();
        doReturn(Arrays.asList(Locale.ENGLISH, Locale.FRENCH, Locale.GERMAN)).when(this.oldCore.getSpyXWiki())
            .getAvailableLocales(xcontext);

        this.componentManager.getComponentUnderTest().execute(query);
    }

    @Test
    public void filterResponse() throws Exception
    {
        ParameterizedType resolverType =
            new DefaultParameterizedType(null, DocumentReferenceResolver.class, SolrDocument.class);
        DocumentReferenceResolver<SolrDocument> resolver = this.componentManager.getInstance(resolverType);

        AuthorizationManager authorizationManager = this.componentManager.getInstance(AuthorizationManager.class);

        DocumentReference currentUserReference = new DocumentReference("xwiki", "XWiki", "currentuser");
        this.oldCore.getXWikiContext().setUserReference(currentUserReference);

        DocumentReference currentAuthorReference = new DocumentReference("xwiki", "XWiki", "currentauthor");
        XWikiDocument currentDocument = new XWikiDocument(currentAuthorReference);
        currentDocument.setContentAuthorReference(currentAuthorReference);
        this.oldCore.getXWikiContext().setDoc(currentDocument);

        DocumentReference aliceReference = new DocumentReference("wiki", "Users", "Alice");
        when(authorizationManager.hasAccess(Right.VIEW, currentAuthorReference, aliceReference)).thenReturn(true);
        SolrDocument alice = new SolrDocument();
        when(resolver.resolve(alice)).thenReturn(aliceReference);

        DocumentReference bobReference = new DocumentReference("wiki", "Users", "Bob");
        when(authorizationManager.hasAccess(Right.VIEW, currentUserReference, bobReference)).thenReturn(true);
        when(authorizationManager.hasAccess(Right.VIEW, currentAuthorReference, bobReference)).thenReturn(true);
        SolrDocument bob = new SolrDocument();
        when(resolver.resolve(bob)).thenReturn(bobReference);

        DocumentReference carolReference = new DocumentReference("wiki", "Users", "Carol");
        when(authorizationManager.hasAccess(Right.VIEW, currentUserReference, carolReference)).thenReturn(true);
        SolrDocument carol = new SolrDocument();
        when(resolver.resolve(carol)).thenReturn(carolReference);

        SolrDocumentList sourceResults = new SolrDocumentList();
        sourceResults.addAll(Arrays.asList(alice, bob, carol));
        sourceResults.setNumFound(3);

        QueryResponse response = mock(QueryResponse.class);
        when(this.solr.query(any(SolrParams.class))).thenReturn(response);

        DefaultQuery query = new DefaultQuery("", null);

        // No right check

        when(response.getResults()).thenReturn((SolrDocumentList) sourceResults.clone());

        SolrDocumentList results =
            ((QueryResponse) this.componentManager.getComponentUnderTest().execute(query).get(0)).getResults();
        assertEquals(Arrays.asList(alice, bob, carol), results);

        // Check current user right

        query.checkCurrentUser(true);
        when(response.getResults()).thenReturn((SolrDocumentList) sourceResults.clone());

        results = ((QueryResponse) this.componentManager.getComponentUnderTest().execute(query).get(0)).getResults();
        assertEquals(Arrays.asList(bob, carol), results);

        // Check both current user and author rights

        query.checkCurrentAuthor(true);
        when(response.getResults()).thenReturn((SolrDocumentList) sourceResults.clone());

        results = ((QueryResponse) this.componentManager.getComponentUnderTest().execute(query).get(0)).getResults();
        assertEquals(Arrays.asList(bob), results);

        // Check current author right

        query.checkCurrentUser(false);
        when(response.getResults()).thenReturn((SolrDocumentList) sourceResults.clone());

        results = ((QueryResponse) this.componentManager.getComponentUnderTest().execute(query).get(0)).getResults();
        assertEquals(Arrays.asList(alice, bob), results);
    }

    @Test
    public void filterResponseWithException() throws Exception
    {
        ParameterizedType resolverType =
            new DefaultParameterizedType(null, DocumentReferenceResolver.class, SolrDocument.class);
        DocumentReferenceResolver<SolrDocument> resolver = this.componentManager.getInstance(resolverType);

        AuthorizationManager authorizationManager = this.componentManager.getInstance(AuthorizationManager.class);

        DocumentReference currentUserReference = new DocumentReference("xwiki", "XWiki", "currentuser");
        this.oldCore.getXWikiContext().setUserReference(currentUserReference);

        DocumentReference aliceReference = new DocumentReference("wiki", "Users", "Alice");
        SolrDocument alice = new SolrDocument();
        when(resolver.resolve(alice)).thenReturn(aliceReference);

        DocumentReference bobReference = new DocumentReference("wiki", "Users", "Bob");
        when(authorizationManager.hasAccess(Right.VIEW, currentUserReference, bobReference)).thenReturn(true);
        SolrDocument bob = new SolrDocument();
        when(resolver.resolve(bob)).thenReturn(bobReference);

        SolrDocumentList sourceResults = new SolrDocumentList();
        sourceResults.addAll(Arrays.asList(alice, bob));
        sourceResults.setNumFound(2);

        QueryResponse response = mock(QueryResponse.class);
        when(this.solr.query(any(SolrParams.class))).thenReturn(response);

        DefaultQuery query = new DefaultQuery("", null);

        // No right check, verify that the setup works.
        when(response.getResults()).thenReturn((SolrDocumentList) sourceResults.clone());
        SolrDocumentList results =
            ((QueryResponse) this.componentManager.getComponentUnderTest().execute(query).get(0)).getResults();
        assertEquals(Arrays.asList(alice, bob), results);
        assertEquals(2, results.getNumFound());

        // Check current user right
        query.checkCurrentUser(true);

        // Throw an exception when resolving Alice
        when(resolver.resolve(alice)).thenThrow(new RuntimeException("Alice"));
        when(response.getResults()).thenReturn((SolrDocumentList) sourceResults.clone());

        results = ((QueryResponse) this.componentManager.getComponentUnderTest().execute(query).get(0)).getResults();
        assertEquals(Collections.singletonList(bob), results);
        assertEquals(1, results.getNumFound());

        // Throw also an exception when resolving Bob
        when(resolver.resolve(bob)).thenThrow(new RuntimeException("Bob"));
        when(response.getResults()).thenReturn((SolrDocumentList) sourceResults.clone());

        // Assert that the results are empty when both throw an exception
        results = ((QueryResponse) this.componentManager.getComponentUnderTest().execute(query).get(0)).getResults();
        assertEquals(Collections.emptyList(), results);
        assertEquals(0, results.getNumFound());
    }
}