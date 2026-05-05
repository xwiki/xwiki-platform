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

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.query.Query;
import org.xwiki.query.QueryBuilder;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.objects.classes.DBListClass;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DefaultDBListQueryBuilder}.
 * 
 * @version $Id$
 * @since 9.8RC1
 */
@ComponentTest
class DefaultDBListQueryBuilderTest
{
    @InjectMockComponents
    private DefaultDBListQueryBuilder builder;

    @MockComponent
    @Named("explicitlyAllowedValues")
    private QueryBuilder<DBListClass> explicitlyAllowedValuesQueryBuilder;

    @MockComponent
    @Named("implicitlyAllowedValues")
    private QueryBuilder<DBListClass> implicitlyAllowedValuesQueryBuilder;

    @Test
    void build() throws Exception
    {
        DBListClass dbListClass = new DBListClass();

        Query explicitlyAllowedValuesQuery = mock(Query.class, "explicit");
        when(this.explicitlyAllowedValuesQueryBuilder.build(dbListClass)).thenReturn(explicitlyAllowedValuesQuery);

        Query implicitlyAllowedValuesQuery = mock(Query.class, "implicit");
        when(this.implicitlyAllowedValuesQueryBuilder.build(dbListClass)).thenReturn(implicitlyAllowedValuesQuery);

        assertSame(implicitlyAllowedValuesQuery, this.builder.build(dbListClass));

        dbListClass.setSql("test");
        assertSame(explicitlyAllowedValuesQuery, this.builder.build(dbListClass));
    }
}
