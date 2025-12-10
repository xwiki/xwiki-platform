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

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Named;
import jakarta.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.query.Query;
import org.xwiki.search.SearchConfiguration;
import org.xwiki.search.solr.SolrUtils;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SolrSearchExclusionsQueryFilter}.
 *
 * @version $Id$
 */
@ComponentTest
class SolrSearchExclusionsQueryFilterTest
{
    @InjectMockComponents
    private SolrSearchExclusionsQueryFilter queryFilter;

    @MockComponent
    private SearchConfiguration searchConfiguration;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    @Named("local")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    @MockComponent
    private SolrUtils solrUtils;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @Mock
    private XWikiContext xcontext;

    @Mock
    private Query query;

    @BeforeEach
    void beforeEach() throws Exception
    {
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getWikiId()).thenReturn("test");

        when(this.wikiDescriptorManager.getAllIds()).thenReturn(List.of("dev", "test"));
    }

    @Test
    void filterStatement()
    {
        assertEquals("foo", this.queryFilter.filterStatement("foo", "solr"));
    }

    @Test
    void filterResults()
    {
        assertEquals(List.of("one", "two"), this.queryFilter.filterResults(List.of("one", "two")));
    }

    @Test
    void filterQueryWithoutExclusions()
    {
        assertSame(this.query, this.queryFilter.filterQuery(this.query));
        verify(this.query, never()).getNamedParameters();
        verify(this.query, never()).bindValue(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void filterQuery()
    {
        DocumentReference excludedDocRef = new DocumentReference("dev", List.of("Path", "To"), "Pa ge");
        SpaceReference excludedSpaceRef = new SpaceReference("test", "Some", "Space");
        when(this.localEntityReferenceSerializer.serialize(excludedDocRef.getParent())).thenReturn("Path.To");
        when(this.localEntityReferenceSerializer.serialize(excludedSpaceRef)).thenReturn("Some.Space");

        when(this.solrUtils.toFilterQueryString("dev")).thenReturn("dEv");
        when(this.solrUtils.toFilterQueryString("Path.To")).thenReturn("Path\\.To");
        when(this.solrUtils.toFilterQueryString("Pa ge")).thenReturn("Pa\\ ge");
        when(this.solrUtils.toFilterQueryString("test")).thenReturn("tESt");
        when(this.solrUtils.toFilterQueryString("Some.Space")).thenReturn("Some\\.Space");

        // Verify when existing filter query is a string.
        when(this.query.getNamedParameters()).thenReturn(Map.of("fq", "existing"));
        when(this.searchConfiguration.getExclusions()).thenReturn(Set.of(excludedDocRef),
            Set.of(excludedSpaceRef, excludedDocRef));

        assertSame(this.query, this.queryFilter.filterQuery(this.query));

        verify(this.xcontext).setWikiId("dev");
        verify(this.xcontext, times(3)).setWikiId("test");
        verify(this.query).bindValue("fq", List.of("-(wiki:dEv AND space_exact:Path\\.To AND name_exact:Pa\\ ge)",
            "-(wiki:tESt AND space_prefix:Some\\.Space)", "existing"));

        // Verify when existing filter query is a list.
        when(this.query.getNamedParameters()).thenReturn(Map.of("fq", List.of("first", "second")));
        when(this.searchConfiguration.getExclusions()).thenReturn(Set.of(excludedDocRef),
            Set.of(excludedSpaceRef, excludedDocRef));

        assertSame(this.query, this.queryFilter.filterQuery(this.query));

        verify(this.xcontext, times(2)).setWikiId("dev");
        verify(this.xcontext, times(6)).setWikiId("test");
        verify(this.query).bindValue("fq", List.of("-(wiki:dEv AND space_exact:Path\\.To AND name_exact:Pa\\ ge)",
            "-(wiki:tESt AND space_prefix:Some\\.Space)", "first", "second"));
    }
}
