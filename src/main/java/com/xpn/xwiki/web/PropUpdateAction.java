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

public class PropUpdateAction extends XWikiAction {
	public boolean action(XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        XWikiForm form = context.getForm();
        XWikiDocument olddoc = (XWikiDocument) doc.clone();

        // Prepare new class
        BaseClass bclass = doc.getxWikiClass();
        BaseClass bclass2 = (BaseClass)bclass.clone();
        bclass2.setFields(new HashMap());

        doc.setxWikiClass(bclass2);

        // Prepare a Map for field renames
        Map fieldsToRename = new HashMap();

        Iterator it = bclass.getFieldList().iterator();
        while (it.hasNext()) {
            PropertyClass property = (PropertyClass)it.next();
            PropertyClass origproperty = (PropertyClass) property.clone();
            String name = property.getName();
            Map map = ((EditForm)form).getObject(name);
            property.getxWikiClass(context).fromMap(map, property);
            String newname = property.getName();
            if (newname.indexOf(" ")!=-1) {
                newname = newname.replaceAll(" ","");
                property.setName(newname);
            }
            bclass2.addField(newname, property);
            if (!newname.equals(name)) {
                fieldsToRename.put(name, newname);
                bclass2.addPropertyForRemoval(origproperty);
            }
        }
        doc.renameProperties(bclass.getName(), fieldsToRename);
        xwiki.saveDocument(doc, olddoc, context);

        // We need to load all documents that use this property and rename it
        if (fieldsToRename.size()>0) {
            List list = xwiki.getStore().searchDocumentsNames(", BaseObject as obj where obj.name="
                                                              + xwiki.getFullNameSQL() + " and obj.className='"
                                                              + Utils.SQLFilter(bclass.getName()) +  "' and " + xwiki.getFullNameSQL() + "<> '"
                                                              + Utils.SQLFilter(bclass.getName()) + "'", context);
            for (int i=0;i<list.size();i++) {
                XWikiDocument doc2 = xwiki.getDocument((String)list.get(i), context);
                doc2.renameProperties(bclass.getName(), fieldsToRename);
                xwiki.saveDocument(doc2, doc2, context);
            }
        }
        xwiki.flushCache();
        // forward to edit
        String redirect = Utils.getRedirect("edit", context);
        sendRedirect(response, redirect);
        return false;
	}
}
