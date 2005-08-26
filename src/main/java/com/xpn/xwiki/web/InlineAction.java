package com.xpn.xwiki.web;

import org.apache.velocity.VelocityContext;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;

public class InlineAction extends XWikiAction {
	public String render(XWikiContext context) throws XWikiException {
        XWikiDocument doc = context.getDoc();
        XWikiForm form = context.getForm();
        VelocityContext vcontext = (VelocityContext) context.get("vcontext");
        Document vdoc = (Document) vcontext.get("doc");
        Document vcdoc = (Document) vcontext.get("cdoc");

        XWikiDocument doc2 = (XWikiDocument) doc.clone();
        Document vdoc2 = new Document(doc2, context);
        context.put("doc", doc2);
        vcontext.put("doc", vdoc2);

        PrepareEditForm peform = (PrepareEditForm) form;
        String parent = peform.getParent();
        if (parent!=null)
            doc2.setParent(parent);
        String creator = peform.getCreator();
        if (creator!=null)
            doc2.setCreator(creator);
        String defaultLanguage = peform.getDefaultLanguage();
        if ((defaultLanguage!=null)&&!defaultLanguage.equals(""))
            doc2.setDefaultLanguage(defaultLanguage);
        if (doc2.getDefaultLanguage().equals(""))
            doc2.setDefaultLanguage(context.getWiki().getLanguagePreference(context));

        doc2.readFromTemplate(peform, context);
        if (vdoc==vcdoc) {
            vcontext.put("cdoc", vdoc2);
        } else {
            XWikiDocument cdoc = vcdoc.getDocument();
            XWikiDocument cdoc2 = (XWikiDocument) cdoc.clone();
            vcontext.put("cdoc", new Document(cdoc2, context));
            cdoc2.readFromTemplate(peform, context);
        }
                
        // Set display context to 'view'
        context.put("display", "edit");
        return "inline";
	}
}
