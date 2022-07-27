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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.script.service.ScriptServiceManager;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.url.internal.standard.StandardURLConfiguration;
import org.xwiki.wiki.configuration.WikiConfiguration;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.internal.descriptor.document.WikiDescriptorDocumentHelper;
import org.xwiki.wiki.manager.WikiManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiContext;

/**
 * Script service to manage wikis.
 * 
 * @version $Id$
 * @since 5.3M2
 */
@Component
@Named(WikiManagerScriptService.ROLEHINT)
@Singleton
public class WikiManagerScriptService implements ScriptService
{
    /**
     * Hint of the component.
     */
    public static final String ROLEHINT = "wiki";

    /**
     * Field name of the last API exception inserted in context.
     */
    @Deprecated
    public static final String CONTEXT_LASTEXCEPTION = "lastexception";

    /**
     * The key under which the last encountered error is stored in the current execution context.
     */
    private static final String WIKIERROR_KEY = "scriptservice.wiki.error";

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

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private ScriptServiceManager scriptServiceManager;

    @Inject
    private StandardURLConfiguration standardURLConfiguration;

    @Inject
    private WikiConfiguration wikiConfiguration;

    @Inject
    private WikiDescriptorDocumentHelper wikiDescriptorDocumentHelper;

    /**
     * Logging tool.
     */
    @Inject
    private Logger logger;

    /**
     * Get a sub script service related to wiki. (Note that we're voluntarily using an API name of "get" to make it
     * extra easy to access Script Services from Velocity (since in Velocity writing <code>$services.wiki.name</code> is
     * equivalent to writing <code>$services.wiki.get("name")</code>). It also makes it a short and easy API name for
     * other scripting languages.
     * 
     * @param serviceName id of the script service
     * @return the service asked or null if none could be found
     */
    public ScriptService get(String serviceName)
    {
        return scriptServiceManager.get(ROLEHINT + '.' + serviceName);
    }

    /**
     * Get the error generated while performing the previously called action.
     *
     * @return an eventual exception or {@code null} if no exception was thrown
     */
    public Exception getLastError()
    {
        return (Exception) this.execution.getContext().getProperty(WIKIERROR_KEY);
    }

    /**
     * Store a caught exception in the context, so that it can be later retrieved using {@link #getLastError()}.
     *
     * @param e the exception to store, can be {@code null} to clear the previously stored exception
     * @see #getLastError()
     */
    private void setLastError(Exception e)
    {
        this.execution.getContext().setProperty(WIKIERROR_KEY, e);
    }

    // TODO: move to new API a soon as a proper helper is provided
    private void checkProgrammingRights() throws AuthorizationException
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        authorizationManager.checkAccess(Right.PROGRAM, xcontext.getDoc().getAuthorReference(), xcontext.getDoc()
            .getDocumentReference());
    }

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
            // Check if the current script has the programing rights
            checkProgrammingRights();

            // Check right access
            WikiReference mainWikiReference = new WikiReference(getMainWikiId());
            authorizationManager.checkAccess(Right.CREATE_WIKI, context.getUserReference(), mainWikiReference);
            if (!failOnExist) {
                authorizationManager.checkAccess(Right.PROGRAM, context.getUserReference(), mainWikiReference);
            }

            // Create the wiki
            descriptor = wikiManager.create(wikiId, wikiAlias, ownerId, failOnExist);
        } catch (Exception e) {
            error(e);
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
        // Test if the script has the programming right
        XWikiContext context = xcontextProvider.get();

        try {
            // Check if the current script has the programming rights
            checkProgrammingRights();

            // Test right
            if (!canDeleteWiki(entityReferenceSerializer.serialize(context.getUserReference()), wikiId)) {
                throw new AuthorizationException("You don't have the right to delete the wiki");
            }

            // Delete the wiki
            wikiManager.delete(wikiId);

            // Return success
            return true;
        } catch (Exception e) {
            error(String.format("Failed to delete wiki [%s]", wikiId), e);
        }

        return false;
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
        try {
            // Get target wiki descriptor
            WikiDescriptor descriptor = wikiDescriptorManager.getById(wikiId);
            if (descriptor == null) {
                error(new Exception(String.format("Could not find descriptor for wiki [%s]]", wikiId)));
                return false;
            }
            // Get the full reference of the given user
            DocumentReference userReference = documentReferenceResolver.resolve(userId);
            String fullUserId = entityReferenceSerializer.serialize(userReference);

            // If the user is the owner
            String owner = descriptor.getOwnerId();
            if (fullUserId.equals(owner)) {
                return true;
            }

            // If the user is an admin
            WikiReference wikiReference = new WikiReference(wikiId);
            if (authorizationManager.hasAccess(Right.ADMIN, userReference, wikiReference)) {
                return true;
            }
        } catch (WikiManagerException e) {
            error(String.format("Error while getting the descriptor of wiki [%s]", wikiId), e);
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
            error(e);
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
            error(e);
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
            error(e);
            wikis = new ArrayList<WikiDescriptor>();
        }

        return wikis;
    }

    /**
     * Get all the wiki identifiers.
     * 
     * @return the list of all wiki identifiers
     * @since 6.2M1
     */
    public Collection<String> getAllIds()
    {
        Collection<String> wikis;
        try {
            wikis = wikiDescriptorManager.getAllIds();
        } catch (WikiManagerException e) {
            error(e);
            wikis = new ArrayList<String>();
        }

        return wikis;
    }

    /**
     * Test if a wiki exists.
     * 
     * @param wikiId unique identifier to test
     * @return true if a wiki with this Id exists on the system or null if some error occurs.
     */
    public Boolean exists(String wikiId)
    {
        try {
            return wikiDescriptorManager.exists(wikiId);
        } catch (WikiManagerException e) {
            error(e);
            return null;
        }
    }

    /**
     * Check if the wikiId is valid and available (the name is not already taken for technical reasons).
     * 
     * @param wikiId the Id to test
     * @return true if the Id is valid and available or null if some error occurs
     */
    public Boolean idAvailable(String wikiId)
    {
        try {
            return wikiManager.idAvailable(wikiId);
        } catch (WikiManagerException e) {
            error(e);
            return null;
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
            error(e);
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
     * @return the reference of the current wiki.
     * @since 12.7RC1
     */
    public WikiReference getCurrentWikiReference()
    {
        return wikiDescriptorManager.getCurrentWikiReference();
    }

    /**
     * @return the descriptor of the current wiki
     */
    public WikiDescriptor getCurrentWikiDescriptor()
    {
        WikiDescriptor descriptor = null;
        try {
            descriptor = wikiDescriptorManager.getCurrentWikiDescriptor();
        } catch (WikiManagerException e) {
            error(e);
        }

        return descriptor;
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

        boolean isAllowed;

        try {
            // Get the wiki owner
            WikiDescriptor oldDescriptor = wikiDescriptorManager.getById(descriptor.getId());
            WikiReference wikiReference = descriptor.getReference();
            if (oldDescriptor != null) {
                // Users that can edit the wiki's descriptor document are allowed to use this API as well. This
                // includes global admins.
                DocumentReference descriptorDocument =
                    wikiDescriptorDocumentHelper.getDocumentReferenceFromId(oldDescriptor.getId());
                isAllowed = authorizationManager.hasAccess(Right.EDIT, context.getUserReference(), descriptorDocument);

                String currentOwner = oldDescriptor.getOwnerId();
                if (!isAllowed) {
                    // The current owner can edit anything.
                    isAllowed = entityReferenceSerializer.serialize(context.getUserReference()).equals(currentOwner);
                }

                if (!isAllowed) {
                    // Local admins can edit the descriptor, except for the "ownerId" field, which should be
                    // editable only by the current owner or main wiki admins.
                    String newOwner = descriptor.getOwnerId();
                    isAllowed =
                        authorizationManager.hasAccess(Right.ADMIN, context.getUserReference(), wikiReference)
                            && StringUtils.equals(newOwner, currentOwner);
                }
            } else {
                // Saving a descriptor that did not already exist should be reserved to global admins
                isAllowed =
                    authorizationManager.hasAccess(Right.ADMIN, context.getUserReference(), new WikiReference(
                        wikiDescriptorManager.getMainWikiId()));
            }

            if (!isAllowed) {
                // Exhausted all options. Deny access for the current user to edit the descriptor.
                throw new AccessDeniedException(context.getUserReference(), wikiReference);
            } else {
                // Execute the operation.
                wikiDescriptorManager.saveDescriptor(descriptor);
            }

            return true;
        } catch (Exception e) {
            error(e);
            return false;
        }
    }

    /**
     * Tell if the path mode is used for subwikis.
     * <p>
     * Example:
     * 
     * <pre>
     * {@code
     * wiki alias: subwiki
     * URL if path mode is enabled:
     *   /xwiki/wiki/subwiki/
     * URL if path mode is disabled:
     *   http://subwiki/
     * }
     * </pre>
     *
     * @return either or not the path mode is enabled
     */
    public boolean isPathMode()
    {
        return standardURLConfiguration.isPathBasedMultiWiki();
    }

    /**
     * @return the default suffix to append to new wiki aliases.
     */
    public String getAliasSuffix()
    {
        return wikiConfiguration.getAliasSuffix();
    }

    /**
     * Log exception and store it in the context.
     *
     * @param e the caught exception
     */
    private void error(Exception e)
    {
        error(null, e);
    }

    /**
     * Log exception and store it in the context.
     * 
     * @param errorMessage error message
     * @param e the caught exception
     */
    private void error(String errorMessage, Exception e)
    {
        String errorMessageToLog = errorMessage;
        if (errorMessageToLog == null) {
            errorMessageToLog = e.getMessage();
        }

        // Log exception.
        logger.error(errorMessageToLog, e);

        // Store exception in context.
        setLastError(e);
        // Deprecated
        this.execution.getContext().setProperty(CONTEXT_LASTEXCEPTION, e);
    }

    /**
     * @return the last exception, or null if there is not
     * @deprecated since 5.4RC1 use {@link #getLastError()} ()} instead
     */
    @Deprecated
    public Exception getLastException()
    {
        return (Exception) this.execution.getContext().getProperty(CONTEXT_LASTEXCEPTION);
    }
}
