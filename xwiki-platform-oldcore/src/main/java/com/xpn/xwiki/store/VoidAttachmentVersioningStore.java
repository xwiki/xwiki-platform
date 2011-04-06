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
package com.xpn.xwiki.store;

import java.util.Date;

import org.suigeneris.jrcs.rcs.Archive;
import org.suigeneris.jrcs.rcs.Version;
import org.suigeneris.jrcs.util.ToString;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentArchive;

/**
 * Void store for attachment versioning when it is disabled. ("xwiki.store.attachment.versioning=0" parameter is set in
 * xwiki.cfg) It says what there is only one version of attachment - latest. It doesn't store anything. It is safe to
 * use with any stores.
 * 
 * @version $Id$
 * @since 1.4M2
 */
@Component("void")
public class VoidAttachmentVersioningStore implements AttachmentVersioningStore
{
    /**
     * Constructor used by {@link XWiki} during storage initialization.
     * 
     * @param context The current context.
     * @deprecated 1.6M1. Use ComponentManager.lookup(AttachmentVersioningStore.class) instead.
     */
    @Deprecated
    public VoidAttachmentVersioningStore(XWikiContext context)
    {
    }

    /**
     * Empty constructor needed for component manager.
     */
    public VoidAttachmentVersioningStore()
    {
    }

    /**
     * {@inheritDoc}
     */
    public void deleteArchive(XWikiAttachment attachment, XWikiContext context, boolean transaction)
        throws XWikiException
    {
        // Don't do anything since it's a void implementation.
    }

    /**
     * {@inheritDoc}
     */
    public void saveArchive(XWikiAttachmentArchive archive, XWikiContext context, boolean transaction)
        throws XWikiException
    {
        // Don't do anything since it's a void implementation.
    }

    /**
     * {@inheritDoc}
     */
    public XWikiAttachmentArchive loadArchive(XWikiAttachment attachment, XWikiContext context, boolean transaction)
        throws XWikiException
    {
        XWikiAttachmentArchive archive = attachment.getAttachment_archive();
        if (!(archive instanceof VoidAttachmentArchive)) {
            archive = new VoidAttachmentArchive(attachment);
        }
        attachment.setAttachment_archive(archive);
        return archive;
    }

    /**
     * Void realization of AttachmentArchive. It says what there is only one version of attachment - latest. Class is
     * public because used in super.clone() via getClass().newInstance()
     */
    public static class VoidAttachmentArchive extends XWikiAttachmentArchive
    {
        /**
         * Default constructor. Used in super.clone().
         */
        public VoidAttachmentArchive()
        {
        }

        /**
         * Helper constructor.
         * 
         * @param attachment attachment of this archive
         */
        public VoidAttachmentArchive(XWikiAttachment attachment)
        {
            setAttachment(attachment);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void updateArchive(byte[] data, XWikiContext context) throws XWikiException
        {
            getAttachment().incrementVersion();
            getAttachment().setDate(new Date());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setArchive(byte[] data) throws XWikiException
        {
            // Don't do anything since it's a void implementation.
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public byte[] getArchive(XWikiContext context) throws XWikiException
        {
            String sdata = getAttachment().toStringXML(true, false, context);
            Object[] lines = ToString.stringToArray(sdata);
            Archive archive = new Archive(lines, getAttachment().getFilename(), getAttachment().getVersion());
            return archive.toByteArray();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setRCSArchive(Archive archive)
        {
            // Don't do anything since it's a void implementation.
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Version[] getVersions()
        {
            return new Version[] {getAttachment().getRCSVersion()};
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public XWikiAttachment getRevision(XWikiAttachment attachment, String rev, XWikiContext context)
            throws XWikiException
        {
            return (attachment.getVersion().equals(rev)) ? attachment : null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object clone()
        {
            // super.clone() is needed for checkstyle
            super.clone();
            return new VoidAttachmentArchive(getAttachment());
        }
    }
}
