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

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.search.solr.internal.api.Fields;
import org.xwiki.search.solr.internal.api.SolrIndexException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PasswordClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

/**
 * Abstract implementation for a metadata extractor.
 * 
 * @version $Id$
 * @since 4.3M2
 */
public abstract class AbstractSolrMetadataExtractor implements SolrMetadataExtractor
{
    /**
     * Logging framework.
     */
    @Inject
    protected Logger logger;

    /**
     * Execution component.
     */
    @Inject
    protected Execution execution;

    /**
     * Reference to String serializer.
     */
    @Inject
    protected EntityReferenceSerializer<String> serializer;

    /**
     * Reference to String serializer. Used for fields such as fullName that are relative to the wiki.
     */
    @Inject
    @Named("compactwiki")
    protected EntityReferenceSerializer<String> compactSerializer;

    /**
     * DocumentAccessBridge component.
     */
    @Inject
    protected DocumentAccessBridge documentAccessBridge;

    @Override
    public String getId(EntityReference reference) throws SolrIndexException
    {
        String result = serializer.serialize(reference);

        // TODO: Include language all the other entities once object/attachment translation is implemented.

        return result;
    }

    /**
     * @return the XWikiContext
     */
    protected XWikiContext getXWikiContext()
    {
        ExecutionContext executionContext = this.execution.getContext();
        XWikiContext context = (XWikiContext) executionContext.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
        // FIXME: Do we need this? Maybe when running an index Thread?
        // if (context == null) {
        // context = this.contextProvider.createStubContext();
        // executionContext.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, context);
        // }
        return context;
    }

    /**
     * Utility method.
     * 
     * @param documentReference reference to a document.
     * @return the {@link XWikiDocument} instance referenced.
     * @throws XWikiException if problems occur.
     */
    protected XWikiDocument getDocument(DocumentReference documentReference) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWikiDocument document = context.getWiki().getDocument(documentReference, context);

        return document;
    }

    /**
     * Fetch translated document.
     * 
     * @param documentReference reference to the document to be translated.
     * @return translated document.
     * @throws SolrIndexException if problems occur.
     */
    protected XWikiDocument getTranslatedDocument(DocumentReference documentReference) throws SolrIndexException
    {
        try {
            XWikiDocument document = getDocument(documentReference);

            // TODO: replace with getLanguage(documentReference) ?
            String doclang = "";
            Locale locale = documentReference.getLocale();
            if (locale != null && !StringUtils.isEmpty(locale.toString())) {
                doclang = documentReference.getLocale().toString();
            }

            XWikiDocument translatedDocument = document.getTranslatedDocument(doclang, getXWikiContext());
            return translatedDocument;
        } catch (Exception e) {
            throw new SolrIndexException(String.format("Failed to get translated document for '%s'",
                serializer.serialize(documentReference)), e);
        }
    }

    /**
     * Adds to a Solr document the fields that are specific to the XWiki document that contains the entity to be
     * indexed. These fields required to identify the owning document and to also reflect some properties of the owning
     * document towards the indexed entity (like language and hidden flag).
     * 
     * @param documentReference reference to document.
     * @param solrDocument the Solr document to which to add the fields.
     * @throws Exception if problems occur.
     */
    protected void addDocumentFields(DocumentReference documentReference, SolrInputDocument solrDocument)
        throws Exception
    {
        solrDocument.addField(Fields.WIKI, documentReference.getWikiReference().getName());
        solrDocument.addField(Fields.SPACE, documentReference.getLastSpaceReference().getName());
        solrDocument.addField(Fields.NAME, documentReference.getName());

        String language = getLanguage(documentReference);
        solrDocument.addField(Fields.LANGUAGE, language);

        XWikiDocument document = getDocument(documentReference);
        solrDocument.addField(Fields.HIDDEN, document.isHidden());
    }

    /**
     * @param documentReference reference to the document.
     * @return the language code of the referenced document.
     * @throws SolrIndexException if problems occur.
     */
    protected String getLanguage(DocumentReference documentReference) throws SolrIndexException
    {
        String language = null;

        try {
            if (documentReference.getLocale() != null
                && !StringUtils.isEmpty(documentReference.getLocale().getDisplayLanguage())) {
                language = documentReference.getLocale().toString();
            } else if (!StringUtils.isEmpty(documentAccessBridge.getDocument(documentReference).getRealLanguage())) {
                language = documentAccessBridge.getDocument(documentReference).getRealLanguage();
            } else {
                // Multilingual and Default placeholder
                language = "en";
            }
        } catch (Exception e) {
            throw new SolrIndexException(String.format("Exception while fetching the language of the document '%s'",
                serializer.serialize(documentReference)), e);
        }

        return language;
    }

    /**
     * Adds the properties of a given object to a Solr document inside the multiValued field
     * {@link Fields#OBJECT_CONTENT}.
     * 
     * @param solrDocument the document where to add the properties.
     * @param object the object whose properties to add.
     * @param language the language of the indexed document. In case of translations, this will obviously be different
     *            than the original document's language.
     */
    protected void addObjectContent(SolrInputDocument solrDocument, BaseObject object, String language)
    {
        if (object == null) {
            // Yes, the platform can return null objects.
            return;
        }

        String fieldName = String.format(Fields.MULTILIGNUAL_FORMAT, Fields.OBJECT_CONTENT, language);

        XWikiContext context = getXWikiContext();

        BaseClass xClass = object.getXClass(context);

        for (Object field : object.getFieldList()) {
            BaseProperty<EntityReference> property = (BaseProperty<EntityReference>) field;

            // Avoid indexing empty properties.
            if (property.getValue() != null) {
                // Avoid indexing password.
                PropertyClass propertyClass = (PropertyClass) xClass.get(property.getName());
                if (!(propertyClass instanceof PasswordClass)) {
                    solrDocument.addField(fieldName, String.format("%s:%s", property.getName(), property.getValue()));
                }
            }
        }
    }
}
