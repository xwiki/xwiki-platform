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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiException;
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
     * Logger instance.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DavWikiFile.class);

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

    /**
     * {@inheritDoc}
     */
    public void init(XWikiDavResource parent, String name, String relativePath)
        throws DavException
    {
        super.init(parent, name, relativePath);
        if (!(name.equals(WIKI_TXT) || name.equals(WIKI_XML))) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        this.parentDoc = ((DavPage) parent).getDocument();
        String timeStamp = DavConstants.creationDateFormat.format(parentDoc.getCreationDate());
        davPropertySet.add(new DefaultDavProperty(DavPropertyName.CREATIONDATE, timeStamp));
        timeStamp = DavConstants.modificationDateFormat.format(parentDoc.getContentUpdateDate());
        davPropertySet.add(new DefaultDavProperty(DavPropertyName.GETLASTMODIFIED, timeStamp));
        davPropertySet.add(new DefaultDavProperty(DavPropertyName.GETETAG, timeStamp));
        davPropertySet.add(new DefaultDavProperty(DavPropertyName.GETCONTENTLANGUAGE, parentDoc
            .getLanguage()));
        String contentType = this.name.equals(WIKI_TXT) ? "text/plain" : "text/xml";
        davPropertySet.add(new DefaultDavProperty(DavPropertyName.GETCONTENTTYPE, contentType));
        int contentLength = 0;
        try {
            contentLength =
                this.name.equals(WIKI_TXT) ? parentDoc.getContent().length() : parentDoc.toXML(
                    xwikiContext).length();
        } catch (XWikiException e) {
            LOG.error("Unexpected Error : ", e);
        } finally {
            davPropertySet.add(new DefaultDavProperty(DavPropertyName.GETCONTENTLENGTH,
                contentLength));
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean exists()
    {
        return !parentDoc.isNew();
    }

    /**
     * {@inheritDoc}
     */
    public void spool(OutputContext outputContext) throws IOException
    {
        if (exists()) {
            OutputStream out = outputContext.getOutputStream();
            if (out != null) {
                try {
                    String content =
                        this.name.equals(WIKI_TXT) ? parentDoc.getContent() : parentDoc
                            .toXML(xwikiContext);
                    out.write(content.getBytes());
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
    public void copy(DavResource destination, boolean shallow) throws DavException
    {
        throw new DavException(DavServletResponse.SC_NOT_IMPLEMENTED);
    }

    /**
     * {@inheritDoc}
     */
    public void move(DavResource destination) throws DavException
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
            return parentDoc.getContentUpdateDate().getTime();
        }
        return IOUtil.UNDEFINED_TIME;
    }

    /**
     * {@inheritDoc}
     */
    public String getSupportedMethods()
    {
        return "OPTIONS, GET, HEAD, PROPFIND, PROPPATCH, COPY, LOCK, UNLOCK";
    }
}
