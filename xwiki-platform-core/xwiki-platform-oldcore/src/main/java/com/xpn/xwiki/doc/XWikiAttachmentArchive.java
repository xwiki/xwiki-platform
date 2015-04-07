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
package com.xpn.xwiki.doc;

import java.io.ByteArrayInputStream;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.suigeneris.jrcs.rcs.Archive;
import org.suigeneris.jrcs.rcs.Version;
import org.suigeneris.jrcs.rcs.impl.Node;
import org.suigeneris.jrcs.util.ToString;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * JRCS based implementation of an archive for XWikiAttachment.
 *
 * @version $Id$
 */
public class XWikiAttachmentArchive implements Cloneable
{
    /** Generic message to put in any exception which occurs in this class. */
    private static final String GENERIC_EXCEPTION_MESSAGE =
        "Exception while manipulating the archive for attachment {0}";

    /** The log, used to log if there is an error while cloning the archive. */
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiAttachmentArchive.class);

    /** The attachment which this is an archive of. */
    private XWikiAttachment attachment;

    /** The underlying JRCS archive. */
    private Archive archive;

    /**
     * @return the id of the attachment which this archive is associated with.
     */
    public long getId()
    {
        return this.attachment.getId();
    }

    /**
     * This does nothing and is only here to satisfy Hibernate.
     *
     * @param id the id of the attachment which this archive is associated with, unused.
     */
    public void setId(final long id)
    {
        // Do nothing as this is here only to please hibernate.
    }

    @Override
    public Object clone()
    {
        XWikiAttachmentArchive attachmentarchive = null;
        try {
            attachmentarchive = getClass().newInstance();
        } catch (Exception e) {
            // This should not happen
            LOGGER.error("Error while attachmentArchive.clone()", e);
        }

        attachmentarchive.setAttachment(getAttachment());
        attachmentarchive.setRCSArchive(getRCSArchive());

        return attachmentarchive;
    }

    /**
     * @deprecated since 2.6M1 please do not use this, it is bound to a jrcs based implementation.
     * @return a JRCS archive.
     */
    @Deprecated
    public Archive getRCSArchive()
    {
        return this.archive;
    }

    /**
     * @deprecated since 2.6M1 please do not use this, it is bound to a jrcs based implementation.
     * @param archive a JRCS archive.
     */
    @Deprecated
    public void setRCSArchive(final Archive archive)
    {
        this.archive = archive;
    }

    /**
     * Get the archive if it is currently stored in RAM.
     *
     * @return a byte array representation of a JRCS archive or an empty array if the archive is not available on the
     *         heap.
     * @throws XWikiException if anything goes wrong.
     */
    public byte[] getArchive() throws XWikiException
    {
        return getArchive(null);
    }

    /**
     * Get the archive, loading it from the database if necessary.
     *
     * @param context the XWikiContext for the request used to load the correct attachment archive from the database.
     * @return a byte array representation of a JRCS archive.
     * @throws XWikiException if anything goes wrong.
     */
    public byte[] getArchive(final XWikiContext context) throws XWikiException
    {
        if (this.archive == null) {
            if (context != null) {
                updateArchive(this.attachment.getContent(context), context);
            }
        }
        if (this.archive == null) {
            return new byte[0];
        } else {
            return this.archive.toByteArray();
        }
    }

    /**
     * Set the archive from a byte array representation of a JRCS archive.
     *
     * @param data a byte array representation of a JRCS archive.
     * @throws XWikiException if anything goes wrong.
     */
    public void setArchive(final byte[] data) throws XWikiException
    {
        if ((data == null) || (data.length == 0)) {
            this.archive = null;
        } else {
            try {
                // attachment.fromXML(data.toString());
                final ByteArrayInputStream is = new ByteArrayInputStream(data);
                this.archive = new Archive(getAttachment().getFilename(), is);

            } catch (Exception e) {
                Object[] args = { getAttachment().getFilename() };
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_STORE_ATTACHMENT_ARCHIVEFORMAT, GENERIC_EXCEPTION_MESSAGE, e, args);
            }
        }
    }

    /**
     * Update the archive.
     *
     * @param data not used for anything, the data is loaded from the attachment included with this archive.
     * @param context the XWikiContext for the request used to load the correct attachment content from the database.
     * @throws XWikiException if anything goes wrong.
     */
    public void updateArchive(final byte[] data, final XWikiContext context) throws XWikiException
    {
        try {
            this.attachment.incrementVersion();
            this.attachment.setDate(new Date());
            final String sdata = this.attachment.toStringXML(true, false, context);
            final Object[] lines = ToString.stringToArray(sdata);

            if (this.archive != null) {
                this.archive.addRevision(lines, "");
            } else {
                this.archive = new Archive(lines, getAttachment().getFilename(), getAttachment().getVersion());
            }
            // Set a standard author, since by default the operating system user is set, and it might contain confusing
            // characters (JRCS is very fragile and breaks easily if a wrong value is used)
            this.archive.findNode(this.archive.getRevisionVersion()).setAuthor("xwiki");
        } catch (Exception e) {
            Object[] args = { getAttachment().getFilename() };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_ATTACHMENT_ARCHIVEFORMAT, GENERIC_EXCEPTION_MESSAGE, e, args);
        }
    }

    /**
     * @return the attachment which this is an archive for.
     */
    public XWikiAttachment getAttachment()
    {
        return this.attachment;
    }

    /**
     * Set the attachment to associate with this archive. This is a dangerous function because it will not change the
     * archive. Using this may cause an attachment to be associated with the wrong history.
     *
     * @param attachment the attachment to set for this archive.
     */
    public void setAttachment(final XWikiAttachment attachment)
    {
        this.attachment = attachment;
    }

    /**
     * @return an array of versions which are available for this attachment, ordered by version number descending.
     */
    public Version[] getVersions()
    {
        final Archive rcsArchive = getRCSArchive();

        Version[] versions;
        if (rcsArchive != null) {
            final Node[] nodes = rcsArchive.changeLog();
            versions = new Version[nodes.length];
            for (int i = 0; i < nodes.length; i++) {
                versions[i] = nodes[i].getVersion();
            }
        } else {
            // No archive means there is no history and only the current version
            versions = new Version[] { this.attachment.getRCSVersion() };
        }

        return versions;
    }

    /**
     * Get an old revision of the attachment which this is an archive of.
     *
     * @param attachment This attachment will be used to get the document to associate the attachment revision with.
     * @param rev a String representation of the version to load.
     * @param context the context for the request which needed this revision.
     * @return an XWikiAttachment for the given revision.
     * @throws XWikiException if any Exception is thrown while getting the revision.
     */
    public XWikiAttachment getRevision(final XWikiAttachment attachment, final String rev, final XWikiContext context)
        throws XWikiException
    {
        try {
            final Archive rcsArchive = getRCSArchive();

            if (rcsArchive == null) {
                // No archive means there is no history and only the current version.
                return this.attachment.getVersion().equals(rev) ? this.attachment : null;
            }

            final Version version = rcsArchive.getRevisionVersion(rev);
            if (version == null) {
                // The requested revision doesn't exist.
                return null;
            }
            final Object[] lines = rcsArchive.getRevision(version);
            final StringBuilder content = new StringBuilder();
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].toString();
                content.append(line);
                if (i != lines.length - 1) {
                    content.append("\n");
                }
            }

            final String scontent = content.toString();
            final XWikiAttachment revattach = new XWikiAttachment();
            revattach.fromXML(scontent);
            revattach.setDoc(attachment.getDoc());
            revattach.setVersion(rev);
            return revattach;
        } catch (Exception e) {
            final Object[] args = { attachment.getFilename() };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_ATTACHMENT_ARCHIVEFORMAT, GENERIC_EXCEPTION_MESSAGE, e, args);
        }
    }
}
