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

import org.junit.jupiter.api.Test;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.query.QueryException}
 *
 * @version $Id$
 * @since 6.1M1
 */
public class QueryExceptionTest
{
    @Test
    public void getMessageWhenStatement()
    {
        Query query = mock(Query.class);
        when(query.isNamed()).thenReturn(false);
        when(query.getStatement()).thenReturn("statement");

        Exception nestedException = mock(Exception.class);
        when(nestedException.getMessage()).thenReturn("nestedmessage");

        QueryException queryException = new QueryException("message", query, nestedException);
        assertEquals("message. Query statement = [statement]", queryException.getMessage());
    }

    @Test
    public void getMessageWhenNamedQuery()
    {
        Query query = mock(Query.class);
        when(query.isNamed()).thenReturn(true);
        when(query.getStatement()).thenReturn("namedquery");

        Exception nestedException = mock(Exception.class);
        when(nestedException.getMessage()).thenReturn("nestedmessage");

        QueryException queryException = new QueryException("message", query, nestedException);
        assertEquals("message. Named query = [namedquery]", queryException.getMessage());
    }
}
