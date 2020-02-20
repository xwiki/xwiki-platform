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
package com.xpn.xwiki.web;

import java.io.IOException;

import org.xwiki.model.EntityType;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceManager;
import org.xwiki.resource.entity.EntityResourceReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.XWikiPluginManager;

public class DownloadRevAction extends DownloadAction
{
    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        String rev = request.getParameter("rev");
        String filename = getFileName();
        XWikiAttachment attachment;

        if (context.getWiki().hasAttachmentRecycleBin(context) && request.getParameter("rid") != null) {
            int recycleId = Integer.parseInt(request.getParameter("rid"));
            attachment = new XWikiAttachment(doc, filename);
            attachment =
                context.getWiki().getAttachmentRecycleBinStore().restoreFromRecycleBin(attachment, recycleId, context,
                    true);
        } else if (request.getParameter("id") != null) {
            int id = Integer.parseInt(request.getParameter("id"));
            attachment = doc.getAttachmentList().get(id);
        } else {
            attachment = doc.getAttachment(filename);
        }
        if (attachment == null) {
            Object[] args = { filename };
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                XWikiException.ERROR_XWIKI_APP_ATTACHMENT_NOT_FOUND, "Attachment {0} not found", null, args);
        }

        synchronized (attachment) {
            try {
                attachment = attachment.getAttachmentRevision(rev, context);
                if (attachment == null) {
                    throw new XWikiException();
                }
            } catch (XWikiException e) {
                String url = context.getDoc().getURL("viewattachrev", true, context);
                url += "/" + filename;
                if (request.getParameter("rid") != null) {
                    url += "?rid=" + request.getParameter("rid");
                }
                try {
                    context.getResponse().sendRedirect(url);
                    return null;
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }

        XWikiPluginManager plugins = context.getWiki().getPluginManager();
        attachment = plugins.downloadAttachment(attachment, context);

        sendContent(attachment, request, response, context);
        return null;
    }

    /**
     * @return the filename of the attachment.
     */
    private String getFileName()
    {
        // Extract the Attachment file name from the parsed request URL that was done before this Action is called
        ResourceReference resourceReference = Utils.getComponent(ResourceReferenceManager.class).getResourceReference();
        EntityResourceReference entityResource = (EntityResourceReference) resourceReference;
        return entityResource.getEntityReference().extractReference(EntityType.ATTACHMENT).getName();
    }
}
