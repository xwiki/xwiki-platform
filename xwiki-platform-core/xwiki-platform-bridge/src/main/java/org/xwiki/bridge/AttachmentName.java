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
 * @deprecated use {@link org.xwiki.model.reference.AttachmentReference} instead since 2.2M1
 */
@Deprecated
public class AttachmentName
{
    /**
     * @see #getDocumentName()
     */
    private DocumentName documentName;
    
    /**
     * @see #getFileName()  
     */
    private String fileName;

    /**
     * @param documentName the attachment's document (ie the document to which the filename is attached to)
     * @param fileName the name of the file attached to the document
     */
    public AttachmentName(DocumentName documentName, String fileName)
    {
        setDocumentName(documentName);
        setFileName(fileName);
    }

    /**
     * @param wiki the wiki to which the attachment's document belongs to (eg "xwiki")
     * @param space the space to which the attachment's document belongs to (eg "Main")
     * @param page the page to which the attachment's document belongs to (eg "WebHome")
     * @param fileName the name of the file attached to the document
     */
    public AttachmentName(String wiki, String space, String page, String fileName)
    {
        this(new DocumentName(wiki, space, page), fileName);
    }

    /**
     * @param documentName the name of the document to which the attachment filename is attached to
     */
    public void setDocumentName(DocumentName documentName)
    {
        this.documentName = documentName;
    }
    
    /**
     * @return the name of the document to which the attachment filename is attached to
     */
    public DocumentName getDocumentName()
    {
        return this.documentName;
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
    
    @Override
    public String toString()
    {
        return "documentName = [" + getDocumentName() + "], fileName = [" + getFileName() + "]";
    }

    @Override
    public boolean equals(Object obj)
    {
        boolean equals = false;

        if (obj == this) {
            equals = true;
        } else if (obj instanceof AttachmentName) {
            AttachmentName attachmentName = (AttachmentName) obj;

            equals = (attachmentName.getDocumentName() == null ? getDocumentName() == null 
                : attachmentName.getDocumentName().equals(getDocumentName()))
                && (attachmentName.getFileName() == null ? getFileName() == null 
                : attachmentName.getFileName().equals(getFileName()));
        }

        return equals;
    }

    @Override
    public int hashCode()
    {
        return toString().hashCode();
    }    
}
