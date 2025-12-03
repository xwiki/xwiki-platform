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
package org.xwiki.livedata.internal.livetable;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.wysiwyg.converter.HTMLConverter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Used by the live table source to update the live data entries that are mapped to XWiki documents (i.e., when entry
 * properties are either document fields or xobject properties).
 *
 * @version $Id$
 * @since 13.4RC1
 */
@Component(roles = { ModelBridge.class })
@Singleton
public class ModelBridge
{
    private static final String NEW_DOCUMENT_UPDATE_ERROR = "We do not support updating new documents.";

    private static final String REQUIRES_HTML_CONVERSION = "RequiresHTMLConversion";

    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private HTMLConverter htmlConverter;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localSerializer;

    @Inject
    private Logger logger;

    /**
     * Update a property of an xobject located in the specified document. If the property starts with {@code doc.} the
     * corresponding document field is updated instead. The first xobject of the specified type (xclass reference) is
     * updated, to update another object, see {@link #update(String, Object, DocumentReference, DocumentReference,
     * int)}.
     *
     * @param property the property id
     * @param value the value of the property
     * @param documentReference the document reference
     * @param classReference the class reference
     * @return the changed field, or {@link Optional#empty()} if nothing has been modified
     * @throws AccessDeniedException if the user cannot edit the document
     * @throws XWikiException in case of error when accessing or updating the document
     * @see #update(String, Object, DocumentReference, DocumentReference, int)
     */
    public Optional<Object> update(String property, Object value, DocumentReference documentReference,
        DocumentReference classReference) throws AccessDeniedException, XWikiException, LiveDataException
    {
        return update(property, value, documentReference, classReference, 0);
    }

    /**
     * Update a property of an nth XObject located in the provided document reference. The nth object is found using the
     * {code objectNumber} index. If the property starts with {@code doc.} the corresponding document's property is
     * updated instead.
     *
     * @param property the property id
     * @param value the value of the property
     * @param documentReference the document reference
     * @param classReference the class reference
     * @param objectNumber the index of the object to update
     * @return the changed field, or {@link Optional#empty()} if nothing has been modified
     * @throws AccessDeniedException if the user cannot edit the document
     * @throws XWikiException in case of error when accessing or updating the document
     */
    public Optional<Object> update(String property, Object value, DocumentReference documentReference,
        DocumentReference classReference, int objectNumber)
        throws AccessDeniedException, XWikiException, LiveDataException
    {
        this.authorization.checkAccess(Right.EDIT, documentReference);
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiDocument document = xcontext.getWiki().getDocument(documentReference, xcontext);

        if (document.isNew()) {
            throw new LiveDataException(NEW_DOCUMENT_UPDATE_ERROR);
        }

        // Avoid modifying the cache document
        document = document.clone();

        Object changedValue = updateProperty(property, value, classReference, objectNumber, document);

        saveDocument(document);
        return Optional.ofNullable(changedValue);
    }

    /**
     * Update multiple properties of a document. The properties can either be from the document itself, if prefixed with
     * {@code doc.}, or from the first XObject instance of the specified type. Custom property mappings are honored
     * while still targeting the first object instance.
     *
     * @param properties the properties to update
     * @param documentReference the document to update
     * @param classReference the default type of XObject to update
     * @param propertyClassReferences optional mapping that specifies the XClass for each property
     * @throws AccessDeniedException in case the current user is not allow to edit the document
     * @throws XWikiException in case of error when loading or saving the updated document
     * @throws LiveDataException in case of error when validating the document
     * @see #updateAll(Map, DocumentReference, DocumentReference, Map, int)
     */
    public void updateAll(Map<String, Object> properties, DocumentReference documentReference,
        DocumentReference classReference, Map<String, DocumentReference> propertyClassReferences)
        throws AccessDeniedException, XWikiException, LiveDataException
    {
        updateAll(properties, documentReference, classReference, propertyClassReferences, 0);
    }

    /**
     * Update all the provided properties of the document. The properties can either be from the document itself, if
     * prefixed with {@code doc.}, or from an XObject instance of the specified type (class reference). The XObject at
     * the provided index number is updated.
     *
     * @param properties the properties to update
     * @param documentReference the document to update
     * @param classReference the default type of XObject to update
     * @param propertyClassReferences optional mapping that specifies the XClass for each property
     * @param objectNumber the index of the XObject to update
     * @throws AccessDeniedException in case the current user is not allow to edit the document
     * @throws XWikiException in case of error when loading or saving the updated document
     * @throws LiveDataException in case of error when validating the document
     * @see #updateAll(Map, DocumentReference, DocumentReference, Map, int)
     */
    public void updateAll(Map<String, Object> properties, DocumentReference documentReference,
        DocumentReference classReference, Map<String, DocumentReference> propertyClassReferences, int objectNumber)
        throws AccessDeniedException, XWikiException, LiveDataException
    {
        this.authorization.checkAccess(Right.EDIT, documentReference);
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiDocument document = xcontext.getWiki().getDocument(documentReference, xcontext);

        if (document.isNew()) {
            throw new LiveDataException(NEW_DOCUMENT_UPDATE_ERROR);
        }

        // Avoid modifying the cache document
        document = document.clone();

        convertPropertiesFromHtml(properties, classReference, propertyClassReferences, objectNumber);

        for (Map.Entry<String, Object> property : properties.entrySet()) {
            DocumentReference propertyClassReference = propertyClassReferences.get(property.getKey());
            DocumentReference targetClassReference = propertyClassReference != null ? propertyClassReference
                : classReference;
            this.updateProperty(property.getKey(), property.getValue(), targetClassReference, objectNumber, document);
        }

        saveDocument(document);
    }

    private void convertPropertiesFromHtml(Map<String, Object> properties, DocumentReference defaultClassReference,
        Map<String, DocumentReference> propertyClassReferences, int objectNumber)
    {
        if (properties.containsKey(REQUIRES_HTML_CONVERSION)) {
            String requiresHTMLConversion = (String) properties.remove(REQUIRES_HTML_CONVERSION);
            Set<String> propertiesRequiringHTMLConversion = getPropertiesRequiringHTMLConversion(
                requiresHTMLConversion, defaultClassReference, propertyClassReferences, objectNumber);
            for (String propertyName : propertiesRequiringHTMLConversion) {
                String syntaxKey = propertyName + "_syntax";
                String cacheKey = propertyName + "_cache";
                properties.computeIfPresent(propertyName, (k, v) ->
                    this.htmlConverter.fromHTML((String) v, (String) properties.get(syntaxKey)));
                properties.remove(syntaxKey);
                properties.remove(cacheKey);
            }
        }
    }

    /**
     * Extract the set of plain property names that require HTML conversion based on the provided parameters.
     *
     * @param requiresHTMLConversion the comma separated list of properties requiring HTML conversion
     * @param defaultClassReference the default class reference
     * @param propertyClassReferences the map of property to class references for custom classes per property
     * @param objectNumber the object number
     * @return the set of property names requiring HTML conversion
     */
    public Set<String> getPropertiesRequiringHTMLConversion(String requiresHTMLConversion,
        DocumentReference defaultClassReference, Map<String, DocumentReference> propertyClassReferences,
        int objectNumber)
    {
        // Use a LinkedHashSet to make the iteration order consistent.
        Set<String> result = new LinkedHashSet<>();
        String defaultClassName = this.localSerializer.serialize(defaultClassReference);
        Set<String> requiresHTMLConversionSet = new LinkedHashSet<>(Arrays.asList(requiresHTMLConversion.split(",")));
        // First, check for every custom class reference in the provided map if it matches any of the
        // requiresHTMLConversion properties.
        for (Map.Entry<String, DocumentReference> entry : propertyClassReferences.entrySet()) {
            String className = this.localSerializer.serialize(entry.getValue());
            String htmlConversionProperty = getPrefix(className, objectNumber) + entry.getKey();
            if (requiresHTMLConversionSet.remove(htmlConversionProperty)) {
                result.add(entry.getKey());
            }
        }

        // For the remaining properties, add them to the result after removing the default prefix.
        for (String requiresHTMLConversionProperty : requiresHTMLConversionSet) {
            String defaultPrefix = getPrefix(defaultClassName, objectNumber);
            result.add(Strings.CS.removeStart(requiresHTMLConversionProperty, defaultPrefix));
        }
        return result;
    }

    private static String getPrefix(String className, int objectNumber)
    {
        return String.format("%s_%d_", className, objectNumber);
    }

    private void saveDocument(XWikiDocument document) throws XWikiException, LiveDataException
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        // Saves and validates only if the document has changed.
        if (document.isContentDirty() || document.isMetaDataDirty()) {
            boolean validate = document.validate(xcontext);
            if (!validate) {
                throw new LiveDataException("Document not validated.");
            }
            document.setAuthorReference(xcontext.getUserReference());
            xcontext.getWiki().saveDocument(document, "LiveData update.", true, xcontext);
        }
    }

    private Object updateProperty(String property, Object value, DocumentReference classReference, int objectNumber,
        XWikiDocument document) throws XWikiException, LiveDataException
    {
        Object changedValue;
        if (StringUtils.defaultIfEmpty(property, "").startsWith("doc.")) {
            changedValue = updateDocument(property.substring(4), value, document);
        } else {
            changedValue = updateXObject(property, value, document, classReference, objectNumber);
        }
        return changedValue;
    }

    private Object updateXObject(String property, Object value, XWikiDocument document,
        DocumentReference classReference, int objectNumber) throws XWikiException, LiveDataException
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        BaseObject baseObject = document.getXObject(classReference, objectNumber);

        if (baseObject == null && objectNumber == document.getXObjectSize(classReference)) {
            // If the object number corresponds to the next free index, we create a new one.
            baseObject = document.newXObject(classReference, xcontext);
        }

        if (baseObject == null) {
            throw new LiveDataException(
                String.format("XObject [%s] not found at index [%d] in [%s]", classReference, objectNumber, document));
        }

        BaseClass xClass = baseObject.getXClass(xcontext);

        PropertyInterface propertyInterface = baseObject.get(property);
        Object changedValue = propertyInterface != null ? propertyInterface.toFormString() : null;

        Object newValue;
        if (value instanceof List<?> list) {
            newValue = list.stream().map(String::valueOf).toArray(String[]::new);
        } else {
            newValue = value;
        }
        xClass.fromMap(Map.of(property, newValue), baseObject);

        return changedValue;
    }

    private Object updateDocument(String property, Object value, XWikiDocument document)
    {
        Object changedValue = null;
        switch (property) {
            case "hidden" -> {
                changedValue = document.isHidden();
                document.setHidden(Boolean.valueOf(String.valueOf(value)));
            }
            case "title" -> {
                changedValue = document.getTitle();
                document.setTitle(String.valueOf(value));
            }
            case "content" -> {
                changedValue = document.getContent();
                document.setContent((String) value);
            }
            case null, default -> {
                // Some property such as fullName as simply ignored and are not editable.
                if (!Objects.equals(property, "fullName")) {
                    this.logger
                        .warn("Unknown property [{}]. Document [{}] will not be updated with value [{}].", property,
                            document,
                            value);
                }
            }
        }
        return changedValue;
    }
}
