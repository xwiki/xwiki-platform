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
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
