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
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Composable;

import com.xpn.xwiki.plugin.webdav.resources.XWikiDavResource;
import com.xpn.xwiki.plugin.webdav.resources.partial.AbstractDavView;
import com.xpn.xwiki.plugin.webdav.utils.XWikiDavUtils.ResourceHint;

/**
 * The root of all views (entry point).
 * 
 * @version $Id$
 */
public class RootView extends AbstractDavView implements Composable
{
    /**
     * Logger instance.
     */
    private static final Logger LOG = LoggerFactory.getLogger(RootView.class);

    /**
     * Plexus component manager.
     */
    private ComponentManager componentManager;

    /**
     * {@inheritDoc}
     */
    public void compose(ComponentManager componentManager)
    {
        this.componentManager = componentManager;
    }

    /**
     * Starts the process of decoding a url.
     * 
     * @param locator The locator which holds the url.
     * @return The resource which corresponds to the whole url.
     * @throws DavException If the url cannot be decoded.
     */
    public XWikiDavResource decode(DavResourceLocator locator) throws DavException
    {
        String workspacePath = locator.getWorkspacePath();
        if (workspacePath == null) {
            return this;
        } else if (workspacePath.equals(BASE_URI)) {
            Stack<XWikiDavResource> stack = new Stack<XWikiDavResource>();
            stack.push(this);
            decode(stack, locator.getResourcePath().split("/"), 2);
            return stack.pop();
        } else {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void decode(Stack<XWikiDavResource> stack, String[] tokens, int next) throws DavException
    {
        if (next < tokens.length) {
            String nextToken = tokens[next];
            XWikiDavResource resource = null;
            try {
                resource =
                    (XWikiDavResource) componentManager.lookup(ROLE, nextToken + "-baseview");
                resource.init(this, nextToken, "/" + nextToken);
                stack.push(resource);
                resource.decode(stack, tokens, next + 1);
            } catch (ComponentLookupException e) {
                throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e);
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
            XWikiDavResource homeView =
                (XWikiDavResource) componentManager.lookup(ROLE, ResourceHint.HOME);
            homeView.init(this, "home", "/home");
            children.add(homeView);
            XWikiDavResource pagesView =
                (XWikiDavResource) componentManager.lookup(ROLE, ResourceHint.PAGES);
            pagesView.init(this, "pages", "/pages");
            children.add(pagesView);
            XWikiDavResource attachmentsView =
                (XWikiDavResource) componentManager.lookup(ROLE, ResourceHint.ATTACHMENTS);
            attachmentsView.init(this, "attachments", "/attachments");
            children.add(attachmentsView);
            XWikiDavResource orphansView =
                (XWikiDavResource) componentManager.lookup(ROLE, ResourceHint.ORPHANS);
            orphansView.init(this, "orphans", "/orphans");
            children.add(orphansView);
            XWikiDavResource whatsnewView =
                (XWikiDavResource) componentManager.lookup(ROLE, ResourceHint.WHATSNEW);
            whatsnewView.init(this, "whatsnew", "/whatsnew");
            children.add(whatsnewView);
        } catch (ComponentLookupException e) {
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
