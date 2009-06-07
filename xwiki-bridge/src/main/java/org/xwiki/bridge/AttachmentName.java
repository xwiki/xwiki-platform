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
package org.xwiki.bridge;

/**
 * Represents an Attachment name (document name and file name). An attachment is always attached to a document.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public class AttachmentName extends DocumentName
{
    /**
     * @see #getFileName(String) 
     */
    private String fileName;
    
    /**
     * @param wiki the wiki to which the attachment's document belongs to (eg "xwiki")
     * @param space the space to which the attachment's document belongs to (eg "Main")
     * @param page the page to which the attachment's document belongs to (eg "WebHome")
     * @param fileName the name of the file attached to the document
     */
    public AttachmentName(String wiki, String space, String page, String fileName)
    {
        super(wiki, space, page);
        this.fileName = fileName;
    }
    
    /**
     * @param documentName the attachment's document (ie the document to which the filename is attached to)
     * @param fileName the name of the file attached to the document
     */
    public AttachmentName(DocumentName documentName, String fileName)
    {
        this(documentName.getWiki(), documentName.getSpace(), documentName.getPage(), fileName);
    }

    /**
     * @param fileName the file name of the attachment in the document's page.
     */
    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }
    
    /**
     * @return the file name of the attachment in the document's page.
     */
    public String getFileName()
    {
        return this.fileName;
    }
}
