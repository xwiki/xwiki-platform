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
package org.xwiki.query.internal;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.xwiki.query.Query;
import org.xwiki.query.WrappingQuery;
import org.xwiki.test.junit5.mockito.ComponentTest;

import static org.mockito.Mockito.mock;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit tests for {@link WrappingQuery}.
 *
 * @version $Id$
 * @since 8.4.5
 * @since 9.3RC1
 */
@ComponentTest
class WrappingQueryTest
{
    @Test
    void bindValueReturnsThisTest()
    {
        Query wrappedQuery =  mock(Query.class);
        Query wrappingQuery = new WrappingQuery(wrappedQuery);
        assertSame(wrappingQuery, wrappingQuery.bindValue("hello", "world"));
        assertSame(wrappingQuery, wrappingQuery.bindValue(0, "hello"));
        assertSame(wrappingQuery, wrappingQuery.bindValues(List.of("hello", "world")));
        assertSame(wrappingQuery, wrappingQuery.bindValues(Map.of("hello", "world")));
    }
}
