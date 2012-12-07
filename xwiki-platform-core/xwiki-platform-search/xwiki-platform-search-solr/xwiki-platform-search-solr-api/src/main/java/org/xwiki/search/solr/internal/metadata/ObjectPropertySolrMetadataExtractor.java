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
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.search.solr.internal.api.Fields;
import org.xwiki.search.solr.internal.api.SolrIndexException;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObjectReference;
import com.xpn.xwiki.objects.BaseProperty;

/**
 * Extract the metadata to be indexed from object properties.
 * 
 * @version $Id$
 * @since 4.3M2
 */
@Component
@Named("object_property")
public class ObjectPropertySolrMetadataExtractor extends AbstractSolrMetadataExtractor
{
    @Override
    public SolrInputDocument getSolrDocument(EntityReference entityReference) throws SolrIndexException,
        IllegalArgumentException
    {
        ObjectPropertyReference objectPropertyReference = new ObjectPropertyReference(entityReference);

        try {
            SolrInputDocument solrDocument = new SolrInputDocument();

            BaseObjectReference objectReference = new BaseObjectReference(objectPropertyReference.getParent());
            DocumentReference classReference = objectReference.getXClassReference();
            DocumentReference documentReference = new DocumentReference(objectReference.getParent());

            XWikiDocument document = getDocument(documentReference);
            BaseProperty<ObjectPropertyReference> objectProperty = document.getXObjectProperty(objectPropertyReference);

            solrDocument.addField(Fields.ID, getId(objectPropertyReference));
            addDocumentFields(documentReference, solrDocument);
            solrDocument.addField(Fields.TYPE, objectPropertyReference.getType().name());
            solrDocument.addField(Fields.CLASS, compactSerializer.serialize(classReference));
            solrDocument.addField(Fields.PROPERTY_NAME, objectProperty.getName());
            solrDocument.addField(Fields.PROPERTY_VALUE, objectProperty.getValue());

            return solrDocument;
        } catch (Exception e) {
            throw new SolrIndexException(String.format("Failed to get Solr document for '%s'",
                serializer.serialize(objectPropertyReference)), e);
        }
    }
}
