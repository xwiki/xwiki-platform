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
package org.xwiki.search.solr.internal.api;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

/**
 * Component that accepts XWiki {@link EntityReference}s to be indexed or deleted from the index if they exist. The
 * references are expanded hierarchically, in the sense that all references beneath it will be processed as well. This
 * is done to try to ensure consistency of the index.
 * 
 * @version $Id$
 * @since 5.1M2
 */
@Unstable
@Role
public interface SolrIndexer
{
    /**
     * Add an entity to the queue of entities to index.
     * <p>
     * Null reference means the whole farm.
     * 
     * @param reference the entity's reference.
     * @param recurse indicate if children entities should be indexed too
     */
    void index(EntityReference reference, boolean recurse);

    /**
     * Add an entity to the queue of entities to delete.
     * <p>
     * Null reference means the whole farm.
     * 
     * @param reference the entity's reference.
     * @param recurse indicate if children entities should be removed too
     */
    void delete(EntityReference reference, boolean recurse);
}
