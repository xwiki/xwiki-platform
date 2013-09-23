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

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.webdav.resources.XWikiDavResource;
import com.xpn.xwiki.plugin.webdav.resources.partial.AbstractDavFile;

/**
 * The dav resource used to represent the wiki content of an {@link XWikiDocument}.
 * 
 * @version $Id$
 */
public class DavWikiFile extends AbstractDavFile
{
    /**
     * Identifier for wiki text file.
     */
    public static final String WIKI_TXT = "wiki.txt";

    /**
     * Identifier for wiki xml file.
     */
    public static final String WIKI_XML = "wiki.xml";

    /**
     * The {@link XWikiDocument} of whose content is represented by this resource (file).
     */
    private XWikiDocument parentDoc;

    @Override
    public void init(XWikiDavResource parent, String name, String relativePath)
        throws DavException
    {
        super.init(parent, name, relativePath);
        if (!(name.equals(WIKI_TXT) || name.equals(WIKI_XML))) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        this.parentDoc = ((DavPage) parent).getDocument();
        String timeStamp = DavConstants.creationDateFormat.format(parentDoc.getCreationDate());
        getProperties().add(new DefaultDavProperty(DavPropertyName.CREATIONDATE, timeStamp));
        timeStamp = DavConstants.modificationDateFormat.format(parentDoc.getContentUpdateDate());
        getProperties().add(new DefaultDavProperty(DavPropertyName.GETLASTMODIFIED, timeStamp));
        getProperties().add(new DefaultDavProperty(DavPropertyName.GETETAG, timeStamp));
        getProperties().add(
            new DefaultDavProperty(DavPropertyName.GETCONTENTLANGUAGE, parentDoc.getLanguage()));
        String contentType = this.name.equals(WIKI_TXT) ? "text/plain" : "text/xml";
        getProperties().add(new DefaultDavProperty(DavPropertyName.GETCONTENTTYPE, contentType));
        int contentLength =
            this.name.equals(WIKI_TXT) ? parentDoc.getContent().length() : getContext().toXML(
                parentDoc).length();
        getProperties().add(
            new DefaultDavProperty(DavPropertyName.GETCONTENTLENGTH, contentLength));
    }

    @Override
    public boolean exists()
    {
        return !parentDoc.isNew() && parentResource.getVirtualMembers().contains(this);
    }

    @Override
    public void spool(OutputContext outputContext) throws IOException
    {
        // Protect against direct url referencing.
        if (!getContext().hasAccess("view", parentDoc.getFullName())) {
            throw new IOException("Access rights violation.");
        }
        outputContext.setContentLanguage(parentDoc.getLanguage());
        int contentLength = 0;
        try {
            contentLength =
                this.name.equals(WIKI_TXT) ? parentDoc.getContent().length() : getContext()
                    .toXML(parentDoc).length();
        } catch (DavException ex) {
            throw new IOException(ex.getMessage());
        }
        outputContext.setContentLength(contentLength);
        outputContext.setContentType(this.name.equals(WIKI_TXT) ? "text/plain" : "text/xml");
        outputContext.setETag(DavConstants.modificationDateFormat.format(getModificationTime()));
        outputContext.setModificationTime(getModificationTime());
        if (exists()) {
            OutputStream out = outputContext.getOutputStream();
            if (out != null) {
                try {
                    String content =
                        this.name.equals(WIKI_TXT) ? parentDoc.getContent() : getContext().toXML(
                            parentDoc);
                    out.write(content.getBytes());
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
        throw new DavException(DavServletResponse.SC_NOT_IMPLEMENTED);
    }

    @Override
    public long getModificationTime()
    {
        if (exists()) {
            return parentDoc.getContentUpdateDate().getTime();
        }
        return IOUtil.UNDEFINED_TIME;
    }
}
