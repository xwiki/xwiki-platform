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
import org.xwiki.search.solr.internal.api.Fields;
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
    public void addFieldsInternal(LengthSolrInputDocument solrDocument, EntityReference entityReference)
        throws Exception
    {
        DocumentReference documentReference = new DocumentReference(entityReference);

        XWikiContext xcontext = this.xcontextProvider.get();

        XWikiDocument translatedDocument = getTranslatedDocument(documentReference);
        Locale locale = getLocale(documentReference);

        solrDocument.addField(Fields.FULLNAME, localSerializer.serialize(documentReference));

        // Convert the XWiki syntax of document to plain text.
        WikiPrinter printer = new DefaultWikiPrinter();
        renderer.render(translatedDocument.getXDOM(), printer);

        // Same for document title
        String plainTitle = translatedDocument.getRenderedTitle(Syntax.PLAIN_1_0, xcontext);

        // Get the rendered plain text title.
        solrDocument.addField(String.format(Fields.MULTILIGNUAL_FORMAT, Fields.TITLE, locale.toString()), plainTitle);
        solrDocument.addField(String.format(Fields.MULTILIGNUAL_FORMAT, Fields.DOCUMENT_CONTENT, locale),
            printer.toString());
        solrDocument.addField(Fields.VERSION, translatedDocument.getVersion());
        solrDocument.addField(Fields.COMMENT, translatedDocument.getComment());

        // Get both serialized user reference string and pretty user name (first_name last_name).
        String authorString = serializer.serialize(translatedDocument.getAuthorReference());
        String authorDisplayString = xcontext.getWiki().getUserName(authorString, null, false, xcontext);
        String creatorString = serializer.serialize(translatedDocument.getCreatorReference());
        String creatorDisplayString = xcontext.getWiki().getUserName(creatorString, null, false, xcontext);

        solrDocument.addField(Fields.AUTHOR, authorString);
        solrDocument.addField(Fields.AUTHOR_DISPLAY, authorDisplayString);
        solrDocument.addField(Fields.CREATOR, creatorString);
        solrDocument.addField(Fields.CREATOR_DISPLAY, creatorDisplayString);

        // Document dates.
        solrDocument.addField(Fields.CREATIONDATE, translatedDocument.getCreationDate());
        solrDocument.addField(Fields.DATE, translatedDocument.getContentUpdateDate());

        // Document translations have their own hidden fields
        solrDocument.setField(Fields.HIDDEN, translatedDocument.isHidden());

        // Add any extra fields (about objects, comments, etc.) that can improve the findability of the document.
        addExtras(documentReference, solrDocument, locale);
    }

    /**
     * @param documentReference the document's reference.
     * @param solrDocument the Solr document where to add the data.
     * @param locale the locale of which to index the extra data.
     * @throws XWikiException if problems occur.
     */
    protected void addExtras(DocumentReference documentReference, SolrInputDocument solrDocument, Locale locale)
        throws XWikiException
    {
        // Index the Comments and Objects in general. Use the original document to get the comment objects since the
        // translated document is just a lightweight object containing the translated content and title.

        // Note: To be able to still find translated documents, we need to redundantly index the same objects (and
        // implicitly comments) for each translation. If we don`t do this, only the original document will be found.
        XWikiDocument originalDocument = getDocument(documentReference);

        // Objects
        addObjects(solrDocument, locale, originalDocument);

        // Note: Not indexing attachment contents at this point because they are considered first class search
        // results. Also, it's easy to see the source XWiki document from the UI.
    }

    /**
     * @param solrDocument the Solr document where to add the objects.
     * @param locale the locale for which to index the objects.
     * @param originalDocument the original document where the objects come from.
     */
    protected void addObjects(SolrInputDocument solrDocument, Locale locale, XWikiDocument originalDocument)
    {
        for (Map.Entry<DocumentReference, List<BaseObject>> objects : originalDocument.getXObjects().entrySet()) {
            for (BaseObject object : objects.getValue()) {
                addObjectContent(solrDocument, object, locale.toString());
            }
        }
    }
}
