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
package org.xwiki.extension.xar.internal.job.diff;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.diff.display.UnifiedDiffBlock;
import org.xwiki.extension.xar.job.diff.DocumentUnifiedDiff;
import org.xwiki.extension.xar.job.diff.EntityUnifiedDiff;
import org.xwiki.mail.GeneralMailConfiguration;
import org.xwiki.model.reference.ClassPropertyReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentVersionReference;
import org.xwiki.model.reference.ObjectReference;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

/**
 * Computes the differences, in unified format, between two versions of a document.
 * 
 * @version $Id$
 * @since 7.0RC1
 */
@Component(roles = DocumentUnifiedDiffBuilder.class)
@Singleton
public class DocumentUnifiedDiffBuilder extends AbstractUnifiedDiffBuilder
{
    /**
     * The reference used to create an empty document when the passed document is null.
     */
    private static final DocumentReference EMPTY_DOCUMENT_REFERENCE = new DocumentReference("wiki", "Space",
        "EmptyDocument");

    @Inject
    private AttachmentUnifiedDiffBuilder attachmentDiffBuilder;

    @Inject
    private Provider<GeneralMailConfiguration> emailProvider;

    /**
     * Computes the differences, in unified format, between two versions of a document. A null document represents a
     * deleted document.
     * 
     * @param previousDocument the previous document version
     * @param nextDocument the next document version
     * @return the differences, in unified format, between the given document versions
     */
    public DocumentUnifiedDiff diff(XWikiDocument previousDocument, XWikiDocument nextDocument)
    {
        DocumentUnifiedDiff diff =
            new DocumentUnifiedDiff(getDocumentVersionReference(previousDocument),
                getDocumentVersionReference(nextDocument));

        if (previousDocument != nextDocument) {
            XWikiDocument nonNullPreviousDocument = emptyDocumentIfNull(previousDocument);
            XWikiDocument nonNullNextDocument = emptyDocumentIfNull(nextDocument);

            addDocumentFieldDiffs(nonNullPreviousDocument, nonNullNextDocument, diff);
            addAttachmentDiffs(nonNullPreviousDocument, nonNullNextDocument, diff);
            addObjectDiffs(nonNullPreviousDocument, nonNullNextDocument, diff);
            addClassPropertyDiffs(nonNullPreviousDocument.getXClass(), nonNullNextDocument.getXClass(), diff);
        }

        return diff;
    }

    private DocumentVersionReference getDocumentVersionReference(XWikiDocument document)
    {
        if (document == null) {
            return null;
        } else {
            DocumentVersionReference documentVersionReference =
                new DocumentVersionReference(document.getDocumentReferenceWithLocale());
            if (documentVersionReference.getVersion() == null) {
                return new DocumentVersionReference(documentVersionReference, document.getVersion());
            }
            return documentVersionReference;
        }
    }

    private XWikiDocument emptyDocumentIfNull(XWikiDocument document)
    {
        if (document == null) {
            XWikiDocument emptyDocument = new XWikiDocument(EMPTY_DOCUMENT_REFERENCE);
            emptyDocument.setSyntax(null);
            return emptyDocument;
        } else {
            return document;
        }
    }

    private boolean isNull(XWikiDocument document)
    {
        return document.getDocumentReference() == EMPTY_DOCUMENT_REFERENCE;
    }

    /**
     * Computes the document field differences between the given two document versions. Only the fields that the user
     * can modify from the UI are taken into account.
     * 
     * @param previousDocument the previous document version
     * @param nextDocument the next document version
     * @param documentDiff where to collect the differences
     */
    private void addDocumentFieldDiffs(XWikiDocument previousDocument, XWikiDocument nextDocument,
        DocumentUnifiedDiff documentDiff)
    {
        maybeAddDiff(documentDiff, "title", previousDocument.getTitle(), nextDocument.getTitle());
        maybeAddDiff(documentDiff, "parent", previousDocument.getParentReference(), nextDocument.getParentReference());
        maybeAddDiff(documentDiff, "hidden", isNull(previousDocument) ? null : previousDocument.isHidden(),
            isNull(nextDocument) ? null : nextDocument.isHidden());
        maybeAddDiff(documentDiff, "defaultLocale", previousDocument.getDefaultLocale(),
            nextDocument.getDefaultLocale());
        maybeAddDiff(documentDiff, "syntax", previousDocument.getSyntax(), nextDocument.getSyntax());
        maybeAddDiff(documentDiff, CONTENT, previousDocument.getContent(), nextDocument.getContent());
    }

    private void addAttachmentDiffs(XWikiDocument previousDocument, XWikiDocument nextDocument,
        DocumentUnifiedDiff documentDiff)
    {
        // Check the attachments that have been deleted of modified.
        for (XWikiAttachment previousAttachment : previousDocument.getAttachmentList()) {
            XWikiAttachment nextAttachment = nextDocument.getAttachment(previousAttachment.getFilename());
            if (previousAttachment != nextAttachment) {
                this.attachmentDiffBuilder.addAttachmentDiff(previousAttachment, nextAttachment, documentDiff);
            }
        }

        // Check the attachments that have been added.
        for (XWikiAttachment nextAttachment : nextDocument.getAttachmentList()) {
            XWikiAttachment previousAttachment = previousDocument.getAttachment(nextAttachment.getFilename());
            if (previousAttachment == null) {
                this.attachmentDiffBuilder.addAttachmentDiff(previousAttachment, nextAttachment, documentDiff);
            }
        }
    }

    private void addObjectDiffs(XWikiDocument previousDocument, XWikiDocument nextDocument,
        DocumentUnifiedDiff documentDiff)
    {
        // Check the objects that have been deleted of modified.
        for (List<BaseObject> previousObjects : previousDocument.getXObjects().values()) {
            for (BaseObject previousObject : previousObjects) {
                // It can be null when objects are deleted and the document is still in the cache storage.
                if (previousObject != null) {
                    BaseObject nextObject =
                        nextDocument.getXObject(previousObject.getXClassReference(), previousObject.getNumber());
                    if (previousObject != nextObject) {
                        addObjectDiff(previousObject, nextObject, documentDiff);
                    }
                }
            }
        }

        // Check the objects that have been added.
        for (List<BaseObject> nextObjects : nextDocument.getXObjects().values()) {
            for (BaseObject nextObject : nextObjects) {
                // It can be null when objects are deleted and the document is still in the cache storage.
                if (nextObject != null) {
                    BaseObject previousObject =
                        previousDocument.getXObject(nextObject.getXClassReference(), nextObject.getNumber());
                    if (previousObject == null) {
                        addObjectDiff(previousObject, nextObject, documentDiff);
                    }
                }
            }
        }
    }

    private void addObjectDiff(BaseObject previousObject, BaseObject nextObject, DocumentUnifiedDiff documentDiff)
    {
        ObjectReference previousReference =
            getObjectVersionReference(previousObject, documentDiff.getPreviousReference());
        ObjectReference nextReference = getObjectVersionReference(nextObject, documentDiff.getNextReference());
        EntityUnifiedDiff<ObjectReference> objectDiff = new EntityUnifiedDiff<>(previousReference, nextReference);

        addObjectDiff(previousObject == null ? new BaseObject() : previousObject, nextObject == null ? new BaseObject()
            : nextObject, objectDiff);

        if (objectDiff.size() > 0) {
            documentDiff.getObjectDiffs().add(objectDiff);
        }
    }

    private ObjectReference getObjectVersionReference(BaseObject object,
        DocumentVersionReference documentVersionReference)
    {
        return object == null ? null : new BaseObjectReference(object.getXClassReference(), object.getNumber(),
            documentVersionReference);
    }

    private void addObjectDiff(BaseCollection<?> previousObject, BaseCollection<?> nextObject,
        Map<String, List<UnifiedDiffBlock<String, Character>>> objectDiff)
    {
        // Check the properties that have been deleted or modified.
        for (String propertyName : previousObject.getPropertyList()) {
            BaseProperty<?> previousProperty = (BaseProperty<?>) previousObject.getField(propertyName);
            BaseProperty<?> nextProperty = (BaseProperty<?>) nextObject.getField(propertyName);
            if (previousProperty != nextProperty) {
                addObjectPropertyDiff(previousProperty, nextProperty, objectDiff);
            }
        }

        // Check the properties that have been added.
        for (String propertyName : nextObject.getPropertyList()) {
            BaseProperty<?> previousProperty = (BaseProperty<?>) previousObject.getField(propertyName);
            BaseProperty<?> nextProperty = (BaseProperty<?>) nextObject.getField(propertyName);
            if (previousProperty == null) {
                addObjectPropertyDiff(previousProperty, nextProperty, objectDiff);
            }
        }
    }

    private void addObjectPropertyDiff(BaseProperty<?> previousProperty, BaseProperty<?> nextProperty,
        Map<String, List<UnifiedDiffBlock<String, Character>>> objectDiff)
    {
        String key = previousProperty == null ? nextProperty.getName() : previousProperty.getName();
        Object previousValue = previousProperty == null ? null : previousProperty.getValue();
        Object nextValue = nextProperty == null ? null : nextProperty.getValue();
        if (maybeAddDiff(objectDiff, key, previousValue, nextValue)
            && (isPrivateProperty(previousProperty) || isPrivateProperty(nextProperty))) {
            // Empty the differences if the property is private.
            objectDiff.get(key).clear();
        }
    }

    private boolean isPrivateProperty(BaseProperty<?> property)
    {
        BaseCollection<?> object = property == null ? null : property.getObject();
        if (object != null) {
            BaseClass xclass = object.getXClass(this.xcontextProvider.get());
            if (xclass != null) {
                PropertyClass propertyClass = (PropertyClass) xclass.get(property.getName());
                String propertyType = propertyClass == null ? null : propertyClass.getClassType();

                return "Password".equals(propertyType)
                    || ("Email".equals(propertyType) && this.emailProvider.get().shouldObfuscate());
            }
        }

        return false;
    }

    private void addClassPropertyDiffs(BaseClass previousClass, BaseClass nextClass, DocumentUnifiedDiff documentDiff)
    {
        // Check the properties that have been deleted or modified.
        for (String propertyName : previousClass.getPropertyList()) {
            PropertyClass previousProperty = (PropertyClass) previousClass.get(propertyName);
            PropertyClass nextProperty = (PropertyClass) nextClass.get(propertyName);
            addClassPropertyDiff(previousProperty, nextProperty, documentDiff);
        }

        // Check the properties that have been added.
        for (String propertyName : nextClass.getPropertyList()) {
            PropertyClass previousProperty = (PropertyClass) previousClass.get(propertyName);
            PropertyClass nextProperty = (PropertyClass) nextClass.get(propertyName);
            if (previousProperty == null) {
                addClassPropertyDiff(previousProperty, nextProperty, documentDiff);
            }
        }
    }

    private void addClassPropertyDiff(PropertyClass previousProperty, PropertyClass nextProperty,
        DocumentUnifiedDiff documentDiff)
    {
        ClassPropertyReference previousReference =
            getClassPropertyVersionReference(previousProperty, documentDiff.getPreviousReference());
        ClassPropertyReference nextReference =
            getClassPropertyVersionReference(nextProperty, documentDiff.getNextReference());
        EntityUnifiedDiff<ClassPropertyReference> classPropertyDiff =
            new EntityUnifiedDiff<>(previousReference, nextReference);

        // Catch a property type change.
        maybeAddDiff(classPropertyDiff, "type", previousProperty == null ? null : previousProperty.getClassType(),
            nextProperty == null ? null : nextProperty.getClassType());

        addObjectDiff(previousProperty == null ? new PropertyClass() : previousProperty, nextProperty == null
            ? new PropertyClass() : nextProperty, classPropertyDiff);

        // The property name is already specified by the previous / next reference.
        classPropertyDiff.remove("name");
        // This meta property is not used (there's no UI to change it).
        classPropertyDiff.remove("unmodifiable");

        if (classPropertyDiff.size() > 0) {
            documentDiff.getClassPropertyDiffs().add(classPropertyDiff);
        }
    }

    private ClassPropertyReference getClassPropertyVersionReference(PropertyClass property,
        DocumentVersionReference documentVersionReference)
    {
        return property == null ? null : new ClassPropertyReference(property.getName(), documentVersionReference);
    }
}
