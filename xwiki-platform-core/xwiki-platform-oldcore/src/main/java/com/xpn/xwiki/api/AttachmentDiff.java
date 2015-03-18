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

import org.xwiki.diff.Delta;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * Safe version of {@link com.xpn.xwiki.doc.AttachmentDiff} that can be used by the scripts.
 * 
 * @since 7.0RC1
 * @version $Id$
 */
@Unstable
public class AttachmentDiff
{
    /**
     * The unsafe attachment difference (exposes protected {@link XWikiAttachment}).
     */
    private com.xpn.xwiki.doc.AttachmentDiff diff;

    /**
     * The XWiki context used to create save versions of {@link XWikiAttachment}.
     */
    private XWikiContext context;

    /**
     * Wraps the given unsafe attachment difference.
     * 
     * @param diff the unsafe attachment difference
     * @param context the XWiki context needed to create the safe versions of {@link XWikiAttachment}
     */
    public AttachmentDiff(com.xpn.xwiki.doc.AttachmentDiff diff, XWikiContext context)
    {
        this.diff = diff;
        this.context = context;
    }

    /**
     * @return the name of the attachment whose versions are being compared
     */
    public String getFileName()
    {
        return this.diff.getFileName();
    }

    /**
     * @return the type of difference (one of 'insert', 'delete', 'change')
     */
    public String getType()
    {
        Delta.Type type = this.diff.getType();
        return type != null ? type.toString().toLowerCase() : null;
    }

    /**
     * @return the original version of the attachment
     */
    public Attachment getOrigAttachment()
    {
        XWikiAttachment origAttachment = this.diff.getOrigAttachment();
        return origAttachment != null ? new Attachment(origAttachment.getDoc().newDocument(context), origAttachment,
            context) : null;
    }

    /**
     * @return the new version of the attachment
     */
    public Attachment getNewAttachment()
    {
        XWikiAttachment newAttachment = this.diff.getNewAttachment();
        return newAttachment != null ? new Attachment(newAttachment.getDoc().newDocument(context), newAttachment,
            context) : null;
    }

    /**
     * NOTE: We kept this method (despite being deprecated) in order to preserve backwards compatibility with existing
     * scripts that were exposed to the unsafe {@link com.xpn.xwiki.doc.AttachmentDiff}.
     * 
     * @return the original attachment version
     */
    @Deprecated
    public String getOrigVersion()
    {
        return this.diff.getOrigVersion();
    }

    /**
     * NOTE: We kept this method (despite being deprecated) in order to preserve backwards compatibility with existing
     * scripts that were exposed to the unsafe {@link com.xpn.xwiki.doc.AttachmentDiff}.
     * 
     * @return the new attachment version
     */
    @Deprecated
    public String getNewVersion()
    {
        return this.diff.getNewVersion();
    }

    @Override
    public String toString()
    {
        return this.diff.toString();
    }
}
