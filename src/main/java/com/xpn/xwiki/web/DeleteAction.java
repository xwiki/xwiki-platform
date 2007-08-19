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

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.DeletedDocument;
import com.xpn.xwiki.doc.XWikiDeletedDocument;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Action for delete document to recycle bin and for delete documents from recycle bin. 
 * @version $Id: $
 */
public class DeleteAction extends XWikiAction
{
    /** confirm parameter name. */
    private static String confirmParam = "confirm";
    /**
     * {@inheritDoc}
     */
    public boolean action(XWikiContext context) throws XWikiException
    {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        boolean redirected = false;
        
        if (doc.isNew()) {
            // delete from recycle bin
            if (xwiki.hasRecycleBin(context)) {
                String sindex = request.getParameter("id");
                long index = Long.parseLong(sindex);
                XWikiDeletedDocument dd = xwiki.getRecycleBinStore()
                    .getDeletedDocument(doc, index, context, true);
                DeletedDocument ddapi = new DeletedDocument(dd, context);
                if (!ddapi.canDelete()) {
                    throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS, 
                        XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                        "You can't delete from recycle bin before certain time will passed");
                }
                xwiki.getRecycleBinStore().deleteFromRecycleBin(doc, index, context, true);
                sendRedirect(response, doc.getURL("view", context));
                redirected = true;
            }
        } else {
            // delete to recycle bin
            // If confirm=1 then delete the page. If not, the render action will go to the "delete"
            // page so that the user can confirm. That "delete" page will then call 
            // the delete action again with confirm=1.
            String confirm = request.getParameter(confirmParam);
            if ((confirm != null) && (confirm.equals("1"))) {
                String language = xwiki.getLanguagePreference(context);
                if ((language == null) || (language.equals(""))
                    || language.equals(doc.getDefaultLanguage()))
                {
                    xwiki.deleteAllDocuments(doc, context);
                } else {
                    // Only delete the translation
                    XWikiDocument tdoc = doc.getTranslatedDocument(language, context);
                    xwiki.deleteDocument(tdoc, context);
                }
            }
        }
        // If a xredirect param is passed then redirect to the page specified instead of
        // going to the default confirmation page.
        String redirect = Utils.getRedirect(request, null);
        if (redirect != null) {
            sendRedirect(response, redirect);
            redirected = true;
        }
        return !redirected;
    }

    /**
     * {@inheritDoc}
     */
    public String render(XWikiContext context) throws XWikiException
    {
        XWikiRequest request = context.getRequest();
        String confirm = request.getParameter(confirmParam);
        if ((confirm != null) && (confirm.equals("1"))) {
            return "deleted";
        } else {
            return "delete";
        }
    }
}
