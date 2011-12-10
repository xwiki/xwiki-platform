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
package org.xwiki.store.legacy.store.internal;

import java.io.InputStream;

import org.xwiki.store.StreamProvider;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * A stream provider based on the content of an attachment.
 * Used to save the content of each attachment to the correct file.
 *
 * @version $Id$
 * @since 3.0M2
 */
public class AttachmentContentStreamProvider implements StreamProvider
{
    /**
     * The attachment to save content of.
     */
    private final XWikiAttachment attachment;

    /**
     * The XWikiContext for getting the content of the attachment.
     */
    private final XWikiContext context;

    /**
     * The Constructor.
     *
     * @param attachment the attachment whose content should become the stream.
     * @param context the XWikiContext needed to get the content from the attachment
     * using {@link XWikiAttachment#getContentInputStream(XWikiContext)}
     */
    public AttachmentContentStreamProvider(final XWikiAttachment attachment,
        final XWikiContext context)
    {
        this.attachment = attachment;
        this.context = context;
    }

    @Override
    public InputStream getStream() throws XWikiException
    {
        return this.attachment.getContentInputStream(this.context);
    }
}
