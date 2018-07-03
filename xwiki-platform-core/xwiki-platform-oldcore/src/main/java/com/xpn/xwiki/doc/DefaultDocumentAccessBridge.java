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
package com.xpn.xwiki.doc;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Exposes methods for accessing Document data. This is temporary until we remodel the Model classes and the Document
 * services. The implementation is inside the old core, and not in a component because it has dependencies on the old
 * core.
 *
 * @version $Id$
 * @since 1.6M1
 */
@Component
@Singleton
public class DefaultDocumentAccessBridge implements DocumentAccessBridge
{
    private static final LocalDocumentReference USERCLASS_REFERENCE = new LocalDocumentReference("XWiki", "XWikiUsers");

    /** Needed for accessing the XWikiContext. */
    @Inject
    private Provider<XWikiContext> contextProvider;

    /**
     * Used to resolve a string into a proper Document Reference using the current document's reference to fill the
     * blanks, except for the page name for which the default page name is used instead.
     */
    @Inject
    @Named("currentmixed")
    private DocumentReferenceResolver<String> currentMixedDocumentReferenceResolver;

    /**
     * Used to serialize full reference of current user.
     */
    @Inject
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    /**
     * Used to convert a Document Reference to string (compact form without the wiki part if it matches the current
     * wiki).
     */
    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactWikiEntityReferenceSerializer;

    @Inject
    private Provider<ContextualAuthorizationManager> authorizationProvider;

    @Inject
    private Logger logger;

    private XWikiContext getContext()
    {
        return this.contextProvider.get();
    }

    @Override
    @Deprecated
    public DocumentModelBridge getDocument(String documentReference) throws Exception
    {
        XWikiContext xcontext = getContext();
        return xcontext.getWiki().getDocument(documentReference, xcontext).getTranslatedDocument(xcontext);
    }

    @Override
    @Deprecated
    public DocumentModelBridge getDocument(DocumentReference documentReference) throws Exception
    {
        return getTranslatedDocumentInstance(documentReference);
    }

    @Override
    public DocumentModelBridge getDocumentInstance(DocumentReference documentReference) throws Exception
    {
        XWikiContext xcontext = getContext();
        return xcontext.getWiki().getDocument(documentReference, xcontext);
    }

    @Override
    public DocumentModelBridge getDocumentInstance(EntityReference reference) throws Exception
    {
        XWikiContext xcontext = getContext();
        return xcontext.getWiki().getDocument(reference, xcontext);
    }

    @Override
    public DocumentModelBridge getTranslatedDocumentInstance(DocumentReference documentReference) throws Exception
    {
        XWikiContext xcontext = getContext();
        return xcontext.getWiki().getDocument(documentReference, xcontext).getTranslatedDocument(xcontext);
    }

    @Override
    public DocumentReference getCurrentDocumentReference()
    {
        XWikiDocument currentDocument = null;
        XWikiContext context = getContext();
        if (context != null) {
            currentDocument = context.getDoc();
        }

        return currentDocument == null ? null : currentDocument.getDocumentReference();
    }

    @Override
    @Deprecated
    public String getDocumentContent(String documentReference) throws Exception
    {
        XWikiContext xcontext = getContext();
        return getDocumentContent(documentReference, xcontext.getLanguage());
    }

    @Override
    public String getDocumentContentForDefaultLanguage(DocumentReference documentReference) throws Exception
    {
        XWikiContext xcontext = getContext();
        return xcontext.getWiki().getDocument(documentReference, xcontext).getContent();
    }

    @Override
    @Deprecated
    public String getDocumentContentForDefaultLanguage(String documentReference) throws Exception
    {
        XWikiContext xcontext = getContext();
        return xcontext.getWiki().getDocument(documentReference, xcontext).getContent();
    }

    @Override
    public String getDocumentContent(DocumentReference documentReference, String language) throws Exception
    {
        XWikiContext xcontext = getContext();
        String originalRev = (String) xcontext.get("rev");
        try {
            xcontext.remove("rev");
            return xcontext.getWiki().getDocument(documentReference, xcontext).getTranslatedContent(language, xcontext);
        } finally {
            if (originalRev != null) {
                xcontext.put("rev", originalRev);
            }
        }
    }

    @Override
    @Deprecated
    public String getDocumentContent(String documentReference, String language) throws Exception
    {
        XWikiContext xcontext = getContext();
        String originalRev = (String) xcontext.get("rev");
        try {
            xcontext.remove("rev");
            return xcontext.getWiki().getDocument(documentReference, xcontext).getTranslatedContent(language, xcontext);
        } finally {
            if (originalRev != null) {
                xcontext.put("rev", originalRev);
            }
        }
    }

    @Override
    public boolean exists(DocumentReference documentReference)
    {
        XWikiContext context = getContext();
        if (context != null) {
            return context.getWiki().exists(documentReference, context);
        } else {
            return false;
        }
    }

    @Override
    @Deprecated
    public boolean exists(String documentReference)
    {
        XWikiContext context = getContext();
        if (context != null) {
            return context.getWiki().exists(documentReference, context);
        } else {
            return false;
        }
    }

    @Override
    public void setDocumentContent(DocumentReference documentReference, String content, String editComment,
        boolean isMinorEdit) throws Exception
    {
        XWikiContext xcontext = getContext();
        XWikiDocument doc = xcontext.getWiki().getDocument(documentReference, xcontext);
        doc.setContent(content);
        saveDocument(doc, editComment, isMinorEdit);
    }

    @Override
    @Deprecated
    public void setDocumentContent(String documentReference, String content, String editComment, boolean isMinorEdit)
        throws Exception
    {
        XWikiContext xcontext = getContext();
        XWikiDocument doc = xcontext.getWiki().getDocument(documentReference, xcontext);
        doc.setContent(content);
        saveDocument(doc, editComment, isMinorEdit);
    }

    @Override
    @Deprecated
    public String getDocumentSyntaxId(String documentReference) throws Exception
    {
        XWikiContext xcontext = getContext();
        XWikiDocument doc = xcontext.getWiki().getDocument(documentReference, xcontext);

        return doc.getSyntaxId();
    }

    @Override
    public void setDocumentSyntaxId(DocumentReference documentReference, String syntaxId) throws Exception
    {
        XWikiContext xcontext = getContext();
        XWikiDocument doc = xcontext.getWiki().getDocument(documentReference, xcontext);
        doc.setSyntaxId(syntaxId);
        saveDocument(doc, String.format("Changed document syntax from [%s] to [%s].", doc.getSyntax(), syntaxId), true);
    }

    @Override
    @Deprecated
    public void setDocumentSyntaxId(String documentReference, String syntaxId) throws Exception
    {
        XWikiContext xcontext = getContext();
        XWikiDocument doc = xcontext.getWiki().getDocument(documentReference, xcontext);
        String oldSyntaxId = doc.getSyntaxId();
        doc.setSyntaxId(syntaxId);
        saveDocument(doc, String.format("Changed document syntax from [%s] to [%s].", oldSyntaxId, syntaxId), true);
    }

    @Override
    public void setDocumentParentReference(DocumentReference documentReference, DocumentReference parentReference)
        throws Exception
    {
        XWikiContext xcontext = getContext();
        XWikiDocument doc = xcontext.getWiki().getDocument(documentReference, xcontext);
        doc.setParent(this.compactWikiEntityReferenceSerializer.serialize(parentReference, doc.getDocumentReference()));
        saveDocument(doc, String.format("Changed document parent to [%s].",
            this.defaultEntityReferenceSerializer.serialize(parentReference)), true);
    }

    @Override
    public void setDocumentTitle(DocumentReference documentReference, String title) throws Exception
    {
        XWikiContext xcontext = getContext();
        XWikiDocument doc = xcontext.getWiki().getDocument(documentReference, xcontext);
        doc.setTitle(title);
        saveDocument(doc, String.format("Changed document title to [%s].", title), true);
    }

    @Override
    public int getObjectNumber(DocumentReference documentReference, DocumentReference classReference,
        String propertyName, String valueToMatch)
    {
        try {
            XWikiContext xcontext = getContext();
            XWikiDocument doc = xcontext.getWiki().getDocument(documentReference, xcontext);
            BaseObject object = doc.getXObject(classReference, propertyName, valueToMatch, false);
            return object != null ? object.getNumber() : -1;
        } catch (XWikiException e) {
            return -1;
        }
    }

    @Override
    public Object getProperty(ObjectPropertyReference objectPropertyReference)
    {
        ObjectReference objectReference = (ObjectReference) objectPropertyReference.extractReference(EntityType.OBJECT);

        return getProperty(objectReference, objectPropertyReference.getName());
    }

    @Override
    public Object getProperty(ObjectReference objectReference, String propertyName)
    {
        Object value = null;

        try {
            XWikiContext xcontext = getContext();

            if (xcontext != null && xcontext.getWiki() != null) {
                DocumentReference documentReference =
                    (DocumentReference) objectReference.extractReference(EntityType.DOCUMENT);
                XWikiDocument doc = xcontext.getWiki().getDocument(documentReference, xcontext);
                BaseObject object = doc.getXObject(objectReference);
                if (object != null) {
                    BaseProperty property = (BaseProperty) object.get(propertyName);
                    if (property != null) {
                        value = property.getValue();
                    }
                }
            }
        } catch (Exception e) {
            this.logger.error("Failed to get property", e);
        }

        return value;
    }

    @Override
    public Object getProperty(String documentReference, String className, int objectNumber, String propertyName)
    {
        Object value = null;

        try {
            XWikiContext xcontext = getContext();

            if (xcontext != null && xcontext.getWiki() != null) {
                XWikiDocument doc = xcontext.getWiki().getDocument(documentReference, xcontext);
                BaseObject object = doc.getObject(className, objectNumber);
                if (object != null) {
                    BaseProperty property = (BaseProperty) object.get(propertyName);
                    if (property != null) {
                        value = property.getValue();
                    }
                }
            }
        } catch (Exception e) {
            this.logger.error("Failed to get property", e);
        }

        return value;
    }

    @Override
    @Deprecated
    public Object getProperty(String documentReference, String className, String propertyName)
    {
        Object value = null;

        try {
            XWikiContext xcontext = getContext();

            if (xcontext != null && xcontext.getWiki() != null) {
                XWikiDocument doc = xcontext.getWiki().getDocument(documentReference, xcontext);
                BaseObject object = doc.getObject(className);
                if (object != null) {
                    BaseProperty property = (BaseProperty) object.get(propertyName);
                    if (property != null) {
                        value = property.getValue();
                    }
                }
            }
        } catch (Exception e) {
            this.logger.error("Failed to get property", e);
        }

        return value;
    }

    @Override
    public Object getProperty(DocumentReference documentReference, DocumentReference classReference,
        String propertyName)
    {
        Object value = null;

        try {
            XWikiContext xcontext = getContext();

            if (xcontext != null && xcontext.getWiki() != null) {
                XWikiDocument doc = xcontext.getWiki().getDocument(documentReference, xcontext);
                BaseObject object = doc.getXObject(classReference);
                if (object != null) {
                    BaseProperty property = (BaseProperty) object.get(propertyName);
                    if (property != null) {
                        value = property.getValue();
                    }
                }
            }
        } catch (Exception e) {
            this.logger.error("Failed to get property", e);
        }

        return value;
    }

    @Override
    public Object getProperty(DocumentReference documentReference, DocumentReference classReference, int objectNumber,
        String propertyName)
    {
        Object value = null;

        try {
            XWikiContext xcontext = getContext();

            if (xcontext != null && xcontext.getWiki() != null) {
                XWikiDocument doc = xcontext.getWiki().getDocument(documentReference, xcontext);
                BaseObject object = doc.getXObject(classReference, objectNumber);
                if (object != null) {
                    BaseProperty property = (BaseProperty) object.get(propertyName);
                    if (property != null) {
                        value = property.getValue();
                    }
                }
            }
        } catch (Exception e) {
            this.logger.error("Failed to get property", e);
        }

        return value;
    }

    @Override
    public Object getProperty(String documentReference, String propertyName)
    {
        Object value = null;

        try {
            XWikiContext xcontext = getContext();

            if (xcontext != null && xcontext.getWiki() != null) {
                XWikiDocument doc = xcontext.getWiki().getDocument(documentReference, xcontext);
                BaseObject object = doc.getFirstObject(propertyName, xcontext);
                if (object != null) {
                    BaseProperty property = (BaseProperty) object.get(propertyName);
                    if (property != null) {
                        value = property.getValue();
                    }
                }
            }
        } catch (Exception e) {
            this.logger.error("Failed to get property", e);
        }

        return value;
    }

    @Override
    public List<Object> getProperties(String documentReference, String className)
    {
        List<Object> result;
        try {
            XWikiContext xcontext = getContext();
            result = new ArrayList<Object>(
                xcontext.getWiki().getDocument(documentReference, xcontext).getObject(className).getFieldList());
        } catch (Exception ex) {
            result = Collections.emptyList();
        }
        return result;
    }

    @Override
    public String getPropertyType(String className, String propertyName) throws Exception
    {
        XWikiContext xcontext = getContext();
        PropertyClass pc = null;
        try {
            pc = (PropertyClass) xcontext.getWiki().getDocument(className, xcontext).getXClass().get(propertyName);
        } catch (XWikiException e) {
            this.logger.warn("Failed to get document [{}]. Root cause: [{}]", className,
                ExceptionUtils.getRootCauseMessage(e));
        }

        return pc == null ? null : pc.newProperty().getClass().getName();
    }

    @Override
    public boolean isPropertyCustomMapped(String className, String property) throws Exception
    {
        XWikiContext xcontext = getContext();
        if (!xcontext.getWiki().hasCustomMappings()) {
            return false;
        }
        List<String> lst = xcontext.getWiki().getClass(className, xcontext).getCustomMappingPropertyList(xcontext);
        return lst != null && lst.contains(property);
    }

    @Override
    @Deprecated
    public void setProperty(String documentReference, String className, String propertyName, Object propertyValue)
        throws Exception
    {
        XWikiContext xcontext = getContext();
        XWikiDocument doc = xcontext.getWiki().getDocument(documentReference, xcontext);
        BaseObject obj = doc.getObject(className, true, xcontext);
        if (obj != null) {
            obj.set(propertyName, propertyValue, xcontext);
            saveDocument(doc, String.format("Property [%s] set.", propertyName), true);
        }
    }

    @Override
    public void setProperty(DocumentReference documentReference, DocumentReference classReference, String propertyName,
        Object propertyValue) throws Exception
    {
        XWikiContext xcontext = getContext();
        XWikiDocument doc = xcontext.getWiki().getDocument(documentReference, xcontext);
        BaseObject obj = doc.getXObject(classReference, true, xcontext);
        if (obj != null) {
            obj.set(propertyName, propertyValue, xcontext);
            saveDocument(doc, String.format("Property [%s] set.", propertyName), true);
        }
    }

    @Override
    @Deprecated
    public byte[] getAttachmentContent(String documentReference, String attachmentFilename) throws Exception
    {
        XWikiContext xcontext = getContext();
        return xcontext.getWiki().getDocument(documentReference, xcontext).getAttachment(attachmentFilename)
            .getContent(xcontext);
    }

    @Override
    public InputStream getAttachmentContent(AttachmentReference attachmentReference) throws Exception
    {
        XWikiContext xcontext = getContext();
        XWikiDocument attachmentDocument =
            xcontext.getWiki().getDocument(attachmentReference.getDocumentReference(), xcontext);
        return new ByteArrayInputStream(
            attachmentDocument.getAttachment(attachmentReference.getName()).getContent(xcontext));
    }

    @Override
    public void setAttachmentContent(AttachmentReference attachmentReference, byte[] attachmentData) throws Exception
    {
        XWikiContext xcontext = getContext();
        XWikiDocument doc = xcontext.getWiki().getDocument(attachmentReference.getDocumentReference(), xcontext);

        setAttachmentContent(doc, attachmentReference.getName(), attachmentData, xcontext);
    }

    @Override
    @Deprecated
    public void setAttachmentContent(String documentReference, String attachmentFilename, byte[] attachmentData)
        throws Exception
    {
        XWikiContext xcontext = getContext();
        XWikiDocument doc = xcontext.getWiki().getDocument(documentReference, xcontext);

        setAttachmentContent(doc, attachmentFilename, attachmentData, xcontext);
    }

    private void setAttachmentContent(XWikiDocument doc, String attachmentFilename, byte[] attachmentData,
        XWikiContext xcontext) throws Exception
    {
        if (doc.getAttachment(attachmentFilename) == null) {
            doc.setComment("Add new attachment " + attachmentFilename);
        } else {
            doc.setComment("Update attachment " + attachmentFilename);
        }

        doc.setAttachment(attachmentFilename,
            new ByteArrayInputStream(attachmentData != null ? attachmentData : new byte[0]), xcontext);

        doc.setAuthorReference(getCurrentUserReference());
        if (doc.isNew()) {
            doc.setCreatorReference(getCurrentUserReference());
        }

        xcontext.getWiki().saveDocument(doc, xcontext);
    }

    @Override
    public List<AttachmentReference> getAttachmentReferences(DocumentReference documentReference) throws Exception
    {
        XWikiContext xcontext = getContext();
        List<XWikiAttachment> attachments =
            xcontext.getWiki().getDocument(documentReference, xcontext).getAttachmentList();

        List<AttachmentReference> attachmentReferences = new ArrayList<AttachmentReference>(attachments.size());
        for (XWikiAttachment attachment : attachments) {
            attachmentReferences.add(attachment.getReference());
        }

        return attachmentReferences;
    }

    @Override
    public String getAttachmentVersion(AttachmentReference attachmentReference) throws Exception
    {
        XWikiContext xcontext = getContext();
        XWikiDocument doc = xcontext.getWiki().getDocument(attachmentReference.getDocumentReference(), xcontext);
        XWikiAttachment attachment = doc.getAttachment(attachmentReference.getName());
        return attachment == null ? null : attachment.getVersion();
    }

    @Override
    public String getDocumentURL(DocumentReference documentReference, String action, String queryString, String anchor)
    {
        return getDocumentURL(documentReference, action, queryString, anchor, false);
    }

    @Override
    public String getDocumentURL(final DocumentReference documentReference, final String action,
        final String queryString, final String anchor, final boolean isFullURL)
    {
        if (documentReference == null) {
            return this.getDocumentURL(this.getContext().getDoc().getDocumentReference(), action, queryString, anchor,
                isFullURL);
        }
        if (isFullURL) {
            return this.getContext().getURLFactory()
                .createExternalURL(extractSpacesFromDocumentReference(documentReference), documentReference.getName(),
                    action, queryString, anchor, documentReference.getWikiReference().getName(), this.getContext())
                .toString();
        } else {
            return this.getContext().getWiki().getURL(documentReference, action, queryString, anchor,
                this.getContext());
        }
    }

    @Override
    @Deprecated
    public String getURL(String documentReference, String action, String queryString, String anchor)
    {
        XWikiContext xcontext = getContext();

        // If the document name is empty then use the current document
        String computedDocumentName = documentReference;
        if (StringUtils.isEmpty(documentReference)) {
            computedDocumentName = xcontext.getDoc().getFullName();
        }

        return xcontext.getWiki().getURL(computedDocumentName, action, queryString, anchor, xcontext);
    }

    @Override
    @Deprecated
    public String getAttachmentURL(String documentReference, String attachmentName)
    {
        XWikiContext xcontext = getContext();
        String attachmentURL;
        try {
            attachmentURL = xcontext.getWiki().getAttachmentURL(
                documentReference == null ? xcontext.getDoc().getFullName() : documentReference, attachmentName,
                xcontext);
        } catch (XWikiException e) {
            // This cannot happen. There's a bug in the definition of XWiki.getAttachmentURL: it says it can generate
            // an exception but in fact no exception is raised in the current implementation.
            throw new RuntimeException("Failed to get attachment URL", e);
        }
        return attachmentURL;
    }

    @Override
    public String getAttachmentURL(AttachmentReference attachmentReference, boolean isFullURL)
    {
        return getAttachmentURL(attachmentReference, null, isFullURL);
    }

    @Override
    public String getAttachmentURL(AttachmentReference attachmentReference, String queryString, boolean isFullURL)
    {
        String url;
        if (isFullURL) {
            XWikiContext xcontext = getContext();
            url = xcontext.getURLFactory()
                .createAttachmentURL(attachmentReference.getName(),
                    extractSpacesFromDocumentReference(attachmentReference.getDocumentReference()),
                    attachmentReference.getDocumentReference().getName(), "download", queryString,
                    attachmentReference.getDocumentReference().getWikiReference().getName(), xcontext)
                .toString();
        } else {
            XWikiContext xcontext = getContext();
            String documentReference =
                this.defaultEntityReferenceSerializer.serialize(attachmentReference.getDocumentReference());
            if (documentReference == null) {
                documentReference = xcontext.getDoc().getFullName();
            }
            String fileName = attachmentReference.getName();
            try {
                url = xcontext.getWiki().getAttachmentURL(documentReference, fileName, queryString, xcontext);
            } catch (XWikiException e) {
                // This cannot happen. There's a bug in the definition of XWiki.getAttachmentURL: it says it can
                // generate an exception but in fact no exception is raised in the current implementation.
                throw new RuntimeException("Failed to get attachment URL", e);
            }
        }
        return url;
    }

    @Override
    @Deprecated
    public List<String> getAttachmentURLs(DocumentReference documentReference, boolean isFullURL) throws Exception
    {
        List<String> urls = new ArrayList<String>();
        for (AttachmentReference attachmentReference : getAttachmentReferences(documentReference)) {
            urls.add(getAttachmentURL(attachmentReference, isFullURL));
        }
        return urls;
    }

    @Override
    public boolean isDocumentViewable(DocumentReference documentReference)
    {
        return hasRight(documentReference, "view");
    }

    @Override
    @Deprecated
    public boolean isDocumentViewable(String documentReference)
    {
        return hasRight(documentReference, "view");
    }

    @Override
    @Deprecated
    public boolean isDocumentEditable(String documentReference)
    {
        return hasRight(documentReference, "edit");
    }

    @Override
    public boolean isDocumentEditable(DocumentReference documentReference)
    {
        return hasRight(documentReference, "edit");
    }

    @Override
    public boolean hasProgrammingRights()
    {
        return this.authorizationProvider.get().hasAccess(Right.PROGRAM);
    }

    @Override
    @Deprecated
    public String getCurrentUser()
    {
        DocumentReference userReference = getContext().getUserReference();

        // Make sure to always return the full reference of the user
        if (userReference != null) {
            return this.defaultEntityReferenceSerializer.serialize(userReference);
        } else {
            return XWikiRightService.GUEST_USER_FULLNAME;
        }
    }

    @Override
    public DocumentReference getCurrentUserReference()
    {
        XWikiContext xcontext = getContext();
        return xcontext != null ? xcontext.getUserReference() : null;
    }

    @Override
    public boolean isAdvancedUser()
    {
        return isAdvancedUser(getCurrentUserReference());
    }

    @Override
    public boolean isAdvancedUser(EntityReference userReference)
    {
        boolean advanced = false;
        if (!XWikiRightService.isGuest(userReference)) {
            advanced = true;

            if (!XWikiRightService.isSuperAdmin(userReference)) {
                XWikiContext xcontext = getContext();

                try {
                    XWikiDocument userDocument = xcontext.getWiki().getDocument(userReference, xcontext);
                    advanced =
                        StringUtils.equals(userDocument.getStringValue(USERCLASS_REFERENCE, "usertype"), "Advanced");
                } catch (XWikiException e) {
                    this.logger.error("Failed to get document", e);
                }
            }
        }

        return advanced;
    }

    @Override
    public void setCurrentUser(String userName)
    {
        getContext().setUser(userName);
    }

    @Override
    public String getDefaultEncoding()
    {
        return getContext().getWiki().getEncoding();
    }

    @Override
    public void popDocumentFromContext(Map<String, Object> backupObjects)
    {
        XWikiDocument.restoreContext(backupObjects, getContext());
    }

    @Override
    @Deprecated
    public void pushDocumentInContext(Map<String, Object> backupObjects, String documentReference) throws Exception
    {
        XWikiContext xcontext = getContext();

        // Backup current context state
        XWikiDocument.backupContext(backupObjects, xcontext);

        // Make sure to get the current XWikiContext after ExcutionContext clone
        xcontext = getContext();

        // Change context document
        xcontext.getWiki().getDocument(documentReference, xcontext).setAsContextDoc(xcontext);
    }

    @Override
    public void pushDocumentInContext(Map<String, Object> backupObjects, DocumentReference documentReference)
        throws Exception
    {
        pushDocumentInContext(backupObjects, getDocumentInstance(documentReference));
    }

    @Override
    public void pushDocumentInContext(Map<String, Object> backupObjects, DocumentModelBridge document) throws Exception
    {
        XWikiContext xcontext = getContext();

        // Backup current context state
        XWikiDocument.backupContext(backupObjects, xcontext);

        // Make sure to get the current XWikiContext after ExcutionContext clone
        xcontext = getContext();

        // Change context document
        ((XWikiDocument) document).setAsContextDoc(xcontext);
    }

    @Override
    public String getCurrentWiki()
    {
        XWikiContext xcontext = getContext();
        return xcontext.getWikiId();
    }

    /**
     * Utility method for checking access rights of the current user on a target document.
     *
     * @param documentReference the reference of the document
     * @param right Access right requested.
     * @return True if the current user has the given access right, false otherwise.
     */
    private boolean hasRight(DocumentReference documentReference, String right)
    {
        return hasRight(this.defaultEntityReferenceSerializer.serialize(documentReference), right);
    }

    /**
     * Utility method for checking access rights of the current user on a target document.
     *
     * @param documentReference the reference of the document
     * @param right Access right requested.
     * @return True if the current user has the given access right, false otherwise.
     */
    private boolean hasRight(String documentReference, String right)
    {
        boolean hasRight = false;
        XWikiContext xcontext = getContext();
        try {
            hasRight = xcontext.getWiki().getRightService().hasAccessLevel(right, xcontext.getUser(), documentReference,
                xcontext);
        } catch (XWikiException e) {
            // Do nothing
        }
        return hasRight;
    }

    /**
     * Utility method for saving an {@link XWikiDocument}. This method takes care of setting authors and creators
     * appropriately.
     *
     * @param doc the {@link XWikiDocument} to be saved.
     * @param comment the edit comment.
     * @param isMinorEdit if the change in document is minor.
     * @throws Exception if an error occurs while saving the document.
     */
    private void saveDocument(XWikiDocument doc, String comment, boolean isMinorEdit) throws Exception
    {
        doc.setAuthorReference(getContext().getUserReference());
        if (doc.isNew()) {
            doc.setCreatorReference(getContext().getUserReference());
        }
        getContext().getWiki().saveDocument(doc, comment, isMinorEdit, getContext());
    }

    private String extractSpacesFromDocumentReference(DocumentReference reference)
    {
        // Extract and escape the spaces portion of the passed reference to pass to the old createURL() API which
        // unfortunately doesn't accept a DocumentReference...
        EntityReference spaceReference = reference.getLastSpaceReference().removeParent(reference.getWikiReference());
        return this.defaultEntityReferenceSerializer.serialize(spaceReference);
    }
}
