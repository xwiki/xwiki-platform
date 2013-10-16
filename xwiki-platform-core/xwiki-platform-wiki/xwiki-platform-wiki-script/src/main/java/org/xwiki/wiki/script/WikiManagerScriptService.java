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
 * @since 5.3M1
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
     * @return the wiki descriptor of the new wiki, or null if problems occur
     */
    public WikiDescriptor createWiki(String wikiId, String wikiAlias)
    {
        WikiDescriptor descriptor = null;

        XWikiContext context = xcontextProvider.get();

        try {
            // Check right access
            authorizationManager.checkAccess(Right.CREATE_WIKI, context.getUserReference(),
                    new WikiReference(context.getMainXWiki()));

            // Create the wiki
            descriptor = wikiManager.create(wikiId, wikiAlias);
        } catch (WikiManagerException e) {
            error(e.getMessage(), e);
        } catch (AccessDeniedException e) {
            error("You don't have the right to create wiki", e);
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
        XWikiContext context = xcontextProvider.get();
        WikiReference wikiReference = new WikiReference(wikiId);

        try {
            // Check right access
            authorizationManager.checkAccess(Right.ADMIN, context.getUserReference(), wikiReference);
            authorizationManager.checkAccess(Right.CREATE_WIKI, context.getUserReference(), wikiReference);

            // Delete the wiki
            wikiManager.delete(wikiId);
        } catch (WikiManagerException e) {
            return false;
        } catch (AccessDeniedException e) {
            error("You don't have the right to delete the wiki", e);
        }

        return true;
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
}
