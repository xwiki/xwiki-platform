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

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.search.solr.internal.api.SolrFieldNameEncoder;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

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
     * BlockRenderer component used to render the wiki content before indexing.
     */
    @Inject
    @Named("plain/1.0")
    private BlockRenderer renderer;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private UserReferenceSerializer<String> userReferenceSerializer;

    // TODO: relying on this serializer should be prevented by properly rewriting XWiki#getPlainUserName
    //  to use UserReference
    @Inject
    @Named("document")
    private UserReferenceSerializer<DocumentReference> documentReferenceUserReferenceSerializer;

    /**
     * Used to serialize entity reference to be used in dynamic field names.
     */
    @Inject
    @Named("solr")
    private EntityReferenceSerializer<String> fieldNameSerializer;

    /**
     * Used to encode dynamic field names that may contain special characters.
     */
    @Inject
    private SolrFieldNameEncoder fieldNameEncoder;

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

        // Rendered title.
        String plainTitle = translatedDocument.getRenderedTitle(Syntax.PLAIN_1_0, xcontext);
        solrDocument.setField(FieldUtils.getFieldName(FieldUtils.TITLE, locale), plainTitle);

        // Raw Content
        solrDocument.setField(FieldUtils.getFieldName(FieldUtils.DOCUMENT_RAW_CONTENT, locale),
            translatedDocument.getContent());

        // Rendered content
        WikiPrinter plainContentPrinter = new DefaultWikiPrinter();
        this.renderer.render(translatedDocument.getXDOM(), plainContentPrinter);
        solrDocument.setField(FieldUtils.getFieldName(FieldUtils.DOCUMENT_RENDERED_CONTENT, locale),
            plainContentPrinter.toString());

        solrDocument.setField(FieldUtils.VERSION, translatedDocument.getVersion());
        solrDocument.setField(FieldUtils.COMMENT, translatedDocument.getComment());

        solrDocument.setField(FieldUtils.DOCUMENT_LOCALE, translatedDocument.getLocale().toString());

        // Add locale inheritance
        addLocales(translatedDocument, translatedDocument.getLocale(), solrDocument);

        // Get both serialized user reference string and pretty user name
        setAuthors(solrDocument, translatedDocument);

        // Document dates.
        solrDocument.setField(FieldUtils.CREATIONDATE, translatedDocument.getCreationDate());
        solrDocument.setField(FieldUtils.DATE, translatedDocument.getDate());

        // Document translations have their own hidden fields
        solrDocument.setField(FieldUtils.HIDDEN, translatedDocument.isHidden());

        // Add links found in the document
        setLinks(solrDocument, translatedDocument, xcontext);

        // Add any extra fields (about objects, etc.) that can improve the findability of the document.
        setExtras(documentReference, solrDocument, locale);

        // Extract more metadata
        this.extractorUtils.extract(documentReference, translatedDocument, solrDocument);

        return true;
    }

    /**
     * @param solrDocument the Solr document
     * @param translatedDocument the XWiki document
     */
    private void setAuthors(SolrInputDocument solrDocument, XWikiDocument translatedDocument)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        DocumentAuthors authors = translatedDocument.getAuthors();

        UserReference originalAuthor = authors.getOriginalMetadataAuthor();
        String authorString = this.userReferenceSerializer.serialize(originalAuthor);
        solrDocument.setField(FieldUtils.AUTHOR, authorString);
        String authorDisplayString = xcontext.getWiki().getPlainUserName(
            this.documentReferenceUserReferenceSerializer.serialize(originalAuthor), xcontext);
        solrDocument.setField(FieldUtils.AUTHOR_DISPLAY, authorDisplayString);

        UserReference creator = authors.getCreator();
        String creatorString = this.userReferenceSerializer.serialize(creator);
        solrDocument.setField(FieldUtils.CREATOR, creatorString);
        String creatorDisplayString = xcontext.getWiki().getPlainUserName(
            this.documentReferenceUserReferenceSerializer.serialize(creator), xcontext);
        solrDocument.setField(FieldUtils.CREATOR_DISPLAY, creatorDisplayString);
    }

    private void setLinks(SolrInputDocument solrDocument, XWikiDocument translatedDocument, XWikiContext xcontext)
    {
        // Extract links
        // TODO: support more than EntityReference (extract and index any type of link found in the content)
        Set<EntityReference> references = translatedDocument.getUniqueLinkedEntities(xcontext);

        if (!references.isEmpty()) {
            Set<String> links = new HashSet<>(references.size());
            Set<String> linksExtended = new HashSet<>(references.size() * 2);

            // Serialize the links and resolve the extended links
            for (EntityReference reference : references) {
                String referenceString = this.linkSerializer.serialize(reference);

                links.add(referenceString);
                linksExtended.add(referenceString);

                // Add the reference without parameters as well as all its parents to the extended list
                extendLink(reference, linksExtended);
            }

            // Add the links to the Solr document
            for (String link : links) {
                solrDocument.addField(FieldUtils.LINKS, link);
            }
            for (String linkExtended : linksExtended) {
                solrDocument.addField(FieldUtils.LINKS_EXTENDED, linkExtended);
            }
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
        // We need to support the following types of queries:
        // * search for documents matching specific values in multiple XObject properties
        // * search for documents matching specific values in attachment meta data
        // In order to avoid using joins we have to index the XObjects and the attachments both separately and on the
        // document rows in the Solr index. This means we'll have duplicated information but we believe the increase in
        // the index size pays off if you take into account the simplified query syntax and the search speed.

        // Use the original document to get the objects and the attachments because the translated document is just a
        // lightweight document containing just the translated content and title.
        XWikiDocument originalDocument = getDocument(documentReference);

        // NOTE: To be able to still find translated documents, we need to redundantly index the same objects (including
        // comments) and attachments for each translation. If we don`t do this then only the original document will be
        // found. That's why we pass the locale of the translated document to the following method calls.
        setObjects(solrDocument, locale, originalDocument);
        setAttachments(solrDocument, locale, originalDocument);
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
                solrDocument.addField(FieldUtils.CLASS, localSerializer.serialize(objects.getKey()));
            }
        }
    }

    @Override
    protected void setPropertyValue(SolrInputDocument solrDocument, BaseProperty<?> property,
        TypedValue typedValue, Locale locale)
    {
        Object value = typedValue.getValue();
        String type = typedValue.getType();

        // We need to be able to query an object property alone.
        EntityReference classReference = property.getObject().getRelativeXClassReference();
        EntityReference propertyReference =
            new EntityReference(property.getName(), EntityType.CLASS_PROPERTY, classReference);
        String serializedPropertyReference = fieldNameEncoder.encode(fieldNameSerializer.serialize(propertyReference));
        String prefix = "property." + serializedPropertyReference;
        // Note that we're using "addField" because we want to collect all the property values, even from multiple
        // objects of the same type.
        solrDocument.addField(FieldUtils.getFieldName(prefix, type, locale), value);

        // We need to be able to sort by a property value and for this we need a dedicated (single valued) field because
        // the field we just added is multiValued and multiValued fields are not sortable.
        // We don't need to sort on properties that hold large localized texts or large strings (e.g. TextArea).
        if ((type != TypedValue.TEXT && type != TypedValue.STRING)
            || String.valueOf(value).length() <= getShortTextLimit()) {
            // Short localized texts are indexed as strings because a sort field is either non-tokenized (i.e. has no
            // Analyzer) or uses an Analyzer that only produces a single Term (i.e. uses the KeywordTokenizer).
            String sortType = "sort" + StringUtils.capitalize(type == TypedValue.TEXT ? TypedValue.STRING : type);
            // We're using "setField" because the sort fields must be single valued. The consequence is that for
            // properties with multiple values the last value we set will be used for sorting (e.g. if a document has
            // two objects of the same type then the value from the second object will be used for sorting).
            solrDocument.setField(FieldUtils.getFieldName(prefix, sortType, locale), value);
        }

        // We need to be able to query all properties of a specific type of object at once.
        String serializedClassReference = fieldNameEncoder.encode(fieldNameSerializer.serialize(classReference));
        String objectOfTypeFieldName = "object." + serializedClassReference;
        // The current method can be called multiple times for the same property value (but with a different type).
        // Since we don't care about the value type here (all the values are collected in a localized field) we need to
        // make sure we don't add the same value twice.
        addFieldValueOnce(solrDocument, FieldUtils.getFieldName(objectOfTypeFieldName, locale), value);

        // We need to be able to query all objects from a document at once.
        super.setPropertyValue(solrDocument, property, typedValue, locale);
    }

    /**
     * @param solrDocument the Solr document where to add the attachments data
     * @param locale the locale for which to index the attachments
     * @param originalDocument the original document, that should be used to access the attachments
     */
    private void setAttachments(SolrInputDocument solrDocument, Locale locale, XWikiDocument originalDocument)
    {
        for (XWikiAttachment attachment : originalDocument.getAttachmentList()) {
            setAttachment(solrDocument, locale, attachment);
        }
    }

    /**
     * Extracts the meta data from the given attachment and adds it to the given Solr document.
     * 
     * @param solrDocument the Solr document where to add the attachment data
     * @param locale the locale for which to index the attachments
     * @param attachment the attachment to index
     */
    private void setAttachment(SolrInputDocument solrDocument, Locale locale, XWikiAttachment attachment)
    {
        XWikiContext xcontext = xcontextProvider.get();

        solrDocument.addField(FieldUtils.FILENAME, attachment.getFilename());
        solrDocument.addField(FieldUtils.MIME_TYPE, attachment.getMimeType(xcontext));
        solrDocument.addField(FieldUtils.ATTACHMENT_DATE, attachment.getDate());
        solrDocument.addField(FieldUtils.ATTACHMENT_SIZE, attachment.getLongSize());

        String attachmentTextContent = getContentAsText(attachment);
        solrDocument.addField(FieldUtils.getFieldName(FieldUtils.ATTACHMENT_CONTENT, locale), attachmentTextContent);

        // Index the full author reference for exact matching (faceting).
        String authorStringReference = entityReferenceSerializer.serialize(attachment.getAuthorReference());
        solrDocument.addField(FieldUtils.ATTACHMENT_AUTHOR, authorStringReference);
        try {
            // Index the author display name for free text search.
            String authorDisplayName = xcontext.getWiki().getPlainUserName(attachment.getAuthorReference(), xcontext);
            solrDocument.addField(FieldUtils.ATTACHMENT_AUTHOR_DISPLAY, authorDisplayName);
        } catch (Exception e) {
            this.logger.error("Failed to get author display name for attachment [{}]", attachment.getReference(), e);
        }
    }
}
