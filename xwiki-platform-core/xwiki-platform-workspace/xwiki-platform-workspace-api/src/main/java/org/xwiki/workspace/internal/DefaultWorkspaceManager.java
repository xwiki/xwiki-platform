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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.workspace.Workspace;
import org.xwiki.workspace.WorkspaceException;
import org.xwiki.workspace.WorkspaceManager;
import org.xwiki.workspace.WorkspaceManagerMessageTool;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseElement;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.plugin.wikimanager.WikiManager;
import com.xpn.xwiki.plugin.wikimanager.WikiManagerMessageTool;
import com.xpn.xwiki.plugin.wikimanager.doc.Wiki;
import com.xpn.xwiki.plugin.wikimanager.doc.XWikiServer;
import com.xpn.xwiki.plugin.wikimanager.internal.WikiManagerScriptService;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.web.XWikiMessageTool;

/**
 * Implementation of a <tt>WorkspaceManager</tt> component.
 * 
 * @version $Id$
 */
@Component
public class DefaultWorkspaceManager implements WorkspaceManager, Initializable
{
    /** Member property of the group class. */
    private static final String GROUP_CLASS_MEMBER_PROPERTY = "member";

    /** Default XWiki space used to store wiki descriptors. */
    private static final String XWIKI_SPACE = "XWiki";

    /** Workspace Class used to mark and extend a wiki descriptor. */
    private static final String WORKSPACE_CLASS = "WorkspaceClass";

    /** Workspace Manager application space. */
    private static final String WORKSPACE_MANAGER_SPACE = "WorkspaceManager";

    /** Admin right. */
    private static final String RIGHT_ADMIN = "admin";

    /** Wiki preferences page for local wiki (unprefixed and relative to the current wiki). */
    private static final String WIKI_PREFERENCES_LOCAL = "XWiki.XWikiPreferences";

    /** Format string for the wiki preferences page of a certain wiki (absolute reference). */
    private static final String WIKI_PREFERENCES_PREFIXED_FORMAT = "%s:" + WIKI_PREFERENCES_LOCAL;

    /** Membership type property name. */
    private static final String WORKSPACE_MEMBERSHIP_TYPE_PROPERTY = "membershipType";

    /** Membership type default value. */
    private static final String WORKSPACE_MEMBERSHIP_TYPE_DEFAULT = "open";

    /** Logging tool. */
    @Inject
    private static Logger logger;

    /** Execution context. */
    @Inject
    private Execution execution;

    /** Observation manager needed to fire events. */
    @Inject
    private ObservationManager observationManager;

    /**
     * Wiki Manager Script service that we are using instead of the deprecated PluginAPI, until a decent WikiManager
     * component becomes available.
     */
    @Inject
    @Named("wikimanager")
    private ScriptService wikiManagerService;

    /** Internal wiki manager tookit required to overcome the rights checking of the API. */
    private WikiManager wikiManagerInternal;

    /** The message tool to use to generate errors or comments. */
    private XWikiMessageTool messageTool;

    @Override
    public void initialize() throws InitializationException
    {
        XWikiContext deprecatedContext = getXWikiContext();

        /* Should be ok if we initialize and cache message tools with the current context. */

        WikiManagerMessageTool wikiManagerMessageTool = WikiManagerMessageTool.getDefault(deprecatedContext);
        this.wikiManagerInternal = new WikiManager(wikiManagerMessageTool);

        this.messageTool = new WorkspaceManagerMessageTool(deprecatedContext);
    }

    /**
     * Convenience method to avoid writing the cast every time.
     * 
     * @return the wiki manager script service implementation that exposes the API.
     */
    private WikiManagerScriptService getWikiManager()
    {
        WikiManagerScriptService scriptService = (WikiManagerScriptService) wikiManagerService;

        return scriptService;
    }

    @Override
    public boolean canCreateWorkspace(String userName, String workspaceName)
    {
        XWikiContext deprecatedContext = getXWikiContext();

        /* If XWiki is not in virtual mode, don`t bother. */
        if (!deprecatedContext.getWiki().isVirtualMode()) {
            return false;
        }

        /* User name input validation. */
        if (userName == null || userName.trim().length() == 0) {
            return false;
        }

        /* Do not allow the guest user. Note: Shouldn't this be decided by the admin trough rights? */
        if (XWikiRightService.GUEST_USER_FULLNAME.equals(userName)) {
            return false;
        }

        /* Check if it already exists. */
        if (isWorkspace(workspaceName)) {
            return false;
        }

        return true;
    }

    @Override
    public boolean canEditWorkspace(String userName, String workspaceName)
    {
        if (!isWorkspace(workspaceName)) {
            return false;
        }

        XWikiContext deprecatedContext = getXWikiContext();

        try {
            XWikiServer wikiServer = getWikiManager().getWikiDocument(workspaceName);
            String wikiOwner = wikiServer.getOwner();

            XWikiRightService rightService = deprecatedContext.getWiki().getRightService();
            String mainWikiPreferencesDocumentName =
                String.format(WIKI_PREFERENCES_PREFIXED_FORMAT, deprecatedContext.getMainXWiki());

            /* Owner or main wiki admin. */
            return wikiOwner.equals(userName)
                || rightService.hasAccessLevel(RIGHT_ADMIN, userName, mainWikiPreferencesDocumentName,
                    deprecatedContext);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("Failed to check if user [{}] can edit workspace [{}]. Assuming false.", new Object[] {
                userName, workspaceName, e});
            }

            return false;
        }
    }

    @Override
    public boolean canDeleteWorkspace(String userName, String workspaceName)
    {
        if (!isWorkspace(workspaceName)) {
            return false;
        }

        XWikiContext deprecatedContext = getXWikiContext();

        try {
            XWikiServer wikiServer = getWikiManager().getWikiDocument(workspaceName);
            String wikiOwner = wikiServer.getOwner();

            XWikiRightService rightService = deprecatedContext.getWiki().getRightService();
            String mainWikiPreferencesDocumentName =
                String.format(WIKI_PREFERENCES_PREFIXED_FORMAT, deprecatedContext.getMainXWiki());

            /* Owner or main wiki admin. */
            return deprecatedContext.getWiki().isVirtualMode()
                && (wikiOwner.equals(userName) || rightService.hasAccessLevel(RIGHT_ADMIN, userName,
                    mainWikiPreferencesDocumentName, deprecatedContext));
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("Failed to check if user [{}] can delete workspace [{}]. Assuming false.", new Object[] {
                userName, workspaceName, e});
            }

            return false;
        }
    }

    /**
     * @return the deprecated xwiki context used to manipulate xwiki objects
     */
    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) execution.getContext().getProperty("xwikicontext");
    }

    @Override
    public XWikiServer createWorkspace(XWikiServer newWikiXObjectDocument) throws WorkspaceException
    {
        return this.createWorkspace(newWikiXObjectDocument, "workspacetemplate");
    }

    @Override
    public XWikiServer createWorkspace(XWikiServer newWikiXObjectDocument, String templateWikiName)
        throws WorkspaceException
    {
        XWikiContext deprecatedContext = getXWikiContext();

        String workspaceName = newWikiXObjectDocument.getWikiName();

        String comment = String.format("Created new workspace '%s'", workspaceName);

        /* Create new wiki. */
        XWikiServer result = null;
        try {
            result =
                this.wikiManagerInternal.createNewWikiFromTemplate(newWikiXObjectDocument, templateWikiName, true,
                    comment, deprecatedContext);
        } catch (Exception e) {
            logAndThrowException(String.format("Failed to create workspace [%s]", workspaceName), e);
        }

        /*
         * Use the XWiki.XWikiAllGroup of the new wiki to add the owner as a member and the XWiki.XWikiAdminGroup of the
         * new wiki to explicitly add the owner as an admin.
         */
        String workspaceOwner = newWikiXObjectDocument.getOwner();

        try {
            initializeOwner(workspaceName, workspaceOwner, comment, deprecatedContext);
        } catch (Exception e) {
            logger.error("Failed to add owner to workspace group for workspace [{}].", workspaceName, e);
        }

        /* Add workspace marker object. */
        XWikiDocument wikiDocument = newWikiXObjectDocument.getDocument();

        try {
            initializeMarker(wikiDocument, comment, deprecatedContext);
        } catch (Exception e) {
            logger.error("Failed to add workspace marker for workspace [{}].", workspaceName, e);
        }

        /* TODO: Launch workspace created event. */

        return result;
    }

    /**
     * Initialize a newly created workspace by explicitly adding its owner in the XWikiAllGroup and XWikiAdminGroup
     * local groups.
     * 
     * @param workspaceName the workspace to initialize
     * @param workspaceOwner the owner user name to be used in the process
     * @param comment the comment used when saving the group documents
     * @param deprecatedContext the XWikiContext instance to use
     * @throws Exception if problems occur
     */
    private static void initializeOwner(String workspaceName, String workspaceOwner, String comment,
        XWikiContext deprecatedContext) throws Exception
    {
        String currentWikiName = deprecatedContext.getDatabase();
        try {
            deprecatedContext.setDatabase(workspaceName);

            XWiki wiki = deprecatedContext.getWiki();
            DocumentReference groupClassReference = wiki.getGroupClass(deprecatedContext).getDocumentReference();

            /* Add user as workspace member. */
            DocumentReference workspaceGroupReference =
                new DocumentReference(workspaceName, Workspace.WORKSPACE_GROUP_SPACE, Workspace.WORKSPACE_GROUP_PAGE);
            XWikiDocument workspaceGroupDocument = wiki.getDocument(workspaceGroupReference, deprecatedContext);

            BaseObject workspaceGroupObject = workspaceGroupDocument.newXObject(groupClassReference, deprecatedContext);
            workspaceGroupObject.setStringValue(GROUP_CLASS_MEMBER_PROPERTY, workspaceOwner);

            wiki.saveDocument(workspaceGroupDocument, comment, deprecatedContext);

            /* Add user as workspace admin. */
            DocumentReference workspaceAdminGroupReference =
                new DocumentReference(workspaceName, XWIKI_SPACE, "XWikiAdminGroup");
            XWikiDocument workspaceAdminGroupDocument =
                wiki.getDocument(workspaceAdminGroupReference, deprecatedContext);

            BaseObject workspaceAdminGroupObject =
                workspaceAdminGroupDocument.newXObject(groupClassReference, deprecatedContext);
            workspaceAdminGroupObject.setStringValue(GROUP_CLASS_MEMBER_PROPERTY, workspaceOwner);

            wiki.saveDocument(workspaceAdminGroupDocument, comment, deprecatedContext);

            // FIXME: See if we need to update the group service cache.
            // try {
            // XWikiGroupService gservice = getGroupService(context);
            // gservice.addUserToGroup(userName, context.getDatabase(), groupName, context);
            // } catch (Exception e) {
            // LOG.error("Failed to update group service cache", e);
            // }
        } finally {
            deprecatedContext.setDatabase(currentWikiName);
        }
    }

    /**
     * @param wikiDocument the wiki descriptor document to initialize
     * @param comment the comment used when saving the wiki descriptor document
     * @param deprecatedContext the XWikiContext instance to use
     * @throws Exception if problems occur
     */
    private static void initializeMarker(XWikiDocument wikiDocument, String comment, XWikiContext deprecatedContext)
        throws Exception
    {
        String mainWikiName = deprecatedContext.getMainXWiki();
        DocumentReference workspaceClassReference =
            new DocumentReference(mainWikiName, WORKSPACE_MANAGER_SPACE, WORKSPACE_CLASS);

        BaseObject workspaceObject = wikiDocument.getXObject(workspaceClassReference);
        if (workspaceObject == null) {
            workspaceObject = wikiDocument.newXObject(workspaceClassReference, deprecatedContext);
        }

        /* Make sure the required workspace attributes are set. */
        if (workspaceObject.getStringValue(WORKSPACE_MEMBERSHIP_TYPE_PROPERTY) == null) {
            workspaceObject.setStringValue(WORKSPACE_MEMBERSHIP_TYPE_PROPERTY, WORKSPACE_MEMBERSHIP_TYPE_DEFAULT);
        }

        deprecatedContext.getWiki().saveDocument(wikiDocument, comment, deprecatedContext);
    }

    @Override
    public void deleteWorkspace(String workspaceName) throws WorkspaceException
    {
        Workspace workspace = getWorkspace(workspaceName);
        if (workspace == null) {
            throw new WorkspaceException(String.format("Workspace '%s' does not exist", workspaceName));
        }

        XWikiContext deprecatedContext = getXWikiContext();
        XWiki xwiki = deprecatedContext.getWiki();

        /*
         * Copy/paste from Wiki.delete(boolean deleteDatabase) because it checks internally for admin rights and the
         * current user, even if he is the owner of a wiki, might not have admin rights to the main wiki. If the method
         * is called from the main wiki, an owner might not be allowed to delete his wiki. This way we fix it.
         */
        try {
            xwiki.getStore().deleteWiki(workspaceName, deprecatedContext);
            observationManager.notify(new WikiDeletedEvent(workspaceName), workspaceName, deprecatedContext);
        } catch (Exception e) {
            throw new WorkspaceException(String.format("Failed to delete wiki '%s' from database", workspaceName), e);
        }

        try {
            xwiki.deleteDocument(workspace.getWikiDescriptor().getDocument(), false, deprecatedContext);
        } catch (Exception e) {
            throw new WorkspaceException(
                String.format("Failed to delete wiki descriptor for workspace '%s'", workspace), e);
        }

    }

    @Override
    public void editWorkspace(String workspaceName, XWikiServer modifiedWikiXObjectDocument) throws WorkspaceException
    {
        Workspace workspace = getWorkspace(workspaceName);

        XWikiContext deprecatedContext = getXWikiContext();
        XWiki xwiki = deprecatedContext.getWiki();

        Wiki wikiDocument = workspace.getWikiDocument();
        XWikiDocument coreWikiDocument = wikiDocument.getDocument();

        /*
         * Handle changes in the wiki descriptor.
         */
        DocumentReference xwikiServerClassReference =
            new DocumentReference(deprecatedContext.getMainXWiki(), XWIKI_SPACE, "XWikiServerClass");

        BaseObject currentWikiObject = coreWikiDocument.getXObject(xwikiServerClassReference);
        BaseObject modifiedWikiObject = modifiedWikiXObjectDocument.getDocument().getXObject(xwikiServerClassReference);

        /* Merge the two. */
        updateObject(modifiedWikiObject, currentWikiObject);

        /*
         * Handle changes in the workspace descriptor.
         */
        DocumentReference workspaceClassReference =
            new DocumentReference(deprecatedContext.getMainXWiki(), WORKSPACE_MANAGER_SPACE, WORKSPACE_CLASS);

        BaseObject currentWorkspaceObject = coreWikiDocument.getXObject(workspaceClassReference);
        BaseObject modifiedWorkspaceObject =
            modifiedWikiXObjectDocument.getDocument().getXObject(workspaceClassReference);

        /* Merge the two. */
        updateObject(modifiedWorkspaceObject, currentWorkspaceObject);

        /*
         * Save the changes.
         */
        try {
            xwiki.saveDocument(coreWikiDocument, "Workspace edited", true, deprecatedContext);
        } catch (Exception e) {
            throw new WorkspaceException("Failed to save modifications.", e);
        }
    }

    /**
     * Update the contents of an object using the contents of another. Objects must be of the same class.
     * 
     * @param source object that provides the new contents to update with
     * @param destination object that is to be updated
     * @throws WorkspaceException if objects' classes differ
     */
    private void updateObject(BaseObject source, BaseObject destination) throws WorkspaceException
    {
        if (!source.getXClassReference().equals(destination.getXClassReference())) {
            throw new WorkspaceException(String.format("Objects classes are not equal: %s vs %s", source
                .getXClassReference().toString(), destination.getXClassReference().toString()));
        }

        Iterator<String> itfields = source.getPropertyList().iterator();
        while (itfields.hasNext()) {
            String name = (String) itfields.next();
            destination.safeput(name, (PropertyInterface) ((BaseElement) source.safeget(name)).clone());
        }
    }

    @Override
    public Workspace getWorkspace(String workspaceName) throws WorkspaceException
    {
        XWikiContext deprecatedContext = getXWikiContext();
        XWiki xwiki = deprecatedContext.getWiki();

        /* Main wiki can not be a workspace. */
        if (deprecatedContext.getMainXWiki().equals(workspaceName)) {
            return null;
        }

        Workspace result = null;
        try {
            Wiki wikiDocument = getWikiManager().getWikiFromName(workspaceName);
            if (wikiDocument == null || wikiDocument.isNew()) {
                return null;
            }

            XWikiServer wikiDescriptor = wikiDocument.getFirstWikiAlias();
            if (wikiDescriptor == null || wikiDescriptor.isNew()) {
                throw new WorkspaceException(messageTool.get(WorkspaceManagerMessageTool.ERROR_WORKSPACEINVALID,
                    Arrays.asList(workspaceName)));
            }

            BaseObject workspaceObject = getWorkspaceObject(wikiDocument);
            if (workspaceObject == null) {
                return null;
            }

            DocumentReference groupReference =
                new DocumentReference(workspaceName, Workspace.WORKSPACE_GROUP_SPACE, Workspace.WORKSPACE_GROUP_PAGE);
            if (!xwiki.exists(groupReference, deprecatedContext)) {
                throw new WorkspaceException(messageTool.get(WorkspaceManagerMessageTool.ERROR_WORKSPACEINVALID,
                    Arrays.asList(workspaceName)));
            }

            result =
                new DefaultWorkspace(wikiDocument, wikiDescriptor, new Document(xwiki.getDocument(groupReference,
                    deprecatedContext), deprecatedContext));
        } catch (Exception e) {
            logAndThrowException(
                messageTool.get(WorkspaceManagerMessageTool.ERROR_WORKSPACEGET, Arrays.asList(workspaceName,
                    e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage())), e);
        }

        return result;
    }

    @Override
    public List<Workspace> getWorkspaces() throws WorkspaceException
    {
        List<Workspace> result = new ArrayList<Workspace>();

        try {
            List<Wiki> wikis = getWikiManager().getAllWikis();
            for (Wiki wiki : wikis) {
                try {
                    BaseObject workspaceObject = getWorkspaceObject(wiki);
                    if (workspaceObject != null) {
                        Workspace workspace = getWorkspace(wiki.getWikiName());
                        result.add(workspace);
                    }
                } catch (Exception e) {
                    /* Log and skip. */
                    logger.warn(
                        messageTool.get(WorkspaceManagerMessageTool.LOG_WORKSPACEINVALID,
                            Arrays.asList(wiki.getWikiName())), e);
                    continue;
                }
            }
        } catch (Exception e) {
            logAndThrowException(messageTool.get(WorkspaceManagerMessageTool.ERROR_WORKSPACEGETALL), e);
        }

        return result;
    }

    @Override
    public List<Workspace> getWorkspaceTemplates() throws WorkspaceException
    {
        List<Workspace> result = new ArrayList<Workspace>();

        List<Workspace> workspaces = getWorkspaces();
        for (Workspace workspace : workspaces) {
            XWikiServer server = workspace.getWikiDescriptor();
            if (server.isWikiTemplate()) {
                result.add(workspace);
            }
        }

        return result;
    }

    /**
     * @param wikiDocument the wiki descriptor document
     * @return the WorkspaceClass object contained by the wiki document, if it is a workspace, <code>null</code>
     *         otherwise
     * @throws Exception if problems occur
     */
    private BaseObject getWorkspaceObject(Wiki wikiDocument) throws Exception
    {
        XWikiContext deprecatedContext = getXWikiContext();
        XWiki xwiki = deprecatedContext.getWiki();

        DocumentReference workspaceClassReference =
            new DocumentReference(deprecatedContext.getMainXWiki(), WORKSPACE_MANAGER_SPACE, WORKSPACE_CLASS);
        XWikiDocument xwikiCoreDocument = xwiki.getDocument(wikiDocument.getDocumentReference(), deprecatedContext);

        BaseObject workspaceObject = xwikiCoreDocument.getXObject(workspaceClassReference);

        return workspaceObject;
    }

    /**
     * Utility method to log and throw a {@link WorkspaceException} wrapping a given exception.
     * 
     * @param message the error message to log
     * @param e the exception to log and wrap in the thrown {@link WorkspaceException}
     * @throws WorkspaceException when called
     */
    private void logAndThrowException(String message, Exception e) throws WorkspaceException
    {
        logger.error(message, e);
        throw new WorkspaceException(message, e);
    }

    @Override
    public boolean isWorkspace(String workspaceName)
    {
        try {
            Workspace workspace = getWorkspace(workspaceName);
            if (workspace != null) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }

        return false;
    }
}
