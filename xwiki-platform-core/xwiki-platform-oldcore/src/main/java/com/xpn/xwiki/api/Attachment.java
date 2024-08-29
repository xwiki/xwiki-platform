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
package com.xpn.xwiki.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.model.reference.AttachmentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;

public class Attachment extends Api
{
    /** Logging helper object. */
    private static final Logger LOGGER = LoggerFactory.getLogger(Attachment.class);

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
        return this.doc;
    }

    /**
     * @return the document Id of the attachment
     */
    public long getId()
    {
        return this.attachment.getId();
    }

    /**
     * @return the id of the document the file is attached to
     */
    public long getDocId()
    {
        return this.doc.getId();
    }

    /**
     * @return the Attachment size
     * @deprecated since 9.0RC1, use {@link #getLongSize()} instead
     */
    @Deprecated
    public int getFilesize()
    {
        long longSize = getLongSize();

        return longSize > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) longSize;
    }

    /**
     * @return the number of bytes in this attachment content
     * @since 9.0RC1
     */
    public long getLongSize()
    {
        return this.attachment.getLongSize();
    }

    /**
     * @return the attachment name
     */
    public String getFilename()
    {
        return this.attachment.getFilename();
    }

    /**
     * @return the login of the person who attach the file
     */
    public String getAuthor()
    {
        return this.attachment.getAuthor();
    }

    /**
     * @return the last version number of the document
     */
    public String getVersion()
    {
        return this.attachment.getVersion();
    }

    /**
     * @return the RCS object version of the document
     */
    public Version getRCSVersion()
    {
        return this.attachment.getRCSVersion();
    }

    /**
     * @return the list of comments
     */
    public String getComment()
    {
        return this.attachment.getComment();
    }

    /**
     * @return the date of the last uploaded version
     */
    public Date getDate()
    {
        return this.attachment.getDate();
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
        try {
            // the input stream can be null if the attachment has been deleted for example.
            InputStream contentInputStream = this.attachment.getContentInputStream(getXWikiContext());
            if (contentInputStream != null) {
                return IOUtils.toByteArray(contentInputStream);
            }
        } catch (IOException ex) {
            // This really shouldn't happen.
        }
        return new byte[0];
    }

    public InputStream getContentInputStream()
    {
        try {
            return this.attachment.getContentInputStream(getXWikiContext());
        } catch (XWikiException e) {
            LOGGER.error("Failed to get attachment input stream", e);

            return null;
        }
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

    /**
     * Get an array containing the versions of the attachment.
     * Versions are represented as a JRCS Version object.
     * This gets versions directly from the database which is slower than {@link #getVersionList()}.
     * WARNING: If there is an error loading content from the database, a single element array will
     *          be returned containing only the current version of the attachment.
     *          Consider using {@link #getVersionList()} instead.
     *
     * @return an array of Versions.
     * @throws XWikiException this will never happen.
     */
    public Version[] getVersions() throws XWikiException
    {
        this.attachment.loadArchive(getXWikiContext());
        return this.attachment.getVersions();
    }

    /**
     * Get a list of attachment versions from 1.1 to the current.
     * Versions are represented as a JRCS Version object.
     * This gets versions by counting backward from the current version to 1.1
     * which will be correct as long as the database is in a consistant state.
     *
     * @return a list of Versions.
     * @throws XWikiException this will never happen.
     */
    public List<Version> getVersionList() throws XWikiException
    {
        return this.attachment.getVersionList();
    }

    /**
     * @return the XWikiAttachment object (without the wrapping) if you have the programming right
     * @see XWikiAttachment
     */
    public XWikiAttachment getAttachment()
    {
        if (hasProgrammingRights()) {
            return this.attachment;
        } else {
            return null;
        }
    }

    /**
     * @return the mimetype of the attachment
     */
    public String getMimeType()
    {
        return this.attachment.getMimeType(getXWikiContext());
    }

    /**
     * @return true if it's an image
     */
    public boolean isImage()
    {
        return this.attachment.isImage(getXWikiContext());
    }

    /**
     * Allow to easily access any revision of an attachment.
     *
     * @param rev Version to access, in the "Major.minor" format.
     * @return Attachment API object, or {@code null} if the requested version does not exist.
     * @throws XWikiException In case of an error.
     */
    public Attachment getAttachmentRevision(String rev) throws XWikiException
    {
        XWikiAttachment att = this.attachment.getAttachmentRevision(rev, getXWikiContext());
        return att == null ? null : new Attachment(getDocument(), att, this.context);
    }

    /**
     * @return the reference of this attachment
     * @since 11.5RC1
     */
    public AttachmentReference getReference()
    {
        return this.attachment.getReference();
    }
}
