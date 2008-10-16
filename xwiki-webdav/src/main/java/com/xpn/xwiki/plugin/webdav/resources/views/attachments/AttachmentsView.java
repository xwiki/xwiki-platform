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

/**
 * This view list all documents containing attachments.
 * 
 * @version $Id$
 */
public class AttachmentsView extends AbstractDavView
{       
    /**
     * Logger instance.
     */
    private static final Logger LOG = LoggerFactory.getLogger(AttachmentsView.class);

    /**
     * {@inheritDoc}
     */
    public void decode(Stack<XWikiDavResource> stack, String[] tokens, int next) throws DavException
    {
        if (next < tokens.length) {
            String spaceName = tokens[next];
            AttachmentsBySpaceNameSubView subView = new AttachmentsBySpaceNameSubView();
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
            List<String> docNames =
                xwikiContext.getWiki().getStore()
                    .searchDocumentsNames(
                        ", XWikiAttachment as attach where doc.id = attach.docId", 0, 0,
                        xwikiContext);
            Set<String> spacesWithAttachments = new HashSet<String>();
            for (String docName : docNames) {
                int dot = docName.lastIndexOf('.');
                if (dot != -1) {
                    spacesWithAttachments.add(docName.substring(0, dot));
                }
            }
            for (String spaceName : spacesWithAttachments) {
                AttachmentsBySpaceNameSubView subView =
                    new AttachmentsBySpaceNameSubView();
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
