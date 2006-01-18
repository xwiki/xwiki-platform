package com.xpn.xwiki.web;

import org.apache.velocity.VelocityContext;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;

public class EditAction extends XWikiAction {
	public String render(XWikiContext context) throws XWikiException {
        XWikiRequest request = context.getRequest();
        XWikiDocument doc = context.getDoc();
        XWikiForm form = context.getForm();
        VelocityContext vcontext = (VelocityContext) context.get("vcontext");

        XWikiDocument tdoc = (XWikiDocument) context.get("tdoc");
        PrepareEditForm peform = (PrepareEditForm) form;
        String parent = peform.getParent();
        if (parent!=null)
            doc.setParent(parent);
        String creator = peform.getCreator();
        if (creator!=null)
            doc.setCreator(creator);
        String defaultTemplate = peform.getDefaultTemplate();
        if (defaultTemplate!=null)
            doc.setDefaultTemplate(defaultTemplate);
        String defaultLanguage = peform.getDefaultLanguage();
        if ((defaultLanguage!=null)&&!defaultLanguage.equals(""))
            doc.setDefaultLanguage(defaultLanguage);
        if (doc.getDefaultLanguage().equals(""))
            doc.setDefaultLanguage(context.getWiki().getLanguagePreference(context));

        String language = context.getWiki().getLanguagePreference(context);
        String languagefromrequest = context.getRequest().getParameter("language");
        String languagetoedit = ((languagefromrequest==null)||(languagefromrequest.equals(""))) ?
                language : languagefromrequest;

        if ((languagetoedit==null)||(languagetoedit.equals("default")))
            languagetoedit = "";
        if (doc.isNew()||(doc.getDefaultLanguage().equals(languagetoedit)))
            languagetoedit = "";

        if (languagetoedit.equals("")) {
            // In this case the created document is going to be the default document
            tdoc = doc;
            context.put("tdoc", doc);
            vcontext.put("tdoc", vcontext.get("doc"));
            if (doc.isNew()) {
                doc.setDefaultLanguage(language);
                doc.setLanguage("");
            }
        } else {
            // If the translated doc object is the same as the doc object
            // this means the translated doc did not exists so we need to create it
            if ((tdoc==doc)) {
                tdoc = new XWikiDocument(doc.getWeb(), doc.getName());
                tdoc.setLanguage(languagetoedit);
                tdoc.setContent(doc.getContent());
                tdoc.setAuthor(context.getUser());
                tdoc.setStore(doc.getStore());
                context.put("tdoc", tdoc);
                vcontext.put("tdoc", new Document(tdoc, context));
            }
        }

        /* Setup a lock */
        try {
        XWikiLock lock = tdoc.getLock(context);
        if (lock == null || lock.getUserName().equals(context.getUser()) || peform.isLockForce())
            tdoc.setLock(context.getUser(),context);
        } catch (Exception e) {
            // Lock should never make XWiki fail
            // But we should log any related information
            // log.error("Exception while setting up lock", e);
        }

        XWikiDocument tdoc2 = (XWikiDocument) tdoc.clone();
        context.put("tdoc", tdoc2);
        vcontext.put("tdoc", new Document(tdoc2, context));
        tdoc2.readFromTemplate(peform, context);
        return "edit";
	}
}
