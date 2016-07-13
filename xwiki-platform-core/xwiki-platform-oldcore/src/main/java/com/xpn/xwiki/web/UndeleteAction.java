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

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.localization.LocaleUtils;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDeletedDocument;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Action for restoring documents from the recycle bin.
 *
 * @version $Id$
 * @since 1.2M1
 */
public class UndeleteAction extends XWikiAction
{
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
        String deletedDocumentLanguage = null;

        if (xwiki.hasRecycleBin(context)) {
            String sindex = request.getParameter("id");
            long index = Long.parseLong(sindex);

            // See exactly what is it that we want to restore by looking at the language of the deleted document.
            // FIXME: don`t use int type for index. Fix xwiki.getDeletedDocument to properly use long.
            XWikiDeletedDocument deletedDocument =
                xwiki.getDeletedDocument(StringUtils.EMPTY, StringUtils.EMPTY, (int) index, context);
            if (deletedDocument != null) {
                deletedDocumentLanguage = deletedDocument.getLanguage();

                // If the document (or the translation) that we want to restore does not exist, restore it.
                DocumentReference translatedDocumentReference =
                    new DocumentReference(doc.getDocumentReference(), LocaleUtils.toLocale(deletedDocumentLanguage,
                        Locale.ROOT));
                if (!xwiki.exists(translatedDocumentReference, context)) {
                    xwiki.restoreFromRecycleBin(doc, index, "Restored from recycle bin", context);
                }
            }
        }

        // Redirect to the undeleted document. Make sure to redirect to the proper translation.
        String queryString = null;
        if (deletedDocumentLanguage != null && xwiki.isMultiLingual(context)) {
            queryString = String.format("language=%s", deletedDocumentLanguage);
        }
        sendRedirect(response, doc.getURL("view", queryString, context));

        return false;
    }
}
