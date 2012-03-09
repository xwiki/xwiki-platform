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

import java.util.ArrayList;
import java.util.List;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceIteratorImpl;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.plugin.webdav.resources.XWikiDavResource;
import com.xpn.xwiki.plugin.webdav.resources.partial.AbstractDavView;
import com.xpn.xwiki.plugin.webdav.resources.views.attachments.AttachmentsView;
import com.xpn.xwiki.plugin.webdav.resources.views.pages.PagesView;
import com.xpn.xwiki.plugin.webdav.utils.XWikiDavUtils.BaseViews;

/**
 * The root of all views (entry point).
 * 
 * @version $Id$
 */
public class RootView extends AbstractDavView
{
    /**
     * Logger instance.
     */
    private static final Logger logger = LoggerFactory.getLogger(RootView.class);

    @Override
    public XWikiDavResource decode(String[] tokens, int next) throws DavException
    {
        String nextToken = tokens[next];
        boolean last = (next == tokens.length - 1);
        XWikiDavResource resource = null;
        if (isTempResource(nextToken)) {
            return super.decode(tokens, next);
        } else if (last && getContext().isCreateOrMoveRequest()) {
            throw new DavException(DavServletResponse.SC_METHOD_NOT_ALLOWED);
        } else if (nextToken.equals(BaseViews.HOME)) {
            resource = new HomeView();
        } else if (nextToken.equals(BaseViews.PAGES)) {
            resource = new PagesView();
        } else if (nextToken.equals(BaseViews.ORPHANS)) {
            resource = new OrphansView();
        } else if (nextToken.equals(BaseViews.WHATSNEW)) {
            resource = new WhatsnewView();
        } else if (nextToken.equals(BaseViews.ATTACHMENTS)) {
            resource = new AttachmentsView();
        } else {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST);
        }
        resource.init(this, nextToken, "/" + nextToken);
        return last ? resource : resource.decode(tokens, next + 1);        
    }

    @Override
    public DavResourceIterator getMembers()
    {
        List<DavResource> children = new ArrayList<DavResource>();
        try {
            XWikiDavResource homeView = new HomeView();
            homeView.init(this, "home", "/home");
            children.add(homeView);
            XWikiDavResource pagesView = new PagesView();
            pagesView.init(this, "spaces", "/spaces");
            children.add(pagesView);
            XWikiDavResource attachmentsView = new AttachmentsView();
            attachmentsView.init(this, "attachments", "/attachments");
            children.add(attachmentsView);
            XWikiDavResource orphansView = new OrphansView();
            orphansView.init(this, "orphans", "/orphans");
            children.add(orphansView);
            XWikiDavResource whatsnewView = new WhatsnewView();
            whatsnewView.init(this, "whatsnew", "/whatsnew");
            children.add(whatsnewView);
        } catch (DavException e) {
            logger.error("Unexpected Error : ", e);
        }
        children.addAll(getVirtualMembers());
        return new DavResourceIteratorImpl(children);
    }
}
