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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.bridge.DocumentName;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;

/**
 * {@link DocumentAccessBridge} mock implementation used for tests.
 * 
 * @version $Id: $
 * @since 2.0M1
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
        componentDescriptor.setRoleHint("default");
        componentDescriptor.setImplementation(MockDocumentAccessBridge.class);
        return componentDescriptor;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getURL(String arg0, String arg1, String arg2, String arg3)
    {
        return "http://localhost/charts";
    }

    /**
     * {@inheritDoc}
     */
    public boolean exists(String arg0)
    {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public byte[] getAttachmentContent(String arg0, String arg1) throws Exception
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getAttachmentURL(String arg0, String arg1)
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public DocumentName getCurrentDocumentName()
    {
        return new DocumentName(null, "Test", "Test");
    }

    /**
     * {@inheritDoc}
     */
    public String getCurrentUser()
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getDefaultEncoding()
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public DocumentModelBridge getDocument(String arg0) throws Exception
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getDocumentContent(String arg0) throws Exception
    {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("wiki.txt");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int data = 0;
        while (-1 != (data = is.read())) {
            baos.write(data);
        }
        return baos.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getDocumentContent(String arg0, String arg1) throws Exception
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getDocumentContentForDefaultLanguage(String arg0) throws Exception
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public DocumentName getDocumentName(String arg0)
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getDocumentSyntaxId(String arg0) throws Exception
    {
        return "xwiki/2.0";
    }

    /**
     * {@inheritDoc}
     */
    public String getProperty(String arg0, String arg1) throws Exception
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getProperty(String arg0, String arg1, String arg2) throws Exception
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getProperty(String arg0, String arg1, int arg2, String arg3) throws Exception
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getPropertyType(String arg0, String arg1) throws Exception
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasProgrammingRights()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDocumentEditable(String arg0)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDocumentViewable(String arg0)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isPropertyCustomMapped(String arg0, String arg1) throws Exception
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void popDocumentFromContext(Map<String, Object> arg0)
    {
    }

    /**
     * {@inheritDoc}
     */
    public void pushDocumentInContext(Map<String, Object> arg0, String arg1) throws Exception
    {
    }

    /**
     * {@inheritDoc}
     */
    public void setAttachmentContent(String arg0, String arg1, byte[] arg2) throws Exception
    {
    }

    /**
     * {@inheritDoc}
     */
    public void setDocumentContent(String arg0, String arg1, String arg2, boolean arg3) throws Exception
    {
    }

    /**
     * {@inheritDoc}
     */
    public void setDocumentSyntaxId(String arg0, String arg1) throws Exception
    {
    }

    /**
     * {@inheritDoc}
     */
    public void setProperty(String arg0, String arg1, String arg2, Object arg3) throws Exception
    {
    }
}
