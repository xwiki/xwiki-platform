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
package org.xwiki.search.solr.internal.rest;

import java.util.List;

import javax.ws.rs.core.UriInfo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.xwiki.properties.ConverterManager;
import org.xwiki.query.QueryManager;
import org.xwiki.query.SecureQuery;
import org.xwiki.query.solr.internal.SafeSolrQueryValidator;
import org.xwiki.search.solr.internal.DefaultSolrUtils;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SOLRSearchSource}.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList({ DefaultSolrUtils.class, SafeSolrQueryValidator.class })
class SOLRSearchSourceTest
{
    private static final String SORT = "sort";

    @InjectMockComponents
    private SOLRSearchSource searchSource;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private SearchResultConverter searchResultConverter;

    /**
     * Converter needed for DefaultSolrUtils.
     */
    @MockComponent
    private ConverterManager converterManager;

    @Mock
    private SecureQuery query;

    @Mock
    private UriInfo uriInfo;

    @BeforeEach
    void configure() throws Exception
    {
        when(this.queryManager.createQuery(anyString(), eq("solr"))).thenReturn(this.query);
        when(this.query.setLimit(anyInt())).thenReturn(this.query);
        when(this.query.setOffset(anyInt())).thenReturn(this.query);
        when(this.searchResultConverter.getSolrSearchResults(any(), any(), any(), eq(false))).thenReturn(List.of());
    }

    @Test
    void searchInWikis() throws Exception
    {
        this.searchSource.search("text", "wiki", "wiki1,wiki 2", false, null, null, false, 10, 0, false, null,
            this.uriInfo);

        ArgumentCaptor<List<String>> filterQueries = ArgumentCaptor.captor();
        verify(this.query).bindValue(eq("fq"), filterQueries.capture());
        assertEquals(List.of("{!tag=type}type:(\"DOCUMENT\")", "{!tag=hidden}hidden:(false)",
            "{!tag=wiki}wiki:(wiki1 OR wiki\\ 2)"), filterQueries.getValue());
    }

    @ParameterizedTest
    @ValueSource(strings = { "title_sort", "property.My$20Space.Été.prop_sortString" })
    void orderByField(String orderField) throws Exception
    {
        this.searchSource.search("text", "wiki", null, false, orderField, "desc", false, 10, 0, false, null,
            this.uriInfo);

        verify(this.query).bindValue(SORT, orderField + " desc");
    }

    @ParameterizedTest
    @ValueSource(strings = { "sum(1,2)", "$param", "1" })
    void orderByUnsupportedField(String orderField) throws Exception
    {
        this.searchSource.search("text", "wiki", null, false, orderField, "desc", false, 10, 0, false, null,
            this.uriInfo);

        verify(this.query, never()).bindValue(eq(SORT), any());
    }
}
