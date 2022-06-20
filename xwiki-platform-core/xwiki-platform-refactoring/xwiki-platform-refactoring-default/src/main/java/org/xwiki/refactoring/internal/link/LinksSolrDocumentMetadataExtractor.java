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
package org.xwiki.refactoring.internal.link;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.solr.common.SolrInputDocument;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.search.solr.SolrEntityMetadataExtractor;
import org.xwiki.search.solr.internal.api.FieldUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 * @since 14.8RC1
 */
@Component
@Singleton
public class LinksSolrDocumentMetadataExtractor implements SolrEntityMetadataExtractor<XWikiDocument>
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private LinkSerializer linkSerializer;

    @Override
    public boolean extract(XWikiDocument entity, SolrInputDocument solrDocument)
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        // Extract links
        // TODO: support more than EntityReference (extract and index any type of link found in the content)
        Set<EntityReference> references = entity.getUniqueLinkedEntities(xcontext);

        if (!references.isEmpty()) {
            Set<String> links = new HashSet<>(references.size());
            Set<String> linksExtended = new HashSet<>(references.size() * 2);

            // Serialize the links and resolve the extended links
            for (EntityReference reference : references) {
                String referenceString = serialize(reference);

                links.add(referenceString);
                linksExtended.add(referenceString);

                // Add the reference without parameters as well as all its parents to the extended list
                extend(reference, linksExtended);
            }

            // Add the links to the Solr document
            for (String link : links) {
                solrDocument.addField(FieldUtils.LINKS, link);
            }
            for (String linkExtended : linksExtended) {
                solrDocument.addField(FieldUtils.LINKS_EXTENDED, linkExtended);
            }

            return true;
        }

        return false;
    }

    private void extend(EntityReference reference, Set<String> linksExtended)
    {
        for (EntityReference parent =
            reference.getParameters().isEmpty() ? reference : new EntityReference(reference.getName(),
                reference.getType(), reference.getParent(), null); parent != null; parent = parent.getParent()) {
            linksExtended.add(serialize(parent));
        }
    }

    private String serialize(EntityReference reference)
    {
        return this.linkSerializer.serialize(reference);
    }
}
