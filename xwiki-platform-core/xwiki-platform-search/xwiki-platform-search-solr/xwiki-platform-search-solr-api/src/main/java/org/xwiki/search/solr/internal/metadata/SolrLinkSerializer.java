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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * Help serialize and unserialize links stored in Solr.
 * 
 * @version $Id$
 * @since 14.8RC1
 */
@Component(roles = SolrLinkSerializer.class)
@Singleton
public class SolrLinkSerializer
{
    // TODO: add support for more than entities
    private static final String ENTITY_PREFIX = "entity:";

    @Inject
    @Named("withtype/withparameters")
    private EntityReferenceSerializer<String> entitySerializer;

    @Inject
    @Named("withparameters")
    private EntityReferenceResolver<String> entityResolver;

    /**
     * @param reference the entity reference to serialize
     * @return the serialized entity reference
     */
    public String serialize(EntityReference reference)
    {
        // TODO: add support for more than entities
        // In the meantime make sure to use a syntax generic enough to support more than entities in the future
        return ENTITY_PREFIX + this.entitySerializer.serialize(reference);
    }

    /**
     * @param link the link to unserialize
     * @return the unserialized link
     */
    public EntityReference unserialize(String link)
    {
        // TODO: add support for more than entities
        if (link.startsWith(ENTITY_PREFIX)) {
            return this.entityResolver.resolve(link.substring(ENTITY_PREFIX.length()), null);
        }

        return null;
    }
}
