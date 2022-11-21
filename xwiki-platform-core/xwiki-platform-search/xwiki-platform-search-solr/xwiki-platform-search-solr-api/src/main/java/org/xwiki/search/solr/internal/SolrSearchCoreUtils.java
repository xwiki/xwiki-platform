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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.search.solr.internal.api.SolrIndexerException;
import org.xwiki.search.solr.internal.reference.SolrReferenceResolver;

/**
 * Provide various helpers around the Solr search code.
 * 
 * @version $Id$
 * @since 14.8RC1
 */
@Component(roles = SolrSearchCoreUtils.class)
@Singleton
public class SolrSearchCoreUtils
{
    @Inject
    private ComponentManager componentManager;

    /**
     * @param entityReference the reference of the entity
     * @return the Solr resolver associated to the entity type
     * @throws SolrIndexerException if any error
     */
    public SolrReferenceResolver getResolver(EntityReference entityReference) throws SolrIndexerException
    {
        try {
            return this.componentManager.getInstance(SolrReferenceResolver.class,
                entityReference.getType().getLowerCase());
        } catch (ComponentLookupException e) {
            throw new SolrIndexerException(
                "Faile to find solr reference resolver for type reference [" + entityReference + "]");
        }
    }

    /**
     * @param entityReference the reference of the entity
     * @return the id of the entity in the Solr search core
     * @throws SolrIndexerException if any error
     * @throws IllegalArgumentException if any error
     */
    public String getId(EntityReference entityReference) throws SolrIndexerException, IllegalArgumentException
    {
        return getResolver(entityReference).getId(entityReference);
    }
}
