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
package org.xwiki.search.solr.internal;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.search.solr.SolrUtils;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Validate {@link DefaultSolrUtils}.
 * 
 * @version $Id$
 */
@ComponentTest
class DefaultSolrUtilsTest
{
    @InjectMockComponents
    private DefaultSolrUtils utils;

    @Test
    void toFilterQueryString()
    {
        assertEquals("\\OR", this.utils.toFilterQueryString("OR"));
    }

    @Test
    void toCompleteFilterQueryString()
    {
        assertEquals("\"\"", this.utils.toCompleteFilterQueryString(""));
    }

    @Test
    void toPhraseQueryString()
    {
        assertEquals("\"My\\ \\\"Space\\\"\\ \\{\\!term\\}\\ a\\\\b\"",
            this.utils.toPhraseQueryString("My \"Space\" {!term} a\\b"));
        assertEquals("\"$\\{q\\}\"", this.utils.toPhraseQueryString("${q}"));
        assertEquals("\"\"", this.utils.toPhraseQueryString(""));
        assertEquals("\"42\"", this.utils.toPhraseQueryString(42));
        assertEquals("\"1970-01-01T00:00:00Z\"", this.utils.toPhraseQueryString(new Date(0)));
        assertNull(this.utils.toPhraseQueryString(null));
    }

    @Test
    void defaultToPhraseQueryString()
    {
        SolrUtils solrUtils = mock();
        when(solrUtils.toPhraseQueryString(any())).thenCallRealMethod();
        when(solrUtils.toFilterQueryString("My Space")).thenReturn("My\\ Space");

        assertEquals("\"My\\ Space\"", solrUtils.toPhraseQueryString("My Space"));
        assertNull(solrUtils.toPhraseQueryString(null));
    }

    @Test
    void getMapFieldName()
    {
        assertEquals("key__map_string", this.utils.getMapFieldName("key", "map", (Type) null));
        assertEquals("key__map_string", this.utils.getMapFieldName("key", "map", String.class));
        assertEquals("key__map_pint", this.utils.getMapFieldName("key", "map", Integer.class));
        assertEquals("key__map_strings", this.utils.getMapFieldName("key", "map", List.class));
        assertEquals("key__map_pints",
            this.utils.getMapFieldName("key", "map", new DefaultParameterizedType(null, List.class, Integer.class)));
    }
}
