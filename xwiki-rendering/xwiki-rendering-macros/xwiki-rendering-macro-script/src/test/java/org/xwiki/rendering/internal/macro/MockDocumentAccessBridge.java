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
package org.xwiki.rendering.internal.macro;

import java.util.Map;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.bridge.DocumentName;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;

/**
 * Mock {@link DocumentAccessBridge} implementation used for testing, since we don't want to pull any dependency on the
 * Model/Skin/etc for the Rendering module's unit tests.
 * 
 * @version $Id: MockDocumentAccessBridge.java 21022 2009-06-09 11:56:25Z vmassol $
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
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getDocumentContent(String, String)
     */
    public String getDocumentContent(String documentName, String language) throws Exception
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getDocumentContentForDefaultLanguage(String)
     */
    public String getDocumentContentForDefaultLanguage(String documentName) throws Exception
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#exists(String)
     */
    public boolean exists(String documentName)
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getURL(String, String, String, String)
     */
    public String getURL(String documentName, String action, String queryString, String anchor)
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getAttachmentURL(String, String)
     */
    public String getAttachmentURL(String documentName, String attachmentName)
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getProperty(String, String, int, String)
     */
    public String getProperty(String documentName, String className, int objectNumber, String propertyName)
        throws Exception
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getProperty(String, String, String)
     */
    public String getProperty(String documentName, String className, String propertyName) throws Exception
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getProperty(String, String)
     */
    public String getProperty(String documentName, String propertyName) throws Exception
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
     * @see org.xwiki.bridge.DocumentAccessBridge#getDocumentSyntaxId(java.lang.String)
     */
    public String getDocumentSyntaxId(String documentName) throws Exception
    {
        throw new RuntimeException("Not implemented");
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
     * @see DocumentAccessBridge#isDocumentViewable(String)
     */
    public boolean isDocumentViewable(String documentName)
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#isDocumentEditable(String)
     */
    public boolean isDocumentEditable(String documentName)
    {
        throw new RuntimeException("Not implemented");
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
     */
    public DocumentModelBridge getDocument(String documentName) throws Exception
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getDocumentName(String)
     */
    public DocumentName getDocumentName(String documentName)
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentAccessBridge#getCurrentDocumentName()
     */
    public DocumentName getCurrentDocumentName()
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
}
