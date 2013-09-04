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

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Action for restoring documents from the recycle bin.
 * 
 * @version $Id$
 * @since 1.2M1
 */
public class UndeleteAction extends XWikiAction
{
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean action(XWikiContext context) throws XWikiException
    {
        // CSRF prevention
        if (!csrfTokenCheck(context)) {
            return false;
        }

        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();

        if (doc.isNew() && xwiki.hasRecycleBin(context)) {
            String sindex = request.getParameter("id");
            long index = Long.parseLong(sindex);
            XWikiDocument newdoc = xwiki.getRecycleBinStore().restoreFromRecycleBin(doc, index, context, true);

            // The attachments have the dirty flag on after restoreFromRecycleBin is called which leads to their
            // versions being incremented when the document is saved. We reset the dirty flags here so that the
            // attachments preserve the version they had before the document was deleted and moved to recycle bin. Note
            // that the packager plugin does the same thing in order to prevent attachment versions from being
            // incremented when importing a document with history. XWiki#copyDocument does the same thing also.
            // We force the attachment content save with the XWikiDocument#saveAllAttachments() call below.
            // See XWIKI-9421: Attachment version is incremented when a document is restored from recycle bin
            for (XWikiAttachment attachment : newdoc.getAttachmentList()) {
                attachment.setMetaDataDirty(false);
                // Play safe in case the attachment content is broken (no content).
                if (attachment.getAttachment_content() != null) {
                    attachment.getAttachment_content().setContentDirty(false);
                }
            }

            xwiki.saveDocument(newdoc, "restored from recycle bin", context);
            xwiki.getRecycleBinStore().deleteFromRecycleBin(doc, index, context, true);

            newdoc.saveAllAttachments(false, true, context);
        }
        sendRedirect(response, doc.getURL("view", context));
        return false;
    }
}
