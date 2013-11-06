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
package org.xwiki.wiki.template.script;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;
import org.xwiki.wiki.template.WikiTemplateManager;
import org.xwiki.wiki.template.WikiTemplateManagerException;

import com.xpn.xwiki.XWikiContext;

/**
 * Script service to manage the wiki templates.
 *
 * @since 5.3M2
 * @version $Id$
 */
@Component
@Named("wikitemplate")
@Singleton
public class WikiTemplateManagerScript implements ScriptService
{
    /**
     * Field name of the last API exception inserted in context.
     */
    public static final String CONTEXT_LASTEXCEPTION = "lastexception";

    @Inject
    private WikiTemplateManager wikiTemplateManager;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private AuthorizationManager authorizationManager;

    /**
     * Used to access current {@link com.xpn.xwiki.XWikiContext}.
     */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Execution context.
     */
    @Inject
    private Execution execution;

    /**
     * Logging tool.
     */
    @Inject
    private Logger logger;

    /**
     * Get the list of all wiki templates.
     *
     * @return list of wiki templates
     */
    public Collection<WikiDescriptor> getTemplates()
    {
        try {
            return wikiTemplateManager.getTemplates();
        } catch (WikiTemplateManagerException e) {
            return new ArrayList<WikiDescriptor>();
        }
    }

    /**
     * Set if the specified wiki is a template or not.
     *
     * @param wikiId the ID of the wiki to specify
     * @param value whether or not the wiki is a template
     * @return true if the action succeed
     */
    public boolean setTemplate(String wikiId, boolean value)
    {
        try {
            XWikiContext context = xcontextProvider.get();
            // Get the descriptor
            WikiDescriptor descriptor = wikiDescriptorManager.getById(wikiId);
            // Get the wiki owner
            String owner = descriptor.getOwnerId();
            // Check right access
            WikiReference wikiReference = new WikiReference(descriptor.getId());
            if (!context.getUserReference().toString().equals(owner)) {
                authorizationManager.checkAccess(Right.ADMIN, context.getUserReference(), wikiReference);
            }
            // Do the job
            wikiTemplateManager.setTemplate(wikiId, value);
            // Return success
            return true;
        } catch (WikiTemplateManagerException e) {
            //TODO log
            return false;
        } catch (AccessDeniedException e) {
            //TODO log
            return false;
        } catch (WikiManagerException e) {
            //TODO log
            return false;
        }
    }

    /**
     * @param wikiId The id of the wiki to test
     * @return if the wiki is a template or not
     * @throws WikiTemplateManagerException if problems occur
     */
    public boolean isTemplate(String wikiId) throws WikiTemplateManagerException
    {
        return wikiTemplateManager.isTemplate(wikiId);
    }

    /**
     * Create a new wiki from the specified template.
     *
     * @param newWikiId ID of the wiki to create
     * @param newWikiAlias Default alias of the wiki to create
     * @param templateId Id of the template to use
     * @param ownerId Id of the wiki owner
     * @param failOnExist fail the creation of the wiki id if not available
     * @return The descriptor of the new wiki or null if problems occur
     */
    public WikiDescriptor createWikiFromTemplate(String newWikiId, String newWikiAlias,
            String templateId, String ownerId, boolean failOnExist)
    {
        WikiDescriptor descriptor = null;
        try {
            XWikiContext context = xcontextProvider.get();
            if (authorizationManager.hasAccess(Right.CREATE_WIKI, context.getUserReference(),
                    new WikiReference(context.getMainXWiki())))
            {
                descriptor = wikiTemplateManager.createWikiFromTemplate(newWikiId, newWikiAlias, templateId, ownerId,
                        failOnExist);
            }
        } catch (WikiTemplateManagerException e) {
            error("Failed to create the wiki from the template.", e);
        }
        return descriptor;
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

    /**
     * Get the status of the wiki creation job.
     *
     * @param wikiId id of the constructing wiki
     * @return the status of the job
     */
    public JobStatus getWikiCreationStatus(String wikiId)
    {
        try {
            return wikiTemplateManager.getWikiCreationStatus(wikiId);
        } catch (WikiTemplateManagerException e) {
            return null;
        }
    }
}
