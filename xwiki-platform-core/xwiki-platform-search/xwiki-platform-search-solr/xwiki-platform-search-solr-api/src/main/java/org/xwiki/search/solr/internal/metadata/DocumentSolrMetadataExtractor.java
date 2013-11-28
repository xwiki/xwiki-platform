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
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.solr.common.SolrInputDocument;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.search.solr.internal.reference.SolrReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Extract the metadata to be indexed from document.
 * 
 * @version $Id$
 * @since 4.3M2
 */
@Component
@Named("document")
@Singleton
public class DocumentSolrMetadataExtractor extends AbstractSolrMetadataExtractor
{
    /**
     * The Solr reference resolver.
     */
    @Inject
    @Named("document")
    protected SolrReferenceResolver resolver;

    /**
     * BlockRenderer component used to render the wiki content before indexing.
     */
    @Inject
    @Named("plain/1.0")
    protected BlockRenderer renderer;

    /**
     * Reference to String serializer.
     */
    @Inject
    protected EntityReferenceSerializer<String> serializer;

    @Override
    public boolean setFieldsInternal(LengthSolrInputDocument solrDocument, EntityReference entityReference)
        throws Exception
    {
        DocumentReference documentReference = new DocumentReference(entityReference);

        XWikiContext xcontext = this.xcontextProvider.get();

        XWikiDocument translatedDocument = getTranslatedDocument(documentReference);
        if (translatedDocument == null) {
            return false;
        }

        Locale locale = getLocale(documentReference);

        solrDocument.setField(FieldUtils.FULLNAME, localSerializer.serialize(documentReference));

        // Same for document title
        try {
            String plainTitle = translatedDocument.getRenderedTitle(Syntax.PLAIN_1_0, xcontext);

            // Rendered title.
            solrDocument.setField(FieldUtils.getFieldName(FieldUtils.TITLE, locale), plainTitle);
        } catch (Throwable e) {
            this.logger.error("Failed to render title for document [{}]", entityReference);
        }

        // Raw Content
        solrDocument.setField(FieldUtils.getFieldName(FieldUtils.DOCUMENT_RAW_CONTENT, locale),
            translatedDocument.getContent());

        // Rendered content
        try {
            WikiPrinter plainContentPrinter = new DefaultWikiPrinter();
            this.renderer.render(translatedDocument.getXDOM(), plainContentPrinter);
            solrDocument.setField(FieldUtils.getFieldName(FieldUtils.DOCUMENT_RENDERED_CONTENT, locale),
                plainContentPrinter.toString());
        } catch (Throwable e) {
            this.logger.error("Failed to render content for document [{}]", entityReference);
        }

        solrDocument.setField(FieldUtils.VERSION, translatedDocument.getVersion());
        solrDocument.setField(FieldUtils.COMMENT, translatedDocument.getComment());

        solrDocument.setField(FieldUtils.DOCUMENT_LOCALE, translatedDocument.getLocale().toString());

        // Add locale inheritance
        addLocales(translatedDocument, translatedDocument.getLocale(), solrDocument);

        // Get both serialized user reference string and pretty user name
        setAuthors(solrDocument, translatedDocument, entityReference);

        // Document dates.
        solrDocument.setField(FieldUtils.CREATIONDATE, translatedDocument.getCreationDate());
        solrDocument.setField(FieldUtils.DATE, translatedDocument.getContentUpdateDate());

        // Document translations have their own hidden fields
        solrDocument.setField(FieldUtils.HIDDEN, translatedDocument.isHidden());

        // Add any extra fields (about objects, etc.) that can improve the findability of the document.
        setExtras(documentReference, solrDocument, locale);

        return true;
    }

    /**
     * @param solrDocument the Solr document
     * @param translatedDocument the XWiki document
     * @param entityReference the document reference
     */
    private void setAuthors(LengthSolrInputDocument solrDocument, XWikiDocument translatedDocument,
        EntityReference entityReference)
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        String authorString = serializer.serialize(translatedDocument.getAuthorReference());
        solrDocument.setField(FieldUtils.AUTHOR, serializer.serialize(translatedDocument.getAuthorReference()));
        try {
            String authorDisplayString = xcontext.getWiki().getUserName(authorString, null, false, xcontext);
            solrDocument.setField(FieldUtils.AUTHOR_DISPLAY, authorDisplayString);
        } catch (Throwable e) {
            this.logger.error("Failed to get author display name for document [{}]", entityReference);
        }

        String creatorString = serializer.serialize(translatedDocument.getCreatorReference());
        solrDocument.setField(FieldUtils.CREATOR, creatorString);
        try {
            String creatorDisplayString = xcontext.getWiki().getUserName(creatorString, null, false, xcontext);
            solrDocument.setField(FieldUtils.CREATOR_DISPLAY, creatorDisplayString);
        } catch (Throwable e) {
            this.logger.error("Failed to get creator display name for document [{}]", entityReference);
        }
    }

    /**
     * @param documentReference the document's reference.
     * @param solrDocument the Solr document where to add the data.
     * @param locale the locale of which to index the extra data.
     * @throws XWikiException if problems occur.
     */
    protected void setExtras(DocumentReference documentReference, SolrInputDocument solrDocument, Locale locale)
        throws XWikiException
    {
        // Index the Objects. Use the original document to get the objects since the
        // translated document is just a lightweight object containing the translated content and title.

        // Note: To be able to still find translated documents, we need to redundantly index the same objects (and
        // implicitly comments) for each translation. If we don`t do this, only the original document will be found.
        XWikiDocument originalDocument = getDocument(documentReference);

        // Objects
        setObjects(solrDocument, locale, originalDocument);

        // Note: Not indexing attachment contents at this point because they are considered first class search
        // results. Also, it's easy to see the source XWiki document from the UI.
    }

    /**
     * @param solrDocument the Solr document where to add the objects.
     * @param locale the locale for which to index the objects.
     * @param originalDocument the original document where the objects come from.
     */
    protected void setObjects(SolrInputDocument solrDocument, Locale locale, XWikiDocument originalDocument)
    {
        for (Map.Entry<DocumentReference, List<BaseObject>> objects : originalDocument.getXObjects().entrySet()) {
            boolean hasObjectsOfThisType = false;
            for (BaseObject object : objects.getValue()) {
                // Yes, the old core can return null objects.
                hasObjectsOfThisType |= object != null;
                setObjectContent(solrDocument, object, locale);
            }
            if (hasObjectsOfThisType) {
                solrDocument.addField("object", localSerializer.serialize(objects.getKey()));
            }
        }
    }
}
