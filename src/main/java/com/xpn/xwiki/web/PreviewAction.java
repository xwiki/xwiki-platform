package com.xpn.xwiki.web;

import org.apache.velocity.VelocityContext;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;

public class PreviewAction extends XWikiAction {
	public String render(XWikiContext context) throws XWikiException {
        XWikiRequest request = context.getRequest();
        XWikiDocument doc = context.getDoc();
        XWikiForm form = context.getForm();
        VelocityContext vcontext = (VelocityContext) context.get("vcontext");

        String language = ((EditForm)form).getLanguage();
        XWikiDocument tdoc;

        // Make sure it is not considered as new
        XWikiDocument doc2 = (XWikiDocument)doc.clone();
        context.put("doc", doc2);

        if ((language==null)||(language.equals(""))||(language.equals("default"))||(language.equals(doc.getDefaultLanguage()))) {
            tdoc = doc2;
            context.put("tdoc", doc2);
            vcontext.put("doc", new Document(doc2, context));
            vcontext.put("tdoc", vcontext.get("doc"));
            vcontext.put("cdoc",  vcontext.get("doc"));
            doc2.readFromTemplate(((EditForm)form).getTemplate(), context);
            doc2.readFromForm((EditForm)form, context);
        } else {
            // Need to save parent and defaultLanguage if they have changed
            tdoc = doc.getTranslatedDocument(language, context);
            tdoc.setLanguage(language);
            tdoc.setTranslation(1);
            XWikiDocument tdoc2 = (XWikiDocument)tdoc.clone();
            context.put("tdoc", tdoc2);
            vcontext.put("tdoc", new Document(tdoc2, context));
            vcontext.put("cdoc",  vcontext.get("tdoc"));
            tdoc2.readFromTemplate(((EditForm)form).getTemplate(), context);
            tdoc2.readFromForm((EditForm)form, context);
        }
        return "preview";
	}
}
