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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import com.xpn.xwiki.plugin.webdav.resources.domain.DavPage;
import com.xpn.xwiki.plugin.webdav.resources.partial.AbstractDavView;
import com.xpn.xwiki.plugin.webdav.utils.XWikiDavUtils;

/**
 * This view groups all pages according to their space name.
 * 
 * @version $Id$
 */
public class PagesBySpaceNameSubView extends AbstractDavView
{   
    /**
     * Logger instance.
     */
    private static final Logger LOG = LoggerFactory.getLogger(PagesBySpaceNameSubView.class);

    /**
     * {@inheritDoc}
     */
    public void decode(Stack<XWikiDavResource> stack, String[] tokens, int next) throws DavException
    {
        if (next < tokens.length) {
            String token = tokens[next];
            // First check if this is an indirect access request,
            // virtual groupings start with an "_" and end with an "_".
            if (token.startsWith(XWikiDavUtils.VIRTUAL_DIRECTORY_PREFIX)
                && token.endsWith(XWikiDavUtils.VIRTUAL_DIRECTORY_POSTFIX)) {
                PagesByFirstLettersSubView subView = new PagesByFirstLettersSubView();
                subView.init(this, token.toUpperCase(), "/" + token.toUpperCase());
                stack.push(subView);
                subView.decode(stack, tokens, next + 1);
            } else {
                // This has to be a page name (Direct access).
                DavPage page = new DavPage();
                page.init(this, this.name + "." + token, "/" + token);
                stack.push(page);
                page.decode(stack, tokens, next + 1);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean exists()
    {
        try {
            List<String> spaces = xwikiContext.getWiki().getSpaces(xwikiContext);
            if (spaces.contains(name)) {
                return true;
            }
        } catch (XWikiException e) {
            LOG.error("Unexpected Error : ", e);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public DavResourceIterator getMembers()
    {
        List<DavResource> children = new ArrayList<DavResource>();
        try {
            List<String> docNames =
                xwikiContext.getWiki().getStore().searchDocumentsNames(
                    "where doc.web='" + this.name + "'", 0, 0, xwikiContext);
            Set<String> subViewNames = new HashSet<String>();
            int subViewNameLength = XWikiDavUtils.getSubViewNameLength(docNames.size());
            for (String docName : docNames) {
                int dot = docName.lastIndexOf('.');
                String pageName = docName.substring(dot + 1);
                if (subViewNameLength < pageName.length()) {
                    subViewNames.add(pageName.substring(0, subViewNameLength).toUpperCase());
                } else {
                    // This is not good.
                    subViewNames.add(pageName.toUpperCase());
                }
            }
            for (String subViewName : subViewNames) {
                try {
                    String modName =
                        XWikiDavUtils.VIRTUAL_DIRECTORY_PREFIX + subViewName
                            + XWikiDavUtils.VIRTUAL_DIRECTORY_POSTFIX;
                    PagesByFirstLettersSubView subView =
                        new PagesByFirstLettersSubView();
                    subView.init(this, modName, "/" + modName);
                    children.add(subView);
                } catch (DavException e) {
                    LOG.error("Unexpected Error : ", e);
                }
            }
        } catch (XWikiException e) {
            LOG.error("Unexpected Error : ", e);
        }
        return new DavResourceIteratorImpl(children);
    }

    /**
     * {@inheritDoc}
     */
    public void addMember(DavResource resource, InputContext inputContext) throws DavException
    {
        if (resource instanceof DavPage) {
            String pName = ((DavPage) resource).getDisplayName();
            try {
                XWikiDocument childDoc = xwikiContext.getWiki().getDocument(pName, xwikiContext);
                childDoc.setContent("This page was created thorugh xwiki-webdav interface.");
                xwikiContext.getWiki().saveDocument(childDoc, xwikiContext);
            } catch (XWikiException e) {
                throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e);
            }
        } else {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeMember(DavResource member) throws DavException
    {
        if (member instanceof DavPage) {
            String pName = ((DavPage) member).getDisplayName();
            try {
                XWikiDocument childDoc = xwikiContext.getWiki().getDocument(pName, xwikiContext);
                if (!childDoc.isNew()) {
                    xwikiContext.getWiki().deleteDocument(childDoc, xwikiContext);
                }
            } catch (XWikiException e) {
                throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e);
            }
        } else {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void move(DavResource destination) throws DavException
    {
        // TODO : Need to check appropriate rights.
        // We only support rename operation for the moment.
        if (destination instanceof PagesBySpaceNameSubView) {
            PagesBySpaceNameSubView dSpace = (PagesBySpaceNameSubView) destination;
            if (!dSpace.exists()) {
                // Now check whether this is a rename operation.
                if (getCollection().equals(dSpace.getCollection())) {
                    try {
                        List<String> docNames =
                            xwikiContext.getWiki().getStore().searchDocumentsNames(
                                "where doc.web='" + this.name + "'", 0, 0, xwikiContext);
                        for (String docName : docNames) {
                            XWikiDocument doc =
                                xwikiContext.getWiki().getDocument(docName, xwikiContext);
                            String newDocName = dSpace.getDisplayName() + "." + doc.getName();
                            doc.rename(newDocName, xwikiContext);
                        }
                    } catch (XWikiException e) {
                        throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e);
                    }
                } else {
                    // Actual moves (perhaps from one view to another) is not
                    // allowed.
                    throw new DavException(DavServletResponse.SC_BAD_REQUEST);
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
    public String getDisplayName()
    {
        return this.name;
    }

    /**
     * {@inheritDoc}
     */
    public String getSupportedMethods()
    {
        return "OPTIONS, GET, HEAD, POST, PROPFIND, PROPPATCH, MKCOL, COPY, PUT, DELETE, MOVE, LOCK, UNLOCK";
    }
}
