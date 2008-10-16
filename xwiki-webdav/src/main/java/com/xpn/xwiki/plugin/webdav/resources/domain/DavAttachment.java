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
import java.util.List;

import org.apache.jackrabbit.server.io.IOUtil;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;

import com.xpn.xwiki.XWikiException;
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

    /**
     * {@inheritDoc}
     */
    public void init(XWikiDavResource parent, String name, String relativePath)
        throws DavException
    {
        super.init(parent, name, relativePath);
        if (parent.exists()) {
            this.attachment = ((DavPage) parent).getDocument().getAttachment(this.name);
        }
        if (exists()) {
            String timeStamp = DavConstants.creationDateFormat.format(attachment.getDate());
            davPropertySet.add(new DefaultDavProperty(DavPropertyName.CREATIONDATE, timeStamp));
            timeStamp = DavConstants.modificationDateFormat.format(attachment.getDate());
            davPropertySet
                .add(new DefaultDavProperty(DavPropertyName.GETLASTMODIFIED, timeStamp));
            davPropertySet.add(new DefaultDavProperty(DavPropertyName.GETETAG, timeStamp));
            davPropertySet.add(new DefaultDavProperty(DavPropertyName.GETCONTENTTYPE, attachment
                .getMimeType(xwikiContext)));
            davPropertySet.add(new DefaultDavProperty(DavPropertyName.GETCONTENTLANGUAGE,
                attachment.getDoc().getLanguage()));
            davPropertySet.add(new DefaultDavProperty(DavPropertyName.GETCONTENTLENGTH,
                attachment.getFilesize()));
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean exists()
    {
        return this.attachment != null;
    }

    /**
     * {@inheritDoc}
     */
    public void spool(OutputContext outputContext) throws IOException
    {
        if (exists()) {
            OutputStream out = outputContext.getOutputStream();
            if (null != out) {
                try {
                    out.write(this.attachment.getContent(xwikiContext));
                    out.flush();
                } catch (XWikiException ex) {
                    throw new IOException(ex.getFullMessage());
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void move(DavResource destination) throws DavException
    {
        if (destination instanceof DavAttachment) {
            DavAttachment dAttachment = (DavAttachment) destination;
            // Check if this is a rename operation.
            if (dAttachment.getCollection().equals(getCollection())) {
                try {
                    // First remove the current attachment (we have it in memory).
                    XWikiDocument owner = attachment.getDoc();
                    owner.deleteAttachment(attachment, xwikiContext);
                    // Rename the (in memory) attachment.
                    attachment.setFilename(dAttachment.getDisplayName());
                    // Add the attachment back to owner doc.
                    owner.getAttachmentList().add(attachment);
                    attachment.setDoc(owner);
                    owner.saveAttachmentContent(attachment, xwikiContext);
                    xwikiContext.getWiki().saveDocument(owner, xwikiContext);
                } catch (XWikiException e) {
                    throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e);
                }
            } else if (dAttachment.getCollection() instanceof DavPage) {
                XWikiDocument dDoc = ((DavPage) dAttachment.getCollection()).getDocument();
                try {
                    // Again remove the current attachment (we have it in memory).
                    XWikiDocument owner = attachment.getDoc();
                    owner.deleteAttachment(attachment, xwikiContext);
                    xwikiContext.getWiki().saveDocument(owner, xwikiContext);
                    // Rename the (in memory) attachment.
                    attachment.setFilename(dAttachment.getDisplayName());
                    // Add the attachment to destination document.
                    dDoc.getAttachmentList().add(attachment);
                    attachment.setDoc(dDoc);
                    dDoc.saveAttachmentContent(attachment, xwikiContext);
                    xwikiContext.getWiki().saveDocument(dDoc, xwikiContext);
                } catch (XWikiException e) {
                    throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e);
                }
            } else {
                throw new DavException(DavServletResponse.SC_BAD_REQUEST);
            }
        } else {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void copy(DavResource destination, boolean shallow) throws DavException
    {
        throw new DavException(DavServletResponse.SC_NOT_IMPLEMENTED);
    }

    /**
     * {@inheritDoc}
     */
    public MultiStatusResponse alterProperties(DavPropertySet setProperties,
        DavPropertyNameSet removePropertyNames) throws DavException
    {
        throw new DavException(DavServletResponse.SC_NOT_IMPLEMENTED);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public MultiStatusResponse alterProperties(List changeList) throws DavException
    {
        throw new DavException(DavServletResponse.SC_NOT_IMPLEMENTED);
    }

    /**
     * {@inheritDoc}
     */
    public void removeProperty(DavPropertyName propertyName) throws DavException
    {
        throw new DavException(DavServletResponse.SC_NOT_IMPLEMENTED);
    }

    /**
     * {@inheritDoc}
     */
    public void setProperty(DavProperty property) throws DavException
    {
        throw new DavException(DavServletResponse.SC_NOT_IMPLEMENTED);
    }

    /**
     * {@inheritDoc}
     */
    public long getModificationTime()
    {
        if (exists()) {
            return attachment.getDate().getTime();
        }
        return IOUtil.UNDEFINED_TIME;
    }

    /**
     * {@inheritDoc}
     */
    public String getSupportedMethods()
    {
        return "OPTIONS, GET, HEAD, PROPFIND, PROPPATCH, COPY, DELETE, MOVE, LOCK, UNLOCK";
    }   
}
