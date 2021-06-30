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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.util.Util;

public class PropUpdateAction extends XWikiAction
{
    @Override
    protected Class<? extends XWikiForm> getFomClass()
    {
        return EditForm.class;
    }

    public boolean propUpdate(XWikiContext context) throws XWikiException
    {
        XWiki xwiki = context.getWiki();
        XWikiDocument doc = context.getDoc();
        XWikiForm form = context.getForm();

        // Prepare new class
        BaseClass bclass = doc.getXClass();
        BaseClass bclass2 = bclass.clone();
        bclass2.setFields(new HashMap());

        // Prepare a Map for field renames
        Map<String, String> fieldsToRename = new HashMap<String, String>();

        for (PropertyClass originalProperty : (Collection<PropertyClass>) bclass.getFieldList()) {
            PropertyClass newProperty = originalProperty.clone();
            String name = newProperty.getName();
            Map<String, ?> map = ((EditForm) form).getObject(name);
            newProperty.getXClass(context).fromMap(map, newProperty);
            String newName = newProperty.getName();

            if (!Util.isValidXMLElementName(newName)) {
                context.put("message", "propertynamenotcorrect");
                return true;
            }

            if (newName.indexOf(" ") != -1) {
                newName = newName.replaceAll(" ", "");
                newProperty.setName(newName);
            }
            bclass2.addField(newName, newProperty);
            if (!newName.equals(name)) {
                fieldsToRename.put(name, newName);
                bclass2.addPropertyForRemoval(originalProperty);
            }
        }

        doc.setXClass(bclass2);
        doc.renameProperties(bclass.getName(), fieldsToRename);
        doc.setMetaDataDirty(true);
        if (doc.isNew()) {
            doc.setCreator(context.getUser());
        }
        doc.setAuthor(context.getUser());
        xwiki.saveDocument(doc, localizePlainOrKey("core.comment.updateClassProperty"), true,
            context);

        // We need to load all documents that use this property and rename it
        if (fieldsToRename.size() > 0) {
            List<String> list =
                xwiki.getStore().searchDocumentsNames(
                    ", BaseObject as obj where obj.name=doc.fullName and obj.className='"
                        + Utils.SQLFilter(bclass.getName()) + "' and doc.fullName <> '"
                        + Utils.SQLFilter(bclass.getName()) + "'", context);
            for (String docName : list) {
                XWikiDocument doc2 = xwiki.getDocument(docName, context);
                doc2.renameProperties(bclass.getName(), fieldsToRename);
                xwiki.saveDocument(doc2, localizePlainOrKey("core.comment.updateClassPropertyName"), true, context);
            }
        }

        return false;
    }

    @Override
    public boolean action(XWikiContext context) throws XWikiException
    {
        // CSRF prevention
        if (!csrfTokenCheck(context)) {
            return false;
        }

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

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        return "exception";
    }
}
