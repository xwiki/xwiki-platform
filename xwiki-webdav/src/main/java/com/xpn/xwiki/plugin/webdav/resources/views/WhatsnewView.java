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
import com.xpn.xwiki.plugin.webdav.resources.domain.DavPage;
import com.xpn.xwiki.plugin.webdav.resources.partial.AbstractDavView;
import com.xpn.xwiki.plugin.webdav.utils.XWikiDavUtils;

/**
 * This view would list the last 20 modified pages.
 * 
 * @version $Id$
 */
public class WhatsnewView extends AbstractDavView
{
    /**
     * Logger instance.
     */
    private static final Logger LOG = LoggerFactory.getLogger(WhatsnewView.class);

    /**
     * {@inheritDoc}
     */
    public void decode(Stack<XWikiDavResource> stack, String[] tokens, int next)
        throws DavException
    {
        if (next < tokens.length) {
            String docName = tokens[next];
            if (docName.indexOf('.') != -1) {
                DavPage page = new DavPage();
                page.init(this, docName, "/" + docName);
                stack.push(page);
                page.decode(stack, tokens, next + 1);
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
        String sql = "where 1=1 order by doc.date desc";
        try {
            List<String> docNames =
                xwikiContext.getWiki().getStore().searchDocumentsNames(sql, 20, 0, xwikiContext);
            for (String docName : docNames) {
                if (XWikiDavUtils.hasAccess("view", docName, xwikiContext)) {
                    DavPage page = new DavPage();
                    page.init(this, docName, "/" + docName);
                    children.add(page);
                }
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
