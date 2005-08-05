package com.xpn.xwiki.web;

import java.util.Vector;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

public class ObjectRemoveAction extends XWikiAction {
	public boolean action(XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        XWikiForm form = context.getForm();

        XWikiDocument olddoc = (XWikiDocument) doc.clone();
        String className = ((ObjectRemoveForm) form).getClassName();
        int classId = ((ObjectRemoveForm) form).getClassId();
        Vector objects = doc.getObjects(className);
        BaseObject object = (BaseObject)objects.get(classId);
        // Remove it from the object list
        objects.set(classId, null);
        doc.addObjectsToRemove(object);
        xwiki.saveDocument(doc, olddoc, context);

        // forward to edit
        String redirect = Utils.getRedirect("edit", context);
        sendRedirect(response, redirect);
        return false;
	}
}
