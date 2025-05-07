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
package org.xwiki.search.solr;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.IndexableField;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.schema.StrField;

/**
 * A custom Solr field type that transforms the value to lowercase for indexing, storage, and query.
 *
 * @version $Id$
 * @since 17.3.0RC1
 */
public class LowerCaseStrField extends StrField
{
    @Override
    public List<IndexableField> createFields(SchemaField field, Object value)
    {
        // Transform the value here, too, to ensure that the value is in lowercase also when stored as doc value.
        Object val;
        if (value instanceof String inputString) {
            val = toInternal(inputString);
        } else {
            val = value;
        }

        return super.createFields(field, val);
    }

    @Override
    public String toInternal(String val)
    {
        // Transform the value to lowercase for indexing, storage, and query.
        return StringUtils.toRootLowerCase(val);
    }
}
