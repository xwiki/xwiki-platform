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

import java.util.Objects;

/**
 * Indicates how to sort documents. Used by the {@code sortDocumentsBy} parameter of the Document Tree Macro.
 * 
 * @version $Id$
 * @since 16.10.3
 * @since 17.0.0RC1
 */
public class DocumentSort
{
    private String field;

    private Boolean ascending;

    /**
     * Creates a new document sort.
     * 
     * @param field the document field to sort by
     * @param ascending {@code true} to sort in ascending order, {@code false} to sort in descending order, {@code null}
     *            to use the default order
     */
    public DocumentSort(String field, Boolean ascending)
    {
        this.field = field;
        this.ascending = ascending;
    }

    /**
     * @return the document field to sort by
     */
    public String getField()
    {
        return field;
    }

    /**
     * @return {@code true} to sort in ascending order, {@code false} to sort in descending order, {@code null} to use
     *         the default order
     */
    public Boolean isAscending()
    {
        return ascending;
    }

    @Override
    public String toString()
    {
        StringBuilder output = new StringBuilder(Objects.toString(getField(), ""));
        if (isAscending() != null) {
            output.append(":").append(Boolean.TRUE.equals(isAscending()) ? "asc" : "desc");
        }
        return output.toString();
    }
}
