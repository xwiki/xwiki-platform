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
import java.util.Date;
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

import com.xpn.xwiki.plugin.webdav.resources.XWikiDavResource;
import com.xpn.xwiki.plugin.webdav.resources.partial.AbstractDavFile;

/**
 * Resource used to represent temporary files demanded by various dav cleints.
 * 
 * @version $Id$
 */
public class DavTempFile extends AbstractDavFile
{    
    /**
     * Content of this resource (file).
     */
    private byte[] data;

    /**
     * {@inheritDoc}
     */
    public void init(XWikiDavResource parent, String name, String relativePath)
        throws DavException
    {
        super.init(parent, name, relativePath);
        if (!name.startsWith(".")) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        Date currentDate = new Date();
        String timeStamp = DavConstants.creationDateFormat.format(currentDate);
        davPropertySet.add(new DefaultDavProperty(DavPropertyName.CREATIONDATE, timeStamp));
        timeStamp = DavConstants.modificationDateFormat.format(currentDate);
        davPropertySet.add(new DefaultDavProperty(DavPropertyName.GETLASTMODIFIED, timeStamp));
        davPropertySet.add(new DefaultDavProperty(DavPropertyName.GETETAG, timeStamp));
        davPropertySet.add(new DefaultDavProperty(DavPropertyName.GETCONTENTLANGUAGE, "en"));
        davPropertySet.add(new DefaultDavProperty(DavPropertyName.GETCONTENTTYPE,
            "application/octet-stream"));
        int contentLength = exists() ? data.length : 0;
        davPropertySet
            .add(new DefaultDavProperty(DavPropertyName.GETCONTENTLENGTH, contentLength));
    }

    /**
     * @param data Data to be set as the content of this temporary file.
     */
    public void setdData(byte[] data)
    {
        this.data = data;
    }

    /**
     * {@inheritDoc}
     */
    public boolean exists()
    {
        return data != null;
    }

    /**
     * {@inheritDoc}
     */
    public void spool(OutputContext outputContext) throws IOException
    {
        if (exists()) {
            OutputStream out = outputContext.getOutputStream();
            if (out != null) {
                out.write(this.data);
                out.flush();
            }
        }
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
    public void copy(DavResource destination, boolean shallow) throws DavException
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
    public MultiStatusResponse alterProperties(DavPropertySet setProperties,
        DavPropertyNameSet removePropertyNames) throws DavException
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
        return IOUtil.UNDEFINED_TIME;
    }

    /**
     * {@inheritDoc}
     */
    public String getSupportedMethods()
    {
        return "OPTIONS, GET, HEAD, PROPFIND, LOCK, UNLOCK";
    }
}
