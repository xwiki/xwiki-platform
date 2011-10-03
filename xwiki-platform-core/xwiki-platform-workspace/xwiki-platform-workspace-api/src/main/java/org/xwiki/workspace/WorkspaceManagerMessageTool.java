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

import java.util.ResourceBundle;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiMessageTool;

/**
 * Helps with translating error messages.
 * 
 * @version $Id$
 */
public class WorkspaceManagerMessageTool extends XWikiMessageTool
{
    /** Used as {@link WorkspaceManagerException} message when failing to get a workspace. */
    public static final String ERROR_WORKSPACEGET = "workspacemanager.error.workspaceget";

    /** Used as {@link WorkspaceManagerException} message when failing to get all workspaces. */
    public static final String ERROR_WORKSPACEGETALL = "workspacemanager.error.workspacegetall";

    /**
     * Used as {@link WorkspaceManagerException} message when a requested workspace ID points to an entity that is not a
     * workspace.
     */
    public static final String ERROR_NOTAWORKSPACE = "workspacemanager.error.notaworkspace";

    /**
     * Used as {@link WorkspaceManagerException} message when a requested workspace is invalid (no XWikiServerClass or
     * WorkspaceClass object in the wiki document).
     */
    public static final String ERROR_WORKSPACEINVALID = "workspacemanager.error.workspaceinvalid";

    /** Used as {@link WorkspaceManagerException} message when trying to get a workspace which does not exists. */
    public static final String ERROR_WORKSPACEDOESNOTEXIST = "workspacemanager.error.workspacedoesnotexist";

    /** Used as logging message when skipping an invalid workspace. */
    public static final String LOG_WORKSPACEINVALID = "workspacemanager.log.workspaceinvalid";

    /**
     * @param context the {@link com.xpn.xwiki.XWikiContext} object, used to get access to XWiki primitives for loading
     *            documents
     */
    public WorkspaceManagerMessageTool(XWikiContext context)
    {
        super(ResourceBundle.getBundle("workspacemanager" + "/ApplicationResources"), context);
    }
}
