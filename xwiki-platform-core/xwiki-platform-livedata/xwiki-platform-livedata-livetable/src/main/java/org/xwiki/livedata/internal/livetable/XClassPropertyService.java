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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Provides the operations to interact with a XClass properties of the rows of a liveTable live data.
 *
 * @version $Id$
 * @since 13.2RC1
 * @since 12.10.6
 */
@Component(roles = { XClassPropertyService.class })
@Singleton
public class XClassPropertyService
{
    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private Logger logger;

    /**
     * Update a property of an XObject located in the provided document reference. If the property starts with {@code
     * doc} the corresponding document's property is updated instead. The first object of the request type if updated,
     * to update another object, see {@link #update(String, Object, DocumentReference, DocumentReference, int)}.
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
            throw new LiveDataException("We do not support updating new documents.");
        }

        Object changedValue = updateProperty(property, value, classReference, objectNumber, document);

        saveDocument(document);
        return Optional.ofNullable(changedValue);
    }

    /**
     * Update all the provided entries of the document. The entiers can either be from the document itself if prefixed
     * with {@code doc.} or from an XObject instance of the classReference parameter. The first XObject found is used,
     * to update another XObject, see {@link #updateAll(Map, DocumentReference, DocumentReference, int)}
     *
     * @param entries tge list of entries to update
     * @param documentReference the document to update
     * @param classReference the type of XObject to update
     * @throws AccessDeniedException in case the current user is not allow to edit the document
     * @throws XWikiException in case of error when loading or saving the updated document
     * @throws LiveDataException in case of error when validating the document
     * @see #updateAll(Map, DocumentReference, DocumentReference, int)
     */
    public void updateAll(Map<String, Object> entries, DocumentReference documentReference,
        DocumentReference classReference) throws AccessDeniedException, XWikiException, LiveDataException
    {
        updateAll(entries, documentReference, classReference, 0);
    }

    /**
     * Update all the provided entries of the document. The entiers can either be from the document itself if prefixed
     * with {@code doc.} or from an XObject instance of the classReference parameter. The XObject at the provided index
     * number is updated.
     *
     * @param entries tge list of entries to update
     * @param documentReference the document to update
     * @param classReference the type of XObject to update
     * @param objectNumber the index of the XObject to update
     * @throws AccessDeniedException in case the current user is not allow to edit the document
     * @throws XWikiException in case of error when loading or saving the updated document
     * @throws LiveDataException in case of error when validating the document
     * @see #updateAll(Map, DocumentReference, DocumentReference)
     */
    public void updateAll(Map<String, Object> entries, DocumentReference documentReference,
        DocumentReference classReference, int objectNumber)
        throws AccessDeniedException, XWikiException, LiveDataException
    {
        this.authorization.checkAccess(Right.EDIT, documentReference);
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiDocument document = xcontext.getWiki().getDocument(documentReference, xcontext);

        for (Map.Entry<String, Object> entry : entries.entrySet()) {
            this.updateProperty(entry.getKey(), entry.getValue(), classReference, objectNumber, document);
        }

        saveDocument(document);
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
        XWikiDocument document) throws XWikiException
    {
        Object changedValue;
        if (StringUtils.defaultIfEmpty(property, "").startsWith("doc")) {
            changedValue = updateDocument(property.replaceFirst("doc\\.", ""), value, document);
        } else {
            changedValue = updateXObject(property, value, document, classReference, objectNumber);
        }
        return changedValue;
    }

    private Object updateXObject(String property, Object value, XWikiDocument document,
        DocumentReference classReference, int objectNumber) throws XWikiException
    {
        Object changedValue = null;
        XWikiContext xcontext = this.xcontextProvider.get();
        BaseObject baseObject = document.getXObject(classReference, objectNumber);
        BaseClass xClass = baseObject.getXClass(xcontext);

        List<Object> properties = Arrays.asList(baseObject.getPropertyNames());
        if (properties.contains(property)) {
            changedValue = baseObject.get(property).toFormString();

            Object nvalue;
            if (value instanceof List) {
                List list = (List) value;
                nvalue = IntStream.range(0, list.size())
                    .mapToObj(i -> String.valueOf(list.get(i)))
                    .toArray(String[]::new);
            } else {
                nvalue = value;
            }
            Map<String, Object> valueMap = new HashMap<>();
            valueMap.put(property, nvalue);
            xClass.fromMap(valueMap, baseObject);
        }
        return changedValue;
    }

    private Object updateDocument(String property, Object value, XWikiDocument document)
    {
        Object changedValue = null;
        if (Objects.equals(property, "hidden")) {
            changedValue = document.isHidden();
            document.setHidden(Objects.equals(String.valueOf(value), "true"));
        } else if (Objects.equals(property, "title")) {
            changedValue = document.getTitle();
            document.setTitle(String.valueOf(value));
        } else if (Objects.equals(property, "content")) {
            changedValue = document.getContent();
            document.setContent((String) value);
        } else {
            // Some property such as fullName as simply ignored and are not editable.
            if (!Objects.equals(property, "fullName")) {
                this.logger
                    .warn("Unknown property [{}]. Document [{}] will not be updated with value [{}].", property,
                        document,
                        value);
            }
        }
        return changedValue;
    }
}
