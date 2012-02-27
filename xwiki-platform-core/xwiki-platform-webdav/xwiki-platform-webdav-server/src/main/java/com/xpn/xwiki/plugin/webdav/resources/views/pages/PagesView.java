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

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceIteratorImpl;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.webdav.resources.XWikiDavResource;
import com.xpn.xwiki.plugin.webdav.resources.partial.AbstractDavView;

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
    private static final Logger logger = LoggerFactory.getLogger(PagesView.class);

    @Override
    public XWikiDavResource decode(String[] tokens, int next) throws DavException
    {
        String nextToken = tokens[next];
        boolean last = (next == tokens.length - 1);
        if (isTempResource(nextToken)) {
            return super.decode(tokens, next);
        } else if (!(last && getContext().isCreateFileRequest())) {
            PagesBySpaceNameSubView subView = new PagesBySpaceNameSubView();
            subView.init(this, nextToken, "/" + nextToken);
            return last ? subView : subView.decode(tokens, next + 1);
        } else {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST);
        }
    }

    @Override
    public DavResourceIterator getMembers()
    {
        List<DavResource> children = new ArrayList<DavResource>();
        try {
            List<String> spaceNames = getContext().getSpaces();
            for (String spaceName : spaceNames) {
                PagesBySpaceNameSubView subView = new PagesBySpaceNameSubView();
                subView.init(this, spaceName, "/" + spaceName);
                children.add(subView);
            }
        } catch (DavException e) {
            logger.error("Unexpected Error : ", e);
        }
        children.addAll(getVirtualMembers());
        return new DavResourceIteratorImpl(children);
    }

    @Override
    public void addMember(DavResource resource, InputContext inputContext) throws DavException
    {
        if (resource instanceof PagesBySpaceNameSubView) {
            String homePage = resource.getDisplayName() + ".WebHome";
            getContext().checkAccess("edit", homePage);
            XWikiDocument doc = getContext().getDocument(resource.getDisplayName() + ".WebHome");
            doc.setContent("This page was created through the WebDAV interface.");
            getContext().saveDocument(doc);
        } else {
            super.addMember(resource, inputContext);
        }
    }

    @Override
    public void removeMember(DavResource member) throws DavException
    {
        XWikiDavResource davResource = (XWikiDavResource) member;
        if (davResource instanceof PagesBySpaceNameSubView) {
            PagesBySpaceNameSubView space = (PagesBySpaceNameSubView) davResource;
            String sql = "where doc.web='" + space.getDisplayName() + "'";
            List<String> docNames = getContext().searchDocumentsNames(sql);
            // Check if the user has delete rights on all child pages.
            for (String docName : docNames) {
                getContext().checkAccess("delete", docName);
            }
            for (String docName : docNames) {
                XWikiDocument doc = getContext().getDocument(docName);
                getContext().deleteDocument(doc);
            }
        } else {
            super.removeMember(member);
        }
        davResource.clearCache();
    }
}
