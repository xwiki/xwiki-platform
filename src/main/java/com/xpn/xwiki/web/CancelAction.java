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
 * @author sdumitriu
 */
package com.xpn.xwiki.web;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;

public class CancelAction extends XWikiAction {
	public boolean action(XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        XWikiForm form = context.getForm();

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

        String username = context.getUser();
        XWikiLock lock = tdoc.getLock(context);
        if (lock != null && lock.getUserName().equals(username)) {
        if ("inline".equals(request.get("action")))
            doc.removeLock(context);
        else
            tdoc.removeLock(context);
        }

        // forward to view
        String ajax = request.getParameter("ajax");
        if(ajax == null || !ajax.equals("1")){
        	String redirect = Utils.getRedirect("view", context);
        	sendRedirect(response, redirect);
        }
        return false;
	}
}
