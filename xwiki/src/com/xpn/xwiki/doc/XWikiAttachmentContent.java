/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 30 janv. 2004
 * Time: 23:05:10
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

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        if (!content.equals(this.content))
         setContentDirty(true);
        this.content = content;
        attachment.setFilesize(content.length);
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
