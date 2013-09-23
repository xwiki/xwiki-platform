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

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.io.InputContext;

import com.xpn.xwiki.plugin.webdav.resources.XWikiDavResource;


/**
 * The DAV resource representing 'file' type entities of XWiki.
 * 
 * @version $Id$
 */
public abstract class AbstractDavFile extends AbstractDavResource
{
    @Override
    public XWikiDavResource decode(String[] tokens, int next) throws DavException
    {
        throw new DavException(DavServletResponse.SC_BAD_REQUEST);
    }

    @Override
    public void addMember(DavResource resource, InputContext inputContext) throws DavException
    {
        throw new DavException(DavServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @Override
    public void removeMember(DavResource member) throws DavException
    {
        throw new DavException(DavServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @Override
    public DavResourceIterator getMembers()
    {
        return null;
    }

    @Override
    public boolean isCollection()
    {
        return false;
    }
}
