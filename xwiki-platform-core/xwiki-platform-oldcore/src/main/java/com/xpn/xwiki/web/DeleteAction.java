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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.refactoring.job.DeleteRequest;
import org.xwiki.refactoring.job.PermanentlyDeleteRequest;
import org.xwiki.refactoring.job.RefactoringJobs;
import org.xwiki.refactoring.script.RefactoringScriptService;
import org.xwiki.script.service.ScriptService;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.DeletedDocument;
import com.xpn.xwiki.doc.XWikiDeletedDocument;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Action for delete document to recycle bin and for delete documents from recycle bin.
 *
 * @version $Id$
 */
@Component
@Named("delete")
@Singleton
public class DeleteAction extends XWikiAction
{
    /** confirm parameter name. */
    protected static final String CONFIRM_PARAM = "confirm";

    protected static final String ACTION_NAME = "delete";

    protected static final String ASYNC_PARAM = "async";

    protected static final String RECYCLED_DOCUMENT_ID_PARAM = "id";

    protected static final String EMPTY_RECYCLE_BIN = "emptybin";

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentStringDocumentReferenceResolver;

    private boolean isAsync(XWikiRequest request)
    {
        return "true".equals(request.get(ASYNC_PARAM));
    }

    private boolean doesAffectChildren(XWikiRequest request, DocumentReference documentReference)
    {
        // Security check: we do not "affect children" of a terminal document
        return StringUtils.isNotEmpty(request.getParameter("affectChildren"))
            && "WebHome".equals(documentReference.getName());
    }

    @Override
    public boolean action(XWikiContext context) throws XWikiException
    {
        XWikiRequest request = context.getRequest();

        // If confirm=1 then delete the page. If not, the render action will go to the "delete" page so that the
        // user can confirm. That "delete" page will then call the delete action again with confirm=1.
        if (!"1".equals(request.getParameter(CONFIRM_PARAM))) {
            return true;
        }

        // CSRF prevention
        if (!csrfTokenCheck(context)) {
            return false;
        }

        boolean redirected = delete(context);

        if (!redirected) {
            // If a xredirect param is passed then redirect to the page specified instead of going to the default
            // confirmation page.
            String redirect = Utils.getRedirect(request, null);
            if (redirect != null) {
                sendRedirect(context.getResponse(), redirect);
                redirected = true;
            }
        }

        return !redirected;
    }

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        XWikiRequest request = context.getRequest();
        XWikiDocument doc = context.getDoc();
        String sindex = request.getParameter(RECYCLED_DOCUMENT_ID_PARAM);
        boolean recycleIdIsValid = false;
        if (sindex != null) {
            long index = Long.parseLong(sindex);
            if (context.getWiki().getRecycleBinStore().getDeletedDocument(index, context, true) != null) {
                recycleIdIsValid = true;
            }
        }

        if ("1".equals(request.getParameter(CONFIRM_PARAM))) {
            return "deleted";
        }
        if (doc.isNew() && !recycleIdIsValid) {
            return Utils.getPage(request, "docdoesnotexist");
        }

        return ACTION_NAME;
    }

    protected boolean delete(XWikiContext context) throws XWikiException
    {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();

        String sindex = request.getParameter(RECYCLED_DOCUMENT_ID_PARAM);
        String emptyBin = request.getParameter(EMPTY_RECYCLE_BIN);

        if ("true".equals(emptyBin)) {
            return deleteAllFromRecycleBin(context);
        } else if (sindex != null && xwiki.hasRecycleBin(context)) {
            deleteFromRecycleBin(Long.parseLong(sindex), context);
            return true;
        } else if (doc.isNew()) {
            // Redirect the user to the view template so that he gets the "document doesn't exist" dialog box.
            sendRedirect(response, Utils.getRedirect("view", context));
            return true;
        } else {
            // The document skips the recycle bin only if the user has explicitly made the choice to skip it.
            // The verification of whether the user actually has the right to skip the recycle bin is checked later.
            // If the user is not allowed to skip the recycle bin, but still requested it, his choice is ignored and the
            // document is sent to the recycle bin.
            boolean shouldSkipRecycleBin =
                Boolean.parseBoolean(request.getParameter(DeleteRequest.SHOULD_SKIP_RECYCLE_BIN));
            DocumentReference newBacklinkTarget =
                currentStringDocumentReferenceResolver.resolve(request.getParameter("newBacklinkTarget"));
            boolean updateLinks = Boolean.parseBoolean(request.getParameter(DeleteRequest.UPDATE_LINKS));
            boolean autoRedirect = Boolean.parseBoolean(request.getParameter(DeleteRequest.AUTO_REDIRECT));
            return deleteDocument(context, shouldSkipRecycleBin, newBacklinkTarget, updateLinks, autoRedirect);
        }
    }

    private boolean deleteAllFromRecycleBin(XWikiContext context) throws XWikiException
    {
        RefactoringScriptService refactoring =
            (RefactoringScriptService) Utils.getComponent(ScriptService.class, "refactoring");
        PermanentlyDeleteRequest deleteRequest =
            refactoring.getRequestFactory().createPermanentlyDeleteRequest(Collections.emptyList());
        deleteRequest.setInteractive(isAsync(context.getRequest()));
        deleteRequest.setCheckAuthorRights(false);

        try {
            JobExecutor jobExecutor = Utils.getComponent(JobExecutor.class);
            Job job = jobExecutor.execute(RefactoringJobs.PERMANENTLY_DELETE, deleteRequest);

            if (isAsync(context.getRequest())) {
                List<String> jobId = job.getRequest().getId();
                sendRedirect(context.getResponse(),
                    Utils.getRedirect("view", "xpage=delete&jobId=" + serializeJobId(jobId), context));

                // A redirect has been performed.
                return true;
            }

            // Otherwise...
            try {
                job.join();
            } catch (InterruptedException e) {
                throw new XWikiException("Failed to delete all from the recycle bin", e);
            }

            // No redirect has been performed.
            return false;

        } catch (JobException e) {
            throw new XWikiException("Failed to schedule the delete all from recycle bin job", e);
        }
    }

    private void deleteFromRecycleBin(long index, XWikiContext context) throws XWikiException
    {
        XWiki xwiki = context.getWiki();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();

        XWikiDeletedDocument dd = xwiki.getRecycleBinStore().getDeletedDocument(index, context, true);
        // If the document hasn't been previously deleted (i.e. it's not in the deleted document store) then
        // don't try to delete it and instead redirect to the view page.
        if (dd != null) {
            DeletedDocument ddapi = new DeletedDocument(dd, context);
            if (!ddapi.canDelete()) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                    "You are not allowed to delete a document from the trash "
                        + "immediately after it has been deleted from the wiki");
            }
            if (!dd.getFullName().equals(doc.getFullName())) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_APP_URL_EXCEPTION,
                    "The specified trash entry does not match the current document");
            }
            xwiki.getRecycleBinStore().deleteFromRecycleBin(index, context, true);
        }
        sendRedirect(response, Utils.getRedirect("view", context));
    }

    private boolean deleteDocument(XWikiContext context, boolean shouldSkipRecycleBin,
        DocumentReference newBacklinkTarget, boolean updateLinks, boolean autoRedirect) throws XWikiException
    {
        XWikiRequest request = context.getRequest();
        XWikiDocument doc = context.getDoc();

        // Make sure the user is allowed to make this modification
        context.getWiki().checkDeletingDocument(context.getUserReference(), doc, context);

        EntityReference documentReference =
            doesAffectChildren(request, doc.getDocumentReference()) ? doc.getDocumentReference().getLastSpaceReference()
                : doc.getTranslatedDocument(context).getDocumentReferenceWithLocale();

        return deleteDocument(documentReference, context, shouldSkipRecycleBin, newBacklinkTarget, updateLinks,
            autoRedirect);
    }

    /**
     * Create a job to delete an entity.
     *
     * An entity can either be deleted permanently or moved to the recycle bin.
     * The preference of the user deleting the entity is stored in the {@code shouldSkipRecycleBin} parameter.
     * When {@code shouldSkipRecycleBin} is {@code true} the entity is preferably permanently deleted.
     * Otherwise, the entity is preferably moved to the recycle bin.
     * Note that it only express a choice made by the user.
     * If the user does not have the right to remove an entity permanently, the entity might still be saved in the
     * recycle bin.
     * If the wiki does not have access to a recycle bin, the entity might be permanently removed, regardless of the
     * user's preferences.
     *
     * @param entityReference the entity to delete
     * @param context the current context, used to access the user's request
     * @param shouldSkipRecycleBin if {@code false} the entity is preferably sent to the recycle bin, if {@code true},
     *            the entity is preferably deleted permanently
     * @param newBacklinkTarget reference of an existing document to be used as new backlink target
     * @param updateLinks {@code true} if the links that target the deleted document should be updated to target the new
     *            reference, {@code false} to preserve the old link
     * @param autoRedirect if {@code true}, a redirection will be set from the deleted document location to the new
     *            target
     * @return {@code true} if the user is redirected, {@code false} otherwise
     * @throws XWikiException if anything goes wrong during the document deletion
     * @since 12.8RC1
     */
    protected boolean deleteDocument(EntityReference entityReference, XWikiContext context,
        boolean shouldSkipRecycleBin, DocumentReference newBacklinkTarget, boolean updateLinks, boolean autoRedirect)
        throws XWikiException
    {
        Job deleteJob =
            startDeleteJob(entityReference, context, shouldSkipRecycleBin, newBacklinkTarget, updateLinks, autoRedirect);

        // If the user have asked for an asynchronous delete action...
        if (isAsync(context.getRequest())) {
            List<String> jobId = deleteJob.getRequest().getId();
            // We don't redirect to the delete action because by the time the redirect request reaches the server the
            // specified entity may be already deleted and the current user may not have the delete right anymore (e.g.
            // the current user is no longer the creator).
            sendRedirect(context.getResponse(),
                Utils.getRedirect("view", "xpage=delete&jobId=" + serializeJobId(jobId), context));

            // A redirect has been performed.
            return true;
        }

        // Otherwise...
        try {
            deleteJob.join();
        } catch (InterruptedException e) {
            throw new XWikiException(String.format("Failed to delete [%s]", entityReference), e);
        }

        // No redirect has been performed.
        return false;
    }

    private String serializeJobId(List<String> jobId)
    {
        return StringUtils.join(jobId, "/");
    }

    private Job startDeleteJob(EntityReference entityReference, XWikiContext context, boolean shouldSkipRecycleBin,
        DocumentReference newBacklinkTarget, boolean updateLinks, boolean autoRedirect) throws XWikiException
    {
        RefactoringScriptService refactoring =
            (RefactoringScriptService) Utils.getComponent(ScriptService.class, "refactoring");
        DeleteRequest deleteRequest =
            (DeleteRequest) refactoring.getRequestFactory().createDeleteRequest(Arrays.asList(entityReference));
        deleteRequest.setInteractive(isAsync(context.getRequest()));
        deleteRequest.setCheckAuthorRights(false);
        deleteRequest.setShouldSkipRecycleBin(shouldSkipRecycleBin);
        deleteRequest.setNewBacklinkTargets(Collections.singletonMap(context.getDoc().getDocumentReference(),
            newBacklinkTarget));
        deleteRequest.setUpdateLinks(updateLinks);
        deleteRequest.setAutoRedirect(autoRedirect);

        try {
            JobExecutor jobExecutor = Utils.getComponent(JobExecutor.class);
            return jobExecutor.execute(RefactoringJobs.DELETE, deleteRequest);
        } catch (JobException e) {
            throw new XWikiException(String.format("Failed to schedule the delete job for [%s]", entityReference), e);
        }
    }
}
