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
package org.xwiki.search.solr.internal;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.search.solr.internal.api.SolrIndexException;

/**
 * Recursively retrieves the references for all the indexable entities contained by the given start entity.
 * 
 * @version $Id$
 * @since 4.3M2
 */
@Role
public interface IndexableReferenceExtractor
{
    /**
     * @param startReference the start entity reference.
     * @return references for the indexable contained entities, including the given one (but only if it is indexable).
     * @throws SolrIndexException if problems occur.
     */
    List<EntityReference> getReferences(EntityReference startReference) throws SolrIndexException;
}
