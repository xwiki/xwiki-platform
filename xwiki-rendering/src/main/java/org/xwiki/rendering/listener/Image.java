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
package org.xwiki.rendering.listener;

/**
 * Represents an image (location, attachment name).
 * 
 * @version $Id$
 * @since 1.7M3
 */
public class Image
{
    /**
     * Name of the document containing the image attachment.
     */
    private String documentName;
    
    private String attachmentName;

    public Image(String documentName, String attachmentName)
    {
        setDocumentName(documentName);
        setAttachmentName(attachmentName);
    }
    
    public void setDocumentName(String documentName)
    {
        this.documentName = documentName;
    }
    
    public String getDocumentName() 
    {
        return this.documentName;
    }
    
    public void setAttachmentName(String attachmentName)
    {
        this.attachmentName = attachmentName;
    }
    
    public String getAttachmentName()
    {
        return this.attachmentName;
    }

    @Override
    public String toString()
    {
        return "document = [" + getDocumentName() + "], attachment = [" + getAttachmentName() + "]";
    }
}
