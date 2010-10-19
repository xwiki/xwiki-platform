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

import java.io.ByteArrayInputStream;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.suigeneris.jrcs.rcs.Archive;
import org.suigeneris.jrcs.rcs.Version;
import org.suigeneris.jrcs.rcs.impl.Node;
import org.suigeneris.jrcs.util.ToString;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

public class XWikiAttachmentArchive implements Cloneable
{
    private static final Log LOG = LogFactory.getLog(XWikiAttachmentArchive.class);

    private XWikiAttachment attachment;

    public long getId()
    {
        return this.attachment.getId();
    }

    public void setId(long id)
    {
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#clone()
     */
    @Override
    public Object clone()
    {
        XWikiAttachmentArchive attachmentarchive = null;
        try {
            attachmentarchive = (XWikiAttachmentArchive) getClass().newInstance();
        } catch (Exception e) {
            // This should not happen
            LOG.error("Error while attachmentArchive.clone()", e);
        }

        attachmentarchive.setAttachment(getAttachment());
        attachmentarchive.setRCSArchive(getRCSArchive());

        return attachmentarchive;
    }

    // Document Archive
    private Archive archive;

    /**
     * @deprecated since 2.6M1 please do not use this, it is bound to a jrcs based implementation.
     */
    @Deprecated
    public Archive getRCSArchive()
    {
        return this.archive;
    }

    /**
     * @deprecated since 2.6M1 please do not use this, it is bound to a jrcs based implementation.
     */
    @Deprecated
    public void setRCSArchive(Archive archive)
    {
        this.archive = archive;
    }

    public byte[] getArchive() throws XWikiException
    {
        return getArchive(null);
    }

    public byte[] getArchive(XWikiContext context) throws XWikiException
    {
        if (this.archive == null) {
            if (context != null)
                updateArchive(this.attachment.getContent(context), context);
        }
        if (this.archive == null) {
            return new byte[0];
        } else {
            return this.archive.toByteArray();
        }
    }

    public void setArchive(byte[] data) throws XWikiException
    {
        if ((data == null) || (data.length == 0)) {
            this.archive = null;
        } else {
            try {
                // attachment.fromXML(data.toString());
                ByteArrayInputStream is = new ByteArrayInputStream(data);
                this.archive = new Archive(getAttachment().getFilename(), is);

            } catch (Exception e) {
                Object[] args = {getAttachment().getFilename()};
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_STORE_ATTACHMENT_ARCHIVEFORMAT,
                    "Exception while manipulating the archive for file {0}", e, args);
            }
        }
    }

    /**
     * Update the archive.
     *
     * @param data not used for anything, the data is loaded from the attachment included with this archive.
     * @param context the XWikiContext for the request used to load the correct attachment content from the database.
     */
    public void updateArchive(byte[] data, XWikiContext context) throws XWikiException
    {
        try {
            this.attachment.incrementVersion();
            this.attachment.setDate(new Date());
            String sdata = this.attachment.toStringXML(true, false, context);
            Object[] lines = ToString.stringToArray(sdata);

            if (this.archive != null) {
                this.archive.addRevision(lines, "");
            } else {
                this.archive = new Archive(lines, getAttachment().getFilename(), getAttachment().getVersion());
            }
        } catch (Exception e) {
            Object[] args = {getAttachment().getFilename()};
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_ATTACHMENT_ARCHIVEFORMAT,
                "Exception while manipulating the archive for file {0}", e, args);
        }
    }

    public XWikiAttachment getAttachment()
    {
        return this.attachment;
    }

    public void setAttachment(XWikiAttachment attachment)
    {
        this.attachment = attachment;
    }

    public Version[] getVersions()
    {
        Node[] nodes = getRCSArchive().changeLog();
        Version[] versions = new Version[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            versions[i] = nodes[i].getVersion();
        }

        return versions;
    }

    public XWikiAttachment getRevision(XWikiAttachment attachment, String rev, XWikiContext context)
        throws XWikiException
    {
        try {
            Archive archive = getRCSArchive();

            if (archive == null) {
                return null;
            }

            Version v = archive.getRevisionVersion(rev);
            if (v == null) {
                return null;
            }
            Object[] lines = archive.getRevision(v);
            StringBuffer content = new StringBuffer();
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].toString();
                content.append(line);
                if (i != lines.length - 1)
                    content.append("\n");
            }

            String scontent = content.toString();
            XWikiAttachment revattach = new XWikiAttachment();
            revattach.fromXML(scontent);
            revattach.setDoc(attachment.getDoc());
            revattach.setVersion(rev);
            return revattach;
        } catch (Exception e) {
            Object[] args = {attachment.getFilename()};
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_ATTACHMENT_ARCHIVEFORMAT,
                "Exception while manipulating the archive for file {0}", e, args);
        }
    }
}
