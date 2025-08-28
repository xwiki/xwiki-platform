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
package org.xwiki.index.tree.internal.macro;

import java.lang.reflect.Type;
import java.util.Objects;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.properties.converter.AbstractConverter;

/**
 * Converts a string to a {@link DocumentSort}.
 *
 * @version $Id$
 * @since 16.10.3
 * @since 17.0.0RC1
 */
@Component
@Singleton
public class DocumentSortConverter extends AbstractConverter<DocumentSort>
{
    @SuppressWarnings("unchecked")
    @Override
    protected DocumentSort convertToType(Type targetType, Object value)
    {
        if (value == null) {
            return null;
        }

        String[] parts = value.toString().split(":", 2);
        String field = parts[0];
        if (field.isEmpty()) {
            field = null;
        }

        Boolean ascending = null;
        if (parts.length == 2) {
            String order = parts[1];
            if ("asc".equalsIgnoreCase(order)) {
                ascending = true;
            } else if ("desc".equalsIgnoreCase(order)) {
                ascending = false;
            }
        }

        return new DocumentSort(field, ascending);
    }

    @Override
    protected String convertToString(DocumentSort sort)
    {
        return Objects.toString(sort, "");
    }
}
