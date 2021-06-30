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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.meta.MetaClass;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import com.xpn.xwiki.util.Util;

public class PropAddAction extends XWikiAction
{
	@Override
    protected Class<? extends XWikiForm> getFomClass()
    {
        return PropAddForm.class;
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
        XWikiForm form = context.getForm();

        String propName = ((PropAddForm) form).getPropName();

        if (!Util.isValidXMLElementName(propName)) {
            context.put("message", "action.addClassProperty.error.invalidName");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST,
                localizePlainOrKey("action.addClassProperty.error.invalidName"));
            return true;
        }

        String propType = ((PropAddForm) form).getPropType();
        BaseClass bclass = doc.getXClass();
        bclass.setName(doc.getFullName());
        if (bclass.get(propName) != null) {
            context.put("message", "action.addClassProperty.error.alreadyExists");
            List<String> parameters = new ArrayList<String>();
            parameters.add(propName);
            context.put("messageParameters", parameters);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST,
                localizePlainOrKey("action.addClassProperty.error.alreadyExists", parameters));
            return true;
        } else {
            MetaClass mclass = xwiki.getMetaclass();
            PropertyMetaClass pmclass = (PropertyMetaClass) mclass.get(propType);
            if (pmclass != null) {
                PropertyClass pclass = (PropertyClass) pmclass.newObject(context);
                pclass.setObject(bclass);
                pclass.setName(propName);
                pclass.setPrettyName(propName);
                bclass.put(propName, pclass);
                doc.setAuthorReference(context.getUserReference());
                if (doc.isNew()) {
                    doc.setCreatorReference(context.getUserReference());
                }
                doc.setMetaDataDirty(true);
                xwiki.saveDocument(doc, localizePlainOrKey("core.comment.addClassProperty"), true, context);
            }
        }
        // forward to edit
        String redirect = Utils.getRedirect("edit", "editor=class", context);
        sendRedirect(response, redirect);
        return false;
    }

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        return "exception";
    }
}
