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
package org.xwiki.store.legacy.doc.internal;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.suigeneris.jrcs.rcs.Archive;
import org.suigeneris.jrcs.rcs.Version;
import org.suigeneris.jrcs.rcs.impl.Node;
import org.suigeneris.jrcs.util.ToString;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentArchive;

/**
 * Implementation of an archive for XWikiAttachment based on a simple list of XWikiAttachments.
 *
 * @version $Id$
 * @since 3.0M2
 */
public class ListAttachmentArchive extends XWikiAttachmentArchive
{
    /**
     * Generic message to put in any exception which occurs in this class.
     */
    private static final String GENERIC_EXCEPTION_MESSAGE =
        "Exception while manipulating the archive for attachment {0}";

    /**
     * Message to place in exceptions which are thrown because functions are not available in this implementation.
     */
    private static final String NOT_IMPLEMENTED_MESSAGE =
        "This function is not available in this implementation.";

    /**
     * The attachment which this is an archive of.
     */
    private XWikiAttachment attachment;

    /**
     * A list of all the revisions of the attachment in this archive ordered by version number ascending.
     */
    private final List<XWikiAttachment> revisions = new ArrayList<XWikiAttachment>();

    /**
     * The Constructor.
     *
     * @param attachment the attachment to associate with this archive.
     */
    public ListAttachmentArchive(final XWikiAttachment attachment)
    {
        this.attachment = attachment;
    }

    /**
     * Constructor from List. Create a new instance of ListAttachmentArchive from a list of attachments.
     *
     * @param revisions a List of XWikiAttachment revisions to put in this archive. All revisions are the same
     * attachment and thus must have the same ID.
     */
    public ListAttachmentArchive(final List<XWikiAttachment> revisions)
    {
        // Empty list:
        if (revisions.size() == 0) {
            return;
        }

        this.revisions.addAll(revisions);
        Collections.sort(this.revisions, XWikiAttachmentVersionComparitor.INSTANCE);

        // Sanity check, all revisions should have the same ID.
        long id = revisions.get(0).getId();
        final String firstAttachName = revisions.get(0).getFilename();

        for (XWikiAttachment attach : revisions) {
            if (attach.getId() != id) {
                throw new IllegalArgumentException("Attachment " + attach.getFilename() + " has a "
                    + "different ID than the first attachment ( "
                    + firstAttachName + " ) so they cannot all be "
                    + "revisions of the same attachment.");
            }
            attach.setAttachment_archive(this);
        }

        // Set the attachment for this archive to the latest version.
        this.attachment = this.revisions.get(revisions.size() - 1);
    }

    @Override
    public Object clone()
    {
        final ListAttachmentArchive out = new ListAttachmentArchive(cloneAttachment(this.attachment));
        out.attachment.setAttachment_archive(out);
        for (XWikiAttachment revision : this.revisions) {
            final XWikiAttachment revClone = cloneAttachment(revision);
            revClone.setAttachment_archive(out);
            out.revisions.add(revClone);
        }
        return out;
    }

    /**
     * Clone an attachment but not it's archive.{@link ListAttachmentArchive#clone()} calls {@link
     * XWikiAttachment#clone()} and if the attachment is associated with an archive it call clone the archive. This
     * function prevents an infinite loop.
     *
     * @param original an attachment to clone.
     * @return a clone of original which has no XWikiAttachmentArchive attached.
     */
    private static XWikiAttachment cloneAttachment(final XWikiAttachment original)
    {
        final XWikiAttachmentArchive arch = original.getAttachment_archive();
        try {
            original.setAttachment_archive(null);
            return (XWikiAttachment) original.clone();
        } finally {
            original.setAttachment_archive(arch);
        }
    }

    @Override
    @Deprecated
    public Archive getRCSArchive()
    {
        throw new RuntimeException(NOT_IMPLEMENTED_MESSAGE);
    }

    @Override
    @Deprecated
    public void setRCSArchive(final Archive rcsArchive)
    {
        throw new RuntimeException(NOT_IMPLEMENTED_MESSAGE);
    }

    /**
     * Convert this attachment archive into JRCS format.
     *
     * @param context the XWikiContext for the request.
     * @return this archive in JRCS format.
     * @throws Exception if something goes wrong while serializing the attachment to XML or inserting it into the RCS
     * archive.
     */
    private Archive toRCS(final XWikiContext context) throws Exception
    {
        final Version[] versions = this.getVersions();
        Archive rcsArch = null;
        for (XWikiAttachment rev : this.revisions) {
            final String sdata = rev.toStringXML(true, false, context);
            final Object[] lines = ToString.stringToArray(sdata);
            if (rcsArch == null) {
                // First cycle.
                rcsArch = new Archive(lines, rev.getFilename(), rev.getVersion());
            } else {
                rcsArch.addRevision(lines, "");
            }
        }
        return rcsArch;
    }

    /**
     * @param rcsArchive the RCS archive to import.
     * @throws Exception if getting a revision from the RCS archive or deserializing an attachment from XML fails
     */
    private void fromRCS(final Archive rcsArchive) throws Exception
    {
        if (rcsArchive == null) {
            return;
        }

        final Node[] nodes = rcsArchive.changeLog();
        for (int i = nodes.length - 1; i > -1; i--) {
            final Object[] lines = rcsArchive.getRevision(nodes[i].getVersion());
            final StringBuilder content = new StringBuilder();
            for (int j = 0; j < lines.length; j++) {
                String line = lines[j].toString();
                content.append(line);
                if (j != lines.length - 1) {
                    content.append("\n");
                }
            }
            final XWikiAttachment rev = new XWikiAttachment();
            rev.fromXML(content.toString());
            rev.setDoc(this.getAttachment().getDoc());
            rev.setAttachment_archive(this);

            // this should not be necessary, keeping to maintain behavior.
            rev.setVersion(nodes[i].getVersion().toString());

            revisions.add(rev);
        }
    }

    /**
     * {@inheritDoc} Not implemented, always returns an empty array.
     *
     * @see com.xpn.xwiki.doc.XWikiAttachmentArchive#getArchive()
     */
    public byte[] getArchive()
    {
        return new byte[0];
    }

    @Override
    public byte[] getArchive(final XWikiContext context) throws XWikiException
    {
        try {
            return this.toRCS(context).toByteArray();
        } catch (Exception e) {
            if (e instanceof XWikiException) {
                throw (XWikiException) e;
            }
            Object[] args = { getAttachment().getFilename() };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_ATTACHMENT_ARCHIVEFORMAT,
                GENERIC_EXCEPTION_MESSAGE, e, args);
        }
    }

    @Override
    public void setArchive(final byte[] data) throws XWikiException
    {
        this.revisions.clear();

        if ((data != null) && (data.length == 0)) {
            try {
                final ByteArrayInputStream is = new ByteArrayInputStream(data);
                final Archive rcsArchive = new Archive(getAttachment().getFilename(), is);
                this.setRCSArchive(rcsArchive);
            } catch (Exception e) {
                if (e instanceof XWikiException) {
                    throw (XWikiException) e;
                }
                Object[] args = { getAttachment().getFilename() };
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_STORE_ATTACHMENT_ARCHIVEFORMAT,
                    GENERIC_EXCEPTION_MESSAGE, e, args);
            }
        }
    }

    @Override
    public void updateArchive(final byte[] data, final XWikiContext context)
    {
        this.update();
    }

    /**
     * Update the archive, increment the attachment version, set the date on the attachment and add the attachment to
     * the list.
     */
    private void update()
    {
        final XWikiAttachment attach = this.getAttachment();
        attach.incrementVersion();
        attach.setDate(new Date());
        // Clone the attachment but don't clone this archive.
        final XWikiAttachment clone = cloneAttachment(attach);
        clone.setAttachment_archive(this);
        this.revisions.add(clone);
    }

    @Override
    public XWikiAttachment getAttachment()
    {
        return this.attachment;
    }

    @Override
    public void setAttachment(final XWikiAttachment attachment)
    {
        this.attachment = attachment;
    }

    @Override
    public Version[] getVersions()
    {
        final Version[] versions = new Version[this.revisions.size()];
        int i = this.revisions.size();
        for (XWikiAttachment attach : this.revisions) {
            i--;
            versions[i] = attach.getRCSVersion();
        }

        return versions;
    }

    @Override
    public XWikiAttachment getRevision(final XWikiAttachment attachment,
        final String rev,
        final XWikiContext context)
    {
        if (rev == null) {
            return null;
        }

        for (XWikiAttachment attach : this.revisions) {
            if (rev.equals(attach.getVersion())) {
                final XWikiAttachment out = cloneAttachment(attach);
                out.setAttachment_archive(this);

                // This is silly, we set the attachment document and passed value.
                // Keeping to maintain current behavior.
                out.setDoc(attachment.getDoc());

                return out;
            }
        }

        return null;
    }

    /**
     * A comparitor which compares attachments by version number.
     */
    private static class XWikiAttachmentVersionComparitor implements Comparator<XWikiAttachment>
    {
        /**
         * A single instance to use instead of constructing one each time.
         */
        public static final XWikiAttachmentVersionComparitor INSTANCE =
            new XWikiAttachmentVersionComparitor();

        @Override
        public int compare(final XWikiAttachment a, final XWikiAttachment b)
        {
            return a.getRCSVersion().compareTo(b.getRCSVersion());
        }
    }
}
