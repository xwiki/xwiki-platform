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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentArchive;

/**
 * Fake store for attachment versioning when it is disabled. 
 * ("xwiki.store.attachment.versioning=0" parameter is set in xwiki.cfg)
 * It says what there is only one version of attachment - latest.
 * It doesn't store anything. It is safe to use with any stores.
 * 
 * @version $Id: $
 * @since 1.4M2
 */
public class FakeAttachmentVersioningStore implements AttachmentVersioningStore
{
    /**
     * Constructor used by {@link XWiki} during storage initialization.
     * 
     * @param context The current context.
     */
    public FakeAttachmentVersioningStore(XWikiContext context)
    { }

    /**
     * {@inheritDoc}
     */
    public void deleteArchive(XWikiAttachment attachment, XWikiContext context,
        boolean transaction) throws XWikiException
    {
        // not needed
    }
    
    /**
     * {@inheritDoc}
     */
    public void saveArchive(XWikiAttachmentArchive archive, XWikiContext context,
        boolean transaction) throws XWikiException
    {
        // not needed
    }

    /**
     * {@inheritDoc}
     */
    public XWikiAttachmentArchive loadArchive(XWikiAttachment attachment, XWikiContext context,
        boolean transaction) throws XWikiException
    {
        XWikiAttachmentArchive archive = new FakeAttachmentArchive(attachment);
        attachment.setAttachment_archive(archive);
        return archive;
    }
    
    /**
     * Fake realization of AttachmentArchive. 
     * It says what there is only one version of attachment - latest.
     * Class is public because used in super.clone() via getClass().newInstance()
     */
    public static class FakeAttachmentArchive extends XWikiAttachmentArchive
    {
        /** 
         * Default constructor. Used in super.clone().
         */
        public FakeAttachmentArchive() { }
        
        /**
         * Helper constructor.
         * @param attachment attachment of this archive
         */
        public FakeAttachmentArchive(XWikiAttachment attachment)
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
            // ignore
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public byte[] getArchive(XWikiContext context) throws XWikiException
        {
            String sdata = getAttachment().toStringXML(true, false, context);
            Object[] lines = ToString.stringToArray(sdata);
            Archive archive = new Archive(lines, getAttachment().getFilename(), 
                getAttachment().getVersion());
            return archive.toByteArray();
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public void setRCSArchive(Archive archive)
        {
            // ignore
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
        public XWikiAttachment getRevision(XWikiAttachment attachment, String rev,
            XWikiContext context) throws XWikiException
        {
            return (attachment.getVersion().equals(rev))
                ? attachment
                : null;
        }
    }
}
