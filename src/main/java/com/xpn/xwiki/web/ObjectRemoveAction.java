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
 *
 */
package com.xpn.xwiki.web;

import java.io.IOException;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.VelocityContext;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

public class ObjectRemoveAction extends XWikiAction
{
    protected BaseObject getObject(XWikiDocument doc, XWikiContext context)
    {
        ObjectRemoveForm form = (ObjectRemoveForm) context.getForm();
        BaseObject obj = null;

        String className = form.getClassName();
        int classId = form.getClassId();
        if (StringUtils.isBlank(className)) {
            ((VelocityContext) context.get("vcontext")).put("message", context.getMessageTool()
                .get("platform.core.action.objectRemove.noClassnameSpecified"));
        } else if (classId < 0) {
            ((VelocityContext) context.get("vcontext")).put("message", context.getMessageTool()
                .get("platform.core.action.objectRemove.noObjectSpecified"));
        } else {
            obj = doc.getObject(className, classId);
            if (obj == null) {
                ((VelocityContext) context.get("vcontext")).put("message", context.getMessageTool()
                    .get("platform.core.action.objectRemove.invalidObject"));                
            }
        }
        return obj;
    }
    /**
     * {@inheritDoc}
     * 
     * @see XWikiAction#action(XWikiContext)
     */
    public boolean action(XWikiContext context) throws XWikiException
    {
        XWiki xwiki = context.getWiki();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        BaseObject obj = getObject(doc, context);
        if (obj == null) {
            return true;
        }
        doc.removeObject(obj);
        xwiki.saveDocument(doc, context.getMessageTool().get("core.comment.deleteObject"), true,
            context);

        if (BooleanUtils.isTrue((Boolean) context.get("ajax"))) {
            response.setStatus(204);
            response.setContentLength(0);
        } else {
            // forward to edit
            String redirect = Utils.getRedirect("edit", context);
            sendRedirect(response, redirect);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiAction#render(XWikiContext)
     */
    public String render(XWikiContext context) throws XWikiException
    {
        if (BooleanUtils.isTrue((Boolean) context.get("ajax"))) {
            XWikiResponse response = context.getResponse();
            response.setStatus(409);
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
