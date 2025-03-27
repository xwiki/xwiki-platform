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
import javax.script.ScriptContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.container.Response;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

public abstract class AbstractObjectRemoveAction extends XWikiAction
{
    private static final String FAIL_MESSAGE = "failed";
    
    protected String noClassNameKey;
    protected String noIdKey;
    protected String invalidKey;
    protected String deleteSuccessfulKey;
    @Inject
    private Logger logger;
    @Inject
    private UserReferenceResolver<CurrentUserReference> currentUserResolver;

    @Override
    protected Class<? extends XWikiForm> getFormClass()
    {
        return ObjectRemoveForm.class;
    }
    
    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        if (Boolean.TRUE.equals(Utils.isAjaxRequest(context))) {
            Response response = this.container.getResponse();
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            response.setContentType("text/plain");
            try {
                response.getOutputStream().write(FAIL_MESSAGE.getBytes());
            } catch (IOException e) {
                logger.error("Failed to send error response to AJAX comment delete request.", e);
            }
            return null;
        } else {
            return "error";
        }
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
                localizePlainOrReturnKey(this.noClassNameKey),
                ScriptContext.ENGINE_SCOPE);
        } else if (classId < 0) {
            getCurrentScriptContext().setAttribute(attributeName,
                localizePlainOrReturnKey(this.noIdKey),
                ScriptContext.ENGINE_SCOPE);
        } else {
            // Object class reference
            DocumentReference objectClass = new DocumentReference(context.getWikiId(), XWiki.SYSTEM_SPACE,
                XWikiDocument.COMMENTSCLASS_REFERENCE.getName());
            obj = doc.getXObject(objectClass, classId);
            if (obj == null) {
                getCurrentScriptContext().setAttribute(attributeName,
                    localizePlainOrReturnKey(this.invalidKey),
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

        doc.removeXObject(obj);
        UserReference currentUserReference = this.currentUserResolver.resolve(CurrentUserReference.INSTANCE);
        doc.getAuthors().setEffectiveMetadataAuthor(currentUserReference);

        String comment = localizePlainOrReturnKey(deleteSuccessfulKey);

        // Make sure the user is allowed to make this modification
        context.getWiki().checkSavingDocument(userReference, doc, comment, true, context);

        xwiki.saveDocument(doc, comment, true, context);

        if (Boolean.TRUE.equals(Utils.isAjaxRequest(context))) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            response.setContentLength(0);
        } else {
            // forward to edit
            String redirect = Utils.getRedirect("edit", context);
            sendRedirect(response, redirect);
        }
        return false;
    }
}
