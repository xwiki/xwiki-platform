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

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.search.solr.internal.api.SolrIndexerException;

/**
 * Internal component interface for handling the extraction of metadata into a {@link SolrInputDocument} from XWiki
 * entities reference. Each entity type is supported by a specific implementation so the correct implementation must be
 * used with the correct entity type.
 * <p>
 * The implementation use as hint the same value as returned by something like
 * {@code org.xwiki.model.EntityType.DOCUMENT.name().toLowerCase()} so they are easily retrievable, if they exist.
 * 
 * @version $Id$
 * @since 4.3M2
 */
@Role
public interface SolrMetadataExtractor
{
    /**
     * Extract data from an XWiki entity and wrap it into a {@link SolrInputDocument} that is indexable by Solr.
     * 
     * @param entityReference the reference to the entity.
     * @return the {@link SolrInputDocument} containing the fields to be indexed for the entity.
     * @throws SolrIndexerException if problems occur.
     * @throws IllegalArgumentException if the passed reference is not supported by the current implementation.
     */
    LengthSolrInputDocument getSolrDocument(EntityReference entityReference) throws SolrIndexerException,
        IllegalArgumentException;
}
