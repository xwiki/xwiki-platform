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
package com.xpn.xwiki.internal.skin;

import java.time.Instant;

import javax.inject.Provider;

import org.xwiki.filter.input.DefaultInputStreamInputSource;
import org.xwiki.filter.input.InputStreamInputSource;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.skin.ResourceRepository;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 * @since 6.4M1
 */
public class AttachmentWikiResource extends AbstractWikiResource<AttachmentReference, InputStreamInputSource>
{
    public AttachmentWikiResource(String path, ResourceRepository repository, AttachmentReference reference,
        DocumentReference authorReference, Provider<XWikiContext> xcontextProvider)
    {
        super(path, path, reference.getName(), repository, reference, authorReference, xcontextProvider);
    }

    @Override
    public InputStreamInputSource getInputSource() throws Exception
    {
        XWikiAttachment attachment = getDocument().getAttachment(this.reference.getName());

        return new DefaultInputStreamInputSource(attachment.getContentInputStream(this.xcontextProvider.get()), true);
    }

    @Override
    public Instant getInstant() throws Exception
    {
        XWikiAttachment attachment = getDocument().getAttachment(this.reference.getName());

        return attachment.getDate().toInstant();
    }

    @Override
    public String getURL(XWikiDocument document) throws Exception
    {
        return document.getAttachmentURL(this.reference.getName(), "skin", this.xcontextProvider.get());
    }
}
