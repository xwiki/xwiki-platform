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
package org.xwiki.rendering.internal;

import java.util.List;
import java.util.Map;

import org.xwiki.bridge.AttachmentName;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.bridge.DocumentName;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;

/**
 * Mock {@link DocumentAccessBridge} implementation used for testing, since we don't want to pull any dependency on the
 * Model/Skin/etc for the Rendering module's unit tests.
 * 
 * @version $Id$
 * @since 18RC2
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
     */
    public String getDocumentContent(String documentName) throws Exception
    {
        return "Some content";
    }

    /**
     * {@inheritDoc}
     */
    public String getDocumentContent(String documentName, String language) throws Exception
    {
        return "Some translated content";
    }

    /**
     * {@inheritDoc}
     */
    public boolean exists(String documentName)
    {
        return (documentName.equals("XWiki.Admin") || documentName.equals("XWiki.ExistingUserWithoutAvatar"));
    }

    /**
     * {@inheritDoc}
     */
    public String getURL(String documentName, String action, String queryString, String anchor)
    {
        String result = "/xwiki/bin/view/" + (documentName == null ? "currentdoc" : documentName.replace(".", "/"));
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
     */
    public String getAttachmentURL(String documentName, String attachmentName)
    {
        return "/xwiki/bin/download/" + (documentName == null ? "currentdoc" : documentName.replace(".", "/")) + "/"
            + attachmentName;
    }

    /**
     * {@inheritDoc}
     */
    public Object getProperty(String documentName, String className, int objectNumber, String propertyName)
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    public Object getProperty(String documentName, String className, String propertyName)
    {
        return documentName.equals("XWiki.Admin") && className.equals("XWiki.XWikiUsers")
            && propertyName.equals("avatar") ? "mockAvatar.png" : null;
    }

    /**
     * {@inheritDoc}
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
     */
    public String getPropertyType(String className, String propertyName) throws Exception
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
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
     */
    public byte[] getAttachmentContent(String documentName, String attachmentName) throws Exception
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasProgrammingRights()
    {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public String getDocumentContentForDefaultLanguage(String arg0) throws Exception
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDocumentViewable(String arg0)
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    public String getCurrentUser()
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    public String getDefaultEncoding()
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    public String getDocumentSyntaxId(String arg0) throws Exception
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDocumentEditable(String arg0)
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    public void setAttachmentContent(String arg0, String arg1, byte[] arg2) throws Exception
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    public void setDocumentContent(String arg0, String arg1, String arg2, boolean arg3) throws Exception
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    public void setDocumentSyntaxId(String arg0, String arg1) throws Exception
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    public DocumentModelBridge getDocument(String documentName) throws Exception
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentAccessBridge#getDocument(org.xwiki.bridge.DocumentName)
     */
    public DocumentModelBridge getDocument(DocumentName documentName) throws Exception
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    public DocumentName getDocumentName(String documentName)
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    public DocumentName getCurrentDocumentName()
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    public void popDocumentFromContext(Map<String, Object> backupObjects)
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    public void pushDocumentInContext(Map<String, Object> backupObjects, String documentName) throws Exception
    {
        throw new RuntimeException("Not implemented");
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getAttachmentURL(AttachmentName, boolean)
     */
    public String getAttachmentURL(AttachmentName attachmentName, boolean isFullURL)
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getAttachmentURLs(DocumentName, boolean)
     */
    public List<String> getAttachmentURLs(DocumentName documentName, boolean isFullURL) throws Exception
    {
        throw new RuntimeException("Not implemented");
    }
}
