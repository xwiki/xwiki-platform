package com.xpn.xwiki.web;

import org.apache.velocity.VelocityContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;

public class InlineAction extends XWikiAction {
    private static final Log log = LogFactory.getLog(InlineAction.class);

    public String render(XWikiContext context) throws XWikiException {
        XWikiDocument doc = context.getDoc();
        synchronized (doc) {
            XWikiForm form = context.getForm();
            VelocityContext vcontext = (VelocityContext) context.get("vcontext");
            Document vdoc = (Document) vcontext.get("doc");
            Document vcdoc = (Document) vcontext.get("cdoc");
            PrepareEditForm peform = (PrepareEditForm) form;

            XWikiDocument doc2 = (XWikiDocument) doc.clone();
            Document vdoc2 = new Document(doc2, context);
            context.put("doc", doc2);
            vcontext.put("doc", vdoc2);

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

            /* Setup a lock */
            try {
                XWikiLock lock = doc.getLock(context);
                if ((lock == null) || (lock.getUserName().equals(context.getUser())) || (peform.isLockForce()))
                    doc.setLock(context.getUser(),context);
            } catch (Exception e) {
                e.printStackTrace();
                // Lock should never make XWiki fail
                // But we should log any related information
                log.error("Exception while setting up lock", e);
            }
        }

        // Set display context to 'view'
        context.put("display", "edit");
        return "inline";
    }
}
