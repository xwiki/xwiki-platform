package com.xpn.xwiki.web;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;

public class CommentAddAction extends XWikiAction {
	public boolean action(XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        ObjectAddForm oform = (ObjectAddForm) context.getForm();

        XWikiDocument olddoc = (XWikiDocument) doc.clone();
        String className = "XWiki.XWikiComments";
        int nb = doc.createNewObject(className, context);

        BaseObject oldobject = doc.getObject(className, nb);
        BaseClass baseclass = oldobject.getxWikiClass(context);
        BaseObject newobject = (BaseObject) baseclass.fromMap(oform.getObject(className), oldobject);
        newobject.setNumber(oldobject.getNumber());
        newobject.setName(doc.getFullName());
        doc.setObject(className, nb, newobject);
        xwiki.saveDocument(doc, olddoc, context);

        // forward to edit
        String redirect = Utils.getRedirect("edit", context);
        sendRedirect(response, redirect);
        return false;
	}
}
