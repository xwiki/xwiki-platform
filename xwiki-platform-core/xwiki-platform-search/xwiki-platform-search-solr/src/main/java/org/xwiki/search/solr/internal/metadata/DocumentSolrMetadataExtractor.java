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

import org.apache.solr.common.SolrInputDocument;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.search.solr.Fields;
import org.xwiki.search.solr.SolrIndexException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Extract the metadata to be indexed from document.
 * 
 * @version $Id$
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
            SolrInputDocument sdoc = new SolrInputDocument();

            // FIXME: Shouldn't we iterate over all the available translations and index them?
            // Or better yet, do this in the getContainedIdexableReferences method.
            XWikiDocument translatedDocument = getTranslatedDocument(documentReference);

            String language = getLanguage(documentReference);
            sdoc.addField(Fields.ID, getId(documentReference));
            addDocumentReferenceFields(documentReference, sdoc, language);
            sdoc.addField(Fields.TYPE, documentReference.getType().name());
            sdoc.addField(Fields.FULLNAME, compactSerializer.serialize(documentReference));

            // Convert the XWiki syntax of document to plain text.
            WikiPrinter printer = new DefaultWikiPrinter();
            renderer.render(translatedDocument.getXDOM(), printer);

            // Same for document title
            String plainTitle = translatedDocument.getRenderedTitle(Syntax.PLAIN_1_0, context);

            // Get the rendered plain text title.
            sdoc.addField(Fields.TITLE + USCORE + language, plainTitle);
            sdoc.addField(Fields.DOCUMENT_CONTENT + USCORE + language, printer.toString());
            sdoc.addField(Fields.VERSION, translatedDocument.getVersion());

            sdoc.addField(Fields.AUTHOR, serializer.serialize(translatedDocument.getAuthorReference()));
            sdoc.addField(Fields.CREATOR, serializer.serialize(translatedDocument.getCreatorReference()));
            sdoc.addField(Fields.CREATIONDATE, translatedDocument.getCreationDate());
            sdoc.addField(Fields.DATE, translatedDocument.getContentUpdateDate());

            // Index the Comments. Use the original document to get the comment objects since the translated document is
            // just a lightweight object containing the translated content and title.

            // FIXME: Is it ok to do it like this (serializing all the comments into a field)? What about the other
            // objects?
            // What about the fact that all translations have the same comments? The field will get copied to each
            // version. Maybe we should handle specially the original document.

            List<BaseObject> comments = context.getWiki().getDocument(documentReference, context).getComments();
            if (comments != null) {
                StringBuffer buffer = new StringBuffer();
                for (BaseObject comment : comments) {
                    String commentString = comment.getStringValue("comment");
                    String author = comment.getStringValue("author");
                    buffer.append(commentString + " by " + author + " ");
                }
                sdoc.addField(Fields.COMMENT, buffer.toString());
            }

            return sdoc;
        } catch (Exception e) {
            throw new SolrIndexException(String.format("Failed to get input document for '%s'",
                serializer.serialize(documentReference)), e);
        }
    }

    @Override
    public String getId(EntityReference reference) throws SolrIndexException, IllegalArgumentException
    {
        DocumentReference documentReference = new DocumentReference(reference);

        String result = super.getId(reference);

        // Document IDs also contain the language code to differentiate between them.
        // Objects, attachments, etc. don`t need this because the only thing that is translated right now is the
        // document title and content.
        result += getLanguage(documentReference);

        return result;
    }
}
