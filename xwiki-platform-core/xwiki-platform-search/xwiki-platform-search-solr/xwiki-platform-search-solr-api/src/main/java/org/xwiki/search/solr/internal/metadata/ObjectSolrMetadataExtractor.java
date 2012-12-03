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

import javax.inject.Named;

import org.apache.solr.common.SolrInputDocument;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.search.solr.internal.api.Fields;
import org.xwiki.search.solr.internal.api.SolrIndexException;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * Extract the metadata to be indexed from attachments.
 * 
 * @version $Id$
 * @since 4.3M2
 */
@Component
@Named("object")
public class ObjectSolrMetadataExtractor extends AbstractSolrMetadataExtractor
{
    @Override
    public SolrInputDocument getSolrDocument(EntityReference entityReference) throws SolrIndexException,
        IllegalArgumentException
    {
        BaseObjectReference objectReference = new BaseObjectReference(entityReference);

        try {
            SolrInputDocument solrDocument = new SolrInputDocument();

            DocumentReference classReference = objectReference.getXClassReference();
            DocumentReference documentReference = new DocumentReference(objectReference.getParent());
            XWikiDocument document = getDocument(documentReference);

            BaseObject object = document.getXObject(objectReference);

            solrDocument.addField(Fields.ID, getId(object.getReference()));
            addDocumentFields(documentReference, solrDocument);
            solrDocument.addField(Fields.TYPE, objectReference.getType().name());
            solrDocument.addField(Fields.CLASS, compactSerializer.serialize(classReference));

            this.addObjectContent(solrDocument, object);

            return solrDocument;
        } catch (Exception e) {
            throw new SolrIndexException(String.format("Failed to get Solr document for '%s'",
                serializer.serialize(objectReference)), e);
        }
    }
}
