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

import org.suigeneris.jrcs.rcs.Version;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocumentArchive;

/**
 * Struts action for delete document versions.
 * 
 * @version $Id: $
 */
public class DeleteVersionsAction extends XWikiAction
{
    /**
     * {@inheritDoc}
     */
    public boolean action(XWikiContext context) throws XWikiException
    {
        XWikiDocument doc = context.getDoc();
        DeleteVersionsForm form = (DeleteVersionsForm) context.getForm();

        boolean confirm = form.isConfirmed();
        Version v1;
        Version v2;
        Version rev = form.getRev();
        if (rev == null) {
            v1 = form.getRev1();
            v2 = form.getRev2();
        } else {
            v1 = rev;
            v2 = rev;
        }
        String language = form.getLanguage();
        XWikiDocument tdoc;

        if (!confirm) {
            return true;
        }
        if (language == null || language.equals("") || language.equals("default")
            || language.equals(doc.getDefaultLanguage())) {
            // Need to save parent and defaultLanguage if they have changed
            tdoc = doc;
        } else {
            tdoc = doc.getTranslatedDocument(language, context);
            if (tdoc == doc) {
                tdoc = new XWikiDocument(doc.getSpace(), doc.getName());
                tdoc.setLanguage(language);
            }
            tdoc.setTranslation(1);
        }

        if (v1 != null && v2 != null) {
            XWikiDocumentArchive archive = tdoc.getDocumentArchive(context);
            archive.removeVersions(v1, v2, context);
            context.getWiki().getVersioningStore().saveXWikiDocArchive(archive, true, context);
            tdoc.setDocumentArchive(archive);
            // Is this the last remaining version? If so, then recycle the document.
            if (archive.getLatestVersion() == null) {
                // TODO Make the code in DeleteAction reusable, then call it from here.
            } else {
                // There are still some versions left.
                // If we delete the most recent (current) version, then rollback to latest undeleted
                // version.
                if (!tdoc.getRCSVersion().equals(archive.getLatestVersion())) {
                    XWikiDocument newdoc =
                        archive.loadDocument(archive.getLatestVersion(), context);
                    context.getWiki().getStore().saveXWikiDoc(newdoc, context);
                    context.setDoc(newdoc);
                }
            }
        }
        sendRedirect(context);
        return false;
    }

    /**
     * redirect back to view history.
     * 
     * @param context used in redirecting
     * @throws XWikiException if any error
     */
    private void sendRedirect(XWikiContext context) throws XWikiException
    {
        // forward to view
        String redirect = Utils.getRedirect("view", "viewer=history", context);
        sendRedirect(context.getResponse(), redirect);
    }

    /**
     * {@inheritDoc}
     */
    public String render(XWikiContext context) throws XWikiException
    {
        return "deleteversionsconfirm";
    }
}
