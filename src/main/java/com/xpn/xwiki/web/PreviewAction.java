/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
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
 * @author sdumitriu
 * @author Phung Hai Nam (phunghainam@xwiki.com)
 */
package com.xpn.xwiki.web;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.captcha.CaptchaParams;
import com.xpn.xwiki.plugin.captcha.CaptchaPluginApi;
import org.apache.velocity.VelocityContext;

public class PreviewAction extends XWikiAction {
	public boolean action(XWikiContext context) throws XWikiException {
		XWikiRequest request = context.getRequest();
		String formaction = request.getParameter("formaction");
		if (formaction == null || formaction.equals("") || formaction.equals("_preview_")) {
			return true;
		} else if (formaction.equals("_save_")) {
			SaveAction sa = new SaveAction();
			if (sa.action(context)) {
				sa.render(context);
			}
			return false;
		} else if (formaction.equals("_cancel_")) {
			CancelAction ca = new CancelAction();
			if (ca.action(context)) {
				ca.render(context);
			}
			return false;
		} else if (formaction.equals("_saveandcontinue_")) {
			SaveAndContinueAction saca = new SaveAndContinueAction();
			if (saca.action(context)) {
				saca.render(context);
			}
			return false;
		}
		return true;
	}

	public String render(XWikiContext context) throws XWikiException {
		XWikiRequest request = context.getRequest();
        XWiki xwiki = context.getWiki();
		String formaction = request.getParameter("formaction");
		if (formaction == null || formaction.equals("") || formaction.equals("_preview_")) {
			XWikiDocument doc = context.getDoc();
			XWikiForm form = context.getForm();
			VelocityContext vcontext = (VelocityContext) context.get("vcontext");

            Boolean isResponseCorrect = Boolean.TRUE;
            CaptchaPluginApi captchaPluginApi = (CaptchaPluginApi) xwiki.getPluginApi("jcaptcha", context);
            if (captchaPluginApi != null) vcontext.put("captchaPlugin", captchaPluginApi);
            else vcontext.put("captchaPlugin", "noCaptchaPlugin");
            if (captchaPluginApi != null) {
                // verify captcha
                CaptchaParams captchaParams = captchaPluginApi.getCaptchaParams(context.getUser(), "edit");
                isResponseCorrect = captchaPluginApi.verifyCaptcha(captchaParams);
            }
            // put isResponseCorrect value to vcontext for save action
            vcontext.put("isResponseCorrect", isResponseCorrect.toString());

			String language = ((EditForm) form).getLanguage();
			XWikiDocument tdoc;

			// Make sure it is not considered as new
			XWikiDocument doc2 = (XWikiDocument) doc.clone();
			context.put("doc", doc2);

           int sectionNumber = 0;
           if (request.getParameter("section") != null && context.getWiki().hasSectionEdit(context)) {
               sectionNumber = Integer.parseInt(request.getParameter("section"));
            }
            vcontext.put("sectionNumber",new Integer(sectionNumber));

			if ((language == null) || (language.equals("")) || (language.equals("default")) || (language.equals(doc.getDefaultLanguage()))) {
				tdoc = doc2;
				context.put("tdoc", doc2);
				vcontext.put("doc", new Document(doc2, context));
				vcontext.put("tdoc", vcontext.get("doc"));
				vcontext.put("cdoc", vcontext.get("doc"));
				doc2.readFromTemplate(((EditForm) form).getTemplate(), context);
				doc2.readFromForm((EditForm) form, context);
			} else {
				// Need to save parent and defaultLanguage if they have changed
				tdoc = doc.getTranslatedDocument(language, context);
				tdoc.setLanguage(language);
				tdoc.setTranslation(1);
				XWikiDocument tdoc2 = (XWikiDocument) tdoc.clone();
				context.put("tdoc", tdoc2);
				vcontext.put("tdoc", new Document(tdoc2, context));
				vcontext.put("cdoc", vcontext.get("tdoc"));
				tdoc2.readFromTemplate(((EditForm) form).getTemplate(), context);
				tdoc2.readFromForm((EditForm) form, context);
			}
            // recomfirm edit (captcha) when jcaptcha is not correct
            if ((context.get("recheckcaptcha") != null) && ((Boolean)context.get("recheckcaptcha")).booleanValue())
                return "captcha";
            else return "preview";
		}
		return "disambiguation";
	}
}
