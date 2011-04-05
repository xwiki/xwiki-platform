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

import java.util.Iterator;
import java.util.Map;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

public class ObjectAddAction extends XWikiAction
{
    private static final String[] EMPTY_PROPERTY = new String[] {""};

    @SuppressWarnings("unchecked")
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
        ObjectAddForm oform = (ObjectAddForm) context.getForm();

        String className = oform.getClassName();
        BaseObject object = doc.newObject(className, context);

        // We need to have a string in the map for each field for the object to be correctly created.
        // Otherwise, queries using the missing properties will fail to return this object.
        BaseClass baseclass = object.getxWikiClass(context);
        Map<String, String[]> objmap = oform.getObject(className);
        Iterator<PropertyClass> itfields = baseclass.getFieldList().iterator();
        while (itfields.hasNext()) {
            PropertyClass property = itfields.next();
            String name = property.getName();
            if (objmap.get(name) == null) {
                objmap.put(name, EMPTY_PROPERTY);
            }
        }

        // Load the object properties that are defined in the request.
        baseclass.fromMap(objmap, object);

        doc.setAuthor(username);
        if (doc.isNew()) {
            doc.setCreator(username);
        }
        xwiki.saveDocument(doc, context.getMessageTool().get("core.comment.addObject"), true, context);

        // forward to edit
        String redirect = Utils.getRedirect("edit", context);
        sendRedirect(response, redirect);
        return false;
    }
}
