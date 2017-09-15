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
package com.xpn.xwiki.internal.objects.classes;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.query.Query;
import org.xwiki.query.QueryBuilder;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.objects.classes.DBListClass;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DefaultDBListQueryBuilder}.
 * 
 * @version $Id$
 * @since 9.8RC1
 */
public class DefaultDBListQueryBuilderTest
{
    @Rule
    public MockitoComponentMockingRule<QueryBuilder<DBListClass>> mocker =
        new MockitoComponentMockingRule<QueryBuilder<DBListClass>>(DefaultDBListQueryBuilder.class);

    @Test
    public void build() throws Exception
    {
        DBListClass dbListClass = new DBListClass();

        DefaultParameterizedType dbListQueryBuilderType =
            new DefaultParameterizedType(null, QueryBuilder.class, DBListClass.class);
        QueryBuilder<DBListClass> explicitlyAllowedValuesQueryBuilder =
            this.mocker.getInstance(dbListQueryBuilderType, "explicitlyAllowedValues");
        QueryBuilder<DBListClass> implicitlyAllowedValuesQueryBuilder =
            this.mocker.getInstance(dbListQueryBuilderType, "implicitlyAllowedValues");

        Query explicitlyAllowedValuesQuery = mock(Query.class, "explicit");
        when(explicitlyAllowedValuesQueryBuilder.build(dbListClass)).thenReturn(explicitlyAllowedValuesQuery);

        Query implicitlyAllowedValuesQuery = mock(Query.class, "implicit");
        when(implicitlyAllowedValuesQueryBuilder.build(dbListClass)).thenReturn(implicitlyAllowedValuesQuery);

        assertSame(implicitlyAllowedValuesQuery, this.mocker.getComponentUnderTest().build(dbListClass));

        dbListClass.setSql("test");
        assertSame(explicitlyAllowedValuesQuery, this.mocker.getComponentUnderTest().build(dbListClass));
    }
}
