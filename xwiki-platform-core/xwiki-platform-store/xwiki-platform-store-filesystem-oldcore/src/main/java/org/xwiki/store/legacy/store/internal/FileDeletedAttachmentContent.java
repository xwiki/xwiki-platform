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
package org.xwiki.store.legacy.store.internal;

import java.io.IOException;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DeletedAttachmentContent;
import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * Filesystem based implementation of DeletedAttachmentContent.
 *
 * @version $Id$
 * @since 9.9RC1
 */
public class FileDeletedAttachmentContent implements DeletedAttachmentContent
{
    private XWikiAttachment attachment;

    /**
     * @param attachment the deleted attachment
     */
    public FileDeletedAttachmentContent(XWikiAttachment attachment)
    {
        this.attachment = attachment;
    }

    @Override
    public String getContentAsString() throws IOException, XWikiException
    {
        return this.attachment.toXML();
    }

    @Override
    public XWikiAttachment getXWikiAttachment(XWikiAttachment returnAttachment) throws XWikiException, IOException
    {
        if (returnAttachment != null) {
            returnAttachment.apply(this.attachment);

            return returnAttachment;
        }

        return this.attachment;
    }
}
