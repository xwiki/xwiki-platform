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
package org.xwiki.store.merge.internal;

import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.diff.ConflictDecision;
import org.xwiki.diff.DiffManager;
import org.xwiki.diff.MergeException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.store.merge.MergeConflictDecisionsManager;
import org.xwiki.store.merge.MergeDocumentResult;
import org.xwiki.store.merge.MergeManager;
import org.xwiki.store.merge.MergeManagerResult;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.AttachmentDiff;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.doc.merge.MergeConfiguration.ConflictFallbackVersion;
import com.xpn.xwiki.doc.merge.MergeResult;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.ElementInterface;
import com.xpn.xwiki.objects.ObjectDiff;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Default implementation of the {@link MergeManager}.
 *
 * @version $Id$
 * @since 11.8RC1
 */
@Component
@Singleton
public class DefaultMergeManager implements MergeManager
{
    private static final String ERROR_COLLISION_OBJECT = "Collision found on object [{}]";
    private static final String ERROR_COLLISION_OBJECT_PROPERTY = "Collision found on object property [{}]";
    private static final String ERROR_COLLISION_ATTACHMENT = "Collision found on attachment [{}]";

    private static final String WARNING_OBJECT_REMOVED = "Object [{}] already removed";

    @Inject
    private DiffManager diffManager;

    @Inject
    private MergeConflictDecisionsManager mergeConflictDecisionsManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    private <T> org.xwiki.diff.MergeConfiguration<T> getDefaultConfiguration(MergeConfiguration configuration)
    {
        EntityReference userReference = configuration.getUserReference();
        DocumentReference concernedDocument = configuration.getConcernedDocument();

        if (userReference != null && concernedDocument != null) {
            List<ConflictDecision> conflictDecisionList =
                this.mergeConflictDecisionsManager.getConflictDecisionList(concernedDocument, userReference);
            if (conflictDecisionList != null) {
                return new org.xwiki.diff.MergeConfiguration(conflictDecisionList);
            } else if (configuration.getConflictFallbackVersion() == ConflictFallbackVersion.CURRENT) {
                return new org.xwiki.diff.MergeConfiguration(org.xwiki.diff.MergeConfiguration.Version.CURRENT,
                    Collections.emptyList());
            } else if (configuration.getConflictFallbackVersion() == ConflictFallbackVersion.NEXT) {
                return new org.xwiki.diff.MergeConfiguration(org.xwiki.diff.MergeConfiguration.Version.NEXT,
                    Collections.emptyList());
            }
        }
        return null;
    }

    private void cleanDecisionList(MergeConfiguration configuration)
    {
        EntityReference userReference = configuration.getUserReference();
        DocumentReference concernedDocument = configuration.getConcernedDocument();

        if (userReference != null && concernedDocument != null) {
            this.mergeConflictDecisionsManager.removeConflictDecisionList(concernedDocument, userReference);
        }
    }

    @Override
    public MergeManagerResult<String, String> mergeLines(String previousStr, String newStr, String currentStr,
        MergeConfiguration configuration)
    {
        MergeManagerResult<String, String> mergeResult = new MergeManagerResult<>();
        try {
            org.xwiki.diff.MergeResult<String> result =
                this.diffManager.merge(toLines(previousStr), toLines(newStr), toLines(currentStr),
                    getDefaultConfiguration(configuration));

            mergeResult.getLog().addAll(result.getLog());
            mergeResult.addConflicts(result.getConflicts());
            String resultStr = fromLines(result.getMerged());
            mergeResult.setMergeResult(resultStr);
            mergeResult.setModified(!resultStr.equals(currentStr));
        } catch (MergeException e) {
            mergeResult.getLog().error("Failed to execute merge lines", e);
        }

        return mergeResult;
    }

    /**
     * Merge an Object. Use Object#equals to find conflicts.
     *
     * @param previousObject previous version of the object
     * @param newObject new version of the object
     * @param currentObject current version of the object
     * @param <T> the type of the objects to merge
     * @return the merged object or the provided current object if the merge fail
     */
    @Override
    public <T> MergeManagerResult<T, T> mergeObject(T previousObject, T newObject, T currentObject,
        MergeConfiguration configuration)
    {
        MergeManagerResult<T, T> mergeResult = new MergeManagerResult<>();
        if (ObjectUtils.notEqual(previousObject, newObject)) {
            if (ObjectUtils.equals(previousObject, currentObject)) {
                mergeResult.setMergeResult(newObject);
                mergeResult.setModified(true);
            } else if (ObjectUtils.equals(newObject, currentObject)) {
                mergeResult.setMergeResult(currentObject);
            } else {
                // the three objects are different, we record a conflict and fallback based on the configuration.
                if (configuration.getConflictFallbackVersion() == ConflictFallbackVersion.CURRENT) {
                    mergeResult.setMergeResult(currentObject);
                } else if (configuration.getConflictFallbackVersion() == ConflictFallbackVersion.NEXT) {
                    mergeResult.setMergeResult(newObject);
                    mergeResult.setModified(true);
                }
                mergeResult.getLog()
                    .error("Failed to merge objects: previous=[{}] new=[{}] current=[{}]", previousObject,
                        newObject, currentObject);
            }
        } else {
            mergeResult.setMergeResult(currentObject);
        }

        return mergeResult;
    }

    /**
     * Merge String at characters level.
     *
     * @param previousStr previous version of the string
     * @param newStr new version of the string
     * @param currentStr current version of the string
     * @return the merged string or the provided current string if the merge fail
     */
    @Override
    public MergeManagerResult<String, Character> mergeCharacters(String previousStr, String newStr, String currentStr,
        MergeConfiguration configuration)
    {
        MergeManagerResult<String, Character> mergeResult = new MergeManagerResult<>();
        if (currentStr == null && newStr == null) {
            mergeResult.setMergeResult(null);
        } else {
            try {
                org.xwiki.diff.MergeResult<Character> result =
                    this.diffManager.merge(toCharacters(previousStr), toCharacters(newStr), toCharacters(currentStr),
                        getDefaultConfiguration(configuration));

                mergeResult.getLog().addAll(result.getLog());
                mergeResult.addConflicts(result.getConflicts());
                String resultStr = fromCharacters(result.getMerged());
                mergeResult.setMergeResult(resultStr);
                mergeResult.setModified(!resultStr.equals(currentStr));
            } catch (MergeException e) {
                mergeResult.getLog().error("Failed to execute merge characters", e);
            }
        }
        return mergeResult;
    }

    @Override
    public <T> MergeManagerResult<List<T>, T> mergeList(List<T> commonAncestor, List<T> next, List<T> current,
        MergeConfiguration configuration)
    {
        MergeManagerResult<List<T>, T> mergeResult = new MergeManagerResult<>();
        try {
            org.xwiki.diff.MergeResult<T> result = this.diffManager.merge(commonAncestor, next, current,
                getDefaultConfiguration(configuration));
            mergeResult.getLog().addAll(result.getLog());
            mergeResult.addConflicts(result.getConflicts());
            mergeResult.setMergeResult(result.getMerged());
            mergeResult.setModified(!result.getMerged().equals(current));
        } catch (MergeException e) {
            mergeResult.getLog().error("Failed to execute merge lists", e);
        }

        return mergeResult;
    }

    @Override
    public MergeDocumentResult mergeDocument(DocumentModelBridge previousDocument,
        DocumentModelBridge newDocument, DocumentModelBridge currentDocument, MergeConfiguration configuration)
    {
        MergeDocumentResult mergeResult = new MergeDocumentResult(currentDocument, previousDocument, newDocument);
        
        if (previousDocument instanceof XWikiDocument 
            && newDocument instanceof XWikiDocument 
            && currentDocument instanceof XWikiDocument) {
            
            XWikiDocument previousDoc = (XWikiDocument) previousDocument;
            XWikiDocument newDoc = (XWikiDocument) newDocument;
            XWikiDocument currentDoc = (XWikiDocument) currentDocument;

            XWikiDocument mergedDocument;

            // if the configuration allows to modify the document, we directly use the current doc for the merge
            if (configuration.isProvidedVersionsModifiables()) {
                mergedDocument = currentDoc;
            // else we clone it.
            } else {
                mergedDocument = currentDoc.clone();
            }
            mergeResult.setMergeResult(mergedDocument);
            
            XWikiContext context = this.contextProvider.get();
            
            configuration.setConcernedDocument(currentDoc.getDocumentReferenceWithLocale());
            configuration.setUserReference(context.getUserReference());

            // Title
            // We don't want to merge titles as Strings, since they are often scripts.
            // So better to merge them as objects.
            MergeManagerResult<String, String> titleMergeResult = this.mergeObject(previousDocument.getTitle(),
                newDocument.getTitle(), currentDocument.getTitle(), configuration);
            mergeResult.putMergeResult(MergeDocumentResult.DocumentPart.TITLE, titleMergeResult);
            mergedDocument.setTitle(titleMergeResult.getMergeResult());

            // Content
            MergeManagerResult<String, String> contentMergeResult = this.mergeLines(previousDocument.getContent(),
                newDocument.getContent(), currentDocument.getContent(), configuration);
            mergeResult.putMergeResult(MergeDocumentResult.DocumentPart.CONTENT, contentMergeResult);
            mergedDocument.setContent(contentMergeResult.getMergeResult());

            // Syntax
            MergeManagerResult<Syntax, Syntax> syntaxMergeResult = this.mergeObject(previousDocument.getSyntax(),
                newDocument.getSyntax(), currentDocument.getSyntax(), configuration);
            mergeResult.putMergeResult(MergeDocumentResult.DocumentPart.SYNTAX, syntaxMergeResult);
            mergedDocument.setSyntax(syntaxMergeResult.getMergeResult());

            // Default locale
            MergeManagerResult<Locale, Locale> localeMergeResult = this.mergeObject(previousDoc.getDefaultLocale(),
                newDoc.getDefaultLocale(), currentDoc.getDefaultLocale(), configuration);
            mergeResult.putMergeResult(MergeDocumentResult.DocumentPart.DEFAULT_LOCALE, localeMergeResult);
            mergedDocument.setDefaultLocale(localeMergeResult.getMergeResult());

            // Parent
            MergeManagerResult<EntityReference, EntityReference> parentReferenceMergeResult =
                this.mergeObject(previousDoc.getRelativeParentReference(), newDoc.getRelativeParentReference(),
                    currentDoc.getRelativeParentReference(), configuration);
            mergeResult.putMergeResult(MergeDocumentResult.DocumentPart.PARENT_REFERENCE, parentReferenceMergeResult);
            mergedDocument.setParentReference(parentReferenceMergeResult.getMergeResult());

            // DefaultTemplate
            MergeManagerResult<String, String> templateMergeResult = this.mergeObject(previousDoc.getDefaultTemplate(),
                newDoc.getDefaultTemplate(), currentDoc.getDefaultTemplate(), configuration);
            mergeResult.putMergeResult(MergeDocumentResult.DocumentPart.DEFAULT_TEMPLATE, templateMergeResult);
            mergedDocument.setDefaultTemplate(templateMergeResult.getMergeResult());

            // Hidden
            MergeManagerResult<Boolean, Boolean> hiddenPropertyMergeResult =
                this.mergeObject(previousDoc.isHidden(), newDoc.isHidden(), currentDoc.isHidden(), configuration);
            mergeResult.putMergeResult(MergeDocumentResult.DocumentPart.HIDDEN, hiddenPropertyMergeResult);
            mergedDocument.setHidden(hiddenPropertyMergeResult.getMergeResult());

            // Enforce required rights
            MergeManagerResult<Boolean, Boolean> enforceRequiredRightsMergeResult =
                this.mergeObject(previousDoc.isEnforceRequiredRights(), newDoc.isEnforceRequiredRights(),
                    currentDoc.isEnforceRequiredRights(), configuration);
            mergeResult.putMergeResult(MergeDocumentResult.DocumentPart.ENFORCE_REQUIRED_RIGHTS,
                enforceRequiredRightsMergeResult);
            mergedDocument.setEnforceRequiredRights(enforceRequiredRightsMergeResult.getMergeResult());

            // CustomClass
            MergeManagerResult<String, String> customClassMergeResult = this.mergeLines(previousDoc.getCustomClass(),
                newDoc.getCustomClass(), currentDoc.getCustomClass(), configuration);
            mergeResult.putMergeResult(MergeDocumentResult.DocumentPart.CUSTOM_CLASS, customClassMergeResult);
            mergedDocument.setCustomClass(customClassMergeResult.getMergeResult());

            // ValidationScript
            MergeManagerResult<String, String> validationScriptMergeResult =
                this.mergeLines(previousDoc.getValidationScript(),
                newDoc.getValidationScript(), currentDoc.getValidationScript(), configuration);
            mergeResult.putMergeResult(MergeDocumentResult.DocumentPart.VALIDATION_SCRIPT, validationScriptMergeResult);
            mergedDocument.setValidationScript(validationScriptMergeResult.getMergeResult());

            // Objects
            MergeManagerResult<Map<DocumentReference, List<BaseObject>>, BaseObject> objectMergeManagerResult =
                mergeXObjects(previousDoc, mergedDocument, newDoc, configuration);
            mergeResult.putMergeResult(MergeDocumentResult.DocumentPart.XOBJECTS, objectMergeManagerResult);

            // Class
            MergeManagerResult<ElementInterface, Object> classMergeManagerResult =
                mergeXClass(previousDoc, mergedDocument, newDoc, configuration);
            mergeResult.putMergeResult(MergeDocumentResult.DocumentPart.XCLASS, classMergeManagerResult);

            // Attachments
            MergeManagerResult<List<XWikiAttachment>, XWikiAttachment> attachmentMergeManagerResult =
                mergeAttachments(previousDoc, mergedDocument, newDoc, configuration);
            mergeResult.putMergeResult(MergeDocumentResult.DocumentPart.ATTACHMENTS, attachmentMergeManagerResult);
        } else {
            mergeResult.setMergeResult(currentDocument);
            mergeResult.getLog().error("Cannot merge documents that are not of XWikiDocument class.");
        }

        // Ensure that the decisions won't be reused at the next merge.
        this.cleanDecisionList(configuration);
        return mergeResult;
    }

    private MergeManagerResult<ElementInterface, Object> mergeXClass(XWikiDocument previousDoc,
        XWikiDocument mergedDocument, XWikiDocument newDoc, MergeConfiguration configuration)
    {
        XWikiContext context = this.contextProvider.get();
        BaseClass classResult = mergedDocument.getXClass();
        BaseClass previousClass = previousDoc.getXClass();
        BaseClass newClass = newDoc.getXClass();
        return classResult.merge(previousClass, newClass, configuration, context);
    }

    private MergeManagerResult<Map<DocumentReference, List<BaseObject>>, BaseObject> mergeXObjects(
        XWikiDocument previousDoc, XWikiDocument mergedDocument, XWikiDocument newDoc, MergeConfiguration configuration)
    {
        XWikiContext context = this.contextProvider.get();

        MergeManagerResult<Map<DocumentReference, List<BaseObject>>, BaseObject> objectMergeResult =
            new MergeManagerResult<>();
        objectMergeResult.setMergeResult(mergedDocument.getXObjects());

        List<List<ObjectDiff>> objectsDiff = mergedDocument.getObjectDiff(previousDoc, newDoc, context);
        if (!objectsDiff.isEmpty()) {
            // Apply diff on result
            for (List<ObjectDiff> objectClassDiff : objectsDiff) {
                for (ObjectDiff diff : objectClassDiff) {
                    BaseObject objectResult = mergedDocument.getXObject(diff.getXClassReference(),
                        diff.getNumber());
                    BaseObject previousObject = previousDoc.getXObject(diff.getXClassReference(), diff.getNumber());
                    BaseObject newObject = newDoc.getXObject(diff.getXClassReference(), diff.getNumber());
                    PropertyInterface propertyResult =
                        objectResult != null ? objectResult.getField(diff.getPropName()) : null;
                    PropertyInterface previousProperty =
                        previousObject != null ? previousObject.getField(diff.getPropName()) : null;
                    PropertyInterface newProperty =
                        newObject != null ? newObject.getField(diff.getPropName()) : null;

                    if (diff.getAction().equals(ObjectDiff.ACTION_OBJECTADDED)) {
                        if (objectResult == null) {
                            mergedDocument.setXObject(newObject.getNumber(), newObject.clone());
                            objectMergeResult.setModified(true);
                        } else {
                            if (!objectResult.equals(newObject)) {
                                // collision between DB and new: object to add but already exists in the DB and not
                                // the same
                                // If fallback is next version then we set it.
                                if (newObject != null &&
                                    configuration.getConflictFallbackVersion() == ConflictFallbackVersion.NEXT) {
                                    mergedDocument.setXObject(newObject.getNumber(), newObject.clone());
                                    objectMergeResult.setModified(true);
                                }
                                // TODO: Manage properly the conflicts.
                                objectMergeResult.getLog().error(ERROR_COLLISION_OBJECT, objectResult.getReference());
                            } else {
                                // Already added, lets assume the user is prescient
                                objectMergeResult.getLog().warn("Object [{}] already added",
                                    objectResult.getReference());
                            }
                        }
                    } else if (diff.getAction().equals(ObjectDiff.ACTION_OBJECTREMOVED)) {
                        if (objectResult != null) {
                            if (objectResult.equals(previousObject)) {
                                mergedDocument.removeXObject(objectResult);
                                objectMergeResult.setModified(true);
                            } else {
                                // collision between DB and new: object to remove but not the same as previous
                                // version
                                // We don't remove any xobject as fallback
                                // TODO: Manage properly the conflicts.
                                objectMergeResult.getLog().error(ERROR_COLLISION_OBJECT, objectResult.getReference());
                            }
                        } else {
                            // Already removed from DB, lets assume the user is prescient
                            objectMergeResult.getLog().warn(WARNING_OBJECT_REMOVED, previousObject.getReference());
                        }
                    } else if (previousObject != null && newObject != null) {
                        if (objectResult != null) {
                            if (diff.getAction().equals(ObjectDiff.ACTION_PROPERTYADDED)) {
                                if (propertyResult == null) {
                                    objectResult.safeput(diff.getPropName(), newProperty);
                                    objectMergeResult.setModified(true);
                                } else {
                                    if (!propertyResult.equals(newProperty)) {
                                        // collision between DB and new:
                                        // property to add but already exists in the DB and not the same
                                        // TODO: Manage properly the conflicts.
                                        // If fallback is next version then we put next version value
                                        if (configuration.getConflictFallbackVersion() == ConflictFallbackVersion.NEXT)
                                        {
                                            objectResult.safeput(diff.getPropName(), newProperty);
                                            objectMergeResult.setModified(true);
                                        }
                                        objectMergeResult.getLog().error(ERROR_COLLISION_OBJECT_PROPERTY,
                                            propertyResult.getReference());
                                    } else {
                                        // Already added, lets assume the user is prescient
                                        objectMergeResult.getLog().warn("Object property [{}] already added",
                                            propertyResult.getReference());
                                    }
                                }
                            } else if (diff.getAction().equals(ObjectDiff.ACTION_PROPERTYREMOVED)) {
                                if (propertyResult != null) {
                                    if (propertyResult.equals(previousProperty)) {
                                        objectResult.removeField(diff.getPropName());
                                        objectMergeResult.setModified(true);
                                    } else {
                                        // collision between DB and new: supposed to be removed but the DB version
                                        // is not the same as the previous version
                                        // TODO: Manage properly the conflicts.
                                        // We don't remove any property as fallback
                                        objectMergeResult.getLog().error(ERROR_COLLISION_OBJECT_PROPERTY,
                                            propertyResult.getReference());
                                    }
                                } else {
                                    // Already removed from DB, lets assume the user is prescient
                                    objectMergeResult.getLog().warn("Object property [{}] already removed",
                                        previousProperty.getReference());
                                }
                            } else if (diff.getAction().equals(ObjectDiff.ACTION_PROPERTYCHANGED)) {
                                if (propertyResult != null) {
                                    if (propertyResult.equals(previousProperty)) {
                                        objectResult.safeput(diff.getPropName(), newProperty);
                                        objectMergeResult.setModified(true);
                                    } else {
                                        // Try to apply a 3 ways merge on the property
                                        MergeManagerResult<ElementInterface, Object> propertyManagerResult =
                                            propertyResult.merge(previousProperty, newProperty, configuration, context);
                                        objectMergeResult.getLog().addAll(propertyManagerResult.getLog());
                                        if (propertyManagerResult.isModified()) {
                                            objectMergeResult.setModified(true);
                                            objectResult.safeput(diff.getPropName(),
                                                (PropertyInterface) propertyManagerResult.getMergeResult());
                                        }
                                    }
                                } else {
                                    // collision between DB and new: property to modify but does not exist in DB
                                    // Lets assume it's a mistake to fix
                                    objectMergeResult.getLog().warn("Object [{}] does not exist",
                                        newProperty.getReference());

                                    objectResult.safeput(diff.getPropName(), newProperty);
                                    objectMergeResult.setModified(true);
                                }
                            }
                        } else {
                            // Object explitely removed from the DB, lets assume we don't care about the changes
                            objectMergeResult.getLog().warn(WARNING_OBJECT_REMOVED, previousObject.getReference());
                        }
                    }
                }
            }
        }

        return objectMergeResult;
    }

    private MergeManagerResult<List<XWikiAttachment>, XWikiAttachment> mergeAttachments(XWikiDocument previousDoc,
         XWikiDocument mergedDocument, XWikiDocument newDoc, MergeConfiguration configuration)
    {
        XWikiContext context = this.contextProvider.get();
        MergeResult attachmentMergeResult = new MergeResult();
        List<AttachmentDiff> attachmentsDiff =
            previousDoc.getAttachmentDiff(previousDoc, newDoc, context);
        if (!attachmentsDiff.isEmpty()) {
            // Apply deleted attachment diff on result (new attachment has already been saved)
            for (AttachmentDiff diff : attachmentsDiff) {
                XWikiAttachment previousAttachment = diff.getOrigAttachment();
                XWikiAttachment nextAttachment = diff.getNewAttachment();
                XWikiAttachment attachment = mergedDocument.getAttachment(diff.getFileName());

                switch (diff.getType()) {
                    case DELETE:
                        if (attachment != null) {
                            try {
                                if (attachment.equalsData(previousAttachment, context)) {
                                    mergedDocument.removeAttachment(attachment);
                                    attachmentMergeResult.setModified(true);
                                } else {
                                    // collision between DB and new: attachment modified by user
                                    // TODO: manage properly conflicts
                                    attachmentMergeResult.getLog().error(ERROR_COLLISION_ATTACHMENT,
                                        attachment.getReference());
                                }
                            } catch (XWikiException e) {
                                attachmentMergeResult.getLog()
                                    .error("Failed to compare attachments with reference [{}]",
                                        attachment.getReference());
                            }
                        } else {
                            // Already removed from DB, lets assume the user is prescient
                            attachmentMergeResult.getLog().warn("Attachment [{}] already removed",
                                previousAttachment.getReference());
                        }
                        break;
                    case INSERT:
                        if (attachment != null) {
                            try {
                                if (!attachment.equalsData(nextAttachment, context)) {
                                    // collision between DB and new: attachment to add but a different one already
                                    // exists in the DB
                                    // TODO: manage conflict properly
                                    attachmentMergeResult.getLog().error(ERROR_COLLISION_ATTACHMENT,
                                        attachment.getReference());
                                } else {
                                    // Already added to the DB, lets assume the user is prescient
                                    attachmentMergeResult.getLog().warn("Attachment [{}] already added",
                                        nextAttachment.getReference());
                                }
                            } catch (XWikiException e) {
                                attachmentMergeResult.getLog().error("Failed to compare attachments with reference "
                                    + "[{}]", attachment.getReference());
                            }
                        } else {
                            mergedDocument.addAttachment(nextAttachment);
                            attachmentMergeResult.setModified(true);
                        }
                        break;
                    case CHANGE:
                        if (attachment != null) {
                            attachment.merge(previousAttachment, nextAttachment, configuration, context,
                                attachmentMergeResult);
                        } else {
                            // collision between DB and new: attachment modified but does not exist in the DB
                            // TODO: manage conflict properly
                            attachmentMergeResult.getLog().error(ERROR_COLLISION_ATTACHMENT,
                                previousAttachment.getReference());
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        MergeManagerResult<List<XWikiAttachment>, XWikiAttachment> attachmentMergeManagerResult =
            new MergeManagerResult<>();
        attachmentMergeManagerResult.setMergeResult(mergedDocument.getAttachmentList());
        attachmentMergeManagerResult.getLog().addAll(attachmentMergeResult.getLog());
        attachmentMergeManagerResult.setModified(attachmentMergeResult.isModified());

        return attachmentMergeManagerResult;
    }

    /**
     * @param lines the lines
     * @return the multilines text
     */
    private static String fromLines(List<String> lines)
    {
        return StringUtils.join(lines, '\n');
    }

    /**
     * @param str the multilines text
     * @return the lines
     */
    private static List<String> toLines(String str)
    {
        List<String> result;
        try {
            result = IOUtils.readLines(new StringReader(str));

            // Handle special case where the string ends with a new line
            if (str.endsWith("\n") || str.endsWith("\r") || str.endsWith("\r\n")) {
                result.add("");
            }

        } catch (UncheckedIOException e) {
            // Should never happen
            result = null;
        }
        return result;
    }

    /**
     * @param characters the characters
     * @return the single line text
     */
    private static String fromCharacters(List<Character> characters)
    {
        return StringUtils.join(characters, null);
    }

    /**
     * @param str the single line text
     * @return the lines
     */
    private static List<Character> toCharacters(String str)
    {
        List<Character> characters;

        if (str != null) {
            characters = new ArrayList<Character>(str.length());

            for (char c : str.toCharArray()) {
                characters.add(c);
            }
        } else {
            characters = Collections.emptyList();
        }

        return characters;
    }
}
