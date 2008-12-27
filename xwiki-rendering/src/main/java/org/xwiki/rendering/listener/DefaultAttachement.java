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
 * Default implementation of {@link Attachment}.
 * 
 * @version $Id$
 * @since 1.7.1
 */
public class DefaultAttachement implements Attachment
{
    /**
     * The name of the document containing the attachment.
     */
    private String documentName;

    /**
     * The name of the attachment.
     */
    private String attachmentName;

    /**
     * @param documentName the name of the document containing the attachment.
     * @param attachmentName the name of the attachment.
     */
    public DefaultAttachement(String documentName, String attachmentName)
    {
        this.documentName = documentName;
        this.attachmentName = attachmentName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Attachment#getDocumentName()
     */
    public String getDocumentName()
    {
        return this.documentName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Attachment#getAttachmentName()
     */
    public String getAttachmentName()
    {
        return this.attachmentName;
    }
}
