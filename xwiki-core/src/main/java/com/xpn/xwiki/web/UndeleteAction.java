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
 *
 */
package com.xpn.xwiki.web;

import java.util.List;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Action for restore document from recycle bin.
 * @version $Id: $
 * @since 1.2M1
 */
public class UndeleteAction extends XWikiAction
{
    /**
     * {@inheritDoc}
     */
    public boolean action(XWikiContext context) throws XWikiException
    {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();

        if (doc.isNew() && xwiki.hasRecycleBin(context)) {
            String sindex = request.getParameter("id");
            long index = Long.parseLong(sindex);
            XWikiDocument newdoc = xwiki.getRecycleBinStore().restoreFromRecycleBin(
                doc, index, context, true);
            xwiki.saveDocument(newdoc, "restored from recycle bin", context);
            // save attachments
            List attachlist = newdoc.getAttachmentList();
            if (attachlist.size() > 0) {
                for (int i = 0; i < attachlist.size(); i++) {
                    XWikiAttachment attachment = (XWikiAttachment) attachlist.get(i);
                    // do not increment attachment version
                    attachment.setMetaDataDirty(false);
                    attachment.getAttachment_content().setContentDirty(false);
                    xwiki.getAttachmentStore().saveAttachmentContent(attachment, false,
                        context, true);
                }
            }
        }
        sendRedirect(response, doc.getURL("view", context));
        return false;
    }
}
