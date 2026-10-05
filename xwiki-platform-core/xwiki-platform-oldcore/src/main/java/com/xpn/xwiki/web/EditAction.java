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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.function.FailableRunnable;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;

/**
 * Initializes a document before it is edited.
 *
 * @version $Id$
 */
@Component
@Named("edit")
@Singleton
public class EditAction extends XWikiAction
{
    /**
     * The object used for logging.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(EditAction.class);

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> documentReferenceUserReferenceResolver;

    @Inject
    private CSRFToken csrf;

    /**
     * Default constructor.
     */
    public EditAction()
    {
        this.waitForXWikiInitialization = false;
    }

    @Override
    protected Class<? extends XWikiForm> getFormClass()
    {
        return EditForm.class;
    }

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        try {
            XWikiDocument editedDocument = prepareEditedDocument(context);
            maybeLockDocument(editedDocument, context);
        } catch (XWikiException e) {
            if (e.getCode() == XWikiException.ERROR_XWIKI_APP_DOCUMENT_NOT_EMPTY) {
                context.put("exception", e);
                return "docalreadyexists";
            } else {
                throw e;
            }
        }

        // Make sure object property fields are displayed in edit mode.
        // See XWikiDocument#display(String, BaseObject, XWikiContext)
        // TODO: Revisit the display mode after the inline action is removed. Is the display mode still needed when
        // there is only one edit action?
        context.put("display", "edit");
        return "edit";
    }

    /**
     * Determines the edited document (translation) and updates it based on the template specified on the request and
     * any additional request parameters that overwrite the default values from the template.
     *
     * @param context the XWiki context
     * @return the edited document
     * @throws XWikiException if something goes wrong
     */
    protected XWikiDocument prepareEditedDocument(XWikiContext context) throws XWikiException
    {
        EditForm editForm = (EditForm) context.getForm();

        // Determine the edited document (translation).
        XWikiDocument editedDocument = getEditedDocument(editForm, context);

        // Update the edited document based on form inputs
        int sectionNumber = updateDocumentFromRequest(editedDocument, editForm, context);

        // Set the current user as creator, author and contentAuthor when the edited document is newly created to avoid
        // using XWikiGuest instead (because those fields were not previously initialized).
        if (editedDocument.isNew()) {
            editedDocument.setCreatorReference(context.getUserReference());
            editedDocument.setAuthorReference(context.getUserReference());
            editedDocument.setContentAuthorReference(context.getUserReference());
        }

        // The section title is extracted from the edited content, which can be provided on the request, so it has to be
        // rendered only once the restricted flag and the authors of the edited document take the request into account.
        String sectionEditingTitle = getSectionEditingTitle(editedDocument, editForm, sectionNumber, context);
        if (sectionEditingTitle != null) {
            updateDocument(editedDocument, () -> editedDocument.setTitle(sectionEditingTitle), context);
        }

        editedDocument.readTemporaryUploadedFiles(editForm);

        // Expose the edited document on the XWiki context and the Velocity context.
        putDocumentOnContext(editedDocument, context);

        return editedDocument;
    }

    /**
     * There are three important use cases:
     * <ul>
     * <li>editing or creating the original translation (for the default language)</li>
     * <li>editing an existing document translation</li>
     * <li>creating a new translation.</i>
     * </ul>
     * Most of the code deals with the really bad way the default language can be specified (empty string, 'default' or
     * a real language code).
     *
     * @param editForm the form inputs
     * @param context the XWiki context
     * @return the edited document translation based on the language specified on the request
     * @throws XWikiException if something goes wrong
     */
    private XWikiDocument getEditedDocument(EditForm editForm, XWikiContext context) throws XWikiException
    {
        XWikiDocument doc = context.getDoc();  
        boolean hasTranslation = doc != context.get("tdoc");

        // We have to clone the context document because it is cached and the changes we are going to make are valid
        // only for the duration of the current request.
        doc = doc.clone();
        context.put("doc", doc);


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
            tdoc.setDefaultLocale(doc.getDefaultLocale());
            // Mark the translation. It's important to know whether a document is a translation or not, especially
            // for the sheet manager which needs to access the objects using the default document not one of its
            // translations.
            tdoc.setTitle(doc.getTitle());
            tdoc.setContent(doc.getContent());
            tdoc.setSyntax(doc.getSyntax());
            tdoc.setAuthorReference(context.getUserReference());
            tdoc.setStore(doc.getStore());
        } else {
            // Edit an existing translation. Clone the translated document object to be sure that the changes we are
            // going to make will last only for the duration of the current request.
            tdoc = ((XWikiDocument) context.get("tdoc")).clone();
        }

        return tdoc;
    }

    /**
     * Updates the given document based on the form inputs, the template and the title and content request parameters.
     *
     * @param document the document to update
     * @param editForm the form inputs
     * @param context the XWiki context
     * @return the number of the edited section, {@code 0} if no section is edited
     * @throws XWikiException if something goes wrong
     */
    private int updateDocumentFromRequest(XWikiDocument document, EditForm editForm, XWikiContext context)
        throws XWikiException
    {
        // Check if section editing is enabled and if a section is specified.
        boolean sectionEditingEnabled = context.getWiki().hasSectionEdit(context);
        int sectionNumber = sectionEditingEnabled ? NumberUtils.toInt(context.getRequest().getParameter("section")) : 0;
        getCurrentScriptContext().setAttribute("sectionNumber", sectionNumber, ScriptContext.ENGINE_SCOPE);

        updateDocument(document, () -> {
            document.readDocMetaFromForm(editForm, context);
            // Update the edited document based on the template specified on the request.
            readFromTemplate(document, editForm.getTemplate(), context);
            // The default values from the template can be overwritten by additional request parameters.
            updateDocumentTitleAndContentFromRequest(document, editForm, sectionNumber);
            document.readAddedUpdatedAndRemovedObjectsFromForm(editForm, context);
        }, context);

        return sectionNumber;
    }

    /**
     * Applies the given modifications to the document and, if they modify it, makes the document restricted when the
     * request doesn't provide a valid CSRF token and makes the current user its author.
     *
     * @param document the document to update
     * @param modifications the modifications to apply
     * @param context the XWiki context
     * @throws XWikiException if something goes wrong
     */
    private void updateDocument(XWikiDocument document, FailableRunnable<XWikiException> modifications,
        XWikiContext context) throws XWikiException
    {
        // Remember dirty status
        boolean metadataDirty = document.isMetaDataDirty();
        boolean contentDirty = document.isContentDirty();

        // Reset the dirty status to find out if the document was modified by the given modifications
        document.setMetaDataDirty(false);
        document.setContentDirty(false);

        modifications.run();

        // Check if the document in modified
        if (document.isMetaDataDirty() || document.isContentDirty()) {
            // If the document is modified make sure a valid CSRF is provided
            String token = context.getRequest().getParameter("form_token");
            if (!this.csrf.isTokenValid(token)) {
                // or make the document restricted
                document.setRestricted(true);
            }
        }

        // Restore dirty status
        document.setMetaDataDirty(document.isMetaDataDirty() || metadataDirty);
        document.setContentDirty(document.isContentDirty() || contentDirty);

        // If the metadata is modified, modify the effective metadata author
        if (document.isMetaDataDirty()) {
            UserReference userReference =
                this.documentReferenceUserReferenceResolver.resolve(context.getUserReference());
            document.setAuthor(userReference);
        }

        // If the content is modified, modify the content author
        if (document.isContentDirty()) {
            UserReference userReference =
                this.documentReferenceUserReferenceResolver.resolve(context.getUserReference());
            document.getAuthors().setContentAuthor(userReference);
        }
    }

    /**
     * Updates the title and content of the given document with values taken from the 'title' and 'content' request
     * parameters or based on the document section specified on the request.
     *
     * @param document the document whose title and content should be updated
     * @param editForm the form inputs
     * @param sectionNumber the number of the edited section, {@code 0} if no section is edited
     * @throws XWikiException if something goes wrong
     */
    private void updateDocumentTitleAndContentFromRequest(XWikiDocument document, EditForm editForm,
        int sectionNumber) throws XWikiException
    {
        // Update the edited content.
        if (editForm.getContent() != null) {
            document.setContent(editForm.getContent());
        } else if (sectionNumber > 0) {
            document.setContent(document.getContentOfSection(sectionNumber));
        }

        // Update the edited title.
        if (editForm.getTitle() != null) {
            document.setTitle(editForm.getTitle());
        }
    }

    /**
     * Computes the title to display while editing a document section. The title of the section is rendered with the
     * rights of the content author of the given document, and in restricted mode if the document is restricted.
     *
     * @param document the edited document
     * @param editForm the form inputs
     * @param sectionNumber the number of the edited section, {@code 0} if no section is edited
     * @param context the XWiki context
     * @return the title to display while editing the section, or {@code null} if the title should not be modified
     * @throws XWikiException if something goes wrong
     */
    private String getSectionEditingTitle(XWikiDocument document, EditForm editForm, int sectionNumber,
        XWikiContext context) throws XWikiException
    {
        if (editForm.getTitle() == null && sectionNumber > 0 && document.getSections().size() > 0) {
            // The edited content is either the content of the specified section or the content provided on the
            // request. We assume the content provided on the request is meant to overwrite the specified section.
            // In both cases the document content is currently having one section, so we can take its title.
            String sectionTitle = document.getDocumentSection(1).getSectionTitle();
            if (StringUtils.isNotBlank(sectionTitle)) {
                // We cannot edit the page title while editing a page section so this title is for display only.
                // Use the edited document as secure document so that the section title is rendered with the rights of
                // its content author and not the rights of the content author of the saved document.
                String sectionPlainTitle = document.getRenderedContent(sectionTitle, document.getSyntax().toIdString(),
                    Syntax.PLAIN_1_0.toIdString(), false, document, context);
                return localizePlainOrKey("core.editors.content.titleField.sectionEditingFormat",
                    document.getRenderedTitle(Syntax.PLAIN_1_0, context), sectionNumber, sectionPlainTitle);
            }
        }

        return null;
    }

    /**
     * Exposes the given document in the XWiki context and the Velocity context under the 'tdoc' and 'cdoc' keys.
     *
     * @param document the document to expose
     * @param context the XWiki context
     */
    private void putDocumentOnContext(XWikiDocument document, XWikiContext context)
    {
        context.put("tdoc", document);
        // Old XWiki applications that are still using the inline action might expect the cdoc (content document) to be
        // properly set on the context. Let's expose the given document also as cdoc for backward compatibility.
        context.put("cdoc", context.get("tdoc"));
    }

    /**
     * Locks the given document unless it is already locked by a different user and the current user didn't request to
     * force the lock.
     *
     * @param document the document to lock
     * @param context the XWiki context
     */
    private void maybeLockDocument(XWikiDocument document, XWikiContext context)
    {
        try {
            XWikiLock lock = document.getLock(context);
            EditForm editForm = (EditForm) context.getForm();
            if (lock == null || lock.getUserName().equals(context.getUser()) || editForm.isLockForce()) {
                document.setLock(context.getUser(), context);
            }
        } catch (Exception e) {
            // Lock should never make XWiki fail, but we should log any related information.
            LOGGER.error("Exception while setting up lock", e);
        }
    }
}
