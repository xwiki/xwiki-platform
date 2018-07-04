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
import com.xpn.xwiki.objects.classes.PageClass;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultPageQueryBuilder}.
 *
 * @version $Id$
 * @since 10.6RC1
 */
@ComponentTest
public class DefaultPageQueryBuilderTest
{
    @InjectMockComponents
    private DefaultPageQueryBuilder queryBuilder;

    @MockComponent
    @Named("explicitlyAllowedValues")
    private QueryBuilder<DBListClass> explicitlyAllowedValuesQueryBuilder;

    @MockComponent
    @Named("implicitlyAllowedValues")
    private QueryBuilder<PageClass> implicitlyAllowedValuesQueryBuilder;

    @Test
    public void build() throws Exception
    {
        PageClass pageClass = new PageClass();

        Query explicitlyAllowedValuesQuery = mock(Query.class, "explicit");
        when(this.explicitlyAllowedValuesQueryBuilder.build(pageClass)).thenReturn(explicitlyAllowedValuesQuery);

        Query implicitlyAllowedValuesQuery = mock(Query.class, "implicit");
        when(this.implicitlyAllowedValuesQueryBuilder.build(pageClass)).thenReturn(implicitlyAllowedValuesQuery);

        assertSame(implicitlyAllowedValuesQuery, this.queryBuilder.build(pageClass));

        pageClass.setSql("test");
        assertSame(explicitlyAllowedValuesQuery, this.queryBuilder.build(pageClass));
    }
}
