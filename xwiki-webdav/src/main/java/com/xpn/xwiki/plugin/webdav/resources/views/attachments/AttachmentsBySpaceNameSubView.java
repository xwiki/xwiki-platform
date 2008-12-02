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
package com.xpn.xwiki.plugin.webdav.resources.views.attachments;

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
import com.xpn.xwiki.plugin.webdav.resources.XWikiDavResource;
import com.xpn.xwiki.plugin.webdav.resources.partial.AbstractDavView;
import com.xpn.xwiki.plugin.webdav.utils.XWikiDavUtils;

/**
 * This view groups all pages having attachments according to their space name.
 * 
 * @version $Id$
 */
public class AttachmentsBySpaceNameSubView extends AbstractDavView
{
    /**
     * Logger instance.
     */
    private static final Logger LOG =
        LoggerFactory.getLogger(AttachmentsBySpaceNameSubView.class);

    /**
     * {@inheritDoc}
     */
    public void decode(Stack<XWikiDavResource> stack, String[] tokens, int next)
        throws DavException
    {
        if (next < tokens.length) {
            String token = tokens[next];
            if (token.startsWith(XWikiDavUtils.VIRTUAL_DIRECTORY_PREFIX)
                && token.endsWith(XWikiDavUtils.VIRTUAL_DIRECTORY_POSTFIX)) {
                AttachmentsByFirstLettersSubView subView = new AttachmentsByFirstLettersSubView();
                subView.init(this, token.toUpperCase(), "/" + token.toUpperCase());
                stack.push(subView);
                subView.decode(stack, tokens, next + 1);
            } else {
                throw new DavException(DavServletResponse.SC_BAD_REQUEST);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public DavResourceIterator getMembers()
    {
        List<DavResource> children = new ArrayList<DavResource>();
        try {
            String sql =
                ", XWikiAttachment as attach where doc.id = attach.docId and doc.web = '"
                    + getDisplayName() + "'";
            List<String> docNames =
                xwikiContext.getWiki().getStore().searchDocumentsNames(sql, 0, 0, xwikiContext);
            Set<String> subViewNames = new HashSet<String>();
            int subViewNameLength = XWikiDavUtils.getSubViewNameLength(docNames.size());
            for (String docName : docNames) {
                if (XWikiDavUtils.hasAccess("view", docName, xwikiContext)) {
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
                        XWikiDavUtils.VIRTUAL_DIRECTORY_PREFIX + subViewName
                            + XWikiDavUtils.VIRTUAL_DIRECTORY_POSTFIX;
                    AttachmentsByFirstLettersSubView subView =
                        new AttachmentsByFirstLettersSubView();
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
    public void addMember(DavResource member, InputContext inputContext) throws DavException
    {
        throw new DavException(DavServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    /**
     * {@inheritDoc}
     */
    public void removeMember(DavResource member) throws DavException
    {
        throw new DavException(DavServletResponse.SC_METHOD_NOT_ALLOWED);
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
        return "OPTIONS, GET, HEAD, PROPFIND, LOCK, UNLOCK";
    }
}
