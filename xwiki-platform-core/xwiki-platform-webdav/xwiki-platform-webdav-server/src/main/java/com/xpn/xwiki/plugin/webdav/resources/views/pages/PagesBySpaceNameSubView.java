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
    private static final Logger logger = LoggerFactory.getLogger(PagesBySpaceNameSubView.class);

    @Override
    public XWikiDavResource decode(String[] tokens, int next) throws DavException
    {
        String nextToken = tokens[next];
        boolean last = (next == tokens.length - 1);
        XWikiDavResource resource = null;
        if (isTempResource(nextToken)) {
            return super.decode(tokens, next);
        } else if ((nextToken.startsWith(XWikiDavUtils.VIRTUAL_DIRECTORY_PREFIX) && nextToken
            .endsWith(XWikiDavUtils.VIRTUAL_DIRECTORY_POSTFIX))
            && !(last && getContext().isCreateOrMoveRequest())) {
            resource = new PagesByFirstLettersSubView();
            resource.init(this, nextToken.toUpperCase(), "/" + nextToken.toUpperCase());
        } else if (getContext().isCreateCollectionRequest() || getContext().exists(this.name + "." + nextToken)) {
            resource = new DavPage();
            resource.init(this, this.name + "." + nextToken, "/" + nextToken);
        } else if (nextToken.startsWith(this.name + ".") && getContext().exists(nextToken)) {
            // For compatibility with FoXWiki
            resource = new DavPage();
            resource.init(this, nextToken, "/" + nextToken);
        } else {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST);
        }
        return last ? resource : resource.decode(tokens, next + 1);
    }

    @Override
    public boolean exists()
    {
        try {
            List<String> spaces = getContext().getSpaces();
            if (spaces.contains(name)) {
                return true;
            }
        } catch (DavException ex) {
            logger.error("Unexpected Error : ", ex);
        }
        return false;
    }

    @Override
    public DavResourceIterator getMembers()
    {
        List<DavResource> children = new ArrayList<DavResource>();
        try {
            String sql = "where doc.web='" + this.name + "'";
            List<String> docNames = getContext().searchDocumentsNames(sql);
            Set<String> subViewNames = new HashSet<String>();
            int subViewNameLength = XWikiDavUtils.getSubViewNameLength(docNames.size());
            for (String docName : docNames) {
                if (getContext().hasAccess("view", docName)) {
                    int dot = docName.lastIndexOf('.');
                    String pageName = docName.substring(dot + 1);
                    if (subViewNameLength < pageName.length()) {
                        subViewNames.add(pageName.substring(0, subViewNameLength).toUpperCase());
                    } else {
                        // This is not good.
                        subViewNames.add(pageName.toUpperCase());
                    }
                }
            }
            for (String subViewName : subViewNames) {
                try {
                    String modName =
                        XWikiDavUtils.VIRTUAL_DIRECTORY_PREFIX + subViewName + XWikiDavUtils.VIRTUAL_DIRECTORY_POSTFIX;
                    PagesByFirstLettersSubView subView = new PagesByFirstLettersSubView();
                    subView.init(this, modName, "/" + modName);
                    children.add(subView);
                } catch (DavException e) {
                    logger.error("Unexpected Error : ", e);
                }
            }
        } catch (DavException ex) {
            logger.error("Unexpected Error : ", ex);
        }
        children.addAll(getVirtualMembers());
        return new DavResourceIteratorImpl(children);
    }

    @Override
    public void addMember(DavResource resource, InputContext inputContext) throws DavException
    {
        if (resource instanceof DavPage) {
            String pName = resource.getDisplayName();
            if (getContext().hasAccess("edit", pName)) {
                XWikiDocument childDoc = getContext().getDocument(pName);
                childDoc.setContent("This page was created through the WebDAV interface.");
                getContext().saveDocument(childDoc);
            }
        } else {
            super.addMember(resource, inputContext);
        }
    }

    @Override
    public void removeMember(DavResource member) throws DavException
    {
        XWikiDavResource davResource = (XWikiDavResource) member;
        if (davResource instanceof DavPage) {
            String pName = davResource.getDisplayName();
            getContext().checkAccess("delete", pName);
            XWikiDocument childDoc = getContext().getDocument(pName);
            if (!childDoc.isNew()) {
                getContext().deleteDocument(childDoc);
            }
        } else if (member instanceof PagesByFirstLettersSubView) {
            // We are going to force a recursive delete.
            String filter =
                member.getDisplayName().substring(XWikiDavUtils.VIRTUAL_DIRECTORY_PREFIX.length(),
                    member.getDisplayName().length() - XWikiDavUtils.VIRTUAL_DIRECTORY_POSTFIX.length());
            String sql = "where doc.web='" + this.name + "'";
            List<String> docNames = getContext().searchDocumentsNames(sql);
            List<String> filteredDocNames = new ArrayList<String>();
            for (String docName : docNames) {
                if (docName.toUpperCase().startsWith(filter)) {
                    filteredDocNames.add(docName);
                }
            }
            // Verify delete rights on all the documents to be removed.
            for (String docName : filteredDocNames) {
                getContext().checkAccess("delete", docName);
            }
            // Delete the documents.
            for (String docName : filteredDocNames) {
                getContext().deleteDocument(getContext().getDocument(docName));
            }
        } else {
            super.removeMember(member);
        }
        davResource.clearCache();
    }

    @Override
    public void move(DavResource destination) throws DavException
    {
        // We only support rename operation for the moment.
        if (destination instanceof PagesBySpaceNameSubView) {
            PagesBySpaceNameSubView dSpace = (PagesBySpaceNameSubView) destination;
            if (!dSpace.exists()) {
                // Now check whether this is a rename operation.
                if (getCollection().equals(dSpace.getCollection())) {
                    String sql = "where doc.web='" + this.name + "'";
                    List<String> docNames = getContext().searchDocumentsNames(sql);
                    // To rename an entire space, user should have edit rights on all the
                    // documents in the current space and delete rights on all the documents that
                    // will be replaced (if they exist).
                    for (String docName : docNames) {
                        String newDocName = dSpace.getDisplayName() + "." + docName;
                        getContext().checkAccess("edit", docName);
                        getContext().checkAccess("overwrite", newDocName);
                    }
                    for (String docName : docNames) {
                        XWikiDocument doc = getContext().getDocument(docName);
                        String newDocName = dSpace.getDisplayName() + "." + doc.getName();
                        getContext().renameDocument(doc, newDocName);
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
        clearCache();
    }
}
