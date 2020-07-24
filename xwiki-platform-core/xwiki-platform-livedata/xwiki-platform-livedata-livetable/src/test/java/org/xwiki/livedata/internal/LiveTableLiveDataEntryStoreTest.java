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
package org.xwiki.livedata.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.xwiki.livedata.LiveData;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.livedata.LiveDataQuery.Filter;
import org.xwiki.livedata.LiveDataQuery.SortEntry;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiRequest;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LiveTableLiveDataEntryStore}.
 * 
 * @version $Id$
 * @since 12.6RC1
 */
@ComponentTest
class LiveTableLiveDataEntryStoreTest
{
    @InjectMockComponents
    private LiveTableLiveDataEntryStore entryStore;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private TemplateManager templateManager;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @Mock
    private XWikiContext xcontext;

    @Mock
    private XWiki xwiki;

    @Mock
    private XWikiRequest originalRequest;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void before()
    {
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getRequest()).thenReturn(this.originalRequest);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);

        this.entryStore.getParameters().clear();
    }

    @Test
    void getFromTemplate() throws Exception
    {
        //
        // Setup
        //

        LiveDataQuery query = new LiveDataQuery();
        query.getSource().put("id", "liveTable");
        query.getSource().put("template", "getdocuments");
        query.getSource().put("childrenOf", "Test");
        query.getSource().put("queryFilters", "unique");
        query.getSource().put("translationPrefix", "core.restore.batch.");
        query.getProperties().addAll(Arrays.asList("doc.title", "status", "hidden", "doc.author"));
        query.getSort().add(new SortEntry("doc.date", true));
        query.getFilters().add(new Filter("doc.author", "contains", false, "mflorea", "tmortagne"));
        query.setLimit(5);
        query.setOffset(13);

        Map<String, Object> row = new HashMap<>();
        row.put("doc_title", "Some title");
        row.put("status", "pending");
        row.put("hidden", true);
        row.put("doc_author", "mflorea");
        row.put("doc_url", "http://www.xwiki.org");

        Map<String, Object> liveTableResults = new HashMap<>();
        liveTableResults.put("totalrows", 23);
        liveTableResults.put("rows", Collections.singletonList(row));

        String liveTableResultsJSON = this.objectMapper.writeValueAsString(liveTableResults);
        when(this.templateManager.render("getdocuments")).thenReturn(liveTableResultsJSON);

        Map<String, Object> entry = new HashMap<>();
        entry.put("doc.title", "Some title");
        entry.put("status", "pending");
        entry.put("hidden", true);
        entry.put("doc.author", "mflorea");
        entry.put("doc.url", "http://www.xwiki.org");

        LiveData expectedLiveData = new LiveData();
        expectedLiveData.setCount(23);
        expectedLiveData.getEntries().add(entry);

        Map<String, String[]> expectedRequestParams = new HashMap<>();
        expectedRequestParams.put("offset", new String[] {"14"});
        expectedRequestParams.put("limit", new String[] {"5"});
        expectedRequestParams.put("collist", new String[] {"doc.title,status,hidden,doc.author"});
        expectedRequestParams.put("sort", new String[] {"doc.date"});
        expectedRequestParams.put("dir", new String[] {"desc"});
        expectedRequestParams.put("queryFilters", new String[] {"unique"});
        expectedRequestParams.put("childrenOf", new String[] {"Test"});
        expectedRequestParams.put("transprefix", new String[] {"core.restore.batch."});
        expectedRequestParams.put("doc.author", new String[] {"mflorea", "tmortagne"});
        expectedRequestParams.put("doc.author_match", new String[] {"partial"});
        expectedRequestParams.put("doc.author/join_mode", new String[] {"OR"});

        //
        // Execution
        //

        assertEquals(expectedLiveData, this.entryStore.get(query));

        //
        // Checks
        //

        ArgumentCaptor<XWikiRequest> requestCaptor = ArgumentCaptor.forClass(XWikiRequest.class);
        verify(this.xcontext, times(2)).setRequest(requestCaptor.capture());
        List<XWikiRequest> requests = requestCaptor.getAllValues();
        assertRequestParameters(expectedRequestParams, requests.get(0).getParameterMap());
        assertSame(this.originalRequest, requests.get(1));
    }

    @Test
    void getFromResultPageWhenAccessDenied() throws Exception
    {
        LiveDataQuery query = new LiveDataQuery();
        query.getSource().put("id", "liveTable");
        query.getSource().put("resultPage", "Some.Page");

        DocumentReference documentReference = new DocumentReference("foo", "Some", "Page");
        when(this.currentDocumentReferenceResolver.resolve("Some.Page")).thenReturn(documentReference);

        DocumentReference userReference = new DocumentReference("xwiki", "Users", "Alice");
        doThrow(new AccessDeniedException(Right.VIEW, userReference, documentReference)).when(this.authorization)
            .checkAccess(Right.VIEW, documentReference);

        try {
            this.entryStore.get(query);
            fail();
        } catch (LiveDataException e) {
            assertTrue(e.getCause() instanceof AccessDeniedException, "Expecting AccessDeniedException");
        }
    }

    @Test
    void getFromResultPage() throws Exception
    {
        //
        // Setup
        //

        LiveDataQuery query = new LiveDataQuery();
        query.getSource().put("id", "liveTable");
        query.getSource().put("resultPage", "Panels.LiveTableResults");
        query.getSource().put("className", "Panels.PanelClass");
        query.getProperties().addAll(Arrays.asList("doc.title", "_actions"));

        DocumentReference documentReference = new DocumentReference("foo", "Panels", "LiveTableResults");
        when(this.currentDocumentReferenceResolver.resolve("Panels.LiveTableResults")).thenReturn(documentReference);

        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(documentReference, this.xcontext)).thenReturn(document);
        when(document.getRenderedContent(Syntax.PLAIN_1_0, this.xcontext)).thenReturn("{\"totalrows\":3,\"rows\":[]}");

        LiveData expectedLiveData = new LiveData();
        expectedLiveData.setCount(3);

        Map<String, String[]> expectedRequestParams = new HashMap<>();
        expectedRequestParams.put("outputSyntax", new String[] {"plain"});
        expectedRequestParams.put("offset", new String[] {"1"});
        expectedRequestParams.put("limit", new String[] {"15"});
        expectedRequestParams.put("collist", new String[] {"doc.title,_actions"});
        expectedRequestParams.put("classname", new String[] {"Panels.PanelClass"});

        //
        // Execution
        //

        assertEquals(expectedLiveData, this.entryStore.get(query));

        //
        // Checks
        //

        ArgumentCaptor<XWikiRequest> requestCaptor = ArgumentCaptor.forClass(XWikiRequest.class);
        verify(this.xcontext, times(2)).setRequest(requestCaptor.capture());
        List<XWikiRequest> requests = requestCaptor.getAllValues();
        assertRequestParameters(expectedRequestParams, requests.get(0).getParameterMap());
        assertSame(this.originalRequest, requests.get(1));

        verify(this.xcontext).setAction("get");
    }

    @Test
    void getFromDefaultResultPage() throws Exception
    {
        LiveDataQuery query = new LiveDataQuery();
        query.getSource().put("id", "liveTable");

        DocumentReference documentReference = new DocumentReference("foo", "XWiki", "LiveTableResults");
        when(this.currentDocumentReferenceResolver.resolve("XWiki.LiveTableResults")).thenReturn(documentReference);

        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(documentReference, this.xcontext)).thenReturn(document);
        when(document.getRenderedContent(Syntax.PLAIN_1_0, this.xcontext)).thenReturn("{\"totalrows\":7,\"rows\":[]}");

        LiveData expectedLiveData = new LiveData();
        expectedLiveData.setCount(7);

        assertEquals(expectedLiveData, this.entryStore.get(query));
    }

    private void assertRequestParameters(Map<String, String[]> expectedParams, Map<String, String[]> actualParams)
    {
        assertEquals(expectedParams.size(), actualParams.size());
        for (Map.Entry<String, String[]> expectedEntry : expectedParams.entrySet()) {
            assertArrayEquals(expectedEntry.getValue(), actualParams.get(expectedEntry.getKey()),
                expectedEntry.getKey() + " does't match!");
        }
    }
}
