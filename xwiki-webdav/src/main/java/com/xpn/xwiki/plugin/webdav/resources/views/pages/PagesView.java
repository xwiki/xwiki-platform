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
package com.xpn.xwiki.plugin.webdav.resources.views.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceIteratorImpl;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.webdav.resources.XWikiDavResource;
import com.xpn.xwiki.plugin.webdav.resources.partial.AbstractDavView;
import com.xpn.xwiki.plugin.webdav.utils.XWikiDavUtils;

/**
 * This view lists all the documents organized by space.
 * 
 * @version $Id$
 */
public class PagesView extends AbstractDavView
{
    /**
     * Logger instance.
     */
    private static final Logger LOG = LoggerFactory.getLogger(PagesView.class);

    /**
     * {@inheritDoc}
     */
    public void decode(Stack<XWikiDavResource> stack, String[] tokens, int next)
        throws DavException
    {
        if (next < tokens.length) {
            String spaceName = tokens[next];
            PagesBySpaceNameSubView subView = new PagesBySpaceNameSubView();
            subView.init(this, spaceName, "/" + spaceName);
            stack.push(subView);
            subView.decode(stack, tokens, next + 1);
        }
    }

    /**
     * {@inheritDoc}
     */
    public DavResourceIterator getMembers()
    {
        List<DavResource> children = new ArrayList<DavResource>();
        try {
            List<String> spaceNames = xwikiContext.getWiki().getSpaces(xwikiContext);
            for (String spaceName : spaceNames) {
                PagesBySpaceNameSubView subView = new PagesBySpaceNameSubView();
                subView.init(this, spaceName, "/" + spaceName);
                children.add(subView);
            }
        } catch (XWikiException e) {
            LOG.error("Unexpected Error : ", e);
        } catch (DavException e) {
            LOG.error("Unexpected Error : ", e);
        }
        return new DavResourceIteratorImpl(children);
    }

    /**
     * {@inheritDoc}
     */
    public void addMember(DavResource resource, InputContext inputContext) throws DavException
    {
        PagesBySpaceNameSubView space = (PagesBySpaceNameSubView) resource;
        String homePage = space.getDisplayName() + ".WebHome";
        XWikiDavUtils.checkAccess("edit", homePage, xwikiContext);
        try {
            XWikiDocument doc =
                xwikiContext.getWiki().getDocument(space.getDisplayName() + ".WebHome",
                    xwikiContext);
            doc.setContent("This page was created thorugh xwiki-webdav interface.");
            xwikiContext.getWiki().saveDocument(doc, xwikiContext);
        } catch (XWikiException e) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeMember(DavResource member) throws DavException
    {
        PagesBySpaceNameSubView space = (PagesBySpaceNameSubView) member;
        try {
            List<String> docNames =
                xwikiContext.getWiki().getStore().searchDocumentsNames(
                    "where doc.web='" + space.getDisplayName() + "'", 0, 0, xwikiContext);
            // Check if the user has delete rights on all child pages.
            for (String docName : docNames) {
                XWikiDavUtils.checkAccess("delete", docName, xwikiContext);
            }
            for (String docName : docNames) {
                XWikiDocument doc = xwikiContext.getWiki().getDocument(docName, xwikiContext);
                xwikiContext.getWiki().deleteDocument(doc, xwikiContext);
            }
        } catch (XWikiException e) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getDisplayName()
    {
        return this.name;
    }

    /**
     * {@inheritDoc}
     */
    public String getSupportedMethods()
    {
        return "OPTIONS, GET, HEAD, POST, PROPFIND, MKCOL, PUT, LOCK, UNLOCK";
    }
}
