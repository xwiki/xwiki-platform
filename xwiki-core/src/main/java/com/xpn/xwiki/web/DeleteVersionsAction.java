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

import org.apache.commons.lang.StringUtils;
import org.suigeneris.jrcs.rcs.Version;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocumentArchive;

/**
 * Struts action for deleting document versions.
 * 
 * @version $Id$
 */
public class DeleteVersionsAction extends XWikiAction
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

        DeleteVersionsForm form = (DeleteVersionsForm) context.getForm();
        if (!form.isConfirmed()) {
            return true;
        }

        Version v1;
        Version v2;
        if (form.getRev() == null) {
            v1 = form.getRev1();
            v2 = form.getRev2();
        } else {
            v1 = form.getRev();
            v2 = form.getRev();
        }

        if (v1 != null && v2 != null) {
            XWikiDocument doc = context.getDoc();
            String language = form.getLanguage();
            XWikiDocument tdoc = getTranslatedDocument(doc, language, context);

            XWikiDocumentArchive archive = tdoc.getDocumentArchive(context);
            archive.removeVersions(v1, v2, context);
            context.getWiki().getVersioningStore().saveXWikiDocArchive(archive, true, context);
            tdoc.setDocumentArchive(archive);
            // Is this the last remaining version? If so, then recycle the document.
            if (archive.getLatestVersion() == null) {
                if (StringUtils.isEmpty(language) || language.equals(doc.getDefaultLanguage())) {
                    context.getWiki().deleteAllDocuments(doc, context);
                } else {
                    // Only delete the translation
                    context.getWiki().deleteDocument(tdoc, context);
                }
            } else {
                // There are still some versions left.
                // If we delete the most recent (current) version, then rollback to latest undeleted version.
                if (!tdoc.getRCSVersion().equals(archive.getLatestVersion())) {
                    XWikiDocument newdoc = archive.loadDocument(archive.getLatestVersion(), context);
                    // Reset the document reference, since the one taken from the archive might be wrong (old name from
                    // before a rename)
                    newdoc.setDocumentReference(tdoc.getDocumentReference());
                    newdoc.setMetaDataDirty(false);
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
    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        return "deleteversionsconfirm";
    }
}
