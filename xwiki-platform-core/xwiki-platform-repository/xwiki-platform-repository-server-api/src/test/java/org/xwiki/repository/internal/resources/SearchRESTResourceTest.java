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
package org.xwiki.repository.internal.resources;

import java.util.List;

import jakarta.inject.Provider;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionsSearchResult;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SearchRESTResource}.
 *
 * @version $Id$
 */
@ComponentTest
class SearchRESTResourceTest
{
    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private QueryManager queryManager;

    @InjectMockComponents
    private SearchRESTResource resource;

    private Query query;

    @BeforeComponent
    void beforeComponent()
    {
        XWikiContext xcontext = mock(XWikiContext.class);
        when(xcontext.getWikiId()).thenReturn("wiki");
        when(this.xcontextProvider.get()).thenReturn(xcontext);
    }

    @BeforeEach
    void beforeEach() throws Exception
    {
        this.query = mock(Query.class);
        when(this.queryManager.createQuery("*", "solr")).thenReturn(this.query);

        QueryResponse response = mock(QueryResponse.class);
        when(response.getResults()).thenReturn(new SolrDocumentList());
        when(this.query.execute()).thenReturn(List.of(response));
    }

    @Test
    void searchOnlyReturnsValidExtensionsWithVersion() throws Exception
    {
        ExtensionsSearchResult result = this.resource.searchGet("", 0, 100, true);

        assertTrue(result.getExtensions().isEmpty());
        verify(this.query).bindValue("fq", List.of("property.ExtensionCode.ExtensionClass.validExtension_boolean:true",
            "property.ExtensionCode.ExtensionClass.lastVersion_string:?*"));
        assertEquals(0, result.getOffset());
    }
}
