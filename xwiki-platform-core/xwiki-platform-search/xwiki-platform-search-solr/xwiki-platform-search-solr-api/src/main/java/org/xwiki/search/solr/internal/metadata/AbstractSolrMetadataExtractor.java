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

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.solr.common.SolrInputDocument;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaMetadataKeys;
import org.slf4j.Logger;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.search.solr.internal.api.SolrIndexerException;
import org.xwiki.search.solr.internal.reference.SolrReferenceResolver;
import org.xwiki.tika.internal.TikaUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.BooleanClass;
import com.xpn.xwiki.objects.classes.ListItem;
import com.xpn.xwiki.objects.classes.PasswordClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;

/**
 * Abstract implementation for a metadata extractor.
 * 
 * @version $Id$
 * @since 4.3M2
 */
public abstract class AbstractSolrMetadataExtractor implements SolrMetadataExtractor
{
    /**
     * The format used when indexing the objcontent field: "&lt;propertyName&gt;:&lt;propertyValue&gt;".
     */
    private static final String OBJCONTENT_FORMAT = "%s : %s";

    /**
     * The maximum number of characters allowed in a short text. This should be the same as the maximum length of a
     * StringProperty, as specified by xwiki.hbm.xml. We need this limit to be able to handle differently short strings
     * and large strings when indexing XObject properties.
     */
    protected static final int SHORT_TEXT_LIMIT = 255;

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
     * Reference to String serializer. Used for fields such as class and fullname that are relative to their wiki and
     * are stored without the wiki name.
     */
    @Inject
    @Named("local")
    protected EntityReferenceSerializer<String> localSerializer;

    /**
     * Used to access current {@link XWikiContext}.
     */
    @Inject
    protected Provider<XWikiContext> xcontextProvider;

    /**
     * Used to find the resolver.
     */
    @Inject
    protected ComponentManager componentManager;

    @Override
    public LengthSolrInputDocument getSolrDocument(EntityReference entityReference)
        throws SolrIndexerException, IllegalArgumentException
    {
        try {
            LengthSolrInputDocument solrDocument = new LengthSolrInputDocument();

            solrDocument.setField(FieldUtils.ID, getResolver(entityReference).getId(entityReference));

            if (!setDocumentFields(new DocumentReference(entityReference.extractReference(EntityType.DOCUMENT)),
                solrDocument)) {
                return null;
            }

            solrDocument.setField(FieldUtils.TYPE, entityReference.getType().name());

            if (!setFieldsInternal(solrDocument, entityReference)) {
                return null;
            }

            return solrDocument;
        } catch (Exception e) {
            String message = String.format("Failed to get input Solr document for entity '%s'", entityReference);
            throw new SolrIndexerException(message, e);
        }
    }

    /**
     * @param solrDocument the {@link LengthSolrInputDocument} to modify
     * @param entityReference the reference of the entity
     * @return false if the entity should not be indexed (generally mean it does not exist), true otherwise
     * @throws Exception in case of errors
     */
    protected abstract boolean setFieldsInternal(LengthSolrInputDocument solrDocument, EntityReference entityReference)
        throws Exception;

    /**
     * @param entityReference the reference of the entity
     * @return the Solr resolver associated to the entity type
     * @throws SolrIndexerException if any error
     */
    protected SolrReferenceResolver getResolver(EntityReference entityReference) throws SolrIndexerException
    {
        try {
            return this.componentManager.getInstance(SolrReferenceResolver.class,
                entityReference.getType().getLowerCase());
        } catch (ComponentLookupException e) {
            throw new SolrIndexerException(
                "Faile to find solr reference resolver for type reference [" + entityReference + "]");
        }
    }

    /**
     * Utility method to retrieve the default translation of a document using its document reference.
     * 
     * @param documentReference reference to a document
     * @return the original {@link XWikiDocument} instance referenced (the default translation)
     * @throws XWikiException if problems occur
     */
    protected XWikiDocument getDocument(DocumentReference documentReference) throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        DocumentReference documentReferenceWithoutLocale = documentReference.getLocale() == null ? documentReference
            : new DocumentReference(documentReference, (Locale) null);
        XWikiDocument document = xcontext.getWiki().getDocument(documentReferenceWithoutLocale, xcontext);

        return document;
    }

    /**
     * Fetch translated document.
     * 
     * @param documentReference reference to the document to be translated.
     * @return translated document.
     * @throws SolrIndexerException if problems occur.
     */
    protected XWikiDocument getTranslatedDocument(DocumentReference documentReference) throws SolrIndexerException
    {
        try {
            XWikiDocument originalDocument = getDocument(documentReference);
            Locale locale = documentReference.getLocale();

            if (locale == null || locale.equals(Locale.ROOT)) {
                return originalDocument;
            }

            XWikiDocument translatedDocument = originalDocument.getTranslatedDocument(locale, this.xcontextProvider.get());

            // XWikiDocument#getTranslatedDocument returns the default document when the locale does not exist
            if (translatedDocument.getRealLocale().equals(locale)) {
                return translatedDocument;
            }
        } catch (Exception e) {
            throw new SolrIndexerException(
                String.format("Failed to get translated document for '%s'", documentReference), e);
        }

        return null;
    }

    /**
     * Adds to a Solr document the fields that are specific to the XWiki document that contains the entity to be
     * indexed. These fields required to identify the owning document and to also reflect some properties of the owning
     * document towards the indexed entity (like locale and hidden flag).
     * 
     * @param documentReference reference to document.
     * @param solrDocument the Solr document to which to add the fields.
     * @return false if the document does not exist, true otherwise
     * @throws Exception if problems occur.
     */
    protected boolean setDocumentFields(DocumentReference documentReference, SolrInputDocument solrDocument)
        throws Exception
    {
        XWikiDocument originalDocument = getDocument(documentReference);
        if (originalDocument.isNew()) {
            return false;
        }

        solrDocument.setField(FieldUtils.HIDDEN, originalDocument.isHidden());

        solrDocument.setField(FieldUtils.WIKI, documentReference.getWikiReference().getName());
        solrDocument.setField(FieldUtils.NAME, documentReference.getName());

        // Set the fields that are used to query / filter the document hierarchy.
        setHierarchyFields(solrDocument, documentReference.getParent());

        Locale locale = getLocale(documentReference);
        solrDocument.setField(FieldUtils.LOCALE, locale.toString());
        solrDocument.setField(FieldUtils.LANGUAGE, locale.getLanguage());

        return true;
    }

    protected Set<Locale> getLocales(DocumentReference documentReference, Locale entityLocale)
        throws XWikiException, SolrIndexerException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        return getLocales(xcontext.getWiki().getDocument(documentReference, xcontext), entityLocale);
    }

    protected Set<Locale> getLocales(XWikiDocument xdocument, Locale entityLocale)
        throws XWikiException, SolrIndexerException
    {
        Set<Locale> locales = new HashSet<Locale>();

        String entityLocaleString = entityLocale != null ? entityLocale.toString() : null;

        // 1) Add entity locale
        if (entityLocale != null) {
            locales.add(entityLocale);
        }

        XWikiContext xcontext = this.xcontextProvider.get();

        // 2) Add locales from the document

        List<Locale> documentLocales = xdocument.getTranslationLocales(this.xcontextProvider.get());

        // If entityLocale is null it means that it's an entity without the support for translations
        // (objects/attachments)
        if (entityLocale == null) {
            for (Locale locale : documentLocales) {
                locales.add(locale);
            }
        }

        // 3) Add locales from preferences

        List<Locale> availableLocales = xcontext.getWiki().getAvailableLocales(xcontext);

        for (Locale locale : availableLocales) {
            // Add locale only if there is no explicit translation for it
            if (!documentLocales.contains(locale)) {
                if (entityLocale == null || locale.toString().startsWith(entityLocaleString)) {
                    locales.add(locale);
                }
            }
        }

        // 4) Make sure that the original document's locale is there as well.
        locales.add(getLocale(xdocument.getDocumentReference()));

        return locales;
    }

    protected void addLocales(XWikiDocument xdocument, Locale entityLocale, SolrInputDocument solrDocument)
        throws SolrIndexerException, XWikiException
    {
        Set<Locale> locales = getLocales(xdocument, entityLocale);
        for (Locale childLocale : locales) {
            solrDocument.addField(FieldUtils.LOCALES, childLocale.toString());
        }
    }

    /**
     * @param documentReference reference to the document.
     * @return the locale code of the referenced document.
     * @throws SolrIndexerException if problems occur.
     */
    protected Locale getLocale(DocumentReference documentReference) throws SolrIndexerException
    {
        Locale locale = null;

        try {
            if (documentReference.getLocale() != null && !documentReference.getLocale().equals(Locale.ROOT)) {
                locale = documentReference.getLocale();
            } else {
                XWikiContext xcontext = this.xcontextProvider.get();
                locale = xcontext.getWiki().getDocument(documentReference, xcontext).getRealLocale();
            }
        } catch (Exception e) {
            throw new SolrIndexerException(
                String.format("Exception while fetching the locale of the document '%s'", documentReference), e);
        }

        return locale;
    }

    /**
     * Adds the properties of a given object to a Solr document.
     * 
     * @param solrDocument the document where to add the properties
     * @param object the object whose properties to add
     * @param locale the locale of the indexed document; in case of translations, this will obviously be different than
     *            the original document's locale
     */
    protected void setObjectContent(SolrInputDocument solrDocument, BaseObject object, Locale locale)
    {
        if (object == null) {
            // Yes, the platform can return null objects.
            return;
        }

        BaseClass xClass = object.getXClass(this.xcontextProvider.get());
        for (Object field : object.getFieldList()) {
            @SuppressWarnings("unchecked")
            BaseProperty<EntityReference> property = (BaseProperty<EntityReference>) field;
            // Avoid indexing empty properties.
            if (property.getValue() != null) {
                PropertyClass propertyClass = (PropertyClass) xClass.get(property.getName());
                setPropertyValue(solrDocument, property, propertyClass, locale);
            }
        }
    }

    /**
     * Add the value of the given object property to a Solr document.
     * 
     * @param solrDocument the document to add the object property value to
     * @param property the object property whose value to add
     * @param propertyClass the class that describes the given property
     * @param locale the locale of the indexed document
     */
    private void setPropertyValue(SolrInputDocument solrDocument, BaseProperty<EntityReference> property,
        PropertyClass propertyClass, Locale locale)
    {
        Object propertyValue = property.getValue();
        if (propertyClass instanceof StaticListClass) {
            setStaticListPropertyValue(solrDocument, property, (StaticListClass) propertyClass, locale);
        } else if (propertyClass instanceof TextAreaClass
            || (propertyClass != null && "String".equals(propertyClass.getClassType()))
            || (propertyValue instanceof CharSequence && String.valueOf(propertyValue).length() > SHORT_TEXT_LIMIT)) {
            // Index TextArea and String properties as text, based on the document locale. We didn't check if the
            // property class is an instance of StringClass because it has subclasses that don't store free text (like
            // the EmailClass). Plus we didn't want to include the PasswordClass (which extends StringClass).
            //
            // We also index large strings as localized text in order to cover custom XClass properties that may not
            // extend TextArea but still have large strings as value, and also the case when a TextArea property is
            // removed from an XClass but there are still objects that have a (large) value set for it (the property
            // class is null in this case). The 255 limit is defined in xwiki.hbm.xml for string properties.

            // It's important here to make sure we give strings to Solr, as it can mutate the value we give it,
            // so we need to make sure we don't endanger the state of the document
            setPropertyValue(solrDocument, property, new TypedValue(String.valueOf(propertyValue), TypedValue.TEXT),
                locale);

            if (!(propertyClass instanceof TextAreaClass)
                && String.valueOf(propertyValue).length() <= SHORT_TEXT_LIMIT) {
                // Also index the raw value that is saved in the database. This provide a stable field name and also
                // allows exact matching
                setPropertyValue(solrDocument, property, new TypedValue(propertyValue), locale);
            }
        } else if (propertyValue instanceof Collection) {
            // We iterate the collection instead of giving it to Solr because, although it supports passing collections,
            // it reuses the collection in some cases, when the value of a field is set for the first time for instance,
            // which can lead to side effects on our side.
            for (Object value : (Collection<?>) propertyValue) {
                if (value != null) {
                    // Avoid indexing null values.
                    setPropertyValue(solrDocument, property, new TypedValue(value), locale);
                }
            }
        } else if (propertyValue instanceof Integer && propertyClass instanceof BooleanClass) {
            // Boolean properties are stored as integers (0 is false and 1 is true).
            Boolean booleanValue = ((Integer) propertyValue) != 0;
            setPropertyValue(solrDocument, property, new TypedValue(booleanValue), locale);
        } else if (!(propertyClass instanceof PasswordClass)) {
            // Avoid indexing passwords.
            setPropertyValue(solrDocument, property, new TypedValue(propertyValue), locale);
        }
    }

    /**
     * Add the values of a static list property to a Solr document. We add both the raw value (what is saved in the
     * database) and the display value (the label seen by the user, which is specified in the XClass).
     * 
     * @param solrDocument the document to add the property value to
     * @param property the static list property whose value to add
     * @param propertyClass the static list class that should be used to get the list of known values
     * @param locale the locale of the indexed document
     * @see "XWIKI-9417: Search does not return any results for Static List values"
     */
    private void setStaticListPropertyValue(SolrInputDocument solrDocument, BaseProperty<EntityReference> property,
        StaticListClass propertyClass, Locale locale)
    {
        // The list of known values specified in the XClass.
        Map<String, ListItem> knownValues = propertyClass.getMap(this.xcontextProvider.get());
        Object propertyValue = property.getValue();
        // When multiple selection is on the value is a list. Otherwise, for single selection, the value is a string.
        List<?> rawValues = propertyValue instanceof List ? (List<?>) propertyValue : Arrays.asList(propertyValue);
        for (Object rawValue : rawValues) {
            // Avoid indexing null values.
            if (rawValue != null) {
                // Index the raw value that is saved in the database. This is most probably a string so we'll be able to
                // perform exact matches on this value.
                setPropertyValue(solrDocument, property, new TypedValue(rawValue), locale);
                ListItem valueInfo = knownValues.get(rawValue);
                if (valueInfo != null && valueInfo.getValue() != null && !valueInfo.getValue().equals(rawValue)) {
                    // Index the display value as text (based on the given locale). This is the text seen by the user
                    // when he edits the static list property. This text is specified on the XClass (but can be
                    // overwritten by translations!).
                    setPropertyValue(solrDocument, property, new TypedValue(valueInfo.getValue(), TypedValue.TEXT),
                        locale);
                }
            }
        }
    }

    /**
     * Add the given value to a Solr document on the field corresponding to the specified object property.
     * 
     * @param solrDocument the document to add the value to
     * @param property the object property instance used to get information about the property the given value
     *            corresponds to
     * @param typedValue the value to add
     * @param locale the locale of the indexed document
     */
    protected void setPropertyValue(SolrInputDocument solrDocument, BaseProperty<EntityReference> property,
        TypedValue typedValue, Locale locale)
    {
        // Collect all the property values from all the objects of a document in a single (localized) field.
        String fieldName = FieldUtils.getFieldName(FieldUtils.OBJECT_CONTENT, locale);
        String fieldValue = String.format(OBJCONTENT_FORMAT, property.getName(), typedValue.getValue());
        // The current method can be called multiple times for the same property value (but with a different type).
        // Since we don't care about the value type here (all the values are collected in a localized field) we need to
        // make sure we don't add the same value twice. Derived classes can override this method and use the value type.
        addFieldValueOnce(solrDocument, fieldName, fieldValue);
    }

    /**
     * Adds a value to a document field, ensuring that the value is not duplicated.
     * 
     * @param solrDocument the document to add the field value to
     * @param fieldName the field name
     * @param fieldValue the field value to add
     */
    protected void addFieldValueOnce(SolrInputDocument solrDocument, String fieldName, Object fieldValue)
    {
        Collection<Object> fieldValues = solrDocument.getFieldValues(fieldName);
        if (fieldValues == null || !fieldValues.contains(fieldValue)) {
            solrDocument.addField(fieldName, fieldValue);
        }
    }

    /**
     * Tries to extract text indexable content from a generic attachment.
     * 
     * @param attachment the attachment to extract the content from
     * @return the text representation of the attachment's content
     * @throws SolrIndexerException if problems occur
     */
    protected String getContentAsText(XWikiAttachment attachment)
    {
        try {
            Metadata metadata = new Metadata();
            metadata.set(TikaMetadataKeys.RESOURCE_NAME_KEY, attachment.getFilename());

            InputStream in = attachment.getContentInputStream(this.xcontextProvider.get());

            try {
                return TikaUtils.parseToString(in, metadata);
            } finally {
                in.close();
            }
        } catch (Exception e) {
            this.logger.error("Failed to retrieve the content of attachment [{}]", attachment.getReference(), e);
            return null;
        }
    }

    private void setHierarchyFields(SolrInputDocument solrDocument, EntityReference path)
    {
        solrDocument.setField(FieldUtils.SPACE_EXACT, this.localSerializer.serialize(path));
        List<EntityReference> ancestors = path.getReversedReferenceChain();
        // Skip the wiki reference because we want to index the local space references.
        for (int i = 1; i < ancestors.size(); i++) {
            solrDocument.addField(FieldUtils.SPACES, ancestors.get(i).getName());
            String localAncestorReference = this.localSerializer.serialize(ancestors.get(i));
            solrDocument.addField(FieldUtils.SPACE_PREFIX, localAncestorReference);
            // We prefix the local ancestor reference with the depth in order to use 'facet.prefix'. We also add a
            // trailing slash in order to distinguish between space names with the same prefix (e.g. 0/Gallery/ and
            // 0/GalleryCode/).
            solrDocument.addField(FieldUtils.SPACE_FACET, (i - 1) + "/" + localAncestorReference + ".");
        }
    }
}
