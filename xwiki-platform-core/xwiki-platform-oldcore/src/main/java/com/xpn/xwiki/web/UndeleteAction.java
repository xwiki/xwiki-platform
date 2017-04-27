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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

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
    private static final String ID_PARAMETER = "id";

    private static final String SHOW_BATCH_PARAMETER = "showBatch";

    private static final String INCLUDE_BATCH_PARAMETER = "includeBatch";

    private static final String CONFIRM_PARAMETER = "confirm";

    private static final String TRUE = "true";

    private static final Logger LOGGER = LoggerFactory.getLogger(UndeleteAction.class);

    @Override
    public boolean action(XWikiContext context) throws XWikiException
    {
        XWikiRequest request = context.getRequest();

        // If showBatch=true and confirm=true then restore the page w/o the batch. If not, the render action will go to
        // the "restore" UI so that the user can confirm. That "restore" UI will then call the action again with
        // confirm=true.
        if (TRUE.equals(request.get(SHOW_BATCH_PARAMETER)) && !TRUE.equals(request.get(CONFIRM_PARAMETER))) {
            return true;
        }

        // CSRF prevention
        if (!csrfTokenCheck(context)) {
            return false;
        }

        XWiki xwiki = context.getWiki();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();

        Locale deletedDocumentLocale = null;

        String sindex = request.getParameter(ID_PARAMETER);
        if (xwiki.hasRecycleBin(context) && StringUtils.isNotBlank(sindex)) {
            long index = Long.parseLong(sindex);

            // See exactly what is it that we want to restore by looking at the language of the deleted document.
            // FIXME: don`t use int type for index. Fix xwiki.getDeletedDocument to properly use long.
            XWikiDeletedDocument deletedDocument =
                xwiki.getDeletedDocument(StringUtils.EMPTY, StringUtils.EMPTY, (int) index, context);
            if (deletedDocument != null) {
                // Remember the locale that we might want to redirect to, later.
                deletedDocumentLocale = deletedDocument.getLocale();

                if (TRUE.equals(request.getParameter(INCLUDE_BATCH_PARAMETER))) {
                    // Restore the entire batch.
                    String batchId = deletedDocument.getBatchId();
                    XWikiDeletedDocument[] batchDeletedDocuments =
                        xwiki.getRecycleBinStore().getAllDeletedDocuments(batchId, context, true);
                    for (XWikiDeletedDocument batchDeletedDocument : batchDeletedDocuments) {
                        // FIXME: Replace this with a proper RestoreJob that does asynchronous work and reports
                        // progress.

                        // Avoid breaking the entire restore operation if some documents fail.
                        try {
                            restoreDocument(batchDeletedDocument, context);
                        } catch (Exception e) {
                            LOGGER.error("Failed to restore document [{}] from batch [{}]",
                                batchDeletedDocument.getFullName(), batchId, e);
                        }
                    }
                } else {
                    // Restore just the current document.
                    restoreDocument(deletedDocument, context);
                }
            }
        }

        // Redirect to the undeleted document. Make sure to redirect to the proper translation.
        String queryString = getRedirectQueryString(context, xwiki, deletedDocumentLocale);
        sendRedirect(response, doc.getURL("view", queryString, context));

        return false;
    }

    private String getRedirectQueryString(XWikiContext context, XWiki xwiki, Locale deletedDocumentLocale)
    {
        String result = null;

        if (deletedDocumentLocale != null && xwiki.isMultiLingual(context)) {
            result = String.format("language=%s", deletedDocumentLocale);
        }

        return result;
    }

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        String result = null;

        XWikiRequest request = context.getRequest();

        // If showBatch=true and user confirmation is required, display the "restore" UI.
        if (TRUE.equals(request.get(SHOW_BATCH_PARAMETER)) && !TRUE.equals(request.get(CONFIRM_PARAMETER))) {
            result = "restore";
        }

        return result;
    }

    private void restoreDocument(XWikiDeletedDocument deletedDocument, XWikiContext context) throws XWikiException
    {
        XWiki xwiki = context.getWiki();

        DocumentReferenceResolver<String> resolver = Utils.getComponent(DocumentReferenceResolver.TYPE_STRING);
        DocumentReference deletedDocumentReference = resolver.resolve(deletedDocument.getFullName());

        // Since the RecycleBin expects an XWikiDocument, let`s produce one from the XWikiDeletedDocument.
        // Note: Always include the locale of the deleted document, so that the correct deleted content is restored.
        XWikiDocument doc = new XWikiDocument(deletedDocumentReference, deletedDocument.getLocale());

        // If the document (or the translation) that we want to restore does not exist, restore it.
        if (!xwiki.exists(doc.getDocumentReferenceWithLocale(), context)) {
            xwiki.restoreFromRecycleBin(doc, deletedDocument.getId(), "Restored from recycle bin", context);
        } else {
            LOGGER.warn("Skipping restore for document [{}] of batch [{}]. Document already exists.",
                deletedDocument.getFullName(), deletedDocument.getBatchId());
        }
    }
}
