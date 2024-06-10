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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.suigeneris.jrcs.diff.DifferentiationFailedException;
import org.suigeneris.jrcs.diff.delta.Delta;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.diff.ConflictDecision;
import org.xwiki.job.Job;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.refactoring.job.CreateRequest;
import org.xwiki.refactoring.script.RefactoringScriptService;
import org.xwiki.script.service.ScriptService;
import org.xwiki.store.TemporaryAttachmentSessionsManager;
import org.xwiki.store.merge.MergeConflictDecisionsManager;
import org.xwiki.store.merge.MergeDocumentResult;
import org.xwiki.store.merge.MergeManager;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.MetaDataDiff;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.internal.mandatory.RedirectClassDocumentInitializer;
import com.xpn.xwiki.objects.ObjectDiff;

/**
 * Action used for saving and proceeding to view the saved page.
 * <p>
 * Used as a generic action for saving documents.
 *
 * @version $Id$
 */
@Component
@Named("save")
@Singleton
public class SaveAction extends EditAction
{
    /** The identifier of the save action. */
    public static final String ACTION_NAME = "save";

    protected static final String ASYNC_PARAM = "async";

    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SaveAction.class);

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

    @Inject
    private DocumentRevisionProvider documentRevisionProvider;

    @Inject
    private MergeManager mergeManager;

    @Inject
    private MergeConflictDecisionsManager conflictDecisionsManager;

    @Inject
    private TemporaryAttachmentSessionsManager temporaryAttachmentSessionsManager;

    @Inject
    private UserReferenceResolver<CurrentUserReference> currentUserResolver;

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

        if (doc.isNew() && !this.isEntityReferenceNameValid(doc.getDocumentReference())) {
            context.put("message", "entitynamevalidation.create.invalidname");
            context.put("messageParameters",
                new Object[] { getLocalSerializer().serialize(doc.getDocumentReference())});
            return true;
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
                // In that specific case, we want the original doc to be the translation document so that we
                // never raised a conflict.
                originalDoc = tdoc;
            } else if (tdoc != doc) {
                // Saving an existing document translation (but not the default one).
                // Same as above, clone the object retrieved from the store cache.
                originalDoc = tdoc;
                tdoc = tdoc.clone();
            }
        }

        if (doc.isNew()) {
            doc.setLocale(Locale.ROOT);
            if (doc.getDefaultLocale() == Locale.ROOT) {
                doc.setDefaultLocale(xwiki.getLocalePreference(context));
            }
        }

        try {
            readFromTemplate(tdoc, form.getTemplate(), context);
        } catch (XWikiException e) {
            if (e.getCode() == XWikiException.ERROR_XWIKI_APP_DOCUMENT_NOT_EMPTY) {
                context.put("exception", e);
                return true;
            }
        }

        // Convert the content and the meta data of the edited document and its translations if the syntax has changed
        // and the request is asking for a syntax conversion. We do this after applying the template because the
        // template may have content in the previous syntax that needs to be converted. We do this before applying the
        // changes from the submitted form because it may contain content that was already converted.
        if (form.isConvertSyntax() && !tdoc.getSyntax().toIdString().equals(form.getSyntaxId())) {
            convertSyntax(tdoc, form.getSyntaxId(), context);
        }

        if (sectionNumber != 0) {
            XWikiDocument sectionDoc = tdoc.clone();
            sectionDoc.readFromForm(form, context);
            String sectionContent = sectionDoc.getContent() + "\n";
            String content = tdoc.updateDocumentSection(sectionNumber, sectionContent);

            // The list of attachments might have been modified by a new upload that is read in sectionDoc.readFromForm
            // so we ensure to keep the updated list of attachments.
            tdoc.setAttachmentList(sectionDoc.getAttachmentList());
            tdoc.setContent(content);
            tdoc.setComment(sectionDoc.getComment());
            tdoc.setMinorEdit(sectionDoc.isMinorEdit());
        } else {
            tdoc.readFromForm(form, context);
        }

        UserReference currentUserReference = this.currentUserResolver.resolve(CurrentUserReference.INSTANCE);
        tdoc.getAuthors().setOriginalMetadataAuthor(currentUserReference);
        tdoc.getAuthors().setEffectiveMetadataAuthor(request.getEffectiveAuthor());

        if (tdoc.isNew()) {
            tdoc.getAuthors().setCreator(currentUserReference);
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
        if (tdoc.getXObject(RedirectClassDocumentInitializer.REFERENCE) != null
            && request.getParameter("XWiki.RedirectClass_0_location") == null) {
            tdoc.removeXObjects(RedirectClassDocumentInitializer.REFERENCE);
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
        xwiki.checkSavingDocument(context.getUserReference(), tdoc, tdoc.getComment(), tdoc.isMinorEdit(), context);

        // We get the comment to be used from the document
        // It was read using readFromForm
        xwiki.saveDocument(tdoc, tdoc.getComment(), tdoc.isMinorEdit(), context);
        this.temporaryAttachmentSessionsManager.removeUploadedAttachments(tdoc.getDocumentReference());

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

    /**
     * Retrieve the conflict decisions made from the request and fill the conflict decision manager with them. We handle
     * two list of parameters here: - mergeChoices: those parameters are on the form [conflict id]=[choice] where the
     * choice is defined by the {@link ConflictDecision.DecisionType} values. - customChoices: those parameters are on
     * the form [conflict id]=[encoded string] where the encoded string is actually the desired value to solve the
     * conflict.
     */
    private void recordConflictDecisions(XWikiContext context, DocumentReference documentReference)
    {
        XWikiRequest request = context.getRequest();
        String[] mergeChoices = request.getParameterValues("mergeChoices");
        String[] customChoices = request.getParameterValues("customChoices");

        // Build a map indexed by the conflict ids and whose values are the actual decoded custom values.
        Map<String, String> customChoicesMap = new HashMap<>();
        if (customChoices != null) {
            for (String customChoice : customChoices) {
                String[] splittedCustomChoiceInfo = customChoice.split("=");
                String conflictReference = splittedCustomChoiceInfo[0];
                String customValue = customChoice.substring(conflictReference.length() + 1);
                try {
                    customValue = URLDecoder.decode(customValue, request.getCharacterEncoding());
                } catch (UnsupportedEncodingException e) {
                    LOGGER.error("Error while decoding a custom value decision.", e);
                }
                customChoicesMap.put(conflictReference, customValue);
            }
        }
        if (mergeChoices != null) {
            for (String choice : mergeChoices) {
                String[] splittedChoiceInfo = choice.split("=");
                String conflictReference = splittedChoiceInfo[0];
                String selectedChoice = splittedChoiceInfo[1];
                List<String> customValue = null;

                ConflictDecision.DecisionType decisionType =
                    ConflictDecision.DecisionType.valueOf(selectedChoice.toUpperCase());

                if (decisionType == ConflictDecision.DecisionType.CUSTOM) {
                    customValue = Collections.singletonList(customChoicesMap.get(conflictReference));
                }
                this.conflictDecisionsManager.recordDecision(documentReference, context.getUserReference(),
                    conflictReference, decisionType, customValue);
            }
        }
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

        Date editingVersionDate = new Date(Long.parseLong(request.getParameter("editingVersionDate")));
        Date latestVersionDate = originalDoc.getDate();

        // we ensure that nobody edited the document between the moment the user started to edit and now
        if (!latestVersion.equals(previousVersion) || latestVersionDate.after(editingVersionDate)) {
            try {
                XWikiDocument previousDoc =
                    this.documentRevisionProvider.getRevision(originalDoc, previousVersion.toString());

                // We also check that the previousDoc revision exists to avoid an exception if it has been deleted
                // Note that if we're here and the request says that the document is new, it's not necessarily a
                // conflict: we might be in the case where the doc has been created during the edition because of
                // an image added to it, without updating the client. So it's still accurate to check that the diff
                // hasn't changed.
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
                        MergeConfiguration mergeConfiguration = new MergeConfiguration();

                        // We need the reference of the user and the document in the config to retrieve
                        // the conflict decision in the MergeManager.
                        mergeConfiguration.setUserReference(context.getUserReference());
                        mergeConfiguration.setConcernedDocument(modifiedDoc.getDocumentReferenceWithLocale());

                        // The modified doc is actually the one we should save, so it's ok to modify it directly
                        // and better for performance.
                        mergeConfiguration.setProvidedVersionsModifiables(true);

                        // We need to retrieve the conflict decisions that might have occurred from the request.
                        recordConflictDecisions(context, modifiedDoc.getDocumentReferenceWithLocale());

                        MergeDocumentResult mergeDocumentResult =
                            this.mergeManager.mergeDocument(previousDoc, originalDoc, modifiedDoc, mergeConfiguration);

                        // Be sure to not keep the conflict decisions we might have made if new conflicts occurred
                        // we don't want to pollute the list of decisions.
                        this.conflictDecisionsManager.removeConflictDecisionList(
                            modifiedDoc.getDocumentReferenceWithLocale(), context.getUserReference());

                        // If we don't get any conflict, or if we want to force the merge even with conflicts,
                        // then we pursue to save the document.
                        if (FORCE_SAVE_MERGE.equals(request.getParameter("forceSave"))
                            || !mergeDocumentResult.hasConflicts()) {
                            context.put(MERGED_DOCUMENTS, "true");
                            return false;

                            // If we got merge conflicts and we don't want to force it, then we record the conflict in
                            // order to allow fixing them independently.
                        } else {
                            this.conflictDecisionsManager.recordConflicts(modifiedDoc.getDocumentReferenceWithLocale(),
                                context.getUserReference(),
                                mergeDocumentResult.getConflicts(MergeDocumentResult.DocumentPart.CONTENT));
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
        DocumentReference templateReference = resolveTemplate(editForm.getTemplate());

        if (templateReference == null) {
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

    private void convertSyntax(XWikiDocument doc, String targetSyntaxId, XWikiContext xcontext) throws XWikiException
    {
        // Convert the syntax without saving. The syntax conversion will be saved later along with the other changes.
        doc.convertSyntax(targetSyntaxId, xcontext);

        for (Locale locale : doc.getTranslationLocales(xcontext)) {
            // Skip the edited translation because we handle it separately.
            if (!Objects.equals(locale, doc.getLocale())) {
                XWikiDocument tdoc = doc.getTranslatedDocument(locale, xcontext);
                // Double check if the syntax has changed because each document translation can have a different syntax.
                if (!tdoc.getSyntax().toIdString().equals(targetSyntaxId)) {
                    // Convert the syntax and save the changes.
                    tdoc.convertSyntax(targetSyntaxId, xcontext);
                    xcontext.getWiki().saveDocument(tdoc,
                        String.format("Document converted from syntax %s to syntax %s", tdoc.getSyntax().toIdString(),
                            targetSyntaxId),
                        xcontext);
                }
            }
        }
    }
}
