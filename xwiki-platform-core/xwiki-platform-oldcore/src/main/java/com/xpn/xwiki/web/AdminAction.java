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
 */
package com.xpn.xwiki.web;

import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;

/**
 * Administration xwiki action.
 *
 * @version $Id$
 */
public class AdminAction extends XWikiAction
{
    /** The logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminAction.class);

    /**
     * Default constructor.
     */
    public AdminAction()
    {
        this.waitForXWikiInitialization = false;
    }

    @Override
    protected Class<? extends XWikiForm> getFomClass()
    {
        return EditForm.class;
    }

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        XWikiRequest request = context.getRequest();
        String content = request.getParameter("content");
        XWikiDocument doc = context.getDoc();
        XWikiForm form = context.getForm();
        VelocityContext vcontext = (VelocityContext) context.get("vcontext");

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
            if (doc.getDefaultLanguage().equals("")) {
                doc.setDefaultLanguage(context.getWiki().getLanguagePreference(context));
            }

            String language = context.getWiki().getLanguagePreference(context);
            String languagefromrequest = context.getRequest().getParameter("language");
            String languagetoedit =
                ((languagefromrequest == null) || (languagefromrequest.equals(""))) ? language : languagefromrequest;

            if ((languagetoedit == null) || (languagetoedit.equals("default"))) {
                languagetoedit = "";
            }
            if (doc.isNew() || (doc.getDefaultLanguage().equals(languagetoedit))) {
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
                if ((tdoc == doc)) {
                    tdoc = new XWikiDocument(doc.getDocumentReference());
                    tdoc.setLanguage(languagetoedit);
                    tdoc.setContent(doc.getContent());
                    tdoc.setSyntax(doc.getSyntax());
                    tdoc.setAuthor(context.getUser());
                    tdoc.setStore(doc.getStore());
                    context.put("tdoc", tdoc);
                    vcontext.put("tdoc", tdoc.newDocument(context));
                }
            }

            XWikiDocument tdoc2 = tdoc.clone();
            if (content != null && !content.equals("")) {
                tdoc2.setContent(content);
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
                LOGGER.error("Exception while setting up lock", e);
            }
        }

        return "admin";
    }
}
