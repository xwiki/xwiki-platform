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

import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.objects.classes.PageClass;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ImplicitlyAllowedValuesPageQueryBuilder}.
 *
 * @version $Id$
 * @since 10.6RC1
 */
@ComponentTest
public class ImplicitlyAllowedValuesPageQueryBuilderTest
{
    @InjectMockComponents
    private ImplicitlyAllowedValuesPageQueryBuilder queryBuilder;

    @Test
    public void build() throws Exception
    {
        PageClass pageClass = mock(PageClass.class);
        DBListClass dbListClass = mock(DBListClass.class);
        when(pageClass.clone()).thenReturn(dbListClass);

        this.queryBuilder.build(pageClass);
        verify(dbListClass).setIdField("doc.fullName");

        when(dbListClass.getIdField()).thenReturn("doc.name");
        this.queryBuilder.build(pageClass);
        // The method shouldn't be called once more when the id field is defined
        verify(dbListClass, times(1)).setIdField("doc.fullName");
    }
}
