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
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;

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
    /** Execution context handler, needed for accessing the XWikiContext. */
    @Inject
    private Execution execution;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

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

    private XWikiContext getContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getDocument(String)
     */
    @Deprecated
    public DocumentModelBridge getDocument(String documentReference) throws Exception
    {
        XWikiContext xcontext = getContext();
        return xcontext.getWiki().getDocument(documentReference, xcontext).getTranslatedDocument(xcontext);
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getDocument(org.xwiki.model.reference.DocumentReference)
     */
    public DocumentModelBridge getDocument(DocumentReference documentReference) throws Exception
    {
        XWikiContext xcontext = getContext();
        return xcontext.getWiki().getDocument(documentReference, xcontext).getTranslatedDocument(xcontext);
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getDocument(org.xwiki.model.reference.DocumentReference)
     */
    @Deprecated
    public DocumentModelBridge getDocument(org.xwiki.bridge.DocumentName documentName) throws Exception
    {
        return getDocument(new DocumentReference(documentName.getWiki(), documentName.getSpace(),
            documentName.getPage()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getDocumentName(String)
     */
    @Deprecated
    public org.xwiki.bridge.DocumentName getDocumentName(String documentReference)
    {
        DocumentReference docReference = this.currentMixedDocumentReferenceResolver.resolve(documentReference);
        return new org.xwiki.bridge.DocumentName(docReference.getWikiReference().getName(), docReference
            .getLastSpaceReference().getName(), docReference.getName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentAccessBridge#getCurrentDocumentName()
     */
    @Deprecated
    public org.xwiki.bridge.DocumentName getCurrentDocumentName()
    {
        XWikiDocument currentDocument = getContext().getDoc();

        return currentDocument == null ? null : new org.xwiki.bridge.DocumentName(currentDocument.getWikiName(),
            currentDocument.getSpaceName(), currentDocument.getPageName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentAccessBridge#getCurrentDocumentReference()
     */
    public DocumentReference getCurrentDocumentReference()
    {
        XWikiDocument currentDocument = null;
        XWikiContext context = getContext();
        if (context != null) {
            currentDocument = context.getDoc();
        }

        return currentDocument == null ? null : currentDocument.getDocumentReference();
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getDocumentContent(String)
     */
    @Deprecated
    public String getDocumentContent(String documentReference) throws Exception
    {
        XWikiContext xcontext = getContext();
        return getDocumentContent(documentReference, xcontext.getLanguage());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentAccessBridge#getDocumentContentForDefaultLanguage(org.xwiki.model.reference.DocumentReference)
     */
    public String getDocumentContentForDefaultLanguage(DocumentReference documentReference) throws Exception
    {
        XWikiContext xcontext = getContext();
        return xcontext.getWiki().getDocument(documentReference, xcontext).getContent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getDocumentContentForDefaultLanguage(java.lang.String)
     */
    @Deprecated
    public String getDocumentContentForDefaultLanguage(String documentReference) throws Exception
    {
        XWikiContext xcontext = getContext();
        return xcontext.getWiki().getDocument(documentReference, xcontext).getContent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentAccessBridge#getDocumentContent(org.xwiki.model.reference.DocumentReference,
     *      java.lang.String)
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getDocumentContent(String, String)
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentAccessBridge#exists(org.xwiki.model.reference.DocumentReference)
     */
    public boolean exists(DocumentReference documentReference)
    {
        return getContext().getWiki().exists(documentReference, getContext());
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#exists(String)
     */
    @Deprecated
    public boolean exists(String documentReference)
    {
        return getContext().getWiki().exists(documentReference, getContext());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentAccessBridge#setDocumentContent(org.xwiki.model.reference.DocumentReference,
     *      java.lang.String, java.lang.String, boolean)
     */
    public void setDocumentContent(DocumentReference documentReference, String content, String editComment,
        boolean isMinorEdit) throws Exception
    {
        XWikiContext xcontext = getContext();
        XWikiDocument doc = xcontext.getWiki().getDocument(documentReference, xcontext);
        doc.setContent(content);
        saveDocument(doc, editComment, isMinorEdit);
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#setDocumentContent(String, String, String, boolean)
     */
    @Deprecated
    public void setDocumentContent(String documentReference, String content, String editComment, boolean isMinorEdit)
        throws Exception
    {
        XWikiContext xcontext = getContext();
        XWikiDocument doc = xcontext.getWiki().getDocument(documentReference, xcontext);
        doc.setContent(content);
        saveDocument(doc, editComment, isMinorEdit);
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getDocumentSyntaxId(java.lang.String)
     */
    @Deprecated
    public String getDocumentSyntaxId(String documentReference) throws Exception
    {
        XWikiContext xcontext = getContext();
        XWikiDocument doc = xcontext.getWiki().getDocument(documentReference, xcontext);

        return doc.getSyntaxId();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentAccessBridge#setDocumentSyntaxId(org.xwiki.model.reference.DocumentReference,
     *      java.lang.String)
     */
    public void setDocumentSyntaxId(DocumentReference documentReference, String syntaxId) throws Exception
    {
        XWikiContext xcontext = getContext();
        XWikiDocument doc = xcontext.getWiki().getDocument(documentReference, xcontext);
        doc.setSyntaxId(syntaxId);
        saveDocument(doc, String.format("Changed document syntax from [%s] to [%s].", doc.getSyntax(), syntaxId), true);
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#setDocumentSyntaxId(String, String)
     */
    @Deprecated
    public void setDocumentSyntaxId(String documentReference, String syntaxId) throws Exception
    {
        XWikiContext xcontext = getContext();
        XWikiDocument doc = xcontext.getWiki().getDocument(documentReference, xcontext);
        String oldSyntaxId = doc.getSyntaxId();
        doc.setSyntaxId(syntaxId);
        saveDocument(doc, String.format("Changed document syntax from [%s] to [%s].", oldSyntaxId, syntaxId), true);
    }

    public void setDocumentParentReference(DocumentReference documentReference, DocumentReference parentReference)
        throws Exception
    {
        XWikiContext xcontext = getContext();
        XWikiDocument doc = xcontext.getWiki().getDocument(documentReference, xcontext);
        doc.setParentReference(parentReference);
        saveDocument(doc, String.format("Changed document parent to [%s].", parentReference), true);
    }

    public void setDocumentTitle(DocumentReference documentReference, String title) throws Exception
    {
        XWikiContext xcontext = getContext();
        XWikiDocument doc = xcontext.getWiki().getDocument(documentReference, xcontext);
        doc.setTitle(title);
        saveDocument(doc, String.format("Changed document title to [%s].", title), true);
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getObjectNumber(DocumentReference, DocumentReference, String, String)
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getProperty(ObjectPropertyReference)
     */
    public Object getProperty(ObjectPropertyReference objectPropertyReference)
    {
        try {
            DocumentReference documentReference =
                (DocumentReference) objectPropertyReference.extractReference(EntityType.DOCUMENT);
            ObjectReference objectReference =
                (ObjectReference) objectPropertyReference.extractReference(EntityType.OBJECT);
            XWikiContext xcontext = getContext();
            return ((BaseProperty) xcontext.getWiki().getDocument(documentReference, xcontext)
                .getXObject(objectReference).get(objectPropertyReference.getName())).getValue();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getProperty(ObjectReference, String propertyName)
     */
    public Object getProperty(ObjectReference objectReference, String propertyName)
    {
        try {
            DocumentReference documentReference =
                (DocumentReference) objectReference.extractReference(EntityType.DOCUMENT);
            XWikiContext xcontext = getContext();
            return ((BaseProperty) xcontext.getWiki().getDocument(documentReference, xcontext)
                .getXObject(objectReference).get(propertyName)).getValue();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getProperty(String, String, int, String)
     */
    public Object getProperty(String documentReference, String className, int objectNumber, String propertyName)
    {
        try {
            XWikiContext xcontext = getContext();
            return ((BaseProperty) xcontext.getWiki().getDocument(documentReference, xcontext)
                .getObject(className, objectNumber).get(propertyName)).getValue();
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getProperty(String, String, String)
     */
    @Deprecated
    public Object getProperty(String documentReference, String className, String propertyName)
    {
        Object value;

        try {
            XWikiContext xcontext = getContext();
            XWikiDocument doc = xcontext.getWiki().getDocument(documentReference, xcontext);
            BaseObject object = doc.getObject(className);
            BaseProperty property = (BaseProperty) object.get(propertyName);
            value = property.getValue();
        } catch (Exception ex) {
            value = null;
        }
        return value;
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getProperty(DocumentReference, DocumentReference, String)
     */
    public Object getProperty(DocumentReference documentReference, DocumentReference classReference, String propertyName)
    {
        Object value;

        try {
            XWikiContext xcontext = getContext();
            XWikiDocument doc = xcontext.getWiki().getDocument(documentReference, xcontext);
            BaseObject object = doc.getXObject(classReference);
            BaseProperty property = (BaseProperty) object.get(propertyName);
            value = property.getValue();
        } catch (Exception ex) {
            value = null;
        }
        return value;
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getProperty(DocumentReference, DocumentReference, int, String)
     */
    public Object getProperty(DocumentReference documentReference, DocumentReference classReference, int objectNumber,
        String propertyName)
    {
        Object value;

        try {
            XWikiContext xcontext = getContext();
            XWikiDocument doc = xcontext.getWiki().getDocument(documentReference, xcontext);
            BaseObject object = doc.getXObject(classReference, objectNumber);
            BaseProperty property = (BaseProperty) object.get(propertyName);
            value = property.getValue();
        } catch (Exception ex) {
            value = null;
        }
        return value;
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getProperty(String, String)
     */
    public Object getProperty(String documentReference, String propertyName)
    {
        try {
            XWikiContext xcontext = getContext();
            return ((BaseProperty) xcontext.getWiki().getDocument(documentReference, xcontext)
                .getFirstObject(propertyName, xcontext).get(propertyName)).getValue();
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentAccessBridge#getProperties(java.lang.String, java.lang.String)
     */
    public List<Object> getProperties(String documentReference, String className)
    {
        List<Object> result;
        try {
            XWikiContext xcontext = getContext();
            result =
                new ArrayList<Object>(xcontext.getWiki().getDocument(documentReference, xcontext).getObject(className)
                    .getFieldList());
        } catch (Exception ex) {
            result = Collections.emptyList();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getPropertyType(String, String)
     */
    public String getPropertyType(String className, String propertyName) throws Exception
    {
        XWikiContext xcontext = getContext();
        PropertyClass pc = xcontext.getWiki().getPropertyClassFromName(className + "_" + propertyName, xcontext);
        if (pc == null) {
            return null;
        } else {
            return pc.newProperty().getClass().getName();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#isPropertyCustomMapped(String, String)
     */
    public boolean isPropertyCustomMapped(String className, String property) throws Exception
    {
        XWikiContext xcontext = getContext();
        if (!xcontext.getWiki().hasCustomMappings()) {
            return false;
        }
        List<String> lst = xcontext.getWiki().getClass(className, xcontext).getCustomMappingPropertyList(xcontext);
        return lst != null && lst.contains(property);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentAccessBridge#setProperty(java.lang.String, java.lang.String, java.lang.String,
     *      java.lang.Object)
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentAccessBridge#setProperty(DocumentReference, DocumentReference, java.lang.String,
     *      java.lang.Object)
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getAttachmentContent(String, String)
     */
    @Deprecated
    public byte[] getAttachmentContent(String documentReference, String attachmentFilename) throws Exception
    {
        XWikiContext xcontext = getContext();
        return xcontext.getWiki().getDocument(documentReference, xcontext).getAttachment(attachmentFilename)
            .getContent(xcontext);
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getAttachmentContent(org.xwiki.model.reference.AttachmentReference)
     */
    public InputStream getAttachmentContent(AttachmentReference attachmentReference) throws Exception
    {
        XWikiContext xcontext = getContext();
        XWikiDocument attachmentDocument =
            xcontext.getWiki().getDocument(attachmentReference.getDocumentReference(), xcontext);
        return new ByteArrayInputStream(attachmentDocument.getAttachment(attachmentReference.getName()).getContent(
            xcontext));
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getAttachmentContent(org.xwiki.bridge.AttachmentName)
     */
    @Deprecated
    public InputStream getAttachmentContent(org.xwiki.bridge.AttachmentName attachmentName) throws Exception
    {
        return getAttachmentContent(new AttachmentReference(attachmentName.getFileName(), new DocumentReference(
            attachmentName.getDocumentName().getWiki(), attachmentName.getDocumentName().getSpace(), attachmentName
                .getDocumentName().getPage())));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentAccessBridge#setAttachmentContent(org.xwiki.model.reference.AttachmentReference,
     *      byte[])
     */
    public void setAttachmentContent(AttachmentReference attachmentReference, byte[] attachmentData) throws Exception
    {
        XWikiContext xcontext = getContext();
        XWikiDocument doc = xcontext.getWiki().getDocument(attachmentReference.getDocumentReference(), xcontext);
        XWikiAttachment attachment = doc.getAttachment(attachmentReference.getName());
        if (attachment == null) {
            attachment = new XWikiAttachment();
            doc.getAttachmentList().add(attachment);
            doc.setComment("Add new attachment " + attachmentReference.getName());
        } else {
            doc.setComment("Update attachment " + attachmentReference.getName());
        }
        attachment.setContent(attachmentData);
        attachment.setFilename(attachmentReference.getName());
        attachment.setAuthor(getCurrentUser());
        attachment.setDoc(doc);
        doc.setAuthorReference(getContext().getUserReference());
        if (doc.isNew()) {
            doc.setCreatorReference(getContext().getUserReference());
        }
        doc.saveAttachmentContent(attachment, xcontext);
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#setAttachmentContent(String, String, byte[])
     */
    @Deprecated
    public void setAttachmentContent(String documentReference, String attachmentFilename, byte[] attachmentData)
        throws Exception
    {
        XWikiContext xcontext = getContext();
        XWikiDocument doc = xcontext.getWiki().getDocument(documentReference, xcontext);
        XWikiAttachment attachment = doc.getAttachment(attachmentFilename);
        if (attachment == null) {
            attachment = new XWikiAttachment();
            doc.getAttachmentList().add(attachment);
            doc.setComment("Add new attachment " + attachmentFilename);
        } else {
            doc.setComment("Update attachment " + attachmentFilename);
        }
        attachment.setContent(attachmentData);
        attachment.setFilename(attachmentFilename);
        attachment.setAuthor(getCurrentUser());
        attachment.setDoc(doc);
        doc.setAuthor(getCurrentUser());
        if (doc.isNew()) {
            doc.setCreator(getCurrentUser());
        }
        doc.saveAttachmentContent(attachment, xcontext);
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getAttachmentReferences(org.xwiki.model.reference.DocumentReference)
     */
    public List<AttachmentReference> getAttachmentReferences(DocumentReference documentReference) throws Exception
    {
        List<AttachmentReference> attachmentReferences = new ArrayList<AttachmentReference>();
        XWikiContext xcontext = getContext();
        DocumentReference resolvedReference = documentReference;
        List<XWikiAttachment> attachments =
            xcontext.getWiki().getDocument(resolvedReference, xcontext).getAttachmentList();
        for (XWikiAttachment attachment : attachments) {
            attachmentReferences.add(new AttachmentReference(attachment.getFilename(), resolvedReference));
        }
        return attachmentReferences;
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getAttachments(org.xwiki.bridge.DocumentName)
     */
    @Deprecated
    public List<org.xwiki.bridge.AttachmentName> getAttachments(org.xwiki.bridge.DocumentName documentName)
        throws Exception
    {
        List<org.xwiki.bridge.AttachmentName> results = new ArrayList<org.xwiki.bridge.AttachmentName>();
        DocumentReference documentReference;
        if (documentName == null) {
            documentReference = this.currentMixedDocumentReferenceResolver.resolve(getContext().getDoc().getFullName());
        } else {
            documentReference =
                new DocumentReference(documentName.getWiki(), documentName.getSpace(), documentName.getPage());
        }
        List<AttachmentReference> references = getAttachmentReferences(documentReference);
        for (AttachmentReference reference : references) {
            results.add(new org.xwiki.bridge.AttachmentName(new org.xwiki.bridge.DocumentName(reference
                .getDocumentReference().getWikiReference().getName(), reference.getDocumentReference()
                .getLastSpaceReference().getName(), reference.getDocumentReference().getName()), reference.getName()));
        }
        return results;
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getAttachmentVersion(AttachmentReference)
     */
    public String getAttachmentVersion(AttachmentReference attachmentReference) throws Exception
    {
        XWikiContext xcontext = getContext();
        XWikiDocument doc = xcontext.getWiki().getDocument(attachmentReference.getDocumentReference(), xcontext);
        XWikiAttachment attachment = doc.getAttachment(attachmentReference.getName());
        return attachment == null ? null : attachment.getVersion();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentAccessBridge#getDocumentURL(org.xwiki.model.reference.DocumentReference,
     *      java.lang.String, java.lang.String, java.lang.String)
     */
    public String getDocumentURL(DocumentReference documentReference, String action, String queryString, String anchor)
    {
        return getDocumentURL(documentReference, action, queryString, anchor, false);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentAccessBridge#getDocumentURL(org.xwiki.model.reference.DocumentReference,
     *      java.lang.String, java.lang.String, java.lang.String, boolean)
     */
    public String getDocumentURL(final DocumentReference documentReference, final String action,
        final String queryString, final String anchor, final boolean isFullURL)
    {
        if (documentReference == null) {
            return this.getDocumentURL(this.getContext().getDoc().getDocumentReference(), action, queryString, anchor,
                isFullURL);
        }
        if (isFullURL) {
            return this
                .getContext()
                .getURLFactory()
                .createExternalURL(documentReference.getLastSpaceReference().getName(), documentReference.getName(),
                    action, queryString, anchor, documentReference.getWikiReference().getName(), this.getContext())
                .toString();
        } else {
            return this.getContext().getWiki()
                .getURL(documentReference, action, queryString, anchor, this.getContext());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getURL(String, String, String, String)
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getAttachmentURL(String, String)
     */
    @Deprecated
    public String getAttachmentURL(String documentReference, String attachmentName)
    {
        XWikiContext xcontext = getContext();
        String attachmentURL;
        try {
            attachmentURL =
                xcontext.getWiki().getAttachmentURL(
                    documentReference == null ? xcontext.getDoc().getFullName() : documentReference, attachmentName,
                    xcontext);
        } catch (XWikiException e) {
            // This cannot happen. There's a bug in the definition of XWiki.getAttachmentURL: it says it can generate
            // an exception but in fact no exception is raised in the current implementation.
            throw new RuntimeException("Failed to get attachment URL", e);
        }
        return attachmentURL;
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getAttachmentURL(org.xwiki.model.reference.AttachmentReference , boolean)
     */
    public String getAttachmentURL(AttachmentReference attachmentReference, boolean isFullURL)
    {
        return getAttachmentURL(attachmentReference, null, isFullURL);
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getAttachmentURL(org.xwiki.model.reference.AttachmentReference, String, boolean)
     * @since 2.5RC1
     */
    public String getAttachmentURL(AttachmentReference attachmentReference, String queryString, boolean isFullURL)
    {
        String url;
        if (isFullURL) {
            XWikiContext xcontext = getContext();
            url =
                xcontext.getURLFactory().getURL(
                    xcontext.getURLFactory().createAttachmentURL(attachmentReference.getName(),
                        attachmentReference.getDocumentReference().getLastSpaceReference().getName(),
                        attachmentReference.getDocumentReference().getName(), "download", queryString,
                        attachmentReference.getDocumentReference().getWikiReference().getName(), xcontext), xcontext);
        } else {
            XWikiContext xcontext = getContext();
            String documentReference =
                this.entityReferenceSerializer.serialize(attachmentReference.getDocumentReference());
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

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getAttachmentURL(org.xwiki.bridge.AttachmentName, boolean)
     */
    @Deprecated
    public String getAttachmentURL(org.xwiki.bridge.AttachmentName attachmentName, boolean isFullURL)
    {
        return getAttachmentURL(new AttachmentReference(attachmentName.getFileName(), new DocumentReference(
            attachmentName.getDocumentName().getWiki(), attachmentName.getDocumentName().getWiki(), attachmentName
                .getDocumentName().getPage())), isFullURL);
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getAttachmentURLs(org.xwiki.model.reference.DocumentReference , boolean)
     */
    @Deprecated
    public List<String> getAttachmentURLs(DocumentReference documentReference, boolean isFullURL) throws Exception
    {
        List<String> urls = new ArrayList<String>();
        for (AttachmentReference attachmentReference : getAttachmentReferences(documentReference)) {
            urls.add(getAttachmentURL(attachmentReference, isFullURL));
        }
        return urls;
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getAttachmentURLs(org.xwiki.bridge.DocumentName, boolean)
     */
    @Deprecated
    public List<String> getAttachmentURLs(org.xwiki.bridge.DocumentName documentName, boolean isFullURL)
        throws Exception
    {
        return getAttachmentURLs(
            new DocumentReference(documentName.getWiki(), documentName.getSpace(), documentName.getPage()), isFullURL);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentAccessBridge#isDocumentViewable(org.xwiki.model.reference.DocumentReference)
     */
    public boolean isDocumentViewable(DocumentReference documentReference)
    {
        return hasRight(documentReference, "view");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentAccessBridge#isDocumentViewable(java.lang.String)
     */
    @Deprecated
    public boolean isDocumentViewable(String documentReference)
    {
        return hasRight(documentReference, "view");
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#isDocumentEditable(String)
     */
    @Deprecated
    public boolean isDocumentEditable(String documentReference)
    {
        return hasRight(documentReference, "edit");
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#isDocumentEditable(org.xwiki.model.reference.DocumentReference)
     */
    public boolean isDocumentEditable(DocumentReference documentReference)
    {
        return hasRight(documentReference, "edit");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentAccessBridge#hasProgrammingRights()
     */
    public boolean hasProgrammingRights()
    {
        XWikiContext xcontext = getContext();

        return xcontext.getWiki().getRightService().hasProgrammingRights(xcontext);
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getCurrentUser()
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#setCurrentUser(String)
     */
    public void setCurrentUser(String userName)
    {
        getContext().setUser(userName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getDefaultEncoding()
     */
    public String getDefaultEncoding()
    {
        return getContext().getWiki().getEncoding();
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#popDocumentFromContext(Map)
     */
    public void popDocumentFromContext(Map<String, Object> backupObjects)
    {
        XWikiDocument.restoreContext(backupObjects, getContext());
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#pushDocumentInContext(Map, String)
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#pushDocumentInContext(Map, DocumentReference)
     */
    public void pushDocumentInContext(Map<String, Object> backupObjects, DocumentReference documentReference)
        throws Exception
    {
        XWikiContext xcontext = getContext();

        // Backup current context state
        XWikiDocument.backupContext(backupObjects, xcontext);

        // Make sure to get the current XWikiContext after ExcutionContext clone
        xcontext = getContext();

        // Change context document
        xcontext.getWiki().getDocument(documentReference, xcontext).setAsContextDoc(xcontext);
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getCurrentWiki()
     */
    public String getCurrentWiki()
    {
        XWikiContext xcontext = getContext();
        return xcontext.getDatabase();
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
        return hasRight(this.entityReferenceSerializer.serialize(documentReference), right);
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
            hasRight =
                xcontext.getWiki().getRightService()
                    .hasAccessLevel(right, xcontext.getUser(), documentReference, xcontext);
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

}
