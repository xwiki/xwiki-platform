/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at 
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 30 janv. 2004
 * Time: 23:12:24
 */
package com.xpn.xwiki.doc;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import org.apache.commons.jrcs.rcs.Archive;
import org.apache.commons.jrcs.rcs.Lines;

import java.io.ByteArrayInputStream;
import java.util.Date;

public class XWikiAttachmentArchive {

    private XWikiAttachment attachment;

    public long getId() {
        return attachment.getId();
    }

    public void setId(long id) {
    }

    // Document Archive
    private Archive archive;

    public Archive getRCSArchive() {
        return archive;
    }

    public void setRCSArchive(Archive archive) {
        this.archive = archive;
    }

    public byte[] getArchive() throws XWikiException {
     return getArchive(null);      
    }

    public byte[] getArchive(XWikiContext context) throws XWikiException {
        if (archive==null)
            updateArchive(attachment.getContent(context));
        if (archive==null)
            return new byte[0];
        else {
            return archive.toByteArray();
        }
    }

    public void setArchive(byte[] data) throws XWikiException {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream(data);
            archive = new Archive(getAttachment().getFilename(), is);
        }
        catch (Exception e) {
            Object[] args = { getAttachment().getFilename() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_ATTACHMENT_ARCHIVEFORMAT,
                    "Exception while manipulating the archive for file {0}", e, args);
        }
    }

    public void updateArchive(byte[] data) throws XWikiException {
        try {
            String sdata = data.toString();
            Lines lines = new Lines(sdata);

            if (archive!=null) {
                archive.addRevision(lines.toArray(),"");
                attachment.incrementVersion();
                attachment.setDate(new Date());
            }
            else
                archive = new Archive(lines.toArray(),getAttachment().getFilename(),getAttachment().getVersion());
        }
        catch (Exception e) {
            Object[] args = { getAttachment().getFilename() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_ATTACHMENT_ARCHIVEFORMAT,
                    "Exception while manipulating the archive for file {0}", e, args);
        }
    }

    public XWikiAttachment getAttachment() {
        return attachment;
    }

    public void setAttachment(XWikiAttachment attachment) {
        this.attachment = attachment;
    }
}
