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
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.refactoring.job.RefactoringJobs;
import org.xwiki.refactoring.job.RestoreRequest;
import org.xwiki.refactoring.script.RefactoringScriptService;
import org.xwiki.script.service.ScriptService;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.DeletedDocument;
import com.xpn.xwiki.doc.XWikiDeletedDocument;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Action for restoring documents from the recycle bin.
 *
 * @version $Id$
 * @since 1.2M1
 */
public class UndeleteAction extends XWikiAction
{
    private static final String ID_PARAMETER = "id";

    private static final String SHOW_BATCH_PARAMETER = "showBatch";

    private static final String INCLUDE_BATCH_PARAMETER = "includeBatch";

    private static final String CONFIRM_PARAMETER = "confirm";

    private static final String ASYNC_PARAM = "async";

    private static final String TRUE = "true";

    private static final String VIEW_ACTION = "view";

    private static final Logger LOGGER = LoggerFactory.getLogger(UndeleteAction.class);

    @Override
    public boolean action(XWikiContext context) throws XWikiException
    {
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();

        // If the provided DeletedDocument ID is invalid, for any reason, redirect to view mode to see the document does
        // not exist screen.
        XWikiDeletedDocument deletedDocument = getDeletedDocument(context);
        if (deletedDocument == null) {
            sendRedirect(response, doc.getURL(VIEW_ACTION, context));
            return false;
        }

        // Note: Check the ID validity before checking the showBatch parameter so that we validate the ID before
        // displaying any restore batch UI.

        // If showBatch=true and confirm=true then restore the page w/o the batch. If not, the render action will go to
        // the "restore" UI so that the user can confirm. That "restore" UI will then call the action again with
        // confirm=true.
        if (TRUE.equals(request.getParameter(SHOW_BATCH_PARAMETER))
            && !TRUE.equals(request.getParameter(CONFIRM_PARAMETER))) {
            return true;
        }

        // CSRF prevention
        if (!csrfTokenCheck(context)) {
            return false;
        }

        // If the current user is not allowed to restore, render the "accessdenied" template.
        DeletedDocument deletedDocumentAPI = new DeletedDocument(deletedDocument, context);
        if (!deletedDocumentAPI.canUndelete()) {
            return true;
        }

        boolean redirected = false;

        if (deletedDocument != null) {
            redirected = restoreDocument(deletedDocument, context);
        }

        // Redirect to the undeleted document. Make sure to redirect to the proper translation.
        if (!redirected) {
            String queryString = getRedirectQueryString(context, deletedDocument.getLocale());
            sendRedirect(response, doc.getURL(VIEW_ACTION, queryString, context));
            redirected = true;
        }

        return !redirected;
    }

    private XWikiDeletedDocument getDeletedDocument(XWikiContext context) throws XWikiException
    {
        XWikiDeletedDocument result = null;

        XWikiRequest request = context.getRequest();
        XWiki xwiki = context.getWiki();

        String sindex = request.getParameter(ID_PARAMETER);
        try {
            long index = Long.parseLong(sindex);

            result = xwiki.getDeletedDocument(index, context);
        } catch (Exception e) {
            LOGGER.error("Failed to get deleted document with ID [{}]", sindex, e);
        }

        return result;
    }

    private boolean restoreDocument(XWikiDeletedDocument deletedDocument, XWikiContext context) throws XWikiException
    {
        Job restoreJob = startRestoreJob(deletedDocument, context);

        // If the user asked for an asynchronous action...
        if (isAsync(context.getRequest())) {
            List<String> jobId = restoreJob.getRequest().getId();

            // Note: We use spaceRedirect=false to fix the link to the document when view mode would normally try to
            // modify the context document to a non-terminal one but the restored document is actually terminal.
            String queryString = "xpage=restore&jobId=" + serializeJobId(jobId) + "&spaceRedirect=false";

            // We redirect to the view action and accept the edge case when the restored document's rights might prevent
            // the restoring user to view the result. In that case, an admin must be contacted to fix the rights.
            sendRedirect(context.getResponse(), Utils.getRedirect(VIEW_ACTION, queryString, context));

            // A redirect has been performed.
            return true;
        }

        // Otherwise...
        try {
            restoreJob.join();
        } catch (InterruptedException e) {
            throw new XWikiException(String.format("Failed to restore [%s] from batch [%s]",
                deletedDocument.getFullName(), deletedDocument.getBatchId()), e);
        }

        // No redirect has been performed.
        return false;
    }

    private String serializeJobId(List<String> jobId)
    {
        return StringUtils.join(jobId, "/");
    }

    private Job startRestoreJob(XWikiDeletedDocument deletedDocument, XWikiContext context) throws XWikiException
    {
        XWikiRequest request = context.getRequest();

        RefactoringScriptService refactoring =
            (RefactoringScriptService) Utils.getComponent(ScriptService.class, "refactoring");

        RestoreRequest restoreRequest = null;
        if (TRUE.equals(request.getParameter(INCLUDE_BATCH_PARAMETER))) {
            // Restore the entire batch, including the current document.
            String batchId = deletedDocument.getBatchId();
            restoreRequest = refactoring.createRestoreRequest(batchId);
        } else {
            // Restore just the current document.
            restoreRequest = refactoring.createRestoreRequest(Arrays.asList(deletedDocument.getId()));
        }
        restoreRequest.setInteractive(isAsync(request));
        restoreRequest.setCheckAuthorRights(false);

        try {
            JobExecutor jobExecutor = Utils.getComponent(JobExecutor.class);
            return jobExecutor.execute(RefactoringJobs.RESTORE, restoreRequest);
        } catch (JobException e) {
            throw new XWikiException(
                String.format("Failed to schedule the restore job for deleted document [%s], id [%s] of batch [%s]",
                    deletedDocument.getFullName(), deletedDocument.getId(), deletedDocument.getBatchId()),
                e);
        }
    }

    private boolean isAsync(XWikiRequest request)
    {
        return TRUE.equals(request.get(ASYNC_PARAM));
    }

    private String getRedirectQueryString(XWikiContext context, Locale deletedDocumentLocale)
    {
        String result = null;

        XWiki xwiki = context.getWiki();

        if (deletedDocumentLocale != null && xwiki.isMultiLingual(context)) {
            result = String.format("language=%s", deletedDocumentLocale);
        }

        return result;
    }

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        String result = null;

        XWikiRequest request = context.getRequest();

        // If showBatch=true and user confirmation is required, display the "restore" UI.
        if (TRUE.equals(request.getParameter(SHOW_BATCH_PARAMETER))
            && !TRUE.equals(request.getParameter(CONFIRM_PARAMETER))) {
            result = "restore";
        }

        // If the current user is not allowed to restore, display the "accessdenied" template.
        XWikiDeletedDocument deletedDocument = getDeletedDocument(context);
        if (deletedDocument != null) {
            // Note: Checking for null because when the document is actually restored, it may no longer be in the
            // recycle bin by the time render() gets called.
            DeletedDocument deletedDocumentAPI = new DeletedDocument(deletedDocument, context);
            if (!deletedDocumentAPI.canUndelete()) {
                return "accessdenied";
            }
        }

        return result;
    }
}
