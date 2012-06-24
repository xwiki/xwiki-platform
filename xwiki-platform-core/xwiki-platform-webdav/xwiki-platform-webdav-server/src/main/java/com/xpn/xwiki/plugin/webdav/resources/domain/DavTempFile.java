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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.jackrabbit.server.io.IOUtil;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceIteratorImpl;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;

import com.xpn.xwiki.plugin.webdav.resources.XWikiDavResource;
import com.xpn.xwiki.plugin.webdav.resources.partial.AbstractDavResource;

/**
 * Resource used to represent temporary resources demanded by various dav clients.
 * 
 * @version $Id$
 */
public class DavTempFile extends AbstractDavResource
{
    /**
     * Flag indicating whether this resource is a collection.
     */
    private boolean isCollection;

    /**
     * Content of this resource (file).
     */
    private byte[] data;

    /**
     * Indicates if this resource has been created or not. Here creation means if the resource has been actually PUT /
     * MKCOL by the client as opposed to being initialized. This flag will be set to true once setModified() has been
     * invoked for the first time.
     */
    private boolean created;

    /**
     * Created on.
     */
    private Date timeOfCreation;

    /**
     * Last modified.
     */
    private Date timeOfLastModification;

    /**
     * Default constructor.
     */
    public DavTempFile()
    {
        timeOfCreation = new Date(IOUtil.UNDEFINED_TIME);
        timeOfLastModification = (Date) timeOfCreation.clone();
    }

    @Override
    public void init(XWikiDavResource parent, String name, String relativePath) throws DavException
    {
        super.init(parent, name, relativePath);
        String strTimeOfCreation = DavConstants.creationDateFormat.format(timeOfCreation);
        getProperties().add(new DefaultDavProperty(DavPropertyName.CREATIONDATE, strTimeOfCreation));
        String strTimeOfModification = DavConstants.modificationDateFormat.format(timeOfLastModification);
        getProperties().add(new DefaultDavProperty(DavPropertyName.GETLASTMODIFIED, strTimeOfModification));
        getProperties().add(new DefaultDavProperty(DavPropertyName.GETETAG, strTimeOfModification));
        getProperties().add(new DefaultDavProperty(DavPropertyName.GETCONTENTLANGUAGE, "en"));
        String contentType = isCollection() ? "text/directory" : "application/octet-stream";
        getProperties().add(new DefaultDavProperty(DavPropertyName.GETCONTENTTYPE, contentType));
        int contentLength = (data != null) ? data.length : 0;
        getProperties().add(new DefaultDavProperty(DavPropertyName.GETCONTENTLENGTH, contentLength));
    }

    @Override
    public XWikiDavResource decode(String[] tokens, int next) throws DavException
    {
        return super.decode(tokens, next);
    }

    @Override
    public boolean exists()
    {
        return parentResource.getVirtualMembers().contains(this);
    }

    @Override
    public void spool(OutputContext outputContext) throws IOException
    {
        outputContext.setContentLanguage("en");
        outputContext.setContentLength(data != null ? data.length : 0);
        outputContext.setContentType(isCollection() ? "text/directory" : "application/octet-stream");
        outputContext.setETag(DavConstants.modificationDateFormat.format(getModificationTime()));
        outputContext.setModificationTime(getModificationTime());
        if (exists() && !isCollection()) {
            OutputStream out = outputContext.getOutputStream();
            if (out != null) {
                out.write(this.data);
                out.flush();
            }
        }
    }

    @Override
    public DavResourceIterator getMembers()
    {
        List<DavResource> children = new ArrayList<DavResource>();
        children.addAll(getVirtualMembers());
        return new DavResourceIteratorImpl(children);
    }

    @Override
    public void addMember(DavResource resource, InputContext inputContext) throws DavException
    {
        if (resource instanceof DavTempFile) {
            addVirtualMember(resource, inputContext);
        } else {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void removeMember(DavResource resource) throws DavException
    {
        if (resource instanceof DavTempFile) {
            removeVirtualMember(resource);
        } else {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void move(DavResource destination) throws DavException
    {
        if (destination instanceof DavTempFile && destination.getCollection().equals(getCollection())
            && !destination.isCollection() && !isCollection()) {
            // A file rename operation
            DavTempFile destTempFile = (DavTempFile) destination;
            parentResource.getVirtualMembers().remove(this);
            parentResource.getVirtualMembers().add(destTempFile);
            destTempFile.update(this.data.clone(), this.timeOfLastModification);
        } else {
            throw new DavException(DavServletResponse.SC_FORBIDDEN);
        }
        clearCache();
    }

    /**
     * @param data Data to be set as the content of this temporary file.
     * @param modificationTime Time of modification.
     */
    public void update(byte[] data, Date modificationTime)
    {
        this.data = data.clone();
        setModified(modificationTime);
        getProperties().add(new DefaultDavProperty(DavPropertyName.GETCONTENTLENGTH, this.data.length));
    }

    /**
     * Changes the time of modification of this resource.
     * 
     * @param modificationTime Time of modification.
     */
    public void setModified(Date modificationTime)
    {
        if (!created) {
            timeOfCreation = (Date) modificationTime.clone();
            String strTimeOfCreation = DavConstants.creationDateFormat.format(timeOfCreation);
            getProperties().add(new DefaultDavProperty(DavPropertyName.CREATIONDATE, strTimeOfCreation));
            created = true;
        }
        timeOfLastModification = (Date) modificationTime.clone();
        String strTimeOfModification = DavConstants.modificationDateFormat.format(timeOfLastModification);
        getProperties().add(new DefaultDavProperty(DavPropertyName.GETLASTMODIFIED, strTimeOfModification));
        getProperties().add(new DefaultDavProperty(DavPropertyName.GETETAG, String.valueOf(timeOfLastModification)));
    }

    /**
     * Sets the isCollection flag to true.
     */
    public void setCollection()
    {
        this.isCollection = true;
    }

    @Override
    public boolean isCollection()
    {
        return isCollection;
    }

    @Override
    public long getModificationTime()
    {
        return timeOfLastModification.getTime();
    }
}
