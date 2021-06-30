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
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocumentArchive;

public class RollbackAction extends XWikiAction
{
    @Override
    protected Class<? extends XWikiForm> getFomClass()
    {
        return RollbackForm.class;
    }

    @Override
    public boolean action(XWikiContext context) throws XWikiException
    {
        // CSRF prevention
        if (!csrfTokenCheck(context)) {
            return false;
        }

        RollbackForm form = (RollbackForm) context.getForm();
        if (!"1".equals(form.getConfirm())) {
            return true;
        }

        XWiki xwiki = context.getWiki();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();

        String rev = form.getRev();
        String language = form.getLanguage();

        // We don't clone the document here because the rollback method does it before making modifications.
        XWikiDocument tdoc = getTranslatedDocument(doc, language, context);

        // Support for the "previous" pseudoversions.
        if ("previous".equals(rev)) {
            XWikiDocumentArchive archive = tdoc.loadDocumentArchive();

            // Note: Using Object to try to avoid to use jrcs objects.
            Object previousVersion = archive.getPrevVersion(archive.getLatestVersion());
            if (previousVersion != null) {
                rev = previousVersion.toString();
            } else {
                // Some inexistent version, since we have found no previous version in the archive.
                rev = "-1";
            }
        }

        // Perform the rollback.
        xwiki.rollback(tdoc, rev, context);

        // Forward to view.
        String redirect = Utils.getRedirect("view", context);
        sendRedirect(response, redirect);
        return false;
    }

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        return "rollback";
    }
}
