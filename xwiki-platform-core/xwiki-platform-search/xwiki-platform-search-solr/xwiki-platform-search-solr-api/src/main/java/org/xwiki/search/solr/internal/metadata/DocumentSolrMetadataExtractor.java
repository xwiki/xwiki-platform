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

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.solr.common.SolrInputDocument;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.search.solr.internal.api.Fields;
import org.xwiki.search.solr.internal.api.SolrIndexException;

import com.xpn.xwiki.XWikiContext;
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
public class DocumentSolrMetadataExtractor extends AbstractSolrMetadataExtractor
{

    /**
     * BlockRenderer component used to render the wiki content before indexing.
     */
    @Inject
    @Named("plain/1.0")
    protected BlockRenderer renderer;

    @Override
    public SolrInputDocument getSolrDocument(EntityReference entityReference) throws SolrIndexException,
        IllegalArgumentException
    {
        DocumentReference documentReference = new DocumentReference(entityReference);

        XWikiContext context = getXWikiContext();

        try {
            SolrInputDocument solrDocument = new SolrInputDocument();

            XWikiDocument translatedDocument = getTranslatedDocument(documentReference);
            String language = getLanguage(documentReference);

            solrDocument.addField(Fields.ID, getId(documentReference));
            addDocumentFields(documentReference, solrDocument);
            solrDocument.addField(Fields.TYPE, documentReference.getType().name());
            solrDocument.addField(Fields.FULLNAME, compactSerializer.serialize(documentReference));

            // Convert the XWiki syntax of document to plain text.
            WikiPrinter printer = new DefaultWikiPrinter();
            renderer.render(translatedDocument.getXDOM(), printer);

            // Same for document title
            String plainTitle = translatedDocument.getRenderedTitle(Syntax.PLAIN_1_0, context);

            // Get the rendered plain text title.
            solrDocument.addField(String.format(Fields.MULTILIGNUAL_FORMAT, Fields.TITLE, language), plainTitle);
            solrDocument.addField(String.format(Fields.MULTILIGNUAL_FORMAT, Fields.DOCUMENT_CONTENT, language),
                printer.toString());
            solrDocument.addField(Fields.VERSION, translatedDocument.getVersion());

            solrDocument.addField(Fields.AUTHOR, serializer.serialize(translatedDocument.getAuthorReference()));
            solrDocument.addField(Fields.CREATOR, serializer.serialize(translatedDocument.getCreatorReference()));
            solrDocument.addField(Fields.CREATIONDATE, translatedDocument.getCreationDate());
            solrDocument.addField(Fields.DATE, translatedDocument.getContentUpdateDate());

            // Document translations have their own hidden fields
            solrDocument.setField(Fields.HIDDEN, translatedDocument.isHidden());

            // Index the Comments and Objects in general. Use the original document to get the comment objects since the
            // translated document is just a lightweight object containing the translated content and title.

            // Note: To be able to still find translated documents, we need to redundantly index the same objects (and
            // implicitly comments) for each translation. If we don`t do this, only the original document will be found.
            XWikiDocument originalDocument = getDocument(documentReference);

            // Comments. TODO: Is this particular handling of comments actually useful?
            addComments(solrDocument, originalDocument, language);

            // Objects
            for (Map.Entry<DocumentReference, List<BaseObject>> objects : originalDocument.getXObjects().entrySet()) {
                for (BaseObject object : objects.getValue()) {
                    this.addObjectContent(solrDocument, object, language);
                }
            }

            // Note: Not indexing attachment contents at this point because they are considered first class search
            // results. Also, it's easy to see the source XWiki document from the UI.

            return solrDocument;
        } catch (Exception e) {
            throw new SolrIndexException(String.format("Failed to get input document for '%s'",
                serializer.serialize(documentReference)), e);
        }
    }

    /**
     * Adds the document comments using the multiValued field {@link Fields#COMMENT}.
     * 
     * @param solrDocument the Solr document where to add the comments.
     * @param originalDocument the XWiki document from which to extract the comments.
     * @param language the language of the indexed document. In case of translations, this will obviously be different
     *            than the original document's language.
     */
    protected void addComments(SolrInputDocument solrDocument, XWikiDocument originalDocument, String language)
    {
        List<BaseObject> comments = originalDocument.getComments();
        if (comments == null) {
            return;
        }

        for (BaseObject comment : comments) {
            // Yes, objects can be null at this point...
            if (comment != null) {
                String commentString = comment.getStringValue("comment");
                String author = comment.getStringValue("author");
                Date date = comment.getDateValue("date");
                solrDocument.addField(String.format(Fields.MULTILIGNUAL_FORMAT, Fields.COMMENT, language),
                    String.format("%s by %s on %s", commentString, author, date));
            }
        }
    }

    @Override
    public String getId(EntityReference reference) throws SolrIndexException, IllegalArgumentException
    {
        DocumentReference documentReference = new DocumentReference(reference);

        String result = super.getId(reference);

        // Document IDs also contain the language code to differentiate between them.
        // Objects, attachments, etc. don`t need this because the only thing that is translated in an XWiki document
        // right now is the document title and content. Objects and attachments are not translated.
        result += Fields.USCORE + getLanguage(documentReference);

        return result;
    }
}
