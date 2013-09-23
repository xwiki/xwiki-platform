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
package com.xpn.xwiki.plugin.webdav.resources.domain;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.jackrabbit.server.io.IOUtil;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.webdav.resources.XWikiDavResource;
import com.xpn.xwiki.plugin.webdav.resources.partial.AbstractDavFile;

/**
 * The DAV resource representing an {@link XWikiAttachment}.
 * 
 * @version $Id$
 */
public class DavAttachment extends AbstractDavFile
{
    /**
     * The {@link XWikiAttachment} represented by this resource.
     */
    private XWikiAttachment attachment;

    @Override
    public void init(XWikiDavResource parent, String name, String relativePath)
        throws DavException
    {
        super.init(parent, name, relativePath);
        if (parent.exists()) {
            this.attachment = ((DavPage) parent).getDocument().getAttachment(this.name);
        }
        if (exists()) {
            String timeStamp = DavConstants.creationDateFormat.format(attachment.getDate());
            getProperties().add(new DefaultDavProperty(DavPropertyName.CREATIONDATE, timeStamp));
            timeStamp = DavConstants.modificationDateFormat.format(attachment.getDate());
            getProperties().add(
                new DefaultDavProperty(DavPropertyName.GETLASTMODIFIED, timeStamp));
            getProperties().add(new DefaultDavProperty(DavPropertyName.GETETAG, timeStamp));
            getProperties().add(
                new DefaultDavProperty(DavPropertyName.GETCONTENTTYPE, getContext().getMimeType(
                    attachment)));
            getProperties().add(
                new DefaultDavProperty(DavPropertyName.GETCONTENTLANGUAGE, attachment.getDoc()
                    .getLanguage()));
            getProperties()
                .add(
                    new DefaultDavProperty(DavPropertyName.GETCONTENTLENGTH, attachment
                        .getFilesize()));
        }
    }

    @Override
    public boolean exists()
    {
        return this.attachment != null;
    }

    @Override
    public void spool(OutputContext outputContext) throws IOException
    {
        // Protect against direct url referencing.
        if (!getContext().hasAccess("view", attachment.getDoc().getFullName())) {
            throw new IOException("Access rights violation.");
        }
        outputContext.setContentLanguage(attachment.getDoc().getLanguage());
        outputContext.setContentLength(attachment.getFilesize());
        outputContext.setContentType(getContext().getMimeType(attachment));
        outputContext.setETag(DavConstants.modificationDateFormat.format(getModificationTime()));
        outputContext.setModificationTime(getModificationTime());
        if (exists()) {
            OutputStream out = outputContext.getOutputStream();
            if (null != out) {
                try {
                    out.write(getContext().getContent(attachment));
                    out.flush();
                } catch (DavException ex) {
                    throw new IOException(ex.getMessage());
                }
            }
        }
    }

    @Override
    public void move(DavResource destination) throws DavException
    {
        getContext().checkAccess("edit", attachment.getDoc().getFullName());
        if (destination instanceof DavAttachment) {
            DavAttachment dAttachment = (DavAttachment) destination;
            // Check if this is a rename operation.
            if (dAttachment.getCollection().equals(getCollection())) {
                getContext().moveAttachment(attachment, attachment.getDoc(),
                    dAttachment.getDisplayName());
            } else if (dAttachment.getCollection() instanceof DavPage) {
                XWikiDocument dDoc = ((DavPage) dAttachment.getCollection()).getDocument();
                getContext().moveAttachment(attachment, dDoc, dAttachment.getDisplayName());
            } else {
                throw new DavException(DavServletResponse.SC_BAD_REQUEST);
            }
        } else {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST);
        }
        clearCache();
    }

    @Override
    public long getModificationTime()
    {
        if (exists()) {
            return attachment.getDate().getTime();
        }
        return IOUtil.UNDEFINED_TIME;
    }
}
