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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;

/**
 * Initializes a document before it is edited.
 * 
 * @version $Id$
 */
public class EditAction extends XWikiAction
{
    /**
     * The object used for logging.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(EditAction.class);

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        XWikiDocument doc = context.getDoc();
        EditForm editForm = (EditForm) context.getForm();
        VelocityContext vcontext = (VelocityContext) context.get("vcontext");

        boolean hasTranslation = doc != context.get("tdoc");

        // We have to clone the context document because it is cached and the changes we are going to make are valid
        // only for the duration of the current request.
        doc = doc.clone();
        context.put("doc", doc);
        vcontext.put("doc", doc.newDocument(context));

        synchronized (doc) {
            doc.readDocMetaFromForm(editForm, context);

            String language = context.getWiki().getLanguagePreference(context);
            if (doc.isNew() && doc.getDefaultLanguage().equals("")) {
                doc.setDefaultLanguage(language);
            }

            String languageToEdit = StringUtils.isEmpty(editForm.getLanguage()) ? language : editForm.getLanguage();

            // If no specific language is set or if it is "default" then we edit the current doc.
            if (languageToEdit == null || languageToEdit.equals("default")) {
                languageToEdit = "";
            }
            // If the document is new or if the language to edit is the default language then we edit the default
            // translation.
            if (doc.isNew() || doc.getDefaultLanguage().equals(languageToEdit)) {
                languageToEdit = "";
            }
            // If the doc does not exist in the language to edit and the language was not explicitly set in the URL then
            // we edit the default document translation. This prevents use from creating unneeded translations.
            if (!hasTranslation && StringUtils.isEmpty(editForm.getLanguage())) {
                languageToEdit = "";
            }

            // Initialize the translated document.
            XWikiDocument tdoc;
            if (languageToEdit.equals("")) {
                // Edit the default document translation (default language).
                tdoc = doc;
                if (doc.isNew()) {
                    doc.setDefaultLanguage(language);
                    doc.setLanguage("");
                }
            } else if (!hasTranslation && context.getWiki().isMultiLingual(context)) {
                // Edit a new translation.
                tdoc = new XWikiDocument(doc.getDocumentReference());
                tdoc.setLanguage(languageToEdit);
                tdoc.setContent(doc.getContent());
                tdoc.setSyntax(doc.getSyntax());
                tdoc.setAuthorReference(context.getUserReference());
                tdoc.setStore(doc.getStore());
            } else {
                // Edit an existing translation. Clone the translated document object to be sure that the changes we are
                // going to make will last only for the duration of the current request.
                tdoc = ((XWikiDocument) context.get("tdoc")).clone();
            }

            // Check if section editing is enabled and if a section is specified.
            boolean sectionEditingEnabled = context.getWiki().hasSectionEdit(context);
            int sectionNumber =
                sectionEditingEnabled ? NumberUtils.toInt(context.getRequest().getParameter("section")) : 0;
            vcontext.put("sectionNumber", sectionNumber);

            try {
                // Try to update the edited document based on the template specified on the request.
                tdoc.readFromTemplate(editForm, context);
            } catch (XWikiException e) {
                if (e.getCode() == XWikiException.ERROR_XWIKI_APP_DOCUMENT_NOT_EMPTY) {
                    context.put("exception", e);
                    return "docalreadyexists";
                }
            }

            // Update the edited content.
            if (editForm.getContent() != null) {
                tdoc.setContent(editForm.getContent());
            } else if (sectionNumber > 0) {
                tdoc.setContent(tdoc.getContentOfSection(sectionNumber));
            }

            // Update the edited title.
            if (editForm.getTitle() != null) {
                tdoc.setTitle(editForm.getTitle());
            } else if (sectionNumber > 0) {
                // The edited content is either the content of the specified section or the content provided on the
                // request. We assume the content provided on the request is meant to overwrite the specified section.
                // In both cases the document content is currently having one section, so we can take its title.
                String sectionTitle = tdoc.getDocumentSection(1).getSectionTitle();
                if (StringUtils.isNotBlank(sectionTitle)) {
                    tdoc.setTitle(context.getMessageTool().get("core.editors.content.titleField.sectionEditingFormat",
                        tdoc.getRenderedTitle(Syntax.PLAIN_1_0, context), sectionNumber, sectionTitle));
                }
            }

            // Update the edited objects.
            tdoc.readObjectsFromForm(editForm, context);

            // Expose the translated document on the XWiki context and the Velocity context.
            context.put("tdoc", tdoc);
            vcontext.put("tdoc", tdoc.newDocument(context));
            // XWiki applications that were previously using the inline action might still expect the cdoc (content
            // document) to be properly set on the context. Expose tdoc (translated document) also as cdoc for backward
            // compatibility.
            context.put("cdoc", context.get("tdoc"));
            vcontext.put("cdoc", vcontext.get("tdoc"));

            /* Setup a lock */
            try {
                XWikiLock lock = tdoc.getLock(context);
                if (lock == null || lock.getUserName().equals(context.getUser()) || editForm.isLockForce()) {
                    tdoc.setLock(context.getUser(), context);
                }
            } catch (Exception e) {
                // Lock should never make XWiki fail, but we should log any related information.
                LOGGER.error("Exception while setting up lock", e);
            }
        }

        // Make sure object property fields are displayed in edit mode.
        // See XWikiDocument#display(String, BaseObject, XWikiContext)
        // TODO: Revisit the display mode after the inline action is removed. Is the display mode still needed when
        // there is only one edit action?
        context.put("display", "edit");
        return "edit";
    }
}
