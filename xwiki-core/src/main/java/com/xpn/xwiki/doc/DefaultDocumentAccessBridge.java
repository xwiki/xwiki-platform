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

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.bridge.DocumentName;
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
     * @see DocumentAccessBridge#getDocumentName(String)
     */
    public DocumentName getDocumentName(String documentName)
    {
        XWikiDocument document = new XWikiDocument();
        document.setFullName(documentName, getContext());

        return new DocumentName(document.getWikiName(), document.getSpaceName(), document.getPageName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentAccessBridge#getCurrentDocumentName()
     */
    public DocumentName getCurrentDocumentName()
    {
        XWikiDocument currentDocument = getContext().getDoc();

        return currentDocument == null ? null : new DocumentName(currentDocument.getWikiName(), currentDocument
            .getSpaceName(), currentDocument.getPageName());
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
     * @see org.xwiki.bridge.DocumentAccessBridge#getDocumentContentForDefaultLanguage(java.lang.String)
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
        return xcontext.getWiki().getDocument(documentName, xcontext).getTranslatedContent(language, xcontext);
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#exists(String)
     */
    public boolean exists(String documentName)
    {
        boolean exists = false;
        try {
            XWikiContext xcontext = getContext();
            XWikiDocument doc = xcontext.getWiki().getDocument(documentName, xcontext);
            exists = (!doc.isNew());
        } catch (XWikiException e) {
            // If we failed to get the document we consider it doesn't exist.
            // Note that this can happen when the storage subsystem is down for example.
        }
        return exists;
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
        xcontext.getWiki().saveDocument(doc, editComment, isMinorEdit, xcontext);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentAccessBridge#getDocumentSyntaxId(java.lang.String)
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
        xcontext.getWiki().saveDocument(doc,
            String.format("Changed document syntax from [%s] to [%s].", oldSyntaxId, syntaxId), xcontext);
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getProperty(String, String, int, String)
     */
    public String getProperty(String documentName, String className, int objectNumber, String propertyName)
        throws Exception
    {
        try {
            XWikiContext xcontext = getContext();
            return ((BaseProperty) xcontext.getWiki().getDocument(documentName, xcontext).getObject(className,
                objectNumber).get(propertyName)).getValue()
                + "";
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getProperty(String, String, String)
     */
    public String getProperty(String documentName, String className, String propertyName) throws Exception
    {
        try {
            XWikiContext xcontext = getContext();

            return ((BaseProperty) xcontext.getWiki().getDocument(documentName, xcontext).getObject(className).get(
                propertyName)).getValue()
                + "";
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getProperty(String, String)
     */
    public String getProperty(String documentName, String propertyName) throws Exception
    {
        try {
            XWikiContext xcontext = getContext();
            return ((BaseProperty) xcontext.getWiki().getDocument(documentName, xcontext).getFirstObject(propertyName,
                xcontext).get(propertyName)).getValue()
                + "";
        } catch (Exception ex) {
            return null;
        }
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
            xcontext.getWiki().saveDocument(doc, xcontext);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getAttachmentContent(String, String)
     */
    public byte[] getAttachmentContent(String documentName, String attachmentName) throws Exception
    {
        XWikiContext xcontext = getContext();
        return xcontext.getWiki().getDocument(documentName, xcontext).getAttachment(attachmentName)
            .getContent(xcontext);
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
        attachment.setAuthor(xcontext.getUser());
        attachment.setDoc(doc);
        doc.saveAttachmentContent(attachment, xcontext);
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
     */
    public String getAttachmentURL(String documentName, String attachmentName)
    {
        XWikiContext xcontext = getContext();
        String attachmentURL;
        try {
            attachmentURL =
                xcontext.getWiki().getAttachmentURL(
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
     */
    public boolean isDocumentEditable(String documentName)
    {
        return hasRight(documentName, "edit");
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
}
