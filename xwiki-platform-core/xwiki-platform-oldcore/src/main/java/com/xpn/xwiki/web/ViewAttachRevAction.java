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

import org.apache.velocity.VelocityContext;
import org.xwiki.model.EntityType;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceManager;
import org.xwiki.resource.entity.EntityResourceReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

public class ViewAttachRevAction extends XWikiAction
{
    /**
     * Default constructor.
     */
    public ViewAttachRevAction()
    {
        this.waitForXWikiInitialization = false;
        this.handleRedirectObject = true;
    }

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        XWikiRequest request = context.getRequest();
        XWikiDocument doc = context.getDoc();
        String filename;
        if (context.getMode() == XWikiContext.MODE_PORTLET) {
            filename = request.getParameter("filename");
        } else {
            filename = getFileName();
        }

        XWikiAttachment attachment;

        if (context.getWiki().hasAttachmentRecycleBin(context) && request.getParameter("rid") != null) {
            int recycleId = Integer.parseInt(request.getParameter("rid"));
            attachment = new XWikiAttachment(doc, filename);
            attachment =
                context.getWiki().getAttachmentRecycleBinStore()
                    .restoreFromRecycleBin(attachment, recycleId, context, true);
        } else if (request.getParameter("id") != null) {
            int id = Integer.parseInt(request.getParameter("id"));
            attachment = doc.getAttachmentList().get(id);
        } else {
            attachment = doc.getAttachment(filename);
            if (attachment == null) {
                context.put("message", "attachmentdoesnotexist");
                return "exception";
            }
        }
        VelocityContext vcontext = (VelocityContext) context.get("vcontext");
        vcontext.put("attachment", new Attachment((Document) vcontext.get("doc"), attachment, context));

        return "viewattachrev";
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
