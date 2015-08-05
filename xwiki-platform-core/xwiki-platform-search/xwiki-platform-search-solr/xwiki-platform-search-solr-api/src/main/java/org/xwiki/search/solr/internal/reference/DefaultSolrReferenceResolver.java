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
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.search.solr.internal.api.SolrIndexerException;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

/**
 * Dispatch to the proper {@link SolrReferenceResolver}.
 * 
 * @version $Id$
 * @since 5.1M2
 */
@Component
@Singleton
public class DefaultSolrReferenceResolver implements SolrReferenceResolver
{
    /**
     * Lazily get reference one wiki at a time.
     * 
     * @version $Id$
     */
    class FarmIterator implements Iterator<EntityReference>
    {
        /**
         * The current iterator.
         */
        private Iterator<EntityReference> currentIterator;

        /**
         * The current wiki.
         */
        private final Iterator<String> currentWiki;

        /**
         * @param wikis the wikis
         */
        public FarmIterator(List<String> wikis)
        {
            this.currentWiki = wikis.iterator();
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasNext()
        {
            update();

            return currentIterator != null;
        }

        @Override
        public EntityReference next()
        {
            update();

            return currentIterator != null ? currentIterator.next() : null;
        }

        /**
         * Make sure to point the caret to the right element.
         */
        private void update()
        {
            if (currentIterator == null || !currentIterator.hasNext()) {
                if (currentWiki.hasNext()) {
                    String wiki = currentWiki.next();
                    try {
                        currentIterator = getReferences(new WikiReference(wiki)).iterator();
                    } catch (SolrIndexerException e) {
                        logger.error("Failed to get references for wiki [" + wiki + "]", e);
                    }

                    if (!currentIterator.hasNext()) {
                        update();
                    }
                } else {
                    currentIterator = null;
                }
            }
        }
    }

    /**
     * Used to find the {@link SolrReferenceResolver}.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * The logger.
     */
    @Inject
    private Logger logger;

    /**
     * Used to get the list of available wikis.
     */
    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    /**
     * @param reference the reference
     * @return the resolver associated to the reference type
     * @throws SolrIndexerException when failed to find a resolve associated to the passed reference
     */
    private SolrReferenceResolver getResover(EntityReference reference) throws SolrIndexerException
    {
        EntityType type = reference.getType();

        SolrReferenceResolver resolver;
        try {
            resolver = this.componentManager.getInstance(SolrReferenceResolver.class, type.getLowerCase());
        } catch (ComponentLookupException e) {
            throw new SolrIndexerException("Failed to get SolrDocumentReferenceResolver corresponding to entity type ["
                + type + "]", e);
        }

        return resolver;
    }

    @Override
    public Iterable<EntityReference> getReferences(EntityReference reference) throws SolrIndexerException
    {
        if (reference != null) {
            return getResover(reference).getReferences(reference);
        } else {
            final List<String> wikis;
            try {
                wikis = new ArrayList<String>(this.wikiDescriptorManager.getAllIds());
            } catch (WikiManagerException e) {
                throw new SolrIndexerException("Failed to get the list of available wikis.", e);
            }

            return new Iterable<EntityReference>()
            {
                @Override
                public Iterator<EntityReference> iterator()
                {
                    return new FarmIterator(wikis);
                }
            };
        }
    }

    @Override
    public String getId(EntityReference reference) throws SolrIndexerException, IllegalArgumentException
    {
        if (reference != null) {
            return getResover(reference).getId(reference);
        } else {
            return null;
        }
    }

    @Override
    public String getQuery(EntityReference reference) throws SolrIndexerException
    {
        if (reference != null) {
            return getResover(reference).getQuery(reference);
        } else {
            return "*:*";
        }
    }
}
