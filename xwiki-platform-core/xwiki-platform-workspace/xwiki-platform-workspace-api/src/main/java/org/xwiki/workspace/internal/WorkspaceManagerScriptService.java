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
package org.xwiki.workspace.internal;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.script.service.ScriptService;
import org.xwiki.workspace.Workspace;
import org.xwiki.workspace.WorkspaceException;
import org.xwiki.workspace.WorkspaceManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.wikimanager.doc.XWikiServer;
import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Makes the WorkspaceManager API available to scripting.
 * 
 * @version $Id$
 */
@Component
@Named("workspace")
@Singleton
public class WorkspaceManagerScriptService implements ScriptService
{
    /** Field name of the last API exception inserted in context. */
    public static final String CONTEXT_LASTEXCEPTION = "lastexception";

    /** Workspace Manager wrapped component. */
    @Inject
    private WorkspaceManager workspaceManager;

    /** Execution context. */
    @Inject
    private Execution execution;

    /** Logging tool. */
    @Inject
    private Logger logger;

    /**
     * User to parse a document reference.
     */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentResolver;

    /**
     * Used to serialize a document reference.
     */
    @Inject
    private EntityReferenceSerializer<String> defaultSerializer;

    /** @return the wrapped component */
    WorkspaceManager getManager()
    {
        return workspaceManager;
    }

    /**
     * @param userName the user to check
     * @param workspaceName the workspace name to check
     * @return true if the it's possible for the specified user to create the speicified workspace
     */
    public boolean canCreateWorkspace(String userName, String workspaceName)
    {
        return workspaceManager.canCreateWorkspace(getPrefixedUserName(userName), workspaceName);
    }

    /**
     * @param userName the user to check
     * @param workspaceName the workspace name to check
     * @return true if the it's possible for the specified user to edit the specified workspace
     */
    public boolean canEditWorkspace(String userName, String workspaceName)
    {
        return workspaceManager.canEditWorkspace(getPrefixedUserName(userName), workspaceName);
    }

    /**
     * @param userName the user to check
     * @param workspaceName the workspace name to check
     * @return true if the it's possible for the specified user to delete the specified workspace
     */
    public boolean canDeleteWorkspace(String userName, String workspaceName)
    {
        return workspaceManager.canDeleteWorkspace(getPrefixedUserName(userName), workspaceName);
    }

    /**
     * Creates a new workspace from a wiki descriptor. The default template will be used.
     * 
     * @param newWikiXObjectDocument a new (in-memory) wiki descriptor document from which the new wiki descriptor
     *            document will be created. This method will take care of saving the document. <b>Note:</b>The name of
     *            the wiki will also have to be set inside this descriptor.
     * @see {@link #getLastException()} to check for abnormal method termination
     */
    public void createWorkspace(XWikiServer newWikiXObjectDocument)
    {
        createWorkspace(newWikiXObjectDocument, "workspacetemplate");
    }

    /**
     * Creates a new workspace from a wiki descriptor.
     * 
     * @param newWikiXObjectDocument a new (in-memory) wiki descriptor document from which the new wiki descriptor
     *            document will be created. This method will take care of saving the document. <b>Note:</b>The name of
     *            the wiki will also have to be set inside this descriptor.
     * @param templateWikiName the name of the wiki template to use when creating the new workspace.
     * @see {@link #getLastException()} to check for abnormal method termination
     */
    public void createWorkspace(XWikiServer newWikiXObjectDocument, String templateWikiName)
    {
        clearException();

        String workspaceName = newWikiXObjectDocument.getWikiName();

        try {
            if (!canCreateWorkspace(getXWikiContext().getUser(), workspaceName)) {
                throw new WorkspaceException(String.format("Access denied for user [%s] to create the workspace [%s]",
                    getXWikiContext().getUser(), workspaceName));
            }

            /* Avoid "traps" by making sure the page from where this is executed has PR. */
            if (!getXWikiContext().getWiki().getRightService().hasProgrammingRights(getXWikiContext())) {
                throw new WorkspaceException(String.format(
                    "The page requires programming rights in order to create the workspace [%s]", workspaceName));
            }

            if (workspaceName == null || workspaceName.trim().equals("")) {
                throw new WorkspaceException(String.format("Workspace name [%s] is invalid", workspaceName));
            }

            this.workspaceManager.createWorkspace(newWikiXObjectDocument, templateWikiName);
        } catch (Exception e) {
            error(String.format("Failed to create workspace [%s]", workspaceName), e);
        }
    }

    /**
     * @param workspaceName name of the workspace to delete
     * @see {@link #getLastException()} to check for abnormal method termination
     */
    public void deleteWorkspace(String workspaceName)
    {
        clearException();

        try {
            /* Get prefixed current user. */
            String currentUser = getPrefixedUserName(getXWikiContext().getUser());

            /* Check rights. */
            if (!canDeleteWorkspace(currentUser, workspaceName)) {
                throw new WorkspaceException(String.format("Access denied for user [%s] to delete the workspace [%s]",
                    currentUser, workspaceName));
            }

            /* Avoid "traps" by making sure the page from where this is executed has PR. */
            if (!getXWikiContext().getWiki().getRightService().hasProgrammingRights(getXWikiContext())) {
                throw new WorkspaceException(String.format(
                    "The page requires programming rights in order to delete the workspace [%s]", workspaceName));
            }

            /* Delegate call. */
            workspaceManager.deleteWorkspace(workspaceName);
        } catch (Exception e) {
            error(String.format("Failed to delete workspace '%s'.", workspaceName), e);
        }
    }

    /**
     * @param workspaceName name of the workspace to edit
     * @param modifiedWikiXObjectDocument an in-memory modified wiki descriptor document. This method will take care of
     *            saving the changes
     * @see {@link #getLastException()} to check for abnormal method termination
     */
    public void editWorkspace(String workspaceName, XWikiServer modifiedWikiXObjectDocument)
    {
        clearException();

        try {
            String currentUser = getPrefixedUserName(getXWikiContext().getUser());

            /* Check rights. */
            if (!canEditWorkspace(currentUser, workspaceName)) {
                throw new WorkspaceException(String.format("Access denied for user '%s' to edit the workspace '%s'",
                    currentUser, workspaceName));
            }

            /* Avoid "traps" by making sure the page from where this is executed has PR. */
            if (!getXWikiContext().getWiki().getRightService().hasProgrammingRights(getXWikiContext())) {
                throw new WorkspaceException(String.format(
                    "The page requires programming rights in order to edit the workspace '%s'", workspaceName));
            }

            /* Delegate call. */
            workspaceManager.editWorkspace(workspaceName, modifiedWikiXObjectDocument);
        } catch (Exception e) {
            error(String.format("Failed to edit workspace '%s'.", workspaceName), e);
        }
    }

    /**
     * @param userName a prefixed or un-prefixed user name
     * @return an always prefixed user name or {@link XWikiRightService#GUEST_USER_FULLNAME} if the guest user name was
     *         given
     */
    private String getPrefixedUserName(String userName)
    {
        if (XWikiRightService.GUEST_USER_FULLNAME.equals(userName)) {
            return userName;
        }

        DocumentReference userReference = this.currentResolver.resolve(userName);
        String prefixedUserName = this.defaultSerializer.serialize(userReference);

        return prefixedUserName;
    }

    /** @return the deprecated xwiki context used to manipulate xwiki objects */
    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) execution.getContext().getProperty("xwikicontext");
    }

    /**
     * Log exception and store it in the context.
     * 
     * @param errorMessage error message
     * @param e the caught exception
     * @see #CONTEXT_LASTEXCEPTION
     */
    private void error(String errorMessage, Exception e)
    {
        String errorMessageToLog = errorMessage;
        if (errorMessageToLog == null) {
            errorMessageToLog = e.getMessage();
        }

        /* Log exception. */
        logger.error(errorMessageToLog, e);

        /* Store exception in context. */
        this.execution.getContext().setProperty(CONTEXT_LASTEXCEPTION, e);
    }

    /**
     * Log exception and store it in the context. The logged message is the exception's message. This allows the
     * underlying component to define it's messages and removes duplication.
     * 
     * @param e the caught exception
     * @see #CONTEXT_LASTEXCEPTION
     */
    private void error(Exception e)
    {
        error(null, e);
    }

    /**
     * Clear the last exception from the context. #see {@link #CONTEXT_LASTEXCEPTION}
     */
    private void clearException()
    {
        this.execution.getContext().setProperty(CONTEXT_LASTEXCEPTION, null);
    }

    /**
     * @return the last exception thrown by a previous method call; {@code null} if no exception was recently thrown.
     */
    public Exception getLastException()
    {
        return (Exception) this.execution.getContext().getProperty(CONTEXT_LASTEXCEPTION);
    }

    /**
     * Retrieves a workspace by name.
     * 
     * @param workspaceId name (ID) of the workspace
     * @return the requested workspace or null if it does not exist
     * @see {@link #getLastException()} to check for abnormal method termination
     */
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

    /**
     * Get the list of all workspaces. It basically gets all wikis that have a {@code WorkspaceManager.WorkspaceClass}
     * object in their {@code XWikiServer<wikiName>} page.
     * 
     * @return list of available workspaces
     * @see #CONTEXT_LASTEXCEPTION to check for abnormal method termination
     */
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

    /**
     * Get the list of all workspace templates. It basically gets all workspace returned by
     * {@code WorkspaceManager.getWorkspaces} and which are considered as template (see
     * {@code XWikiServer.isWikiTemplate}).
     * 
     * @return list of available workspace templates
     * @see #CONTEXT_LASTEXCEPTION to check for abnormal method termination
     */
    public List<Workspace> getWorkspaceTemplates()
    {
        List<Workspace> result = null;
        try {
            result = workspaceManager.getWorkspaceTemplates();
        } catch (Exception e) {
            error(e);
        }

        return result;
    }

    /**
     * @param workspaceName name of the workspace to check
     * @return true if a workspace with the given name exists, false otherwise
     */
    public boolean isWorkspace(String workspaceName)
    {
        return workspaceManager.isWorkspace(workspaceName);
    }
}
