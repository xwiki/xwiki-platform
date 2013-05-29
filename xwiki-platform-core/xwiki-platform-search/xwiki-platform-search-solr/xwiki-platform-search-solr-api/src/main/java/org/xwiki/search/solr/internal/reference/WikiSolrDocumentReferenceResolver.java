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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.search.solr.internal.api.SolrIndexException;

/**
 * Resolve wiki references.
 * 
 * @version $Id$
 * @since 5.1M2
 */
@Component
@Named("wiki")
@Singleton
public class WikiSolrDocumentReferenceResolver extends AbstractSolrDocumentReferenceResolver
{
    /**
     * Used to resolve space references.
     */
    @Inject
    @Named("space")
    private SolrDocumentReferenceResolver spaceResolver;

    /**
     * Query manager used to perform queries on the XWiki model.
     */
    @Inject
    private QueryManager queryManager;

    @Override
    public List<EntityReference> getReferences(EntityReference wikiReference) throws SolrIndexException
    {
        List<EntityReference> result = new ArrayList<EntityReference>();

        // Ignore the wiki reference because it is not indexable.

        List<String> spaces = null;

        // Make sure the list of spaces is from the requested wiki.
        try {
            spaces = this.queryManager.getNamedQuery("getSpaces").setWiki(wikiReference.getName()).execute();
        } catch (QueryException e) {
            throw new SolrIndexException("Failed to query wiki [" + wikiReference.getName() + "] spaces", e);
        }

        // Visit each space
        for (String space : spaces) {
            SpaceReference spaceReference = new SpaceReference(space, wikiReference);

            try {
                result.addAll(this.spaceResolver.getReferences(spaceReference));
            } catch (Exception e) {
                this.logger.error("Failed to resolve references for space [" + spaceReference + "]", e);
            }
        }

        return result;
    }
}
