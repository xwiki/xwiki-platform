/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
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
 * @author ludovic
 * @author namphunghai
 * @author sdumitriu
 */
package com.xpn.xwiki.web;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;

import java.util.Date;

public class SaveAction extends XWikiAction {
    public boolean action(XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        XWikiForm form = context.getForm();

        synchronized (doc) {
            String language = ((EditForm)form).getLanguage();
            String defaultLanguage = ((EditForm)form).getDefaultLanguage();
            XWikiDocument tdoc;

            if ((language==null)||(language.equals(""))||(language.equals("default"))||(language.equals(doc.getDefaultLanguage()))) {
                // Need to save parent and defaultLanguage if they have changed
                tdoc = doc;
            } else {
                tdoc = doc.getTranslatedDocument(language, context);
                if (tdoc == doc) {
                    tdoc = new XWikiDocument(doc.getWeb(), doc.getName());
                    tdoc.setLanguage(language);
                    tdoc.setStore(doc.getStore());
                }
                tdoc.setTranslation(1);
            }

            XWikiDocument olddoc = (XWikiDocument) tdoc.clone();
            try {
                tdoc.readFromTemplate(((EditForm)form).getTemplate(), context);
            } catch (XWikiException e) {
                if (e.getCode() == XWikiException.ERROR_XWIKI_APP_DOCUMENT_NOT_EMPTY) {
                    context.put("exception", e);
                    return true;
                }
            }


            tdoc.readFromForm((EditForm)form, context);

            // TODO: handle Author
            String username = context.getUser();
            tdoc.setAuthor(username);
            if (tdoc.isNew())
                tdoc.setCreator(username);

            xwiki.saveDocument(tdoc, olddoc, context);
            XWikiLock lock = tdoc.getLock(context);
            if (lock != null )
                tdoc.removeLock(context);
        }

        // forward to view
        String redirect = Utils.getRedirect("view", context);
        sendRedirect(response, redirect);
        return false;
    }

    public String render(XWikiContext context) throws XWikiException {
        XWikiException e = (XWikiException) context.get("exception");
        if ((e!=null)&&(e.getCode() == XWikiException.ERROR_XWIKI_APP_DOCUMENT_NOT_EMPTY)) {
            return "docalreadyexists";
        } else
            return "exception";
    }
}
