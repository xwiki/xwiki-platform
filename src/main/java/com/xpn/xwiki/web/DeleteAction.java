/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 * Action for delete, restore from recycle bin. 
 * @version $Id: $
 */
public class DeleteAction extends XWikiAction
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

        if (doc.isNew()) {
            if (xwiki.hasRecycleBin(context)) {
                String sindex = request.getParameter("id");
                if (request.getParameter("delete") != null) {
                    long index = Long.parseLong(sindex);
                    if (!xwiki.getRightService().checkAccess("delete", doc, context))
                        throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS,
                            XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                            "User has no \"delete\" access to document " + doc.getFullName());
                    xwiki.getRecycleBinStore().deleteFromRecycleBin(doc, index, context, true);
                    sendRedirect(response, doc.getURL("view", context));
                    return false;
                } else if (request.getParameter("restore") != null) {
                    long index = Long.parseLong(sindex);
                    if (!xwiki.getRightService().checkAccess("undelete", doc, context))
                        throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS,
                            XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                            "User has no \"undelete\" access to document " + doc.getFullName());
                    XWikiDocument newdoc = xwiki.getRecycleBinStore().restoreFromRecycleBin(
                        doc, index, context, true);
                    xwiki.saveDocument(newdoc, "restored from recycle bin", context);
                    // save attachments. need for save archive
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
                    sendRedirect(response, doc.getURL("view", context));
                    return false;
                }
            }
            return true;
        }

        // If confirm=1 then delete the page. If not, the render action will go to the "delete"
        // page so that the user can confirm. That "delete" page will then call the delete action
        // again with confirm=1.
        String confirm = request.getParameter("confirm");
        if ((confirm != null) && (confirm.equals("1"))) {
            String language = xwiki.getLanguagePreference(context);
            if ((language == null) || (language.equals("")) ||
                language.equals(doc.getDefaultLanguage()))
            {
                xwiki.deleteAllDocuments(doc, context);
            } else {
                // Only delete the translation
                XWikiDocument tdoc = doc.getTranslatedDocument(language, context);
                xwiki.deleteDocument(tdoc, context);
            }

            // If a xredirect param is passed then redirect to the page specified instead of
            // going to the default confirmation page.
            String redirect = Utils.getRedirect(request, null);
            if (redirect != null) {
                sendRedirect(response, redirect);
                return false;
            }
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    public String render(XWikiContext context) throws XWikiException
    {
        XWikiRequest request = context.getRequest();
        String confirm = request.getParameter("confirm");
        if ((confirm != null) && (confirm.equals("1"))) {
            return "deleted";
        } else {
            return "delete";
        }
    }
}
