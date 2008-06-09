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

package com.xpn.xwiki.api;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;

import org.suigeneris.jrcs.rcs.Version;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;

public class Attachment extends Api
{
    private Document doc;

    private XWikiAttachment attachment;

    public Attachment(Document doc, XWikiAttachment attachment, XWikiContext context)
    {
        super(context);
        this.doc = doc;
        this.attachment = attachment;
    }

    /**
     * @return the document the file is attached to
     */
    public Document getDocument()
    {
        return doc;
    }

    /**
     * @return the document Id of the attachment
     */
    public long getId()
    {
        return attachment.getId();
    }

    /**
     * @return the id of the document the file is attached to
     */
    public long getDocId()
    {
        return doc.getId();
    }

    /**
     * @return the Attachment size
     */
    public int getFilesize()
    {
        return attachment.getFilesize();
    }

    /**
     * @return the attachment name
     */
    public String getFilename()
    {
        return attachment.getFilename();
    }

    /**
     * @return the login of the person who attach the file
     */
    public String getAuthor()
    {
        return attachment.getAuthor();
    }

    /**
     * @return the last version number of the document
     */
    public String getVersion()
    {
        return attachment.getVersion();
    }

    /**
     * @return the RCS object version of the document
     */
    public Version getRCSVersion()
    {
        return attachment.getRCSVersion();
    }

    /**
     * @return the list of comments
     */
    public String getComment()
    {
        return attachment.getComment();
    }

    /**
     * @return the date of the last uploaded version
     */
    public Date getDate()
    {
        return attachment.getDate();
    }

    /**
     * @return the content of the attachment
     * @throws XWikiException
     */
    public byte[] getContent() throws XWikiException
    {
        return getContentAsBytes();
    }

    public byte[] getContentAsBytes() throws XWikiException
    {
        return attachment.getContent(getXWikiContext());
    }

    public String getContentAsString() throws XWikiException
    {
        // TODO: detect correct encoding for XML files?
        return new String(getContentAsBytes());
    }

    public String getContentAsString(String charset) throws XWikiException
    {
        // TODO: detect correct encoding for XML files?
        byte[] contentBytes = getContentAsBytes();
        try {
            return new String(contentBytes, charset);
        } catch (UnsupportedEncodingException ex) {
            return new String(contentBytes);
        }
    }

    public Version[] getVersions() throws XWikiException
    {
        attachment.loadArchive(getXWikiContext());
        return attachment.getVersions();
    }

    /**
     * @return a list of string with all the versions number in String
     * @throws XWikiException
     */
    public List<Version> getVersionList() throws XWikiException
    {
        attachment.loadArchive(getXWikiContext());
        return attachment.getVersionList();
    }

    /**
     * @return the XWikiAttachment object (without the wrapping) if you have the programming right
     * @see XWikiAttachment
     */
    public XWikiAttachment getAttachment()
    {
        if (hasProgrammingRights())
            return attachment;
        else
            return null;
    }

    /**
     * @return the mimetype of the attachment
     */
    public String getMimeType()
    {
        return attachment.getMimeType(getXWikiContext());
    }

    /**
     * @return true if it's an image
     */
    public boolean isImage()
    {
        return attachment.isImage(getXWikiContext());
    }

    /**
     * Allow to easily access any revision of an attachment.
     * 
     * @param rev Version to access, in the "Major.minor" format.
     * @return Attachment API object, or <tt>null</tt> if the requested version does not exist.
     * @throws XWikiException In case of an error.
     */
    public Attachment getAttachmentRevision(String rev) throws XWikiException
    {
        XWikiAttachment att = attachment.getAttachmentRevision(rev, getXWikiContext());
        return att == null ? null : new Attachment(getDocument(), att, context);
    }
}
