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

import org.apache.commons.lang.StringUtils;
import org.apache.velocity.VelocityContext;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * <p>
 * Action for previewing document changes. It prepares a temporarily changed document which is placed in the context,
 * without actually saving anything. The response is normally rendered by the {@code preview.vm} template.
 * </p>
 * <p>
 * This action also works like a request dispatcher, an early work-around for the fact that in HTML a form can only have
 * one destination URL. Thus, the form had to be submitted to one action which would further dispatch the request to
 * other actions, based on the clicked form button. Since preview is the safest method of the possible form actions, it
 * was chosen as the dispatcher. Currently this functionality is deprecated and maintained only for backwards
 * compatibility with older skins, since a cleaner dispatcher was implemented in {@link ActionFilter}.
 * </p>
 * 
 * @version $Id$
 */
public class PreviewAction extends XWikiAction
{
    /**
     * Check if a certain action was selected by the user. This is needed in older skins, which don't make use of the
     * {@link ActionFilter}'s dispatcher functionality, but rely on detecting the submit button that was clicked.
     * 
     * @param action the request parameter value that should be tested
     * @return {@code true} if the value is a non-empty string, {@code false} otherwise
     */
    private boolean isActionSelected(String action)
    {
        return StringUtils.isNotEmpty(action);
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiAction#action(XWikiContext)
     */
    @Override
    public boolean action(XWikiContext context) throws XWikiException
    {
        XWikiRequest request = context.getRequest();
        String formactionsave = request.getParameter("formactionsave");
        String formactioncancel = request.getParameter("formactioncancel");
        String formactionsac = request.getParameter("formactionsac");

        if (isActionSelected(formactionsave)) {
            SaveAction sa = new SaveAction();
            if (sa.action(context)) {
                sa.render(context);
            }
            return false;
        }

        if (isActionSelected(formactioncancel)) {
            CancelAction ca = new CancelAction();
            if (ca.action(context)) {
                ca.render(context);
            }
            return false;
        }

        if (isActionSelected(formactionsac)) {
            SaveAndContinueAction saca = new SaveAndContinueAction();
            if (saca.action(context)) {
                saca.render(context);
            }
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiAction#render(XWikiContext)
     */
    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        XWikiRequest request = context.getRequest();
        XWiki xwiki = context.getWiki();
        XWikiDocument doc = context.getDoc();
        XWikiForm form = context.getForm();
        VelocityContext vcontext = (VelocityContext) context.get("vcontext");

        String language = ((EditForm) form).getLanguage();

        // Make sure it is not considered as new
        XWikiDocument doc2 = doc.clone();
        context.put("doc", doc2);

        int sectionNumber = 0;
        if (request.getParameter("section") != null && context.getWiki().hasSectionEdit(context)) {
            sectionNumber = Integer.parseInt(request.getParameter("section"));
        }
        vcontext.put("sectionNumber", new Integer(sectionNumber));

        if ((language == null) || (language.equals("")) || (language.equals("default"))
            || (language.equals(doc.getDefaultLanguage()))) {
            context.put("tdoc", doc2);
            vcontext.put("doc", doc2.newDocument(context));
            vcontext.put("tdoc", vcontext.get("doc"));
            vcontext.put("cdoc", vcontext.get("doc"));
            doc2.readFromTemplate(((EditForm) form).getTemplate(), context);
            doc2.readFromForm((EditForm) form, context);
            doc2.setAuthor(context.getUser());
            doc2.setContentAuthor(context.getUser());
            if (doc2.isNew()) {
                doc2.setCreator(context.getUser());
            }
        } else {
            // Need to save parent and defaultLanguage if they have changed
            XWikiDocument tdoc = doc.getTranslatedDocument(language, context).clone();
            tdoc.setLanguage(language);
            tdoc.setTranslation(1);
            context.put("tdoc", tdoc);
            vcontext.put("tdoc", tdoc.newDocument(context));
            vcontext.put("cdoc", vcontext.get("tdoc"));
            tdoc.readFromTemplate(((EditForm) form).getTemplate(), context);
            tdoc.readFromForm((EditForm) form, context);
            tdoc.setAuthor(context.getUser());
            tdoc.setContentAuthor(context.getUser());
            if (tdoc.isNew()) {
                tdoc.setCreator(context.getUser());
            }
        }
        // reconfirm edit (captcha) when jcaptcha is not correct
        if ((context.get("recheckcaptcha") != null) && ((Boolean) context.get("recheckcaptcha")).booleanValue()) {
            return "captcha";
        } else {
            return "preview";
        }
    }
}
