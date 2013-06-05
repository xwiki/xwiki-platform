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
import org.xwiki.search.solr.internal.api.Fields;
import org.xwiki.search.solr.internal.reference.SolrReferenceResolver;

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
@Singleton
public class ObjectSolrMetadataExtractor extends AbstractSolrMetadataExtractor
{
    /**
     * The Solr reference resolver.
     */
    @Inject
    @Named("object")
    private SolrReferenceResolver resolver;

    @Override
    public void addFieldsInternal(LengthSolrInputDocument solrDocument, EntityReference entityReference)
        throws Exception
    {
        BaseObjectReference objectReference = new BaseObjectReference(entityReference);

        DocumentReference classReference = objectReference.getXClassReference();
        DocumentReference documentReference = new DocumentReference(objectReference.getParent());
        XWikiDocument document = getDocument(documentReference);

        BaseObject object = document.getXObject(objectReference);

        solrDocument.addField(Fields.ID, resolver.getId(object.getReference()));
        addDocumentFields(documentReference, solrDocument);
        solrDocument.addField(Fields.TYPE, objectReference.getType().name());
        solrDocument.addField(Fields.CLASS, localSerializer.serialize(classReference));
        solrDocument.addField(Fields.NUMBER, objectReference.getObjectNumber());

        addLocaleAndContentFields(documentReference, solrDocument, object);
    }

    /**
     * Set the locale to all the translations that the owning document has. This ensures that this entity is found for
     * all the translations of a document, not just the original document.
     * <p/>
     * Also, index the content with each locale so that the right analyzer is used.
     * 
     * @param documentReference the original document's reference.
     * @param solrDocument the Solr document where to add the fields.
     * @param object the object.
     * @throws Exception if problems occur.
     */
    protected void addLocaleAndContentFields(DocumentReference documentReference, SolrInputDocument solrDocument,
        BaseObject object) throws Exception
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

            addObjectContent(solrDocument, object, documentLocale);
        }

        // We can`t rely on the schema's copyField here because we would trigger it for each locale. Doing the copy to
        // the text_general field manually.
        addObjectContent(solrDocument, object, Fields.MULTILINGUAL);
    }
}
