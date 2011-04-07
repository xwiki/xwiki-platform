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
package org.xwiki.contrib.wiki30.internal;

import java.util.List;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.context.Execution;
import org.xwiki.contrib.wiki30.Workspace;
import org.xwiki.contrib.wiki30.WorkspaceManager;
import org.xwiki.script.service.ScriptService;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.applicationmanager.core.api.XWikiExceptionApi;
import com.xpn.xwiki.plugin.wikimanager.WikiManagerException;
import com.xpn.xwiki.plugin.wikimanager.doc.XWikiServer;

/**
 * Makes the WorkspaceManager API available to scripting.
 * 
 * @version $Id:$
 */
@Component("workspaceManager")
public class WorkspaceManagerScriptService extends AbstractLogEnabled implements ScriptService
{
    /** Field name of the last error code inserted in context. */
    public static final String CONTEXT_LASTERRORCODE = "lasterrorcode";

    /** Field name of the last API exception inserted in context. */
    public static final String CONTEXT_LASTEXCEPTION = "lastexception";

    @Requirement
    private WorkspaceManager workspaceManager;

    /** Execution context. */
    @Requirement
    private Execution execution;

    public WorkspaceManager getManager()
    {
        return workspaceManager;
    }

    /** @see org.xwiki.contrib.wiki30.WorkspaceManager#canCreateWorkspace(java.lang.String, java.lang.String) */
    public boolean canCreateWorkspace(String userName, String workspaceName)
    {
        return workspaceManager.canCreateWorkspace(userName, workspaceName);
    }

    /** @see org.xwiki.contrib.wiki30.WorkspaceManager#canEditWorkspace(java.lang.String, java.lang.String) */
    public boolean canEditWorkspace(String userName, String workspaceName)
    {
        return workspaceManager.canEditWorkspace(userName, workspaceName);
    }

    /** @see org.xwiki.contrib.wiki30.WorkspaceManager#canDeleteWorkspace(java.lang.String, java.lang.String) */
    public boolean canDeleteWorkspace(String userName, String workspaceName)
    {
        return workspaceManager.canDeleteWorkspace(userName, workspaceName);
    }

    /**
     * @see org.xwiki.contrib.wiki30.WorkspaceManager#createWorkspace(java.lang.String, java.lang.String,
     *      com.xpn.xwiki.plugin.wikimanager.doc.XWikiServer)
     */
    public int createWorkspace(String workspaceName, XWikiServer newWikiXObjectDocument)
    {
        int returncode = XWikiExceptionApi.ERROR_NOERROR;

        try {
            if (!canCreateWorkspace(getXWikiContext().getUser(), workspaceName)) {
                throw new WikiManagerException(XWikiException.ERROR_XWIKI_ACCESS_DENIED, String.format(
                    "Access denied to create the workspace '%s'", workspaceName));
            }

            if (workspaceName == null || workspaceName.trim().equals("")) {
                throw new WikiManagerException(WikiManagerException.ERROR_WM_WIKINAMEFORBIDDEN, String.format(
                    "Workspace name '%s' is invalid.", workspaceName));
            }

            this.workspaceManager.createWorkspace(workspaceName, newWikiXObjectDocument);
        } catch (XWikiException e) {
            error(String.format("Failed to create workspace '%s'.", workspaceName), e);

            returncode = e.getCode();
        }

        return returncode;
    }

    /** @see org.xwiki.contrib.wiki30.WorkspaceManager#deleteWorkspace(java.lang.String) */
    public int deleteWorkspace(String workspaceName)
    {
        int returncode = XWikiExceptionApi.ERROR_NOERROR;

        try {
            if (!canDeleteWorkspace(getXWikiContext().getUser(), workspaceName)) {
                throw new WikiManagerException(XWikiException.ERROR_XWIKI_ACCESS_DENIED, String.format(
                    "Access denied to delete the workspace '%s'", workspaceName));
            }

            workspaceManager.deleteWorkspace(workspaceName);
        } catch (XWikiException e) {
            error(String.format("Failed to delete workspace '%s'.", workspaceName), e);

            returncode = e.getCode();
        }

        return returncode;
    }

    /**
     * @see org.xwiki.contrib.wiki30.WorkspaceManager#editWorkspace(java.lang.String,
     *      com.xpn.xwiki.plugin.wikimanager.doc.XWikiServer)
     */
    public int editWorkspace(String workspaceName, XWikiServer modifiedWikiXObjectDocument)
    {
        int returncode = XWikiExceptionApi.ERROR_NOERROR;

        try {
            if (!canDeleteWorkspace(getXWikiContext().getUser(), workspaceName)) {
                throw new WikiManagerException(XWikiException.ERROR_XWIKI_ACCESS_DENIED, String.format(
                    "Access denied to edit the workspace '%s'", workspaceName));
            }

            workspaceManager.editWorkspace(workspaceName, modifiedWikiXObjectDocument);
        } catch (XWikiException e) {
            error(String.format("Failed to edit workspace '%s'.", workspaceName), e);

            returncode = e.getCode();
        }

        return returncode;
    }

    /** @return the deprecated xwiki context used to manipulate xwiki objects */
    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) execution.getContext().getProperty("xwikicontext");
    }

    /**
     * Log error and store details in the context.
     * 
     * @param errorMessage error message.
     * @param e the caught exception.
     * @deprecated stop using {@link XWikiException}, put exception in context under {@link #CONTEXT_LASTEXCEPTION} key
     *             instead.
     */
    private void error(String errorMessage, XWikiException e)
    {
        getLogger().error(errorMessage, e);

        XWikiContext deprecatedContext = getXWikiContext();

        deprecatedContext.put(CONTEXT_LASTERRORCODE, Integer.valueOf(e.getCode()));
        deprecatedContext.put(CONTEXT_LASTEXCEPTION, new XWikiExceptionApi(e, deprecatedContext));
    }

    /**
     * Log exception and store it in the context.
     * 
     * @param errorMessage error message.
     * @param e the caught exception.
     * @see #CONTEXT_LASTEXCEPTION
     */
    private void error(String errorMessage, Exception e)
    {
        if (errorMessage == null) {
            errorMessage = e.getMessage();
        }

        /* Log exception. */
        getLogger().error(errorMessage, e);

        /* Store exception in context. */
        XWikiContext deprecatedContext = getXWikiContext();
        deprecatedContext.put(CONTEXT_LASTEXCEPTION, e);
    }

    /**
     * Log exception and store it in the context. The logged message is the exception's message. This allows the
     * underlying component to define it's messages and removes duplication.
     * 
     * @param e the caught exception.
     * @see #CONTEXT_LASTEXCEPTION
     */
    private void error(Exception e)
    {
        error(null, e);
    }

    /** @see org.xwiki.contrib.wiki30.WorkspaceManager#getWorkspace(String) */
    public Workspace getWorkspace(String workspaceId)
    {
        Workspace result = null;
        try {
            result = workspaceManager.getWorkspace(workspaceId);
        } catch (Exception e) {
            error(e);
        }

        return result;
    }

    /** @see org.xwiki.contrib.wiki30.WorkspaceManager#getWorkspaces() */
    public List<Workspace> getWorkspaces()
    {
        List<Workspace> result = null;
        try {
            result = workspaceManager.getWorkspaces();
        } catch (Exception e) {
            error(e);
        }

        return result;
    }
}
