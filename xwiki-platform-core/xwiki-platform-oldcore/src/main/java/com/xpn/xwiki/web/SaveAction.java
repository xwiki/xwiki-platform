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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.script.ScriptContext;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.suigeneris.jrcs.diff.DifferentiationFailedException;
import org.suigeneris.jrcs.diff.delta.Delta;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.job.Job;
import org.xwiki.localization.LocaleUtils;
import org.xwiki.logging.LogLevel;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.refactoring.job.CreateRequest;
import org.xwiki.refactoring.script.RefactoringScriptService;
import org.xwiki.script.service.ScriptService;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.MetaDataDiff;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.doc.merge.MergeResult;
import com.xpn.xwiki.objects.ObjectDiff;

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

    protected static final String ASYNC_PARAM = "async";

    /**
     * The key to retrieve the saved object version from the context.
     */
    private static final String SAVED_OBJECT_VERSION_KEY = "SaveAction.savedObjectVersion";

    /**
     * The context key to know if a document has been merged for saving it.
     */
    private static final String MERGED_DOCUMENTS = "SaveAction.mergedDocuments";

    /**
     * Parameter value used with forceSave to specify that the merge should be performed even if there is conflicts.
     */
    private static final String FORCE_SAVE_MERGE = "merge";

    /**
     * Parameter value used with forceSave to specify that no merge should be done but the current document should
     * override the previous one.
     */
    private static final String FORCE_SAVE_OVERRIDE = "override";

    private DocumentRevisionProvider documentRevisionProvider;

    /**
     * The redirect class, used to mark pages that are redirect place-holders, i.e. hidden pages that serve only for
     * redirecting the user to a different page (e.g. when a page has been moved).
     */
    private static final EntityReference REDIRECT_CLASS =
        new EntityReference("RedirectClass", EntityType.DOCUMENT, new EntityReference("XWiki", EntityType.SPACE));

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
        EditForm form = (EditForm) context.getForm();

        // Check save session
        int sectionNumber = 0;
        if (request.getParameter("section") != null && xwiki.hasSectionEdit(context)) {
            sectionNumber = Integer.parseInt(request.getParameter("section"));
        }

        XWikiDocument originalDoc = doc;

        // We need to clone this document first, since a cached storage would return the same object for the
        // following requests, so concurrent request might get a partially modified object, or worse, if an error
        // occurs during the save, the cached object will not reflect the actual document at all.
        doc = doc.clone();

        String language = form.getLanguage();
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
                originalDoc = tdoc;
            }
        }

        if (doc.isNew()) {
            doc.setLocale(Locale.ROOT);
            if (doc.getDefaultLocale() == Locale.ROOT) {
                doc.setDefaultLocale(
                    LocaleUtils.toLocale(context.getWiki().getLanguagePreference(context), Locale.ROOT));
            }
        }

        try {
            tdoc.readFromTemplate(form.getTemplate(), context);
        } catch (XWikiException e) {
            if (e.getCode() == XWikiException.ERROR_XWIKI_APP_DOCUMENT_NOT_EMPTY) {
                context.put("exception", e);
                return true;
            }
        }

        if (sectionNumber != 0) {
            XWikiDocument sectionDoc = tdoc.clone();
            sectionDoc.readFromForm(form, context);
            String sectionContent = sectionDoc.getContent() + "\n";
            String content = tdoc.updateDocumentSection(sectionNumber, sectionContent);
            tdoc.setContent(content);
            tdoc.setComment(sectionDoc.getComment());
            tdoc.setMinorEdit(sectionDoc.isMinorEdit());
        } else {
            tdoc.readFromForm(form, context);
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
                // Force the "Inline form" edit mode.
                getCurrentScriptContext().setAttribute("editor", "inline", ScriptContext.ENGINE_SCOPE);

                return true;
            }
        }

        // Remove the redirect object if the save request doesn't update it. This allows users to easily overwrite
        // redirect place-holders that are created when we move pages around.
        if (tdoc.getXObject(REDIRECT_CLASS) != null && request.getParameter("XWiki.RedirectClass_0_location") == null) {
            tdoc.removeXObjects(REDIRECT_CLASS);
        }

        // We only proceed on the check between versions in case of AJAX request, so we currently stay in the edit form
        // This can be improved later by displaying a nice UI with some merge options in a sync request.
        // For now we don't want our user to loose their changes.
        if (isConflictCheckEnabled() && Utils.isAjaxRequest(context)
            && request.getParameter("previousVersion") != null) {
            if (isConflictingWithVersion(context, originalDoc, tdoc)) {
                return true;
            }
        }

        // Make sure the user is allowed to make this modification
        context.getWiki().checkSavingDocument(context.getUserReference(), tdoc, tdoc.getComment(), tdoc.isMinorEdit(),
            context);

        // We get the comment to be used from the document
        // It was read using readFromForm
        xwiki.saveDocument(tdoc, tdoc.getComment(), tdoc.isMinorEdit(), context);

        context.put(SAVED_OBJECT_VERSION_KEY, tdoc.getRCSVersion());

        Job createJob = startCreateJob(tdoc.getDocumentReference(), form);
        if (createJob != null) {
            if (isAsync(request)) {
                if (Utils.isAjaxRequest(context)) {
                    // Redirect to the job status URL of the job we have just launched.
                    sendRedirect(context.getResponse(), String.format("%s/rest/jobstatus/%s?media=json",
                        context.getRequest().getContextPath(), serializeJobId(createJob.getRequest().getId())));
                }

                // else redirect normally and the operation will eventually finish in the background.
                // Note: It is preferred that async mode is called in an AJAX request that can display the progress.
            } else {
                // Sync mode, default, wait for the work to finish.
                try {
                    createJob.join();
                } catch (InterruptedException e) {
                    throw new XWikiException(String.format(
                        "Interrupted while waiting for template [%s] to be processed when creating the document [%s]",
                        form.getTemplate(), tdoc.getDocumentReference()), e);
                }
            }
        } else {
            // Nothing more to do, just unlock the document.
            XWikiLock lock = tdoc.getLock(context);
            if (lock != null) {
                tdoc.removeLock(context);
            }
        }

        return false;
    }

    private boolean isConflictCheckEnabled()
    {
        ConfigurationSource configurationSource = Utils.getComponent(ConfigurationSource.class, "xwikiproperties");
        return configurationSource.getProperty("edit.conflictChecking.enabled", true);
    }

    private DocumentRevisionProvider getDocumentRevisionProvider()
    {
        if (this.documentRevisionProvider == null) {
            this.documentRevisionProvider = Utils.getComponent(DocumentRevisionProvider.class);
        }

        return this.documentRevisionProvider;
    }

    /**
     * Check if the version of the document being saved is conflicting with another version. This check is done by
     * getting the "previousVersion" parameter from the request and comparing it with latest version of the document. If
     * the current version of the document is not the same as the previous one, a diff is computed on the document
     * content: a conflict is detected only if the contents are different.
     * 
     * @param context the current context of the request.
     * @param originalDoc the original version of the document being modified that will be saved (i.e. before content
     *            changes). We don't retrieve it through context since it can be a translation.
     * @return true in case of conflict. If it's true, the answer is immediately sent to the client.
     */
    private boolean isConflictingWithVersion(XWikiContext context, XWikiDocument originalDoc, XWikiDocument modifiedDoc)
        throws XWikiException
    {
        XWikiRequest request = context.getRequest();

        // in case of force save we skip the check.
        if (FORCE_SAVE_OVERRIDE.equals(request.getParameter("forceSave"))) {
            return false;
        }

        // the document is new we don't have to check the version date or anything
        if ("true".equals(request.getParameter("isNew")) && originalDoc.isNew()) {
            return false;
        }

        // TODO The check of the previousVersion should be done at a lower level or with a semaphore since
        // another job might have saved a different version of the document
        Version previousVersion = new Version(request.getParameter("previousVersion"));
        Version latestVersion = originalDoc.getRCSVersion();

        DateTime editingVersionDate = new DateTime(request.getParameter("editingVersionDate"));
        DateTime latestVersionDate = new DateTime(originalDoc.getDate());

        // we ensure that nobody edited the document between the moment the user started to edit and now
        if (!latestVersion.equals(previousVersion) || latestVersionDate.isAfter(editingVersionDate)) {
            try {
                XWikiDocument previousDoc =
                    getDocumentRevisionProvider().getRevision(originalDoc, previousVersion.toString());

                // if doc is new and we're here: it's a conflict, we can skip the diff check
                // we also check that the previousDoc revision exists to avoid an exception if it has been deleted
                if (!originalDoc.isNew() && previousDoc != null) {
                    // if changes between previousVersion and latestVersion didn't change the content, it means it's ok
                    // to save the current changes.
                    List<Delta> contentDiff =
                        originalDoc.getContentDiff(previousVersion.toString(), latestVersion.toString(), context);

                    // we also need to check the object diff, to be sure there's no conflict with the inline form.
                    List<List<ObjectDiff>> objectDiff =
                        originalDoc.getObjectDiff(previousVersion.toString(), latestVersion.toString(), context);

                    // we finally check the metadata: we want to get a conflict if the title changed, or the syntax,
                    // the default language etc.
                    // However we have to filter out the author: we don't care if the author reference changed and it's
                    // actually most certainly the case if we are here.
                    List<MetaDataDiff> metaDataDiff =
                        originalDoc.getMetaDataDiff(previousVersion.toString(), latestVersion.toString(), context);

                    List<MetaDataDiff> filteredMetaDataDiff = new ArrayList<>();
                    for (MetaDataDiff dataDiff : metaDataDiff) {
                        if (!dataDiff.getField().equals("author")) {
                            filteredMetaDataDiff.add(dataDiff);
                        }
                    }

                    if (contentDiff.isEmpty() && objectDiff.isEmpty() && filteredMetaDataDiff.isEmpty()) {
                        return false;
                    } else {
                        MergeResult mergeResult =
                            modifiedDoc.merge(previousDoc, originalDoc, new MergeConfiguration(), context);
                        if (FORCE_SAVE_MERGE.equals(request.getParameter("forceSave"))
                            || mergeResult.getLog().getLogs(LogLevel.ERROR).isEmpty()) {
                            context.put(MERGED_DOCUMENTS, "true");
                            return false;
                        }
                    }
                }

                // if the revision has been deleted or if the content/object diff is not empty
                // we have a conflict.
                // TODO: Improve it to return the diff between the current version and the latest recorder
                Map<String, String> jsonObject = new LinkedHashMap<>();
                jsonObject.put("previousVersion", previousVersion.toString());
                jsonObject.put("previousVersionDate", editingVersionDate.toString());
                jsonObject.put("latestVersion", latestVersion.toString());
                jsonObject.put("latestVersionDate", latestVersionDate.toString());
                this.answerJSON(context, HttpStatus.SC_CONFLICT, jsonObject);
                return true;
            } catch (DifferentiationFailedException e) {
                throw new XWikiException("Error while loading the diff", e);
            }
        }
        return false;
    }

    @Override
    public boolean action(XWikiContext context) throws XWikiException
    {
        // CSRF prevention
        if (!csrfTokenCheck(context, true)) {
            return false;
        }

        if (save(context)) {
            return true;
        }

        // forward to view
        if (Utils.isAjaxRequest(context)) {
            Map<String, String> jsonAnswer = new LinkedHashMap<>();
            Version newVersion = (Version) context.get(SAVED_OBJECT_VERSION_KEY);
            jsonAnswer.put("newVersion", newVersion.toString());
            if ("true".equals(context.get(MERGED_DOCUMENTS))) {
                jsonAnswer.put("mergedDocument", "true");
            }
            answerJSON(context, HttpStatus.SC_OK, jsonAnswer);
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

    private boolean isAsync(XWikiRequest request)
    {
        return "true".equals(request.get(ASYNC_PARAM));
    }

    private Job startCreateJob(EntityReference entityReference, EditForm editForm) throws XWikiException
    {
        if (StringUtils.isBlank(editForm.getTemplate())) {
            // No template specified, nothing more to do.
            return null;
        }

        // If a template is set in the request, then this is a create action which needs to be handled by a create job,
        // but skipping the target document, which is now already saved by the save action.

        RefactoringScriptService refactoring =
            (RefactoringScriptService) Utils.getComponent(ScriptService.class, "refactoring");

        CreateRequest request = refactoring.getRequestFactory().createCreateRequest(Arrays.asList(entityReference));
        request.setCheckAuthorRights(false);
        // Set the target document.
        request.setEntityReferences(Arrays.asList(entityReference));
        // Set the template to use.
        DocumentReferenceResolver<String> resolver =
            Utils.getComponent(DocumentReferenceResolver.TYPE_STRING, "currentmixed");
        EntityReference templateReference = resolver.resolve(editForm.getTemplate());
        request.setTemplateReference(templateReference);
        // We`ve already created and populated the fields of the target document, focus only on the remaining children
        // specified in the template.
        request.setSkippedEntities(Arrays.asList(entityReference));

        Job createJob = refactoring.create(request);
        if (createJob != null) {
            return createJob;
        } else {
            throw new XWikiException(String.format("Failed to schedule the create job for [%s]", entityReference),
                refactoring.getLastError());
        }
    }

    private String serializeJobId(List<String> jobId)
    {
        return StringUtils.join(jobId, "/");
    }
}
