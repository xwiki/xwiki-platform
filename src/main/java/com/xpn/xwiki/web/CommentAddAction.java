/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.plugin.captcha.CaptchaPluginApi;

public class CommentAddAction extends XWikiAction {
    public boolean action(XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        ObjectAddForm oform = (ObjectAddForm) context.getForm();

        Boolean isResponseCorrect = Boolean.TRUE;
        if (xwiki.hasCaptcha(context)) {
            CaptchaPluginApi captchaPluginApi = (CaptchaPluginApi) xwiki.getPluginApi("jcaptcha", context);
            if (captchaPluginApi != null)
                isResponseCorrect = captchaPluginApi.verifyCaptcha("comment");
        }

        // Make sure this class exists
        BaseClass baseclass = xwiki.getCommentsClass(context);
        if (isResponseCorrect.booleanValue()) {
            if (doc.isNew()) {
                return true;
            } else {
                String className = "XWiki.XWikiComments";
                int nb = doc.createNewObject(className, context);
                BaseObject oldobject = doc.getObject(className, nb);
                BaseObject newobject = (BaseObject) baseclass.fromMap(oform.getObject(className), oldobject);
                newobject.setNumber(oldobject.getNumber());
                newobject.setName(doc.getFullName());
                doc.setObject(className, nb, newobject);
                xwiki.saveDocument(doc, context.getMessageTool().get("core.comment.addComment"), context);
            }
            // forward to edit
            String redirect = Utils.getRedirect("edit", context);
            sendRedirect(response, redirect);
        } else {
                String url = context.getDoc().getURL("view", "xpage=comments&confirm=false",context);
            try {
                response.sendRedirect(url);
            } catch (Exception e) {
                System.err.println(e.getStackTrace().toString());
            }
        }
        return false;
    }

    public String render(XWikiContext context) throws XWikiException {
        context.put("message", "nocommentwithnewdoc");
        return "exception";
    }
}
