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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

public class PropUpdateAction extends XWikiAction
{
    public boolean propUpdate(XWikiContext context) throws XWikiException
    {
        XWiki xwiki = context.getWiki();
        XWikiDocument doc = context.getDoc();
        XWikiForm form = context.getForm();
        XWikiDocument olddoc = (XWikiDocument) doc.clone();

        // Prepare new class
        BaseClass bclass = doc.getxWikiClass();
        BaseClass bclass2 = (BaseClass) bclass.clone();
        bclass2.setFields(new HashMap());

        // Prepare a Map for field renames
        Map fieldsToRename = new HashMap();

        Iterator it = bclass.getFieldList().iterator();
        while (it.hasNext()) {
            PropertyClass origproperty = (PropertyClass) it.next();
            PropertyClass property = (PropertyClass) origproperty.clone();
            String name = property.getName();
            Map map = ((EditForm) form).getObject(name);
            property.getxWikiClass(context).fromMap(map, property);
            String newname = property.getName();

            if (newname == null || newname.equals("") || !newname.matches("[\\w\\.\\-\\_]+")) {
                context.put("message", "propertynamenotcorrect");
                return true;
            }

            if (newname.indexOf(" ") != -1) {
                newname = newname.replaceAll(" ", "");
                property.setName(newname);
            }
            bclass2.addField(newname, property);
            if (!newname.equals(name)) {
                fieldsToRename.put(name, newname);
                bclass2.addPropertyForRemoval(origproperty);
            }
        }

        doc.setxWikiClass(bclass2);
        doc.renameProperties(bclass.getName(), fieldsToRename);
        xwiki.saveDocument(doc, olddoc, context.getMessageTool().get("core.comment.updateclassproperty"), context);

        // We need to load all documents that use this property and rename it
        if (fieldsToRename.size() > 0) {
            List list =
                xwiki.getStore().searchDocumentsNames(
                    ", BaseObject as obj where obj.name=" + xwiki.getFullNameSQL()
                        + " and obj.className='" + Utils.SQLFilter(bclass.getName()) + "' and "
                        + xwiki.getFullNameSQL() + "<> '" + Utils.SQLFilter(bclass.getName())
                        + "'", context);
            for (int i = 0; i < list.size(); i++) {
                XWikiDocument doc2 = xwiki.getDocument((String) list.get(i), context);
                doc2.renameProperties(bclass.getName(), fieldsToRename);
                xwiki.saveDocument(doc2, doc2, context.getMessageTool().get("core.comment.updateclasspropertyname"), context);
            }
        }
        xwiki.flushCache();
        return false;
    }

    public boolean action(XWikiContext context) throws XWikiException
    {
        try {
            if (propUpdate(context)) {
                return true;
            }
        } catch (XWikiException ex) {
            context.put("exception", ex);
            return true;
        }
        // forward to view
        String redirect = Utils.getRedirect("view", context);
        sendRedirect(context.getResponse(), redirect);
        return false;
    }

    public String render(XWikiContext context) throws XWikiException
    {
        return "exception";
    }
}
