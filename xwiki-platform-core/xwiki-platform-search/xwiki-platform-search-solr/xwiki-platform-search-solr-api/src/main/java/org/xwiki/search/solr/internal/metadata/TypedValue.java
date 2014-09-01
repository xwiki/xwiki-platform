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
package org.xwiki.search.solr.internal.metadata;

import java.util.Date;

/**
 * A (value, type) pair.
 * 
 * @version $Id$
 * @since 5.3RC1
 */
public final class TypedValue
{
    /**
     * The string type is used for fields that require exact matching. String fields are stored as is in the index
     * without being analyzed.
     */
    public static final String STRING = "string";

    /**
     * The text type is used for fields that should be analyzed (split in tokens, strip stop words, etc.). These fields
     * usually contain free text and are indexed in a specific locale.
     */
    public static final String TEXT = null;

    private final Object value;

    private final String type;

    /**
     * Creates a new (value, type) pair where the type is inferred from the value.
     * 
     * @param value the value
     */
    public TypedValue(Object value)
    {
        this(value, typeOf(value));
    }

    /**
     * Creates a new (value, type) pair.
     * 
     * @param value the value
     * @param type the data type
     */
    public TypedValue(Object value, String type)
    {
        this.value = value;
        this.type = type;
    }

    /**
     * @return the value
     */
    public Object getValue()
    {
        return value;
    }

    /**
     * @return the data type
     */
    public String getType()
    {
        return type;
    }

    /**
     * Utility method that can be used to get a suffix to add to a dynamic field name so that its value is indexed
     * properly.
     * 
     * @param value the value of a dynamic field
     * @return the corresponding type, as per schema.xml, or {@code null} if the given value doesn't have a type known
     *         by schema.xml
     */
    private static String typeOf(Object value)
    {
        if (value instanceof Integer) {
            // We could have grouped Integer with the rest of the final types but we use it's short name in schema.xml
            return "int";
        } else if (value instanceof Date) {
            // Date is not final so we use "date" for any of its subclasses.
            return "date";
        } else if (value instanceof Boolean || value instanceof Long || value instanceof Double
            || value instanceof Float) {
            // All these types are final so we are safe with using the simple class name.
            return value.getClass().getSimpleName().toLowerCase();
        }

        // If we don't know the type then we index the value as string to be able to perform exact matches.
        return STRING;
    }
}
