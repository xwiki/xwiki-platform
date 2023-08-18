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
package org.xwiki.whatsnew.internal;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.whatsnew.NewsCategory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link CategoriesConverter}.
 *
 * @version $Id$
 */
@ComponentTest
class CategoriesConverterTest
{
    @InjectMockComponents
    private CategoriesConverter converter;

    @Test
    void convert()
    {
        Type type = new DefaultParameterizedType(null, Set.class, NewsCategory.class);
        Set<NewsCategory> results =
            this.converter.convert(type, "admin_user, advanced_user, simple_user, extension, something");

        assertTrue(results.contains(NewsCategory.SIMPLE_USER));
        assertTrue(results.contains(NewsCategory.ADMIN_USER));
        assertTrue(results.contains(NewsCategory.ADVANCED_USER));
        assertTrue(results.contains(NewsCategory.EXTENSION));
        assertTrue(results.contains(NewsCategory.UNKNOWN));
    }

    @Test
    void convertWhenNull()
    {
        Type type = new DefaultParameterizedType(null, Set.class, NewsCategory.class);

        assertEquals(Collections.emptySet(), this.converter.convert(type, null));
    }
}
