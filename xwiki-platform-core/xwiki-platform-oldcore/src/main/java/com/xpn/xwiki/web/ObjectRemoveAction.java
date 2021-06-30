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

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

public class ObjectRemoveAction extends XWikiAction
{
	@Override
    protected Class<? extends XWikiForm> getFomClass()
    {
        return ObjectAddForm.class;
    }

    protected BaseObject getObject(XWikiDocument doc, XWikiContext context)
    {
        ObjectRemoveForm form = (ObjectRemoveForm) context.getForm();
        BaseObject obj = null;

        String className = form.getClassName();
        int classId = form.getClassId();
        if (StringUtils.isBlank(className)) {
            ((VelocityContext) context.get("vcontext")).put("message",
                localizePlainOrKey("platform.core.action.objectRemove.noClassnameSpecified"));
        } else if (classId < 0) {
            ((VelocityContext) context.get("vcontext")).put("message",
                localizePlainOrKey("platform.core.action.objectRemove.noObjectSpecified"));
        } else {
            obj = doc.getObject(className, classId);
            if (obj == null) {
                ((VelocityContext) context.get("vcontext")).put("message",
                    localizePlainOrKey("platform.core.action.objectRemove.invalidObject"));
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
        String username = context.getUser();
        XWikiDocument doc = context.getDoc();
        BaseObject obj = getObject(doc, context);
        if (obj == null) {
            return true;
        }

        doc.removeObject(obj);
        doc.setAuthor(username);
        xwiki.saveDocument(doc, localizePlainOrKey("core.comment.deleteObject"), true, context);

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
            }
            return null;
        } else {
            return "error";
        }
    }
}
