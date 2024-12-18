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

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.script.ScriptContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.store.TemporaryAttachmentSessionsManager;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Action used to remove a comment from a page, requires comment right but not edit right.
 *
 * @version $Id$
 * @since 16.1.0RC1
 */
@Component
@Named("commentdelete")
@Singleton
public class CommentDeleteAction extends XWikiAction
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CommentDeleteAction.class);
    @Inject
    private UserReferenceResolver<CurrentUserReference> currentUserReferenceUserReferenceResolver;

    @Inject
    private TemporaryAttachmentSessionsManager temporaryAttachmentSessionsManager;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Override
    protected Class<? extends XWikiForm> getFormClass()
    {
        return ObjectRemoveForm.class;
    }

    protected BaseObject getObject(XWikiDocument doc, XWikiContext context)
    {
        ObjectRemoveForm form = (ObjectRemoveForm) context.getForm();
        BaseObject obj = null;

        String className = form.getClassName();
        int classId = form.getClassId();
        String attributeName = "message";
        if (StringUtils.isBlank(className)) {
            getCurrentScriptContext().setAttribute(attributeName,
                    localizePlainOrReturnKey("platform.core.action.commentRemove.noClassnameSpecified"),
                    ScriptContext.ENGINE_SCOPE);
        } else if (classId < 0) {
            getCurrentScriptContext().setAttribute(attributeName,
                    localizePlainOrReturnKey("platform.core.action.commentRemove.noCommentSpecified"),
                    ScriptContext.ENGINE_SCOPE);
        } else {
            obj = doc.getObject(className, classId);
            if (obj == null) {
                getCurrentScriptContext().setAttribute(attributeName,
                        localizePlainOrReturnKey("platform.core.action.commentRemove.invalidComment"),
                        ScriptContext.ENGINE_SCOPE);
            }
        }

        return obj;
    }

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
        DocumentReference userReference = context.getUserReference();

        // We need to clone this document first, since a cached storage would return the same object for the
        // following requests, so concurrent request might get a partially modified object, or worse, if an error
        // occurs during the save, the cached object will not reflect the actual document at all.
        doc = doc.clone();

        BaseObject obj = getObject(doc, context);
        if (obj == null) {
            return true;
        }

        doc.removeObject(obj);
        doc.setAuthorReference(userReference);

        String changeComment = localizePlainOrReturnKey("core.comment.deleteComment");

        // Make sure the user is allowed to make this modification
        context.getWiki().checkSavingDocument(userReference, doc, changeComment, true, context);

        xwiki.saveDocument(doc, changeComment, true, context);

        if (Utils.isAjaxRequest(context)) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            response.setContentLength(0);
        } else {
            // forward to edit
            String redirect = Utils.getRedirect("edit", context);
            sendRedirect(response, redirect);
        }
        return false;
    }

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        if (Utils.isAjaxRequest(context)) {
            XWikiResponse response = context.getResponse();
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            response.setContentType("text/plain");
            try {
                response.getWriter().write("failed");
                response.setContentLength(6);
            } catch (IOException e) {
                LOGGER.error("Failed to send error response to AJAX comment delete request.", e);
            }
            return null;
        } else {
            return "error";
        }
    }
}
