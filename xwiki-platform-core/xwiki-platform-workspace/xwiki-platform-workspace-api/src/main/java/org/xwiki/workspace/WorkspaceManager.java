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
package org.xwiki.workspace;

import java.util.List;

import org.xwiki.component.annotation.Role;

import com.xpn.xwiki.plugin.wikimanager.doc.XWikiServer;

/**
 * Component in charge of creating and managing workspaces.
 * 
 * @version $Id$
 */
@Role
public interface WorkspaceManager
{
    /**
     * @param userName the user to check
     * @param workspaceName the workspace name to check
     * @return true if the it's possible for the specified user to create the specified workspace
     */
    boolean canCreateWorkspace(String userName, String workspaceName);

    /**
     * @param userName the user to check
     * @param workspaceName the workspace name to check
     * @return true if the it's possible for the specified user to edit the specified workspace
     */
    boolean canEditWorkspace(String userName, String workspaceName);

    /**
     * @param userName the user to check
     * @param workspaceName the workspace name to check
     * @return true if the it's possible for the specified user to delete the specified workspace
     */
    boolean canDeleteWorkspace(String userName, String workspaceName);

    /**
     * Creates a new workspace from a wiki descriptor.
     * 
     * @param newWikiXObjectDocument a new (in-memory) wiki descriptor document from which the new wiki descriptor
     *            document will be created. This method will take care of saving the document. <b>Note:</b>The name of
     *            the wiki will also have to be set inside this descriptor.
     * @param templateWikiName the name of the wiki template to use when creating the new workspace.
     * @return {@link XWikiServer} descriptor for the newly created workspace
     * @throws WorkspaceException if problems occur
     */
    XWikiServer createWorkspace(XWikiServer newWikiXObjectDocument, String templateWikiName) throws WorkspaceException;

    /**
     * Creates a new workspace from a wiki descriptor. The default template will be used.
     * 
     * @param newWikiXObjectDocument a new (in-memory) wiki descriptor document from which the new wiki descriptor
     *            document will be created. This method will take care of saving the document.
     * @return {@link XWikiServer} descriptor for the newly created workspace
     * @throws WorkspaceException if problems occur
     */
    XWikiServer createWorkspace(XWikiServer newWikiXObjectDocument) throws WorkspaceException;

    /**
     * @param workspaceName name of the workspace to delete
     * @throws WorkspaceException if problems occur
     */
    void deleteWorkspace(String workspaceName) throws WorkspaceException;

    /**
     * @param workspaceName name of the workspace to edit
     * @param modifiedWikiXObjectDocument an in-memory modified wiki descriptor document. This method will take care of
     *            saving the changes. <b>Note</b>: The wiki name is not modifiable.
     * @throws WorkspaceException if problems occur
     */
    void editWorkspace(String workspaceName, XWikiServer modifiedWikiXObjectDocument) throws WorkspaceException;

    /**
     * Retrieves a workspace by name.
     * 
     * @param workspaceName name (ID) of the workspace
     * @return the requested workspace or null if it does not exist
     * @throws WorkspaceException if problems occur
     */
    Workspace getWorkspace(String workspaceName) throws WorkspaceException;

    /**
     * Get the list of all workspaces. It basically gets all wikis that have a {@code WorkspaceManager.WorkspaceClass}
     * object in their {@code XWikiServer<wikiName>} page.
     * 
     * @return list of available workspaces
     * @throws WorkspaceException if problems occur
     */
    List<Workspace> getWorkspaces() throws WorkspaceException;

    /**
     * Get the list of all workspace templates. It basically gets all workspace returned by {@link #WorkspaceManager.getWorkspaces}
     * and which are considered as template (see {@link #XWikiServer.isWikiTemplate}).
     * 
     * @return list of available workspace templates
     * @throws WorkspaceException if problems occur
     */
    List<Workspace> getWorkspaceTemplates() throws WorkspaceException;

    /**
     * @param workspaceName name of the workspace to check
     * @return true if a workspace with the given name exists, false otherwise
     */
    boolean isWorkspace(String workspaceName);
}
