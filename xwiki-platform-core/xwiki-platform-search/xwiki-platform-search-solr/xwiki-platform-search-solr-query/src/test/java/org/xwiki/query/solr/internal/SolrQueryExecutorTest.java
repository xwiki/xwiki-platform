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
package org.xwiki.query.solr.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.SolrParams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.component.internal.ContextComponentManagerProvider;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.query.internal.DefaultQuery;
import org.xwiki.query.internal.DefaultQueryExecutorManager;
import org.xwiki.query.internal.DefaultQueryManager;
import org.xwiki.search.solr.internal.api.SolrInstance;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Basic test for the {@link SolrQueryExecutor}.
 *
 * @version $Id$
 */
@OldcoreTest
@ComponentList({DefaultQueryManager.class, DefaultQueryExecutorManager.class, ContextComponentManagerProvider.class,
    SolrResponseRightsFilter.class, SafeSolrQueryValidator.class})
@ReferenceComponentList
class SolrQueryExecutorTest
{
    private static final String ROWS = "rows";

    private static final String START = "start";

    private static final String QT = "qt";

    private static final String FL = "fl";

    private static final String EXPAND_MACROS = "expandMacros";

    private static final String ITERABLE_PARAM_NAME = "multiParam";

    private static final String[] ITERABLE_PARAM_EXPECTED = { "value1", "value2" };

    private static final Iterable<String> ITERABLE_PARAM_VALUE = List.of(ITERABLE_PARAM_EXPECTED);

    private static final String INT_ARR_PARAM_NAME = "intArrayParam";

    private static final String[] INT_ARR_PARAM_EXPECTED = { "-42", "4711" };

    private static final int[] INT_ARR_PARAM_VALUE = { -42, 4711 };

    private static final String STR_ARR_PARAM_NAME = "stringArrayParam";

    private static final String[] STR_ARR_PARAM_EXPECTED = { "valueA", "valueB" };

    private static final String[] STR_ARR_PARAM_VALUE = STR_ARR_PARAM_EXPECTED;

    private static final String SINGLE_PARAM_NAME = "singleParam";

    private static final Object SINGLE_PARAM_VALUE = new Object();

    private static final Object SINGLE_PARAM_EXPECTED = SINGLE_PARAM_VALUE.toString();

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private SolrQueryExecutor solrQueryExecutor;

    @MockComponent
    private SolrInstance solrInstance;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @MockComponent
    private DocumentReferenceResolver<SolrDocument> solrDocumentReferenceResolver;

    @MockComponent
    private JobProgressManager progress;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @Test
    void executorRegistration() throws Exception
    {
        QueryManager queryManager = this.componentManager.getInstance(QueryManager.class);

        assertTrue(queryManager.getLanguages().contains(SolrQueryExecutor.SOLR));
    }

    @Test
    void multiValuedQueryArgs(MockitoOldcore oldcore) throws Exception
    {
        when(this.solrInstance.query(any(SolrQuery.class))).then(invocation -> {
            SolrQuery solrQuery = (SolrQuery) invocation.getArguments()[0];

            assertArrayEquals(ITERABLE_PARAM_EXPECTED, solrQuery.getParams(ITERABLE_PARAM_NAME));
            assertArrayEquals(INT_ARR_PARAM_EXPECTED, solrQuery.getParams(INT_ARR_PARAM_NAME));
            assertArrayEquals(STR_ARR_PARAM_EXPECTED, solrQuery.getParams(STR_ARR_PARAM_NAME));
            assertEquals(SINGLE_PARAM_EXPECTED, solrQuery.get(SINGLE_PARAM_NAME));

            // Check that the default list of supported locales is taken from the wiki configuration.
            assertEquals("en,fr,de", solrQuery.get("xwiki.supportedLocales"));

            QueryResponse r = mock(QueryResponse.class);
            when(r.getResults()).thenReturn(new SolrDocumentList());
            return r;
        });

        DefaultQuery query = new DefaultQuery("TestQuery", null);
        query.bindValue(ITERABLE_PARAM_NAME, ITERABLE_PARAM_VALUE);
        query.bindValue(INT_ARR_PARAM_NAME, INT_ARR_PARAM_VALUE);
        query.bindValue(STR_ARR_PARAM_NAME, STR_ARR_PARAM_VALUE);
        query.bindValue(SINGLE_PARAM_NAME, SINGLE_PARAM_VALUE);

        // The default list of supported locales should be taken from the wiki configuration.
        doReturn(List.of(Locale.ENGLISH, Locale.FRENCH, Locale.GERMAN))
            .when(oldcore.getSpyXWiki()).getAvailableLocales(oldcore.getXWikiContext());

        this.solrQueryExecutor.execute(query);
    }

    @Test
    void filterResponse(MockitoOldcore oldcore) throws Exception
    {
        DocumentReference currentUserReference = new DocumentReference("xwiki", "XWiki", "currentuser");
        oldcore.getXWikiContext().setUserReference(currentUserReference);

        DocumentReference currentAuthorReference = new DocumentReference("xwiki", "XWiki", "currentauthor");
        XWikiDocument currentDocument = new XWikiDocument(currentAuthorReference);
        currentDocument.setContentAuthorReference(currentAuthorReference);
        oldcore.getXWikiContext().setDoc(currentDocument);

        DocumentReference aliceReference = new DocumentReference("wiki", "Users", "Alice");
        when(this.authorizationManager.hasAccess(Right.VIEW, currentAuthorReference, aliceReference)).thenReturn(true);
        SolrDocument alice = new SolrDocument();
        when(this.solrDocumentReferenceResolver.resolve(alice)).thenReturn(aliceReference);

        DocumentReference bobReference = new DocumentReference("wiki", "Users", "Bob");
        when(this.authorizationManager.hasAccess(Right.VIEW, currentUserReference, bobReference)).thenReturn(true);
        when(this.authorizationManager.hasAccess(Right.VIEW, currentAuthorReference, bobReference)).thenReturn(true);
        SolrDocument bob = new SolrDocument();
        when(this.solrDocumentReferenceResolver.resolve(bob)).thenReturn(bobReference);

        DocumentReference carolReference = new DocumentReference("wiki", "Users", "Carol");
        when(this.authorizationManager.hasAccess(Right.VIEW, currentUserReference, carolReference)).thenReturn(true);
        SolrDocument carol = new SolrDocument();
        when(this.solrDocumentReferenceResolver.resolve(carol)).thenReturn(carolReference);

        SolrDocumentList sourceResults = new SolrDocumentList();
        sourceResults.addAll(List.of(alice, bob, carol));
        sourceResults.setNumFound(3);

        QueryResponse response = mock(QueryResponse.class);
        when(this.solrInstance.query(any(SolrParams.class))).thenReturn(response);

        DefaultQuery query = new DefaultQuery("", null);

        // No right check

        when(response.getResults()).thenReturn((SolrDocumentList) sourceResults.clone());

        SolrDocumentList results = ((QueryResponse) this.solrQueryExecutor.execute(query).get(0)).getResults();
        assertEquals(List.of(alice, bob, carol), results);

        // Check current user right

        query.checkCurrentUser(true);
        when(response.getResults()).thenReturn((SolrDocumentList) sourceResults.clone());

        results = ((QueryResponse) this.solrQueryExecutor.execute(query).get(0)).getResults();
        assertEquals(List.of(bob, carol), results);

        // Check both current user and author rights

        query.checkCurrentAuthor(true);
        when(response.getResults()).thenReturn((SolrDocumentList) sourceResults.clone());

        results = ((QueryResponse) this.solrQueryExecutor.execute(query).get(0)).getResults();
        assertEquals(List.of(bob), results);

        // Check current author right

        query.checkCurrentUser(false);
        when(response.getResults()).thenReturn((SolrDocumentList) sourceResults.clone());

        results = ((QueryResponse) this.solrQueryExecutor.execute(query).get(0)).getResults();
        assertEquals(List.of(alice, bob), results);
    }

    @Test
    void filterResponseWithException(MockitoOldcore oldcore) throws Exception
    {
        DocumentReference currentUserReference = new DocumentReference("xwiki", "XWiki", "currentuser");
        oldcore.getXWikiContext().setUserReference(currentUserReference);

        DocumentReference aliceReference = new DocumentReference("wiki", "Users", "Alice");
        SolrDocument alice = new SolrDocument();
        when(this.solrDocumentReferenceResolver.resolve(alice)).thenReturn(aliceReference);

        DocumentReference bobReference = new DocumentReference("wiki", "Users", "Bob");
        when(this.authorizationManager.hasAccess(Right.VIEW, currentUserReference, bobReference)).thenReturn(true);
        SolrDocument bob = new SolrDocument();
        when(this.solrDocumentReferenceResolver.resolve(bob)).thenReturn(bobReference);

        SolrDocumentList sourceResults = new SolrDocumentList();
        sourceResults.addAll(List.of(alice, bob));
        sourceResults.setNumFound(2);

        QueryResponse response = mock(QueryResponse.class);
        when(this.solrInstance.query(any(SolrParams.class))).thenReturn(response);

        DefaultQuery query = new DefaultQuery("", null);

        // No right check, verify that the setup works.
        when(response.getResults()).thenReturn((SolrDocumentList) sourceResults.clone());
        SolrDocumentList results = ((QueryResponse) this.solrQueryExecutor.execute(query).get(0)).getResults();
        assertEquals(List.of(alice, bob), results);
        assertEquals(2, results.getNumFound());

        // Check current user right
        query.checkCurrentUser(true);

        // Throw an exception when resolving Alice
        when(this.solrDocumentReferenceResolver.resolve(alice)).thenThrow(new RuntimeException("Alice"));
        when(response.getResults()).thenReturn((SolrDocumentList) sourceResults.clone());

        results = ((QueryResponse) this.solrQueryExecutor.execute(query).get(0)).getResults();
        assertEquals(List.of(bob), results);
        assertEquals(1, results.getNumFound());

        // Throw also an exception when resolving Bob
        when(this.solrDocumentReferenceResolver.resolve(bob)).thenThrow(new RuntimeException("Bob"));
        when(response.getResults()).thenReturn((SolrDocumentList) sourceResults.clone());

        // Assert that the results are empty when both throw an exception
        results = ((QueryResponse) this.solrQueryExecutor.execute(query).get(0)).getResults();
        assertEquals(List.of(), results);
        assertEquals(0, results.getNumFound());

        // One warning for Alice in the previous execution, two for Alice and Bob in this one.
        assertEquals("Removing bad result: [SolrDocument{}]",
            this.logCapture.getMessage(0));
        assertEquals("Removing bad result: [SolrDocument{}]",
            this.logCapture.getMessage(1));
        assertEquals("Removing bad result: [SolrDocument{}]",
            this.logCapture.getMessage(2));
    }

    @Test
    void namedParametersTakePrecedenceOverLimitAndOffset() throws Exception
    {
        QueryResponse response = mock(QueryResponse.class);
        when(response.getResults()).thenReturn(new SolrDocumentList());
        List<SolrQuery> executedQueries = new ArrayList<>();
        when(this.solrInstance.query(any(SolrQuery.class))).then(invocation -> {
            executedQueries.add(((SolrQuery) invocation.getArgument(0)).getCopy());
            return response;
        });

        DefaultQuery query = new DefaultQuery("*:*", null);
        query.setLimit(10);
        query.setOffset(20);

        // Without named parameters, the limit and offset are used.
        this.solrQueryExecutor.execute(query);

        query.bindValue(ROWS, 5000);
        query.bindValue(START, 100);
        this.solrQueryExecutor.execute(query);

        assertEquals(2, executedQueries.size());
        assertEquals("10", executedQueries.get(0).get(ROWS));
        assertEquals("20", executedQueries.get(0).get(START));
        assertEquals("5000", executedQueries.get(1).get(ROWS));
        assertEquals("100", executedQueries.get(1).get(START));
    }

    @ParameterizedTest
    @ValueSource(strings = { "/update", "/admin/cores", "/terms", "select" })
    void unsupportedRequestHandler(String requestHandler) throws Exception
    {
        DefaultQuery query = new DefaultQuery("*:*", null);
        query.bindValue(QT, requestHandler);

        QueryException exception = assertThrows(QueryException.class, () -> this.solrQueryExecutor.execute(query));

        assertEquals(String.format(
            "Unsupported request handler [%s], only [/select, /query, /spell] are supported. Named query = [*:*]", requestHandler),
            exception.getCause().getMessage());
        verify(this.solrInstance, never()).query(any(SolrParams.class));
    }

    @ParameterizedTest
    @ValueSource(strings = { "/select", "/query", "/spell" })
    void supportedRequestHandler(String requestHandler) throws Exception
    {
        QueryResponse response = mock(QueryResponse.class);
        when(response.getResults()).thenReturn(new SolrDocumentList());
        when(this.solrInstance.query(any(SolrQuery.class))).thenReturn(response);

        DefaultQuery query = new DefaultQuery("*:*", null);
        query.bindValue(QT, requestHandler);

        assertEquals(List.of(response), this.solrQueryExecutor.execute(query));
    }

    @Test
    void idAddedToFieldList() throws Exception
    {
        QueryResponse response = mock(QueryResponse.class);
        when(response.getResults()).thenReturn(new SolrDocumentList());
        List<SolrQuery> executedQueries = new ArrayList<>();
        when(this.solrInstance.query(any(SolrQuery.class))).then(invocation -> {
            executedQueries.add(((SolrQuery) invocation.getArgument(0)).getCopy());
            return response;
        });

        // Without field list, Solr returns the default fields, which include the id.
        DefaultQuery query = new DefaultQuery("*:*", null);
        this.solrQueryExecutor.execute(query);

        query.bindValue(FL, "wiki spaces name");
        this.solrQueryExecutor.execute(query);

        assertEquals(2, executedQueries.size());
        assertNull(executedQueries.get(0).getParams(FL));
        assertArrayEquals(new String[] { "wiki spaces name", "id" },
            executedQueries.get(1).getParams(FL));
    }

    @Test
    void macroExpansionOnlyWhenRequested() throws Exception
    {
        QueryResponse response = mock(QueryResponse.class);
        when(response.getResults()).thenReturn(new SolrDocumentList());
        List<SolrQuery> executedQueries = new ArrayList<>();
        when(this.solrInstance.query(any(SolrQuery.class))).then(invocation -> {
            executedQueries.add(((SolrQuery) invocation.getArgument(0)).getCopy());
            return response;
        });

        DefaultQuery query = new DefaultQuery("*:*", null);
        query.bindValue("fq", "space_exact:\"${q}\"");
        this.solrQueryExecutor.execute(query);

        query.bindValue(EXPAND_MACROS, true);
        this.solrQueryExecutor.execute(query);

        assertEquals(2, executedQueries.size());
        assertEquals("false", executedQueries.get(0).get(EXPAND_MACROS));
        assertEquals("true", executedQueries.get(1).get(EXPAND_MACROS));
    }

    @ParameterizedTest
    @ValueSource(strings = { "fl", "x.fl" })
    void fieldListOverridingCheckedFields(String parameter) throws Exception
    {
        DefaultQuery query = new DefaultQuery("*:*", null);
        query.bindValue(parameter, List.of("title_", "name:[value v=Page]"));

        QueryException exception = assertThrows(QueryException.class, () -> this.solrQueryExecutor.execute(query));

        assertEquals("Unsupported field list [name:[value v=Page]]: the fields [class, doclocale, filename, id, name, "
            + "number, propertyname, spaces, type, wiki] can only be returned with their own values. "
            + "Named query = [*:*]", exception.getCause().getMessage());
        verify(this.solrInstance, never()).query(any(SolrParams.class));
    }

    @Test
    void fieldListKeepingCheckedFields() throws Exception
    {
        QueryResponse response = mock(QueryResponse.class);
        when(response.getResults()).thenReturn(new SolrDocumentList());
        when(this.solrInstance.query(any(SolrQuery.class))).thenReturn(response);

        DefaultQuery query = new DefaultQuery("*:*", null);
        // Each field list is checked separately, like Solr parses them.
        query.bindValue(FL, List.of("wiki spaces:spaces name,title:title_", "[explain],count:sum(1,2)"));
        query.bindValue("hl.fl", "doccontent_*");

        assertEquals(List.of(response), this.solrQueryExecutor.execute(query));
    }

    @ParameterizedTest
    @ValueSource(strings = { "x.qt", "x.y.qt" })
    void unsupportedNestedRequestHandler(String parameter) throws Exception
    {
        DefaultQuery query = new DefaultQuery("*:*", null);
        query.bindValue(FL, List.of("id", "x:[subquery]"));
        query.bindValue(parameter, List.of("/select", "/admin/cores"));

        QueryException exception = assertThrows(QueryException.class, () -> this.solrQueryExecutor.execute(query));

        assertEquals("Unsupported request handler [/admin/cores], only [/select, /query, /spell] are supported. "
            + "Named query = [*:*]", exception.getCause().getMessage());
        verify(this.solrInstance, never()).query(any(SolrParams.class));
    }

    @ParameterizedTest
    @ValueSource(strings = { "json", "json.fields", "json.queries", "json.params", "x.json", "x.json.fields",
        "json.facet.x" })
    void unsupportedJSONParameter(String parameter) throws Exception
    {
        DefaultQuery query = new DefaultQuery("*:*", null);
        query.bindValue(parameter, "{\"fields\":[\"name:[value v=Page]\"]}");

        QueryException exception = assertThrows(QueryException.class, () -> this.solrQueryExecutor.execute(query));

        assertEquals(String.format("Unsupported parameter [%s], of the JSON request parameters only "
            + "[json.nl, json.wrf, json.facet] are supported. Named query = [*:*]", parameter),
            exception.getCause().getMessage());
        verify(this.solrInstance, never()).query(any(SolrParams.class));
    }

    @ParameterizedTest
    @ValueSource(strings = { "json.nl", "json.wrf", "json.facet", "x.json.facet", "jsonp", "myjson.x" })
    void supportedJSONParameter(String parameter) throws Exception
    {
        QueryResponse response = mock(QueryResponse.class);
        when(response.getResults()).thenReturn(new SolrDocumentList());
        when(this.solrInstance.query(any(SolrQuery.class))).thenReturn(response);

        DefaultQuery query = new DefaultQuery("*:*", null);
        query.bindValue(parameter, "value");

        assertEquals(List.of(response), this.solrQueryExecutor.execute(query));
    }
}
