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

import java.util.Locale;

import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.xwiki.localization.LocaleUtils;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;

/**
 * Action used for saving and proceeding to view the saved page.
 * <p>
 * Used as a generic action for saving documents.
 *
 * @version $Id$
 */
public class SaveAction extends PreviewAction
{
    /** The identifier of the save action. */
    public static final String ACTION_NAME = "save";

    public SaveAction()
    {
        this.waitForXWikiInitialization = true;
    }

    /**
     * Saves the current document, updated according to the parameters sent in the request.
     *
     * @param context The current request {@link XWikiContext context}.
     * @return <code>true</code> if there was an error and the response needs to render an error page,
     *         <code>false</code> if the document was correctly saved.
     * @throws XWikiException If an error occured: cannot communicate with the storage module, or cannot update the
     *             document because the request contains invalid parameters.
     */
    public boolean save(XWikiContext context) throws XWikiException
    {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiDocument doc = context.getDoc();
        XWikiForm form = context.getForm();

        // Check save session
        int sectionNumber = 0;
        if (request.getParameter("section") != null && xwiki.hasSectionEdit(context)) {
            sectionNumber = Integer.parseInt(request.getParameter("section"));
        }

        // We need to clone this document first, since a cached storage would return the same object for the
        // following requests, so concurrent request might get a partially modified object, or worse, if an error
        // occurs during the save, the cached object will not reflect the actual document at all.
        doc = doc.clone();

        String language = ((EditForm) form).getLanguage();
        // FIXME Which one should be used: doc.getDefaultLanguage or
        // form.getDefaultLanguage()?
        // String defaultLanguage = ((EditForm) form).getDefaultLanguage();
        XWikiDocument tdoc;

        if (doc.isNew() || (language == null) || (language.equals("")) || (language.equals("default"))
            || (language.equals(doc.getDefaultLanguage()))) {
            // Saving the default document translation.
            // Need to save parent and defaultLanguage if they have changed
            tdoc = doc;
        } else {
            tdoc = doc.getTranslatedDocument(language, context);
            if ((tdoc == doc) && xwiki.isMultiLingual(context)) {
                // Saving a new document translation.
                tdoc = new XWikiDocument(doc.getDocumentReference());
                tdoc.setLanguage(language);
                tdoc.setStore(doc.getStore());
            } else if (tdoc != doc) {
                // Saving an existing document translation (but not the default one).
                // Same as above, clone the object retrieved from the store cache.
                tdoc = tdoc.clone();
            }
        }

        if (doc.isNew()) {
            doc.setLocale(Locale.ROOT);
            if (doc.getDefaultLocale() == Locale.ROOT) {
                doc.setDefaultLocale(LocaleUtils
                    .toLocale(context.getWiki().getLanguagePreference(context), Locale.ROOT));
            }
        }

        try {
            tdoc.readFromTemplate(((EditForm) form).getTemplate(), context);
        } catch (XWikiException e) {
            if (e.getCode() == XWikiException.ERROR_XWIKI_APP_DOCUMENT_NOT_EMPTY) {
                context.put("exception", e);
                return true;
            }
        }

        if (sectionNumber != 0) {
            XWikiDocument sectionDoc = tdoc.clone();
            sectionDoc.readFromForm((EditForm) form, context);
            String sectionContent = sectionDoc.getContent() + "\n";
            String content = tdoc.updateDocumentSection(sectionNumber, sectionContent);
            tdoc.setContent(content);
            tdoc.setComment(sectionDoc.getComment());
            tdoc.setMinorEdit(sectionDoc.isMinorEdit());
        } else {
            tdoc.readFromForm((EditForm) form, context);
        }

        // TODO: handle Author
        String username = context.getUser();
        tdoc.setAuthor(username);
        if (tdoc.isNew()) {
            tdoc.setCreator(username);
        }

        // Make sure we have at least the meta data dirty status
        tdoc.setMetaDataDirty(true);

        // Validate the document if we have xvalidate=1 in the request
        if ("1".equals(request.getParameter("xvalidate"))) {
            boolean validationResult = tdoc.validate(context);
            // If the validation fails we should show the "Inline form" edit mode
            if (validationResult == false) {
                // Set display context to 'edit'
                context.put("display", "edit");
                // Set the action used by the "Inline form" edit mode as the context action. See #render(XWikiContext).
                context.setAction(tdoc.getDefaultEditMode(context));
                // Set the document in the context
                context.put("doc", doc);
                context.put("cdoc", tdoc);
                context.put("tdoc", tdoc);
                Document vdoc = tdoc.newDocument(context);
                VelocityContext vcontext = (VelocityContext) context.get("vcontext");
                vcontext.put("doc", doc.newDocument(context));
                vcontext.put("cdoc", vdoc);
                vcontext.put("tdoc", vdoc);
                // Force the "Inline form" edit mode.
                vcontext.put("editor", "inline");
                return true;
            }
        }

        // We get the comment to be used from the document
        // It was read using readFromForm
        xwiki.saveDocument(tdoc, tdoc.getComment(), tdoc.isMinorEdit(), context);
        XWikiLock lock = tdoc.getLock(context);
        if (lock != null) {
            tdoc.removeLock(context);
        }

        return false;
    }

    @Override
    public boolean action(XWikiContext context) throws XWikiException
    {
        // CSRF prevention
        if (!csrfTokenCheck(context)) {
            return false;
        }

        if (save(context)) {
            return true;
        }
        // forward to view
        if (Utils.isAjaxRequest(context)) {
            context.getResponse().setStatus(HttpServletResponse.SC_NO_CONTENT);
        } else {
            sendRedirect(context.getResponse(), Utils.getRedirect("view", context));
        }
        return false;
    }

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        XWikiException e = (XWikiException) context.get("exception");
        if ((e != null) && (e.getCode() == XWikiException.ERROR_XWIKI_APP_DOCUMENT_NOT_EMPTY)) {
            return "docalreadyexists";
        }

        if ("edit".equals(context.get("display"))) {
            // When form validation (xvalidate) fails the save action forwards to the "Inline form" edit mode. In this
            // case the context action is not "save" anymore because it was changed in #save(XWikiContext). The context
            // action should be the action used by the "Inline form" edit mode (either "edit" or "inline").
            return context.getAction();
        }

        return "exception";
    }
}
