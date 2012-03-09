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
package com.xpn.xwiki.plugin.webdav.resources.views;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceIteratorImpl;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.property.DavPropertySet;

import com.xpn.xwiki.plugin.webdav.resources.XWikiDavResource;
import com.xpn.xwiki.plugin.webdav.resources.domain.DavPage;
import com.xpn.xwiki.plugin.webdav.resources.partial.AbstractDavView;

/**
 * This view allows to browse the pages starting from Main.WebHome using the parent child relationship.
 * 
 * @version $Id$
 */
public class HomeView extends AbstractDavView
{
    /**
     * {@link DavPage} representing Main.WebHome.
     */
    private DavPage mPage;

    @Override
    public void init(XWikiDavResource parent, String name, String relativePath) throws DavException
    {
        super.init(parent, name, relativePath);
        mPage = new DavPage();
        mPage.init(this, "Main.WebHome", "");
    }

    @Override
    public XWikiDavResource decode(String[] tokens, int next) throws DavException
    {
        String nextToken = tokens[next];
        if (mPage.exists()) {
            return mPage.decode(tokens, next);
        } else if (isTempResource(nextToken)) {
            return super.decode(tokens, next);
        } else {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST);
        }
    }

    @Override
    public long getModificationTime()
    {
        return mPage.exists() ? mPage.getModificationTime() : super.getModificationTime();
    }

    @Override
    public DavPropertySet getProperties()
    {
        return mPage.exists() ? mPage.getProperties() : super.getProperties();
    }

    @Override
    public DavResourceIterator getMembers()
    {
        return mPage.exists() ? mPage.getMembers() : new DavResourceIteratorImpl(getVirtualMembers());
    }

    @Override
    public void addMember(DavResource resource, InputContext inputContext) throws DavException
    {
        if (mPage.exists()) {
            mPage.addMember(resource, inputContext);
        } else {
            super.addVirtualMember(resource, inputContext);
        }
    }

    @Override
    public void removeMember(DavResource member) throws DavException
    {
        if (mPage.exists()) {
            mPage.removeMember(member);
        } else {
            super.removeMember(member);
        }
    }
}
