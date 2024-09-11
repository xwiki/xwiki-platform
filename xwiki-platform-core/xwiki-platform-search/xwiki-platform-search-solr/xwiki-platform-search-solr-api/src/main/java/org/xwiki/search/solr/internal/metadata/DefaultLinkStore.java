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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.xwiki.component.annotation.Component;
import org.xwiki.link.LinkException;
import org.xwiki.link.LinkStore;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.reference.AbstractEntityReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.PageReference;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.SolrUtils;
import org.xwiki.search.solr.internal.SolrClientInstance;
import org.xwiki.search.solr.internal.SolrSearchCoreUtils;
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.search.solr.internal.api.SolrIndexer;
import org.xwiki.store.ReadyIndicator;

/**
 * @version $Id$
 */
@Component
@Singleton
public class DefaultLinkStore implements LinkStore
{
    @Inject
    private Solr solr;

    @Inject
    private SolrUtils utils;

    @Inject
    private SolrSearchCoreUtils searchUtils;

    @Inject
    private SolrLinkSerializer linkSerializer;

    @Inject
    private SolrIndexer solrIndexer;

    @Inject
    private EntityReferenceResolver<EntityReference> referenceConverter;

    @Inject
    @Named("withparameters")
    private EntityReferenceResolver<String> referenceResolver;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<PageReference> currentDocumentResolver;

    private SolrClient getClient() throws LinkException
    {
        try {
            return this.solr.getClient(SolrClientInstance.CORE_NAME);
        } catch (SolrException e) {
            throw new LinkException("Failed to acces Solr search core", e);
        }
    }

    @Override
    @SuppressWarnings("resource")
    public Set<EntityReference> resolveLinkedEntities(EntityReference target) throws LinkException
    {
        // Get the id of the reference
        String solrID;
        try {
            solrID = this.searchUtils.getId(target);
        } catch (Exception e) {
            throw new LinkException("Failed to serialize the Solr id for the reference [" + target + "]", e);
        }

        // Load the Solr document corresponding to the reference
        SolrDocument solrDocument;
        try {
            solrDocument = getClient().getById(solrID);
        } catch (Exception e) {
            throw new LinkException(
                "Failed to load the Solr document for the reference [" + target + "] (id [" + solrID + "])", e);
        }

        if (solrDocument == null) {
            return Collections.emptySet();
        }

        // Get the links from the solr document
        Collection<Object> links = solrDocument.getFieldValues(FieldUtils.LINKS);
        Set<EntityReference> entities = new HashSet<>(links.size());
        for (Object link : links) {
            if (link instanceof String) {
                EntityReference entityLink = this.linkSerializer.unserialize((String) link);

                if (entityLink != null) {
                    // Make sure to resolve the reference as a DOCUMENT based references and not a PAGE one
                    EntityReference documentLink = toDocumentBasedReference(entityLink);

                    entities.add(documentLink);
                }
            }
        }

        return entities;
    }

    @Override
    public Set<EntityReference> resolveBackLinkedEntities(EntityReference reference) throws LinkException
    {
        // Get the PAGE based reference
        EntityReference pageBasedReference = toPageBasedReference(reference);
        // Get the DOCUMENT based reference
        EntityReference documentBasedReference = toDocumentBasedReference(reference);

        // Search for the Solr entities which contains extended links with the resolved DOCUMENT and PAGE references

        // Get only the entities with links to the passed reference (either as DOCUMENT or PAGE version)
        StringBuilder filter = new StringBuilder();
        if (pageBasedReference != null) {
            filter.append(FieldUtils.LINKS_EXTENDED);
            filter.append(':');
            filter.append(this.utils.toCompleteFilterQueryString(this.linkSerializer.serialize(pageBasedReference)));
        }
        if (filter.length() > 0) {
            filter.append(" OR ");
        }
        filter.append(FieldUtils.LINKS_EXTENDED);
        filter.append(':');
        filter.append(this.utils.toCompleteFilterQueryString(this.linkSerializer.serialize(documentBasedReference)));

        SolrQuery solrQuery = new SolrQuery(filter.toString());

        solrQuery.setRows(Integer.MAX_VALUE - 1);

        // Load only the field we need
        solrQuery.setFields(FieldUtils.REFERENCE);

        QueryResponse response;
        try {
            response = getClient().query(solrQuery);
        } catch (Exception e) {
            throw new LinkException("Failed to search Solr for the backlinks of an entity", e);
        }

        SolrDocumentList solrDocuments = response.getResults();

        Set<EntityReference> references = new HashSet<>(solrDocuments.size());
        for (SolrDocument solrDocument : solrDocuments) {
            String referenceStr = (String) solrDocument.getFieldValue(FieldUtils.REFERENCE);

            if (referenceStr != null) {
                references.add(this.referenceResolver.resolve(referenceStr, null));
            }
        }

        return references;
    }

    @Override
    public ReadyIndicator waitReady()
    {
        return this.solrIndexer.getReadyIndicator();
    }

    EntityReference toDocumentBasedReference(EntityReference entityReference)
    {
        // Check if it's already a DOCUMENT based reference
        EntityReference pageReference = entityReference.extractReference(EntityType.PAGE);
        if (pageReference == null) {
            return entityReference;
        }

        if (entityReference.getType() == EntityType.PAGE) {
            return this.currentDocumentResolver.resolve(pageReference instanceof PageReference
                ? (PageReference) pageReference : new PageReference(pageReference));
        }

        EntityType documentBasedType =
            AbstractEntityReferenceResolver.TYPE_CONVERT_MAPPING.get(entityReference.getType());

        // Convert to DOCUMENT based reference
        EntityReference documentBasedReference = this.referenceConverter.resolve(entityReference, documentBasedType);

        // Find the right DOCUMENT reference
        DocumentReference documentReference = this.currentDocumentResolver.resolve(
            pageReference instanceof PageReference ? (PageReference) pageReference : new PageReference(pageReference));

        // Switch the parent
        return documentBasedReference.replaceParent(documentBasedReference.extractReference(EntityType.DOCUMENT),
            documentReference);
    }

    EntityReference toPageBasedReference(EntityReference entityReference)
    {
        // Check if it's already a PAGE based reference
        if (entityReference.extractReference(EntityType.PAGE) != null) {
            return entityReference;
        }

        EntityType documentBasedType;
        if (entityReference.getType() == EntityType.DOCUMENT) {
            documentBasedType = EntityType.PAGE;
        } else {
            documentBasedType = AbstractEntityReferenceResolver.TYPE_CONVERT_MAPPING.get(entityReference.getType());
        }

        // Convert the reference to PAGE based reference
        EntityReference pageBasedReference = this.referenceConverter.resolve(entityReference, documentBasedType);

        // Make sure it really is the same document currently
        if (entityReference.extractReference(EntityType.DOCUMENT) != null) {
            EntityReference documentBasedReference = toDocumentBasedReference(pageBasedReference);
            if (!documentBasedReference.equals(entityReference)) {
                // It's not the same document so we cannot use it
                return null;
            }
        }

        return pageBasedReference;
    }
}
