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

import java.lang.reflect.ParameterizedType;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.internal.multi.ComponentManagerManager;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.EntityType;
import org.xwiki.model.namespace.WikiNamespace;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.search.solr.SolrEntityMetadataExtractor;

/**
 * @version $Id$
 */
@Component(roles = SolrMetadataExtractorUtils.class)
@Singleton
public class SolrMetadataExtractorUtils
{
    @Inject
    private ComponentManagerManager componentManagerManager;

    @Inject
    private ComponentManager defaultComponentManager;

    @Inject
    private Logger logger;

    /**
     * @param reference the reference of the entity to read
     * @param entity the entity to read
     * @param solrDocument the Solr document to increment
     * @return true of the Solr document was updated
     */
    public boolean extract(EntityReference reference, Object entity, SolrInputDocument solrDocument)
    {
        // Create the right generic type
        ParameterizedType type =
            new DefaultParameterizedType(null, SolrEntityMetadataExtractor.class, entity.getClass());

        // Get the right ComponentManager
        EntityReference wikiReference = reference.extractReference(EntityType.WIKI);
        ComponentManager namespaceComponentManager = this.componentManagerManager
            .getComponentManager(new WikiNamespace(wikiReference.getName()).serialize(), false);
        if (namespaceComponentManager == null) {
            // Fallback on this component ComponentManager
            namespaceComponentManager = this.defaultComponentManager;
        }

        // Call all SolrEntityMetadataExtractor instances
        boolean updated = false;
        try {
            for (SolrEntityMetadataExtractor<Object> extractor : namespaceComponentManager
                .<SolrEntityMetadataExtractor<Object>>getInstanceList(type)) {
                updated |= extractor.extract(entity, solrDocument);
            }
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to retrieve SolrEntityMetadataExtractor instances for type [{}]", type, e);
        }

        return updated;
    }
}
