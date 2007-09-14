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
 *
 */

package com.xpn.xwiki.doc;

public class XWikiAttachmentContent {

    private XWikiAttachment attachment;
    private byte[] content;

    private boolean isContentDirty = false;

    public XWikiAttachmentContent(XWikiAttachment attachment) {
        this();
        setAttachment(attachment);
    }

    public XWikiAttachmentContent() {
        content = new byte[0];
    }

    public long getId() {
        return attachment.getId();
    }

    public void setId(long id) {
    }

    public Object clone() {
        XWikiAttachmentContent attachmentcontent = null;
        try {
            attachmentcontent = (XWikiAttachmentContent) getClass().newInstance();
        } catch (Exception e) {
            // This should not happen
        }

        attachmentcontent.setAttachment(getAttachment());
        attachmentcontent.setContent(getContent());
        return attachmentcontent;
    }

    public byte[] getContent() {
        if (content==null)
            return new byte[0];
        else
            return content;
    }

    public void setContent(byte[] content) {
        if (content==null)
         this.content = null;
        else {
            if (!content.equals(this.content))
                setContentDirty(true);
            this.content = content;
            attachment.setFilesize(content.length);
        }
    }

    public XWikiAttachment getAttachment() {
        return attachment;
    }

    public void setAttachment(XWikiAttachment attachment) {
        this.attachment = attachment;
    }

    public boolean isContentDirty() {
        return isContentDirty;
    }

    public void setContentDirty(boolean contentDirty) {
        isContentDirty = contentDirty;
    }
}
