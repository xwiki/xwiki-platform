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
import java.util.LinkedHashSet;
import java.util.Set;

import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.properties.converter.AbstractConverter;
import org.xwiki.whatsnew.NewsCategory;

/**
 * Converts News Categories defined as a comma-separated list of strings to a collection of {@link NewsCategory}
 * objects.
 *
 * @version $Id$
 * @since 15.2RC1
 */
@Component
@Singleton
public class CategoriesConverter extends AbstractConverter<Set<NewsCategory>>
{
    @Override
    protected Set<NewsCategory> convertToType(Type targetType, Object value)
    {
        if (value == null) {
            return Collections.emptySet();
        }

        if (value instanceof Set) {
            return (Set) value;
        }

        Set<NewsCategory> categories = new LinkedHashSet<>();
        String categoriesString = value.toString();
        for (String categoryString : StringUtils.split(categoriesString, ", ")) {
            if (NewsCategory.ADMIN_USER.toString().equalsIgnoreCase(categoryString)) {
                categories.add(NewsCategory.ADMIN_USER);
            } else if (NewsCategory.SIMPLE_USER.toString().equalsIgnoreCase(categoryString)) {
                categories.add(NewsCategory.SIMPLE_USER);
            } else if (NewsCategory.ADVANCED_USER.toString().equalsIgnoreCase(categoryString)) {
                categories.add(NewsCategory.ADVANCED_USER);
            } else if (NewsCategory.EXTENSION.toString().equalsIgnoreCase(categoryString)) {
                categories.add(NewsCategory.EXTENSION);
            } else {
                categories.add(NewsCategory.UNKNOWN);
            }
        }

        return categories;
    }
}
