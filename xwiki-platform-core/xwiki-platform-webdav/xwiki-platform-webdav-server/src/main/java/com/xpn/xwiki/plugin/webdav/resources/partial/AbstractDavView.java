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
package com.xpn.xwiki.plugin.webdav.resources.partial;

import java.io.IOException;

import org.apache.jackrabbit.server.io.IOUtil;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.io.OutputContext;

import com.xpn.xwiki.plugin.webdav.resources.domain.DavTempFile;


/**
 * A view represents a collection of webdav resources, usually a view is a logical grouping of
 * resources (pages, spaces etc.).
 * 
 * @version $Id$
 */
public abstract class AbstractDavView extends AbstractDavResource
{
    @Override
    public boolean exists()
    {
        return true;
    }

    @Override
    public long getModificationTime()
    {
        return IOUtil.UNDEFINED_TIME;
    }

    @Override
    public boolean isCollection()
    {
        return true;
    }

    @Override
    public void move(DavResource destination) throws DavException
    {
        throw new DavException(DavServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @Override
    public void spool(OutputContext outputContext) throws IOException
    {
        throw new IOException("Views cannot be spooled.");
    }
    
    @Override
    public void addMember(DavResource resource, InputContext inputContext) throws DavException
    {
        if (resource instanceof DavTempFile) {
            addVirtualMember(resource, inputContext);
        } else {
            throw new DavException(DavServletResponse.SC_FORBIDDEN);
        }
    }

    @Override
    public void removeMember(DavResource member) throws DavException
    {
        if (member instanceof DavTempFile) {
            removeVirtualMember(member);
        } else {
            throw new DavException(DavServletResponse.SC_FORBIDDEN);
        }
    }
}
