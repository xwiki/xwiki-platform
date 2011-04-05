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

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

public class RollbackAction extends XWikiAction
{
    @Override
    public boolean action(XWikiContext context) throws XWikiException
    {
        // CSRF prevention
        if (!csrfTokenCheck(context)) {
            return false;
        }

        XWiki xwiki = context.getWiki();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        RollbackForm form = (RollbackForm) context.getForm();

        String confirm = form.getConfirm();
        String rev = form.getRev();
        String language = form.getLanguage();

        if ((confirm == null) || (!confirm.equals("1"))) {
            return true;
        }

        XWikiDocument tdoc = getTranslatedDocument(doc, language, context);
        XWikiDocument newdoc = xwiki.rollback(tdoc, rev, context);
        // forward to view
        String redirect = Utils.getRedirect("view", context);
        sendRedirect(response, redirect);
        return false;
    }

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        handleRevision(context);
        return "rollback";
    }
}
