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
package org.xwiki.wiki.script;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiContext;

/**
 * Script service to manager wikis.
 *
 * @version $Id$
 * @since 5.3M2
 */
@Component
@Named("wiki")
@Singleton
public class WikiManagerScriptService implements ScriptService
{
    /**
     * Field name of the last API exception inserted in context.
     */
    public static final String CONTEXT_LASTEXCEPTION = "lastexception";

    @Inject
    private WikiManager wikiManager;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Execution context.
     */
    @Inject
    private Execution execution;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    /**
     * Logging tool.
     */
    @Inject
    private Logger logger;

    /**
     * Create a new wiki.
     *
     * @param wikiId unique identifier of the new wiki
     * @param wikiAlias default alias of the new wiki
     * @param ownerId Id of the user that will own the wiki
     * @param failOnExist Fail the operation if the wiki id already exists
     * @return the wiki descriptor of the new wiki, or null if problems occur
     */
    public WikiDescriptor createWiki(String wikiId, String wikiAlias, String ownerId, boolean failOnExist)
    {
        WikiDescriptor descriptor = null;

        XWikiContext context = xcontextProvider.get();

        try {
            // Check right access
            WikiReference mainWikiReference = new WikiReference(getMainWikiId());
            authorizationManager.checkAccess(Right.CREATE_WIKI, context.getUserReference(), mainWikiReference);
            if (!failOnExist) {
                authorizationManager.checkAccess(Right.PROGRAM, context.getUserReference(), mainWikiReference);
            }

            // Create the wiki
            descriptor = wikiManager.create(wikiId, wikiAlias, failOnExist);
            // Set the owner
            descriptor.setOwnerId(ownerId);
            wikiDescriptorManager.saveDescriptor(descriptor);
        } catch (WikiManagerException e) {
            error(e.getMessage(), e);
        } catch (AccessDeniedException e) {
            error("You don't have the right to create wiki or to set failOnExist to false.", e);
        }

        return descriptor;
    }

    /**
     * Delete the specified wiki.
     *
     * @param wikiId unique identifier of the wiki to delete
     * @return true if the wiki has been successfully deleted
     */
    public boolean deleteWiki(String wikiId)
    {
        // Test right
        XWikiContext context = xcontextProvider.get();
        if (!canDeleteWiki(context.getUser(), wikiId)) {
            String errorMessage = "You don't have the right to delete the wiki";
            error(errorMessage, new Exception(errorMessage));
            return false;
        }
        // Delete the wiki
        try {
            wikiManager.delete(wikiId);
        } catch (WikiManagerException e) {
            error(String.format("Failed to delete wiki [%s]", wikiId), e);
            return false;
        }
        // Return success
        return true;
    }

    /**
     * Test if a given user can delete a given wiki.
     *
     * @param userId the id of the user to test
     * @param wikiId the id of the wiki
     * @return whether or not the user can delete the specified wiki
     */
    public boolean canDeleteWiki(String userId, String wikiId)
    {
        String errorMessage = String.format("Error while getting the descriptor of wiki [%s]", wikiId);
        try {
            // Get the wiki owner
            WikiDescriptor descriptor = wikiDescriptorManager.getById(wikiId);
            if (descriptor == null) {
                error(errorMessage, new Exception(errorMessage));
                return false;
            }
            String owner = descriptor.getOwnerId();
            // If the user is the owner
            if (userId.equals(owner)) {
                return true;
            }
            // If the user is an admin
            DocumentReference userReference = documentReferenceResolver.resolve(userId);
            WikiReference wikiReference = new WikiReference(wikiId);
            if (authorizationManager.hasAccess(Right.ADMIN, userReference, wikiReference)) {
                return true;
            }
        } catch (WikiManagerException e) {
            error(errorMessage, e);
        }

        return false;
    }

    /**
     * Get a wiki descriptor from one of its alias.
     *
     * @param wikiAlias alias to search
     * @return the wiki descriptor corresponding to the alias, or null if no descriptors match the alias
     */
    public WikiDescriptor getByAlias(String wikiAlias)
    {
        WikiDescriptor descriptor = null;

        try {
            descriptor = wikiDescriptorManager.getByAlias(wikiAlias);
        } catch (WikiManagerException e) {
            error(e.getMessage(), e);
        }

        return descriptor;
    }

    /**
     * Get a wiki descriptor from its unique identifier.
     *
     * @param wikiId unique identifier of the wiki to search
     * @return the wiki descriptor corresponding to the Id, or null if no descriptors match the id
     */
    public WikiDescriptor getById(String wikiId)
    {
        WikiDescriptor descriptor = null;

        try {
            descriptor = wikiDescriptorManager.getById(wikiId);
        } catch (WikiManagerException e) {
            error(e.getMessage(), e);
        }

        return descriptor;
    }

    /**
     * Get all the wiki descriptors.
     *
     * @return the list of all wiki descriptors
     */
    public Collection<WikiDescriptor> getAll()
    {
        Collection<WikiDescriptor> wikis;
        try {
            wikis = wikiDescriptorManager.getAll();
        } catch (WikiManagerException e) {
            error(e.getMessage(), e);
            wikis = new ArrayList<WikiDescriptor>();
        }
        return wikis;
    }

    /**
     * Test if a wiki exists.
     *
     * @param wikiId unique identifier to test
     * @return true if a wiki with this Id exists on the system.
     */
    public boolean exists(String wikiId)
    {
        try {
            return wikiDescriptorManager.exists(wikiId);
        } catch (WikiManagerException e) {
            error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if the wikiId is valid and available (the name is not already taken for technical reasons).
     *
     * @param wikiId the Id to test
     * @return true if the Id is valid and available
     */
    public boolean idAvailable(String wikiId)
    {
        try {
            return wikiManager.idAvailable(wikiId);
        } catch (WikiManagerException e) {
            error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * @return the descriptor of the main wiki or null if problems occur
     */
    public WikiDescriptor getMainWikiDescriptor()
    {
        WikiDescriptor descriptor = null;
        try {
            descriptor = wikiDescriptorManager.getMainWikiDescriptor();
        } catch (WikiManagerException e) {
            error(e.getMessage(), e);
        }
        return descriptor;
    }

    /**
     * @return the Id of the main wiki
     */
    public String getMainWikiId()
    {
        return wikiDescriptorManager.getMainWikiId();
    }

    /**
     * @return the id of the current wiki
     */
    public String getCurrentWikiId()
    {
        return wikiDescriptorManager.getCurrentWikiId();
    }

    /**
     * Save the specified descriptor (if you have the right).
     *
     * @param descriptor descriptor to save
     * @return true if it succeed
     */
    public boolean saveDescriptor(WikiDescriptor descriptor)
    {
        XWikiContext context = xcontextProvider.get();

        try {
            // Get the wiki owner
            WikiDescriptor oldDescriptor = wikiDescriptorManager.getById(descriptor.getId());
            String owner = oldDescriptor.getOwnerId();
            // Check right access
            WikiReference wikiReference = new WikiReference(oldDescriptor.getId());
            if (!context.getUserReference().toString().equals(owner)) {
                authorizationManager.checkAccess(Right.ADMIN, context.getUserReference(), wikiReference);
            }
            wikiDescriptorManager.saveDescriptor(descriptor);
            return true;
        } catch (WikiManagerException e) {
            error(e.getMessage(), e);
            return false;
        } catch (AccessDeniedException e) {
            error(e.getMessage(), e);
            return false;
        }
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
     * @return the last exception, or null if there is not
     */
    public Exception getLastException()
    {
        return (Exception) this.execution.getContext().getProperty(CONTEXT_LASTEXCEPTION);
    }
}
