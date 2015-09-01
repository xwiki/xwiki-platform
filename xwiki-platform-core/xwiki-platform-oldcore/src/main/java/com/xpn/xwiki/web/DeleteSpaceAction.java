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

import java.util.List;

import org.xwiki.job.Job;
import org.xwiki.model.reference.SpaceReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Action for deleting an entire space, optionally saving all the deleted documents to the document trash, if enabled.
 *
 * @version $Id$
 * @since 3.4M1
 */
public class DeleteSpaceAction extends DeleteAction
{
    @Override
    protected boolean delete(XWikiContext context) throws XWikiException
    {
        XWikiResponse response = context.getResponse();
        
        // Delete to recycle bin.
        SpaceReference spaceReference = context.getDoc().getDocumentReference().getLastSpaceReference();
        Job deleteJob = startDeleteJob(spaceReference);

        // If the user have asked for an asynchronous delete action
        if (isAsync(context.getRequest())) {
            List<String> jobId = deleteJob.getRequest().getId();
            sendRedirect(response,
                    Utils.getRedirect("delete", String.format("%s=%s", JOB_ID_PARAM, serializeJobId(jobId)), context));

            // A redirect has been performed.
            return true;
        }
        
        // Otherwise...
        try {
            deleteJob.join();
        } catch (InterruptedException e) {
            throw new XWikiException(String.format("Failed to delete [%s]", spaceReference), e);
        }
        
        return false;
    }

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        XWikiRequest request = context.getRequest();
        String result = "deletespace";
        if ("1".equals(request.getParameter(CONFIRM_PARAM))) {
            result = "deletedspace";
        }
        return result;
    }
}
