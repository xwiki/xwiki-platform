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

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.plugin.captcha.CaptchaPluginApi;

public class SaveAction extends PreviewAction {
	public boolean save(XWikiContext context) throws XWikiException {
		XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
		XWikiDocument doc = context.getDoc();
		XWikiForm form = context.getForm();

        // Confirm edit to avoid spam robots
        Boolean isResponseCorrect = Boolean.TRUE;
        // If  'save' action after preview
        String isResponsePreviewCorrect = request.getParameter("isResponsePreviewCorrect");
        if ((isResponsePreviewCorrect != null))
            isResponseCorrect = Boolean.valueOf(isResponsePreviewCorrect);
        else {
            if (xwiki.hasCaptcha(context)) {
                CaptchaPluginApi captchaPluginApi = (CaptchaPluginApi) xwiki.getPluginApi("jcaptcha", context);
                if (captchaPluginApi != null)
                      isResponseCorrect = captchaPluginApi.verifyCaptcha("edit");
            }
        }
        // If captcha is not correct it will be required again
        if (!isResponseCorrect.booleanValue()) return true;

		// This is pretty useless, since contexts aren't shared between threads.
		// It just slows down execution.
        String title = doc.getTitle();
        // Check save session
        int sectionNumber = 0;
        if (request.getParameter("section") != null && xwiki.hasSectionEdit(context)) {
           sectionNumber = Integer.parseInt(request.getParameter("section"));
        }
		synchronized (doc) {
			String language = ((EditForm) form).getLanguage();
			// FIXME Which one should be used: doc.getDefaultLanguage or
			// form.getDefaultLanguage()?
			// String defaultLanguage = ((EditForm) form).getDefaultLanguage();
			XWikiDocument tdoc;

			if (doc.isNew() || (language == null) || (language.equals("")) || (language.equals("default")) || (language.equals(doc.getDefaultLanguage()))) {
				// Need to save parent and defaultLanguage if they have changed
				tdoc = doc;
			} else {
				tdoc = doc.getTranslatedDocument(language, context);
				if ((tdoc == doc) && xwiki.isMultiLingual(context)) {
					tdoc = new XWikiDocument(doc.getSpace(), doc.getName());
					tdoc.setLanguage(language);
					tdoc.setStore(doc.getStore());
				}
				tdoc.setTranslation(1);
			}

            if (doc.isNew()) {
                doc.setLanguage("");
                if ((doc.getDefaultLanguage()==null)||(doc.getDefaultLanguage().equals("")))
                    doc.setDefaultLanguage(context.getWiki().getLanguagePreference(context));
            }

            try {
				tdoc.readFromTemplate(((EditForm) form).getTemplate(), context);
			} catch (XWikiException e) {
				if (e.getCode() == XWikiException.ERROR_XWIKI_APP_DOCUMENT_NOT_EMPTY) {
					context.put("exception", e);
					return true;
				}
			}

            if(sectionNumber != 0 ){
                XWikiDocument sectionDoc = (XWikiDocument)tdoc.clone();
                sectionDoc.readFromForm((EditForm)form, context);
                String sectionContent = sectionDoc.getContent() +"\n";
                String content = doc.updateDocumentSection(sectionNumber, sectionContent);
                tdoc.setContent(content);
                tdoc.setTitle(title);
                tdoc.setComment(sectionDoc.getComment());
                tdoc.setMinorEdit(sectionDoc.isMinorEdit());
            }else{
		    	tdoc.readFromForm((EditForm) form, context);
            }

			// TODO: handle Author
			String username = context.getUser();
			tdoc.setAuthor(username);
			if (tdoc.isNew())
				tdoc.setCreator(username);

            // Make sure we have at least the meta data dirty status
            tdoc.setMetaDataDirty(true);

            // We get the comment to be used from the document
            // It was read using readFromForm
            xwiki.saveDocument(tdoc, tdoc.getComment(), tdoc.isMinorEdit(), context);
			XWikiLock lock = tdoc.getLock(context);
			if (lock != null)
				tdoc.removeLock(context);
		}
		return false;
	}

	public boolean action(XWikiContext context) throws XWikiException {
		if (save(context)) {
			return true;
		}
		// forward to view
		sendRedirect(context.getResponse(), Utils.getRedirect("view", context));
		return false;
	}

	public String render(XWikiContext context) throws XWikiException {
		XWikiException e = (XWikiException) context.get("exception");
		if ((e != null) && (e.getCode() == XWikiException.ERROR_XWIKI_APP_DOCUMENT_NOT_EMPTY)) {
			return "docalreadyexists";
        } else if (e != null) {
            return "exception";
        } else {
            // If captcha is not correct it will require to confirm again
            context.put("recheckcaptcha",new Boolean(true));
            return super.render(context);
        }
    }
}
