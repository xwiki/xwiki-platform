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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.solr.common.SolrInputDocument;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.search.solr.internal.api.Fields;
import org.xwiki.search.solr.internal.reference.SolrReferenceResolver;

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
@Singleton
public class ObjectPropertySolrMetadataExtractor extends AbstractSolrMetadataExtractor
{
    /**
     * The Solr reference resolver.
     */
    @Inject
    @Named("object_property")
    private SolrReferenceResolver resolver;

    @Override
    public void addFieldsInternal(LengthSolrInputDocument solrDocument, EntityReference entityReference)
        throws Exception
    {
        ObjectPropertyReference objectPropertyReference = new ObjectPropertyReference(entityReference);

        BaseObjectReference objectReference = new BaseObjectReference(objectPropertyReference.getParent());
        DocumentReference classReference = objectReference.getXClassReference();
        DocumentReference documentReference = new DocumentReference(objectReference.getParent());

        XWikiDocument document = getDocument(documentReference);
        BaseProperty<ObjectPropertyReference> objectProperty = document.getXObjectProperty(objectPropertyReference);

        // Object
        solrDocument.addField(Fields.CLASS, localSerializer.serialize(classReference));
        solrDocument.addField(Fields.NUMBER, objectReference.getObjectNumber());

        // Property
        solrDocument.addField(Fields.PROPERTY_NAME, objectReference.getName());

        addLocaleAndContentFields(documentReference, solrDocument, objectProperty);
    }

    /**
     * Set the locale to all the translations that the owning document has. This ensures that this entity is found for
     * all the translations of a document, not just the original document.
     * <p/>
     * Also, index the content with each locale so that the right analyzer is used.
     * 
     * @param documentReference the original document's reference.
     * @param solrDocument the Solr document where to add the fields.
     * @param objectProperty the object property.
     * @throws Exception if problems occur.
     */
    protected void addLocaleAndContentFields(DocumentReference documentReference, SolrInputDocument solrDocument,
        BaseProperty<ObjectPropertyReference> objectProperty) throws Exception
    {
        XWikiDocument originalDocument = getDocument(documentReference);

        // Get all the locales in which the document is available.
        List<String> documentLocales = originalDocument.getTranslationList(this.xcontextProvider.get());
        // Make sure that the original document's locale is there as well.
        String originalDocumentLocale = getLocale(documentReference);
        if (!documentLocales.contains(originalDocumentLocale)) {
            documentLocales.add(originalDocumentLocale);
        }

        // Do the work for each locale.
        for (String documentLocale : documentLocales) {
            if (!documentLocale.equals(originalDocumentLocale)) {
                // The original document's locale is already set by the call to the addDocumentFields method.
                solrDocument.addField(Fields.LOCALE, documentLocale);
            }

            solrDocument.addField(String.format(Fields.MULTILIGNUAL_FORMAT, Fields.PROPERTY_VALUE, documentLocale),
                objectProperty.getValue());
        }

        // We can`t rely on the schema's copyField here because we would trigger it for each locale. Doing the copy to
        // the text_general field manually.
        solrDocument.addField(String.format(Fields.MULTILIGNUAL_FORMAT, Fields.PROPERTY_VALUE, Fields.MULTILINGUAL),
            objectProperty.getValue());
    }
}
