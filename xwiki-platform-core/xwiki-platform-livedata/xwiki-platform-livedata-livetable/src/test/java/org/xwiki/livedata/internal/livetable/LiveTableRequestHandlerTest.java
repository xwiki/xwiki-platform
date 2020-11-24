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
package org.xwiki.livedata.internal.livetable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.livedata.LiveDataQuery.Filter;
import org.xwiki.livedata.LiveDataQuery.SortEntry;
import org.xwiki.livedata.LiveDataQuery.Source;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LiveTableRequestHandler}.
 * 
 * @version $Id$
 * @since 12.10
 */
@ComponentTest
public class LiveTableRequestHandlerTest
{
    @InjectMockComponents
    private LiveTableRequestHandler handler;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @Mock
    private XWikiContext xcontext;

    @Mock
    private XWikiRequest originalRequest;

    @Mock
    private XWikiResponse originalResponse;

    @BeforeEach
    void before()
    {
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getRequest()).thenReturn(this.originalRequest);
        when(this.xcontext.getResponse()).thenReturn(this.originalResponse);
        when(this.xcontext.getAction()).thenReturn("view");
    }

    @Test
    void getLiveTableResultsFromSupplier() throws Exception
    {
        //
        // Setup
        //

        LiveDataQuery query = new LiveDataQuery();
        query.setSource(new Source("liveTable"));
        query.getSource().setParameter("template", "getdocuments");
        query.getSource().setParameter("resultPage", "Panels.LiveTableResults");
        query.getSource().setParameter("className", "Panels.PanelClass");
        query.getSource().setParameter("childrenOf", "Test");
        query.getSource().setParameter("queryFilters", "unique");
        query.getSource().setParameter("translationPrefix", "core.restore.batch.");
        query.setProperties(Arrays.asList("doc.title", "status", "hidden", "doc.author"));
        query.setSort(Collections.singletonList(new SortEntry("doc.date", true)));
        query
            .setFilters(Collections.singletonList(new Filter("doc.author", "contains", false, "mflorea", "tmortagne")));
        query.setLimit(5);
        query.setOffset(13L);

        Map<String, String[]> expectedRequestParams = new HashMap<>();
        expectedRequestParams.put("reqNo", new String[] {"1"});
        expectedRequestParams.put("outputSyntax", new String[] {"plain"});
        expectedRequestParams.put("classname", new String[] {"Panels.PanelClass"});
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

        when(this.xcontext.isFinished()).thenReturn(true);

        //
        // Execution
        //

        assertEquals("live table JSON", this.handler.getLiveTableResults(query, () -> "live table JSON"));

        //
        // Checks
        //

        ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);
        verify(this.xcontext, times(2)).setAction(actionCaptor.capture());
        assertEquals(Arrays.asList("get", "view"), actionCaptor.getAllValues());

        ArgumentCaptor<XWikiRequest> requestCaptor = ArgumentCaptor.forClass(XWikiRequest.class);
        verify(this.xcontext, times(2)).setRequest(requestCaptor.capture());
        List<XWikiRequest> requests = requestCaptor.getAllValues();
        assertRequestParameters(expectedRequestParams, requests.get(0).getParameterMap());
        assertSame(this.originalRequest, requests.get(1));

        ArgumentCaptor<XWikiResponse> responseCaptor = ArgumentCaptor.forClass(XWikiResponse.class);
        verify(this.xcontext, times(2)).setResponse(responseCaptor.capture());
        List<XWikiResponse> responses = responseCaptor.getAllValues();
        assertSame(this.originalResponse, responses.get(1));

        verify(this.xcontext).setFinished(true);
    }

    @Test
    void getLiveTableResultsFromResponse() throws Exception
    {
        assertEquals("JSON from response", this.handler.getLiveTableResults(new LiveDataQuery(), () -> {
            ArgumentCaptor<XWikiResponse> responseCaptor = ArgumentCaptor.forClass(XWikiResponse.class);
            verify(this.xcontext).setResponse(responseCaptor.capture());
            XWikiResponse response = responseCaptor.getValue();
            try {
                response.getWriter().print("JSON from response");
                response.flushBuffer();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return "supplier output";
        }));

        verify(this.xcontext).setFinished(false);
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
