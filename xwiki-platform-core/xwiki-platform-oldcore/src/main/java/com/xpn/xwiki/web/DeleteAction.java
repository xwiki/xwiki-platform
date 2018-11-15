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

import org.apache.commons.lang3.StringUtils;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.refactoring.job.EntityRequest;
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
public class DeleteAction extends XWikiAction
{
    /** confirm parameter name. */
    protected static final String CONFIRM_PARAM = "confirm";

    protected static final String ACTION_NAME = "delete";

    protected static final String ASYNC_PARAM = "async";

    protected static final String RECYCLED_DOCUMENT_ID_PARAM = "id";

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
        if (sindex != null && xwiki.hasRecycleBin(context)) {
            deleteFromRecycleBin(Long.parseLong(sindex), context);
            return true;
        } else if (doc.isNew()) {
            // Redirect the user to the view template so that he gets the "document doesn't exist" dialog box.
            sendRedirect(response, Utils.getRedirect("view", context));
            return true;
        } else {
            // Delete to recycle bin.
            return deleteToRecycleBin(context);
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

    private boolean deleteToRecycleBin(XWikiContext context) throws XWikiException
    {
        XWikiRequest request = context.getRequest();
        XWikiDocument doc = context.getDoc();

        EntityReference documentReference =
            doesAffectChildren(request, doc.getDocumentReference()) ? doc.getDocumentReference()
                .getLastSpaceReference() : doc.getTranslatedDocument(context).getDocumentReferenceWithLocale();

        return deleteToRecycleBin(documentReference, context);
    }

    protected boolean deleteToRecycleBin(EntityReference entityReference, XWikiContext context) throws XWikiException
    {
        Job deleteJob = startDeleteJob(entityReference, context);

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

    private Job startDeleteJob(EntityReference entityReference, XWikiContext context) throws XWikiException
    {
        RefactoringScriptService refactoring =
            (RefactoringScriptService) Utils.getComponent(ScriptService.class, "refactoring");
        EntityRequest deleteRequest = refactoring.createDeleteRequest(Arrays.asList(entityReference));
        deleteRequest.setInteractive(isAsync(context.getRequest()));
        deleteRequest.setCheckAuthorRights(false);

        try {
            JobExecutor jobExecutor = Utils.getComponent(JobExecutor.class);
            return jobExecutor.execute(RefactoringJobs.DELETE, deleteRequest);
        } catch (JobException e) {
            throw new XWikiException(String.format("Failed to schedule the delete job for [%s]", entityReference), e);
        }
    }
}
