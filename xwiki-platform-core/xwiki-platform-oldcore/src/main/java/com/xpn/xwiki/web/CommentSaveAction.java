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

import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Action used to edit+save an existing comment in a page, saves the comment
 * object in the document, requires comment right but not edit right.
 *
 * @version $Id$
 * @since 8.4RC1
 */
public class CommentSaveAction extends CommentAddAction
{
    private static final String COMMENT_FIELD_NAME = "comment";

    /**
     * Entity reference resolver.
     */
    private DocumentReferenceResolver<String> documentReferenceResolver =
            Utils.getComponent(DocumentReferenceResolver.TYPE_STRING, "current");

    /**
     * Authorization manager.
     */
    private AuthorizationManager authorizationManager = Utils.getComponent(AuthorizationManager.class);

    /**
     *  Localization manager.
     */
    private ContextualLocalizationManager localizationManager = Utils.getComponent(ContextualLocalizationManager.class);

    /**
     * Pattern to get the comment's number.
     */
    private final Pattern pattern = Pattern.compile("XWiki.XWikiComments_(\\d+)_comment");

    private int getCommentIdFromRequest(XWikiRequest request) throws XWikiException
    {
        // Get the comment object
        Enumeration parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String parameterName = (String) parameterNames.nextElement();
            // Matcher
            Matcher m = pattern.matcher(parameterName);
            if (m.find()) {
                String number = m.group(1);
                return Integer.parseInt(number);
            }
        }
        throw new XWikiException("Failed to find the comment to save.", null);
    }

    @Override
    public boolean action(XWikiContext context) throws XWikiException
    {
        // Get the XWiki utilities
        XWiki xwiki = context.getWiki();
        XWikiResponse response = context.getResponse();
        XWikiRequest request = context.getRequest();
        XWikiDocument doc = context.getDoc();

        if (!csrfTokenCheck(context) || doc.isNew()) {
            return false;
        }

        // Comment class reference
        DocumentReference commentClass = new DocumentReference(context.getWikiId(), XWiki.SYSTEM_SPACE,
                XWikiDocument.COMMENTSCLASS_REFERENCE.getName());

        // Edit comment
        int commentId = getCommentIdFromRequest(request);
        BaseObject commentObj = doc.getXObject(commentClass, commentId);
        if (commentObj == null) {
            return false;
        }

        // Check if the author is the current user or if the current user has the ADMIN right
        String commentAuthor = commentObj.getStringValue("author");
        DocumentReference authorReference = documentReferenceResolver.resolve(commentAuthor);
        if (!authorReference.equals(context.getUserReference())
                && !authorizationManager.hasAccess(Right.ADMIN, context.getUserReference(),
                        context.getDoc().getDocumentReference())) {
            return false;
        }

        // Edit the comment
        commentObj.set(COMMENT_FIELD_NAME, request.getParameter(
            String.format("XWiki.XWikiComments_%d_comment", commentId)), context);

        // Save it
        xwiki.saveDocument(doc, localizationManager.getTranslationPlain("core.comment.editComment"),
                true, context);

        // If xpage is specified then allow the specified template to be parsed.
        if (context.getRequest().get("xpage") != null) {
            return true;
        }

        // forward to edit
        String redirect = Utils.getRedirect("edit", context);
        sendRedirect(response, redirect);
        return false;
    }
}
