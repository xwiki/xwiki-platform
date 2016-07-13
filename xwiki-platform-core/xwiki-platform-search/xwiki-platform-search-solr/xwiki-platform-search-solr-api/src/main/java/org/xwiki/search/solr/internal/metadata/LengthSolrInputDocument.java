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

    /**
     * @return the length (generally the number of characters). It's not the exact byte length, it's more a scale value.
     */
    public int getLength()
    {
        return this.length;
    }

    @Override
    public void setField(String name, Object value, float boost)
    {
        super.setField(name, value, boost);

        if (value instanceof String) {
            this.length += ((String) value).length();
        } else if (value instanceof byte[]) {
            this.length += ((byte[]) value).length;
        }

        // TODO: support more type ?
    }
}
