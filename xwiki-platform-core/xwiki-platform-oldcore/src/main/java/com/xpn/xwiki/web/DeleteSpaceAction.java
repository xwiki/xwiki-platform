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

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.query.QueryException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiStoreInterface;

/**
 * Action for deleting an entire space, optionally saving all the deleted documents to the document trash, if enabled.
 * 
 * @version $Id$
 * @since 3.4M1
 */
public class DeleteSpaceAction extends XWikiAction
{
    /**
     * Space variable name for binding in the query statement.
     */
    private static final String SPACE_BIND_NAME = "space";

    /** Logging helper object. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteSpaceAction.class);

    /** Confirm parameter name. */
    private static final String CONFIRM_PARAM = "confirm";

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean action(XWikiContext context) throws XWikiException
    {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        String wiki = context.getDoc().getDocumentReference().getWikiReference().getName();
        String space = context.getDoc().getDocumentReference().getLastSpaceReference().getName();
        EntityReferenceResolver<String> nameResolver =
            Utils.getComponent(EntityReferenceResolver.TYPE_STRING, "current");
        boolean redirected = false;
        // If confirm=1 then delete the space. If not, the render action will go to the "deletespace" template asking
        // for user confirmation. The "deletespace" template will then call the /deletespace/ action again with
        // confirm=1.
        if (!"1".equals(request.getParameter(CONFIRM_PARAM))) {
            return true;
        }
        // CSRF prevention
        if (!csrfTokenCheck(context)) {
            return false;
        }

        List<String> documentsInSpace = getDocumentsInSpace(space, xwiki.getStore());
        if (documentsInSpace.isEmpty()) {
            // Redirect the user to the view template so that he gets the "document doesn't exist" box.
            sendRedirect(response, Utils.getRedirect("view", context));
            redirected = true;
        } else {
            // Delete to recycle bin
            for (String docName : documentsInSpace) {
                DocumentReference docReference = new DocumentReference(wiki, space, docName);
                xwiki.deleteAllDocuments(xwiki.getDocument(docReference, context), context);
            }
        }
        if (!redirected) {
            // If a xredirect param is passed then redirect to the page specified instead of going to the default
            // confirmation page.
            String redirect = Utils.getRedirect(request, null);
            if (redirect != null) {
                sendRedirect(response, redirect);
                redirected = true;
            }
        }
        return !redirected;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        XWikiRequest request = context.getRequest();
        String result = "deletespace";
        if ("1".equals(request.getParameter(CONFIRM_PARAM))) {
            result = "deletedspace";
        }
        return result;
    }

    /**
     * @param space the space to get documents from
     * @param store the store implementation to use
     * @return the list of documents (even hidden ones) located in the passed space
     */
    private List<String> getDocumentsInSpace(String space, XWikiStoreInterface store)
    {
        List<String> results;
        try {
            // Note: We're not using the "hidden" filter since we want to be able to remove all pages even if there are
            // only hidden documents.
            results = store.getQueryManager().getNamedQuery("getSpaceDocsName")
                .bindValue(SPACE_BIND_NAME, space)
                .execute();
        } catch (QueryException e) {
            LOGGER.warn("Failed to get the list of documents while trying to delete space [{}]: [{}]", space,
                e.getMessage());
            results = Collections.EMPTY_LIST;
        }

        return results;
    }
}
