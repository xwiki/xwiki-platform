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

import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;

public class EditAction extends XWikiAction
{
    private static final Log log = LogFactory.getLog(EditAction.class);

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        XWikiRequest request = context.getRequest();
        String content = request.getParameter("content");
        String title = request.getParameter("title");
        XWikiDocument doc = context.getDoc();
        XWiki xwiki = context.getWiki();
        XWikiForm form = context.getForm();
        VelocityContext vcontext = (VelocityContext) context.get("vcontext");

        boolean hasTranslation = false;
        if (doc != context.get("tdoc")) {
            hasTranslation = true;
        }

        // we need to clone so that nothing happens in memory
        doc = (XWikiDocument) doc.clone();
        context.put("doc", doc);
        vcontext.put("doc", doc.newDocument(context));

        // Check for edit section
        String sectionContent = "";
        int sectionNumber = 0;
        if (request.getParameter("section") != null && xwiki.hasSectionEdit(context)) {
            sectionNumber = NumberUtils.toInt(request.getParameter("section"));
            sectionContent = doc.getContentOfSection(sectionNumber);
        }
        vcontext.put("sectionNumber", new Integer(sectionNumber));

        synchronized (doc) {
            XWikiDocument tdoc = (XWikiDocument) context.get("tdoc");
            EditForm peform = (EditForm) form;
            String parent = peform.getParent();
            if (parent != null) {
                doc.setParent(parent);
            }
            String creator = peform.getCreator();
            if (creator != null) {
                doc.setCreator(creator);
            }
            String defaultTemplate = peform.getDefaultTemplate();
            if (defaultTemplate != null) {
                doc.setDefaultTemplate(defaultTemplate);
            }
            String defaultLanguage = peform.getDefaultLanguage();
            if ((defaultLanguage != null) && !defaultLanguage.equals("")) {
                doc.setDefaultLanguage(defaultLanguage);
            }
            if (doc.isNew() && doc.getDefaultLanguage().equals("")) {
                doc.setDefaultLanguage(context.getWiki().getLanguagePreference(context));
            }

            String language = context.getWiki().getLanguagePreference(context);
            String languagefromrequest = context.getRequest().getParameter("language");
            languagefromrequest = (languagefromrequest == null) ? "" : languagefromrequest;
            String languagetoedit = languagefromrequest.equals("") ? language : languagefromrequest;

            // if no specific language is set or if it is "default" then we edit the current doc
            if ((languagetoedit == null) || (languagetoedit.equals("default"))) {
                languagetoedit = "";
            }
            // if the document is new then we edit it as the default
            // if the language to edit is the one of the default document then the language is the
            // default
            if (doc.isNew() || (doc.getDefaultLanguage().equals(languagetoedit))) {
                languagetoedit = "";
            }
            // if the doc does not exist in the language to edit and the language was not
            // explicitely set in the URL
            // then we edit the default doc, otherwise this can cause to create translations without
            // wanting it.
            if ((!hasTranslation) && languagefromrequest.equals("")) {
                languagetoedit = "";
            }

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
                if ((!hasTranslation) && context.getWiki().isMultiLingual(context)) {
                    tdoc = new XWikiDocument(doc.getSpace(), doc.getName());
                    tdoc.setLanguage(languagetoedit);
                    tdoc.setContent(doc.getContent());
                    tdoc.setSyntaxId(doc.getSyntaxId());
                    tdoc.setAuthor(context.getUser());
                    tdoc.setStore(doc.getStore());
                    context.put("tdoc", tdoc);
                    vcontext.put("tdoc", tdoc.newDocument(context));
                }
            }

            XWikiDocument tdoc2 = (XWikiDocument) tdoc.clone();
            if (content != null) {
                tdoc2.setContent(content);
                tdoc2.setTitle(title);
            }
            if (sectionContent != null && !sectionContent.equals("")) {
                if (content != null) {
                    tdoc2.setContent(content);
                } else {
                    tdoc2.setContent(sectionContent);
                }
                if (title != null) {
                    tdoc2.setTitle(doc.getDocumentSection(sectionNumber).getSectionTitle());
                } else {
                    tdoc2.setTitle(title);
                }
            }
            context.put("tdoc", tdoc2);
            vcontext.put("tdoc", tdoc2.newDocument(context));
            try {
                tdoc2.readFromTemplate(peform, context);
            } catch (XWikiException e) {
                if (e.getCode() == XWikiException.ERROR_XWIKI_APP_DOCUMENT_NOT_EMPTY) {
                    context.put("exception", e);
                    return "docalreadyexists";
                }
            }

            /* Setup a lock */
            try {
                XWikiLock lock = tdoc.getLock(context);
                if ((lock == null) || (lock.getUserName().equals(context.getUser())) || (peform.isLockForce())) {
                    tdoc.setLock(context.getUser(), context);
                }
            } catch (Exception e) {
                e.printStackTrace();
                // Lock should never make XWiki fail
                // But we should log any related information
                log.error("Exception while setting up lock", e);
            }
        }

        return "edit";
    }
}
