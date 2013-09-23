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

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceIteratorImpl;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(AttachmentsView.class);

    @Override
    public XWikiDavResource decode(String[] tokens, int next) throws DavException
    {
        String nextToken = tokens[next];
        boolean last = (next == tokens.length - 1);
        if (isTempResource(nextToken)) {
            return super.decode(tokens, next);
        } else if (getContext().getSpaces().contains(nextToken) && !(last && getContext().isCreateOrMoveRequest())) {
            AttachmentsBySpaceNameSubView subView = new AttachmentsBySpaceNameSubView();
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
            String sql = ", XWikiAttachment as attach where doc.id = attach.docId";
            List<String> docNames = getContext().searchDocumentsNames(sql);
            Set<String> spacesWithAttachments = new HashSet<String>();
            for (String docName : docNames) {
                if (getContext().hasAccess("view", docName)) {
                    int dot = docName.lastIndexOf('.');
                    if (dot != -1) {
                        spacesWithAttachments.add(docName.substring(0, dot));
                    }
                }
            }
            for (String spaceName : spacesWithAttachments) {
                AttachmentsBySpaceNameSubView subView = new AttachmentsBySpaceNameSubView();
                subView.init(this, spaceName, "/" + spaceName);
                children.add(subView);
            }
        } catch (DavException e) {
            logger.error("Unexpected Error : ", e);
        }
        children.addAll(getVirtualMembers());
        return new DavResourceIteratorImpl(children);
    }
}
