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
package org.xwiki.rendering.scaffolding;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxType;

/**
 * Mock {@link DocumentAccessBridge} implementation used for testing, since we don't want to pull any dependency on the
 * Model/Skin/etc for the Rendering module's unit tests.
 * 
 * @version $Id$
 * @since 1.6M1
 */
public class MockDocumentAccessBridge implements DocumentAccessBridge
{
    /**
     * Create and return a descriptor for this component.
     * 
     * @return the descriptor of the component.
     */
    public static ComponentDescriptor<DocumentAccessBridge> getComponentDescriptor()
    {
        DefaultComponentDescriptor<DocumentAccessBridge> componentDescriptor =
            new DefaultComponentDescriptor<DocumentAccessBridge>();

        componentDescriptor.setRole(DocumentAccessBridge.class);
        componentDescriptor.setImplementation(MockDocumentAccessBridge.class);

        return componentDescriptor;
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getDocumentContent(String)
     */
    public String getDocumentContent(String documentName) throws Exception
    {
        return "Some content";
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getDocumentContent(String, String)
     */
    public String getDocumentContent(String documentName, String language) throws Exception
    {
        return "Some translated content";
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentAccessBridge#getDocumentContent(org.xwiki.model.reference.DocumentReference,
     *      java.lang.String)
     */
    public String getDocumentContent(DocumentReference documentReference, String language) throws Exception
    {
        return "Some content";
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getDocumentContentForDefaultLanguage(String)
     */
    public String getDocumentContentForDefaultLanguage(String documentName) throws Exception
    {
        return "Some content";
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentAccessBridge#getDocumentContentForDefaultLanguage(org.xwiki.model.reference.DocumentReference)
     */
    public String getDocumentContentForDefaultLanguage(DocumentReference documentReference) throws Exception
    {
        return "Some content";
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#exists(String)
     */
    public boolean exists(String documentName)
    {
        return documentName.equals("Space.ExistingPage");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentAccessBridge#exists(org.xwiki.model.reference.DocumentReference)
     */
    public boolean exists(DocumentReference documentReference)
    {
        return documentReference.getLastSpaceReference().getName().equals("Space")
            && documentReference.getName().equals("ExistingPage");
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getURL(String, String, String, String)
     */
    public String getURL(String documentName, String action, String queryString, String anchor)
    {
        String result =
            "/xwiki/bin/view/" + (StringUtils.isBlank(documentName) ? "currentdoc" : documentName.replace(".", "/"));
        if (anchor != null) {
            result = result + "#" + anchor;
        }
        if (queryString != null) {
            result = result + "?" + queryString;
        }
        return result;
    }

    public String getDocumentURL(DocumentReference documentReference, String action, String queryString, String anchor)
    {
        String result =
            "/xwiki/bin/view/"
                + (documentReference == null ? "currentdoc" : documentReference.getLastSpaceReference().getName() + "/"
                    + documentReference.getName());
        if (anchor != null) {
            result = result + "#" + anchor;
        }
        if (queryString != null) {
            result = result + "?" + queryString;
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getAttachmentURL(String, String)
     */
    public String getAttachmentURL(String documentName, String attachmentName)
    {
        return "/xwiki/bin/download/" + (documentName == null ? "currentdoc" : documentName.replace(".", "/")) + "/"
            + attachmentName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getProperty(String, String, int, String)
     */
    public Object getProperty(String documentName, String className, int objectNumber, String propertyName)
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getProperty(String, String, String)
     * @deprecated since 2.2M1 use {@link #getProperty(DocumentReference, DocumentReference, String)} instead
     */
    public Object getProperty(String documentName, String className, String propertyName)
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getProperty(String, String, String)
     * @since 2.2M1
     */
    public Object getProperty(DocumentReference documentName, DocumentReference classReference, String propertyName)
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getProperty(String, String)
     */
    public Object getProperty(String documentName, String propertyName)
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getProperties(String, String)
     */
    public List<Object> getProperties(String documentName, String className)
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getPropertyType(String, String)
     */
    public String getPropertyType(String className, String propertyName) throws Exception
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#isPropertyCustomMapped(String, String)
     */
    public boolean isPropertyCustomMapped(String className, String propertyName) throws Exception
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    public void setProperty(String documentName, String className, String propertyName, Object propertyValue)
        throws Exception
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @since 2.2M1
     */
    public InputStream getAttachmentContent(AttachmentReference attachmentReference) throws Exception
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @since 2.2M1
     */
    public List<AttachmentReference> getAttachmentReferences(DocumentReference documentReference) throws Exception
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getAttachmentContent(String, String)
     */
    public byte[] getAttachmentContent(String documentName, String attachmentName) throws Exception
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#setAttachmentContent(String, String, byte[])
     */
    public void setAttachmentContent(String documentName, String AttachmentName, byte[] attachmentData)
        throws Exception
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentAccessBridge#setAttachmentContent(org.xwiki.model.reference.AttachmentReference,
     *      byte[])
     */
    public void setAttachmentContent(AttachmentReference attachmentReference, byte[] attachmentData) throws Exception
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#setDocumentContent(String, String, String, boolean)
     */
    public void setDocumentContent(String documentName, String content, String editComment, boolean isMinorEdit)
        throws Exception
    {
        throw new RuntimeException("Not implemented");
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
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentAccessBridge#getDocumentSyntaxId(java.lang.String)
     */
    public String getDocumentSyntaxId(String documentName) throws Exception
    {
        return new Syntax(SyntaxType.XWIKI, "2.0").toIdString();
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#setDocumentSyntaxId(String, String)
     */
    public void setDocumentSyntaxId(String documentName, String syntaxId) throws Exception
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentAccessBridge#setDocumentSyntaxId(org.xwiki.model.reference.DocumentReference,
     *      java.lang.String)
     */
    public void setDocumentSyntaxId(DocumentReference documentReference, String syntaxId) throws Exception
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#isDocumentViewable(String)
     */
    public boolean isDocumentViewable(String documentName)
    {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentAccessBridge#isDocumentViewable(org.xwiki.model.reference.DocumentReference)
     */
    public boolean isDocumentViewable(DocumentReference documentReference)
    {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#isDocumentEditable(String)
     */
    public boolean isDocumentEditable(String documentName)
    {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#isDocumentEditable(org.xwiki.model.reference.DocumentReference)
     * @since 2.2M1
     */
    public boolean isDocumentEditable(DocumentReference documentReference)
    {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#hasProgrammingRights()
     */
    public boolean hasProgrammingRights()
    {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getCurrentUser()
     */
    public String getCurrentUser()
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getDefaultEncoding()
     */
    public String getDefaultEncoding()
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getDocument(String)
     * @deprecated use {@link #getDocument(org.xwiki.model.reference.DocumentReference)} instead
     */
    @Deprecated
    public DocumentModelBridge getDocument(String documentName) throws Exception
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentAccessBridge#getDocument(org.xwiki.model.reference.DocumentReference)
     * @since 2.2M1
     */
    public DocumentModelBridge getDocument(DocumentReference documentReference) throws Exception
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#popDocumentFromContext(Map)
     */
    public void popDocumentFromContext(Map<String, Object> backupObjects)
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#pushDocumentInContext(Map, String)
     */
    public void pushDocumentInContext(Map<String, Object> backupObjects, String documentName) throws Exception
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentAccessBridge#pushDocumentInContext(java.util.Map,
     *      org.xwiki.model.reference.DocumentReference)
     */
    public void pushDocumentInContext(Map<String, Object> backupObjects, DocumentReference documentReference)
        throws Exception
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getAttachmentURL(org.xwiki.model.reference.AttachmentReference , boolean)
     * @since 2.2M1
     */
    public String getAttachmentURL(AttachmentReference attachmentReference, boolean isFullURL)
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getAttachmentURLs(org.xwiki.model.reference.DocumentReference , boolean)
     * @since 2.2M1
     */
    public List<String> getAttachmentURLs(DocumentReference documentReference, boolean isFullURL) throws Exception
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getCurrentWiki()
     */
    public String getCurrentWiki()
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentAccessBridge#getCurrentDocumentName()
     * @deprecated
     */
    public org.xwiki.bridge.DocumentName getCurrentDocumentName()
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentAccessBridge#getCurrentDocumentReference()
     * @since 2.2M1
     */
    public DocumentReference getCurrentDocumentReference()
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @deprecated
     */
    public DocumentModelBridge getDocument(org.xwiki.bridge.DocumentName documentName) throws Exception
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @deprecated
     */
    public org.xwiki.bridge.DocumentName getDocumentName(String documentName)
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @deprecated
     */
    public InputStream getAttachmentContent(org.xwiki.bridge.AttachmentName attachmentName) throws Exception
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @deprecated
     */
    public List<org.xwiki.bridge.AttachmentName> getAttachments(org.xwiki.bridge.DocumentName documentName)
        throws Exception
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @deprecated
     */
    public String getAttachmentURL(org.xwiki.bridge.AttachmentName attachmentName, boolean isFullURL)
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @deprecated
     */
    public List<String> getAttachmentURLs(org.xwiki.bridge.DocumentName documentName, boolean isFullURL)
        throws Exception
    {
        throw new RuntimeException("Not implemented");
    }
}
