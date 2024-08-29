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

import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.solr.common.SolrInputDocument;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.search.solr.internal.api.FieldUtils;
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
    public boolean setFieldsInternal(LengthSolrInputDocument solrDocument, EntityReference entityReference)
        throws Exception
    {
        BaseObjectReference objectReference = new BaseObjectReference(entityReference);

        DocumentReference classReference = objectReference.getXClassReference();
        DocumentReference documentReference = new DocumentReference(objectReference.getParent());

        XWikiDocument originalDocument = getDocument(documentReference);
        BaseObject object = originalDocument.getXObject(objectReference);
        if (object == null) {
            return false;
        }

        solrDocument.setField(FieldUtils.ID, resolver.getId(object.getReference()));
        setDocumentFields(documentReference, solrDocument);
        solrDocument.setField(FieldUtils.TYPE, objectReference.getType().name());
        solrDocument.setField(FieldUtils.CLASS, localSerializer.serialize(classReference));
        solrDocument.setField(FieldUtils.NUMBER, objectReference.getObjectNumber());

        setLocaleAndContentFields(documentReference, solrDocument, object);

        // TODO: Add links found in the object

        // Extract more metadata
        this.extractorUtils.extract(objectReference, object, solrDocument);

        return true;
    }

    /**
     * Set the locale to all the translations that the owning document has. This ensures that this entity is found for
     * all the translations of a document, not just the original document.
     * <p>
     * Also, index the content with each locale so that the right analyzer is used.
     * 
     * @param documentReference the original document's reference.
     * @param solrDocument the Solr document where to add the fields.
     * @param object the object.
     * @throws Exception if problems occur.
     */
    protected void setLocaleAndContentFields(DocumentReference documentReference, SolrInputDocument solrDocument,
        BaseObject object) throws Exception
    {
        // Do the work for each locale.
        for (Locale documentLocale : getLocales(documentReference, null)) {
            solrDocument.addField(FieldUtils.LOCALES, documentLocale);

            setObjectContent(solrDocument, object, documentLocale);
        }

        // We can`t rely on the schema's copyField here because we would trigger it for each locale. Doing the copy to
        // the text_general field manually.
        setObjectContent(solrDocument, object, null);
    }
}
