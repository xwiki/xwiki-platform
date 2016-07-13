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
package org.xwiki.search.solr.internal.reference;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.search.solr.internal.api.SolrIndexerException;

/**
 * Recursively retrieves the references for all the indexable entities contained by the given start entity.
 * 
 * @version $Id$
 * @since 5.1M2
 */
@Role
public interface SolrReferenceResolver
{
    /**
     * @param rootReference the root entity reference.
     * @return references for the indexable contained entities, including the given one (but only if it is indexable).
     * @throws SolrIndexerException if problems occur.
     */
    Iterable<EntityReference> getReferences(EntityReference rootReference) throws SolrIndexerException;

    /**
     * @param reference reference to an entity.
     * @return the ID of the entity, as it is used in the index.
     * @throws SolrIndexerException if problems occur.
     * @throws IllegalArgumentException if the passed reference is not supported by the current implementation.
     */
    String getId(EntityReference reference) throws SolrIndexerException, IllegalArgumentException;

    /**
     * @param reference reference to an entity.
     * @return the criteria to access this entity (and its children)
     * @throws SolrIndexerException when failing to generate query
     */
    String getQuery(EntityReference reference) throws SolrIndexerException;
}
