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

import org.apache.commons.lang.StringUtils;
import org.xwiki.model.AttachmentName;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.model.DocumentName;
import org.xwiki.model.DocumentNameFactory;
import org.xwiki.model.DocumentNameSerializer;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.context.Execution;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.PropertyClass;

/**
 * Exposes methods for accessing Document data. This is temporary until we remodel the Model classes and the Document
 * services. The implementation is inside the old core, and not in a component because it has dependencies on the old
 * core.
 * 
 * @version $Id$
 * @since 1.6M1
 */
@Component
public class DefaultDocumentAccessBridge implements DocumentAccessBridge
{
    /** Execution context handler, needed for accessing the XWikiContext. */
    @Requirement
    private Execution execution;

    @Requirement 
    private DocumentNameSerializer documentNameSerializer;

    @Requirement
    private DocumentNameFactory documentNameFactory;

    private XWikiContext getContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getDocument(String)
     */
    public DocumentModelBridge getDocument(String documentName) throws Exception
    {
        XWikiContext xcontext = getContext();
        return xcontext.getWiki().getDocument(documentName, xcontext);
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getDocument(DocumentName)
     * @since 2.2M1
     */
    public DocumentModelBridge getDocument(DocumentName documentName) throws Exception
    {
        XWikiContext xcontext = getContext();
        XWikiDocument doc = new XWikiDocument();
        doc.setDatabase(documentName.getWiki());
        doc.setSpace(documentName.getSpace());
        doc.setName(documentName.getPage());

        return xcontext.getWiki().getDocument(doc, xcontext);
    }

    /**
     * {@inheritDoc}
     *
     * @see DocumentAccessBridge#getDocument(DocumentName)
     * @deprecated replaced by {@link #getDocument(DocumentName)} since 2.2M1
     */
    @Deprecated
    public DocumentModelBridge getDocument(org.xwiki.bridge.DocumentName documentName) throws Exception
    {
        return getDocument(new DocumentName(documentName.getWiki(), documentName.getSpace(), documentName.getPage()));
    }

    /**
     * {@inheritDoc}
     *
     * @see DocumentAccessBridge#getModelDocumentName(String)
     * @since 2.2M1
     */
    public DocumentName getModelDocumentName(String documentName)
    {
        XWikiDocument document = new XWikiDocument();
        document.setFullName(documentName, getContext());

        return new DocumentName(document.getWikiName(), document.getSpaceName(), document.getPageName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getDocumentName(String)
     * @deprecated use {@link #getModelDocumentName(String)} since 2.2.M1
     */
    @Deprecated
    public org.xwiki.bridge.DocumentName getDocumentName(String documentName)
    {
        DocumentName docName = getModelDocumentName(documentName);
        return new org.xwiki.bridge.DocumentName(docName.getWiki(), docName.getSpace(), docName.getPage());
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.bridge.DocumentAccessBridge#getCurrentDocumentName()
     * @deprecated replaced by {@link org.xwiki.model.Model#getCurrentDocumentName()} since 2.2M1
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
     * @see DocumentAccessBridge#getDocumentContent(String)
     */
    public String getDocumentContent(String documentName) throws Exception
    {
        XWikiContext xcontext = getContext();
        return getDocumentContent(documentName, xcontext.getLanguage());
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getDocumentContentForDefaultLanguage(java.lang.String)
     */
    public String getDocumentContentForDefaultLanguage(String documentName) throws Exception
    {
        XWikiContext xcontext = getContext();
        return xcontext.getWiki().getDocument(documentName, xcontext).getContent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getDocumentContent(String, String)
     */
    public String getDocumentContent(String documentName, String language) throws Exception
    {
        XWikiContext xcontext = getContext();
        String originalRev = (String) xcontext.get("rev");
        try {
            xcontext.remove("rev");
            return xcontext.getWiki().getDocument(documentName, xcontext).getTranslatedContent(language, xcontext);
        } finally {
            if (originalRev != null) {
                xcontext.put("rev", originalRev);
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#exists(String)
     */
    public boolean exists(String documentName)
    {
        return getContext().getWiki().exists(documentName, getContext());
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#setDocumentContent(String, String, String, boolean)
     */
    public void setDocumentContent(String documentName, String content, String editComment, boolean isMinorEdit)
        throws Exception
    {
        XWikiContext xcontext = getContext();
        XWikiDocument doc = xcontext.getWiki().getDocument(documentName, xcontext);
        doc.setContent(content);
        saveDocument(doc, editComment, isMinorEdit);
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getDocumentSyntaxId(java.lang.String)
     */
    public String getDocumentSyntaxId(String documentName) throws Exception
    {
        XWikiContext xcontext = getContext();
        XWikiDocument doc = xcontext.getWiki().getDocument(documentName, xcontext);

        return doc.getSyntaxId();
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#setDocumentSyntaxId(String, String)
     */
    public void setDocumentSyntaxId(String documentName, String syntaxId) throws Exception
    {
        XWikiContext xcontext = getContext();
        XWikiDocument doc = xcontext.getWiki().getDocument(documentName, xcontext);
        String oldSyntaxId = doc.getSyntaxId();
        doc.setSyntaxId(syntaxId);
        saveDocument(doc, String.format("Changed document syntax from [%s] to [%s].", oldSyntaxId, syntaxId), true);
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getProperty(String, String, int, String)
     */
    public Object getProperty(String documentName, String className, int objectNumber, String propertyName)
    {
        try {
            XWikiContext xcontext = getContext();
            return ((BaseProperty) xcontext.getWiki().getDocument(documentName, xcontext).getObject(className,
                objectNumber).get(propertyName)).getValue();
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getProperty(String, String, String)
     */
    public Object getProperty(String documentName, String className, String propertyName)
    {
        Object value;
        
        try {
            XWikiContext xcontext = getContext();
            XWikiDocument doc = xcontext.getWiki().getDocument(documentName, xcontext);
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
     * @see DocumentAccessBridge#getProperty(String, String)
     */
    public Object getProperty(String documentName, String propertyName)
    {
        try {
            XWikiContext xcontext = getContext();
            return ((BaseProperty) xcontext.getWiki().getDocument(documentName, xcontext).getFirstObject(propertyName,
                xcontext).get(propertyName)).getValue();
        } catch (Exception ex) {
            return null;
        }
    }

    public List<Object> getProperties(String documentName, String className)
    {
        List<Object> result;
        try {
            XWikiContext xcontext = getContext();
            result = new ArrayList<Object>(
                xcontext.getWiki().getDocument(documentName, xcontext).getObject(className).getFieldList());
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
     */
    public void setProperty(String documentName, String className, String propertyName, Object propertyValue)
        throws Exception
    {
        XWikiContext xcontext = getContext();
        XWikiDocument doc = xcontext.getWiki().getDocument(documentName, xcontext);
        BaseObject obj = doc.getObject(className, true, xcontext);
        if (obj != null) {
            obj.set(propertyName, propertyValue, xcontext);
            saveDocument(doc, String.format("Property [%s] set.", propertyName), true);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getAttachmentContent(String, String)
     * @deprecated use {@link #getAttachmentContent(AttachmentName)} instead
     */
    @Deprecated
    public byte[] getAttachmentContent(String documentName, String attachmentName) throws Exception
    {
        XWikiContext xcontext = getContext();
        return xcontext.getWiki().getDocument(documentName, xcontext).getAttachment(attachmentName).getContent(xcontext);
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getAttachmentContent(AttachmentName)
     * @since 2.2M1
     */
    public InputStream getAttachmentContent(AttachmentName attachmentName) throws Exception
    {
        XWikiContext xcontext = getContext();
        XWikiDocument attachmentDocument = xcontext.getWiki().getDocument(
            this.documentNameSerializer.serialize(attachmentName.getDocumentName()), xcontext);
        return new ByteArrayInputStream(
            attachmentDocument.getAttachment(attachmentName.getFileName()).getContent(xcontext));
    }

    /**
     * {@inheritDoc}
     *
     * @see DocumentAccessBridge#getAttachmentContent(org.xwiki.bridge.AttachmentName)
     * @deprecated replaced by {@link #getAttachmentContent(AttachmentName)} since 2.2M1
     */
    @Deprecated
    public InputStream getAttachmentContent(org.xwiki.bridge.AttachmentName attachmentName) throws Exception
    {
        return getAttachmentContent(new AttachmentName(
            new DocumentName(attachmentName.getDocumentName().getWiki(), attachmentName.getDocumentName().getSpace(),
                attachmentName.getDocumentName().getPage()), attachmentName.getFileName()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#setAttachmentContent(String, String, byte[])
     */
    public void setAttachmentContent(String documentName, String attachmentName, byte[] attachmentData)
        throws Exception
    {
        XWikiContext xcontext = getContext();
        XWikiDocument doc = xcontext.getWiki().getDocument(documentName, xcontext);
        XWikiAttachment attachment = doc.getAttachment(attachmentName);
        if (attachment == null) {
            attachment = new XWikiAttachment();
            doc.getAttachmentList().add(attachment);
            doc.setComment("Add new attachment " + attachmentName);
        } else {
            doc.setComment("Update attachment " + attachmentName);
        }
        attachment.setContent(attachmentData);
        attachment.setFilename(attachmentName);
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
     * @see DocumentAccessBridge#getAttachmentNames(org.xwiki.model.DocumentName)
     * @since 2.2M1
     */
    public List<AttachmentName> getAttachmentNames(DocumentName documentName) throws Exception
    {
        List<AttachmentName> attachmentNames = new ArrayList<AttachmentName>();
        XWikiContext xcontext = getContext();
        DocumentName resolvedName = documentName;
        if (documentName == null) {
            resolvedName = this.documentNameFactory.createDocumentName(xcontext.getDoc().getFullName());
        }
        List<XWikiAttachment> attachments = xcontext.getWiki().getDocument(
            this.documentNameSerializer.serialize(resolvedName), xcontext).getAttachmentList();
        for (XWikiAttachment attachment : attachments) {
            attachmentNames.add(new AttachmentName(resolvedName, attachment.getFilename()));
        }
        return attachmentNames;
    }

    /**
     * {@inheritDoc}
     *
     * @see DocumentAccessBridge#getAttachments(DocumentName)
     * @deprecated replaced by {@link #getAttachmentNames(DocumentName)} since 2.2M1
     */
    @Deprecated
    public List<org.xwiki.bridge.AttachmentName> getAttachments(org.xwiki.bridge.DocumentName documentName)
        throws Exception
    {
        List<org.xwiki.bridge.AttachmentName> results = new ArrayList<org.xwiki.bridge.AttachmentName>();
        List<AttachmentName> names = getAttachmentNames(new DocumentName(documentName.getWiki(),
            documentName.getSpace(), documentName.getPage()));
        for (AttachmentName name : names) {
            results.add(new org.xwiki.bridge.AttachmentName(new org.xwiki.bridge.DocumentName(
                name.getDocumentName().getWiki(), name.getDocumentName().getSpace(), name.getDocumentName().getPage()),
                name.getFileName()));
        }
        return results;
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getURL(String, String, String, String)
     */
    public String getURL(String documentName, String action, String queryString, String anchor)
    {
        XWikiContext xcontext = getContext();

        // If the document name is empty then use the current document
        String computedDocumentName = documentName;
        if (StringUtils.isEmpty(documentName)) {
            computedDocumentName = xcontext.getDoc().getFullName();
        }

        return xcontext.getWiki().getURL(computedDocumentName, action, queryString, anchor, xcontext);
    }


    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getAttachmentURL(String, String)
     * @deprecated use {@link #getAttachmentURL(AttachmentName, boolean)} instead
     */
    @Deprecated
    public String getAttachmentURL(String documentName, String attachmentName)
    {
        XWikiContext xcontext = getContext();
        String attachmentURL;
        try {
            attachmentURL = xcontext.getWiki().getAttachmentURL(
                documentName == null ? xcontext.getDoc().getFullName() : documentName, attachmentName, xcontext);
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
     * @see DocumentAccessBridge#getAttachmentURL(AttachmentName, boolean)
     * @since 2.2M1
     */
    public String getAttachmentURL(AttachmentName attachmentName, boolean isFullURL)
    {
        String url;
        if (isFullURL) {
            XWikiContext xcontext = getContext();
            url = xcontext.getURLFactory().createAttachmentURL(attachmentName.getFileName(), 
                attachmentName.getDocumentName().getSpace(), attachmentName.getDocumentName().getPage(),
                "download", null, attachmentName.getDocumentName().getWiki(), xcontext).toString();
        } else {
            url = getAttachmentURL(this.documentNameSerializer.serialize(attachmentName.getDocumentName()), 
                attachmentName.getFileName()); 
        }
        return url; 
    }

    /**
     * {@inheritDoc}
     *
     * @see DocumentAccessBridge#getAttachmentURL(org.xwiki.bridge.AttachmentName, boolean)
     * @deprecated replaced by {@link #getAttachmentURL(AttachmentName, boolean)} since 2.2M1
     */
    @Deprecated
    public String getAttachmentURL(org.xwiki.bridge.AttachmentName attachmentName, boolean isFullURL)
    {
        return getAttachmentURL(new AttachmentName(new DocumentName(attachmentName.getDocumentName().getWiki(),
            attachmentName.getDocumentName().getWiki(), attachmentName.getDocumentName().getPage()),
            attachmentName.getFileName()), isFullURL);
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getAttachmentURLs(DocumentName, boolean)
     * @deprecated use {@link #getAttachmentNames(DocumentName)} instead
     * @since 2.2M1
     */
    @Deprecated
    public List<String> getAttachmentURLs(DocumentName documentName, boolean isFullURL) throws Exception
    {
        List<String> urls = new ArrayList<String>();
        for (AttachmentName attachmentName : getAttachmentNames(documentName)) {
            urls.add(getAttachmentURL(attachmentName, isFullURL));
        }
        return urls;
    }

    /**
     * {@inheritDoc}
     *
     * @see DocumentAccessBridge#getAttachmentURLs(org.xwiki.bridge.DocumentName, boolean)
     * @deprecated use {@link #getAttachmentNames(DocumentName)} instead
     */
    @Deprecated
    public List<String> getAttachmentURLs(org.xwiki.bridge.DocumentName documentName, boolean isFullURL)
        throws Exception
    {
        return getAttachmentURLs(new DocumentName(documentName.getWiki(), documentName.getSpace(),
            documentName.getPage()), isFullURL);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentAccessBridge#isDocumentViewable(java.lang.String)
     */
    public boolean isDocumentViewable(String documentName)
    {
        return hasRight(documentName, "view");
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#isDocumentEditable(String)
     * @deprecated use {@link #isDocumentEditable(DocumentName)} instead
     */
    @Deprecated
    public boolean isDocumentEditable(String documentName)
    {
        return hasRight(documentName, "edit");
    }

    /**
     * {@inheritDoc}
     *
     * @see DocumentAccessBridge#isDocumentEditable(DocumentName)
     * @since 2.2M1
     */
    public boolean isDocumentEditable(DocumentName documentName)
    {
        return hasRight(this.documentNameSerializer.serialize(documentName), "edit");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentAccessBridge#hasProgrammingRights()
     */
    public boolean hasProgrammingRights()
    {
        XWikiContext xcontext = getContext();

        return xcontext.getWiki().getRightService().hasProgrammingRights(xcontext.getDoc(), xcontext);
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getCurrentUser()
     */
    public String getCurrentUser()
    {
        return getContext().getUser();
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
    public void pushDocumentInContext(Map<String, Object> backupObjects, String documentName) throws Exception
    {
        XWikiContext xcontext = getContext();
        XWikiDocument.backupContext(backupObjects, xcontext);
        xcontext.getWiki().getDocument(documentName, xcontext).setAsContextDoc(xcontext);
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
     * @param documentName The name of the document.
     * @param right Access right requested.
     * @return True if the current user has the given access right, false otherwise.
     */
    private boolean hasRight(String documentName, String right)
    {
        boolean hasRight = false;
        XWikiContext xcontext = getContext();
        try {
            hasRight =
                xcontext.getWiki().getRightService().hasAccessLevel(right, xcontext.getUser(), documentName, xcontext);
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
        doc.setAuthor(getCurrentUser());
        if (doc.isNew()) {
            doc.setCreator(getCurrentUser());
        }
        getContext().getWiki().saveDocument(doc, comment, isMinorEdit, getContext());
    }
}
