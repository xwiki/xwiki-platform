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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.solr.common.SolrInputDocument;

/**
 * Extended SolrInputDocument with calculated size.
 * 
 * @version $Id$
 * @since 5.1M2
 */
public class LengthSolrInputDocument extends SolrInputDocument
{
    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    /**
     * @see #getLength()
     */
    private int length;

    private final Map<String, Set<Object>> uniqueFields = new HashMap<>();

    /**
     * @return the length (generally the number of characters). It's not the exact byte length, it's more a scale value.
     */
    public int getLength()
    {
        return this.length;
    }

    @Override
    public void setField(String name, Object value)
    {
        super.setField(name, value);

        if (value instanceof String) {
            this.length += ((String) value).length();
        } else if (value instanceof byte[]) {
            this.length += ((byte[]) value).length;
        }

        this.uniqueFields.remove(name);

        // TODO: support more type ?
    }

    /**
     * Add a value to a field if it's not already present.
     *
     * @param name the field name
     * @param value the value to add
     * @since 16.4.7
     * @since 16.10.5
     * @since 17.2.0RC1
     */
    protected void addFieldOnce(String name, Object value)
    {
        Set<Object> existingValues = this.uniqueFields.computeIfAbsent(name, k -> {
            Collection<Object> fieldValues = getFieldValues(name);
            if (fieldValues != null) {
                return new HashSet<>(fieldValues);
            } else {
                return new HashSet<>();
            }
        });

        if (existingValues.add(value)) {
            addField(name, value);
        }
    }

    @Override
    public void addField(String name, Object value)
    {
        // Add the value, but only if the field was already used with the "once" method.
        // This adds the value again when called from the "once" method.
        this.uniqueFields.computeIfPresent(name, (k, existingValues) -> {
            existingValues.add(value);
            return existingValues;
        });
        super.addField(name, value);
    }
}
