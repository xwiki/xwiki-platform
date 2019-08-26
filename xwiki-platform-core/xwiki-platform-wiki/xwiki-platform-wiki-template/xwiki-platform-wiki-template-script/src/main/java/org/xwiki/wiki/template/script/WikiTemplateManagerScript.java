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
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;
import org.xwiki.wiki.provisioning.WikiProvisioningJob;
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
@Named("wiki.template")
@Singleton
public class WikiTemplateManagerScript implements ScriptService
{
    /**
     * Field name of the last API exception inserted in context.
     */
    @Deprecated
    public static final String CONTEXT_LASTEXCEPTION = "lastexception";

    /**
     * The key under which the last encountered error is stored in the current execution context.
     */
    private static final String WIKITEMPLATEERROR_KEY = "scriptservice.wiki.template.error";

    @Inject
    private WikiTemplateManager wikiTemplateManager;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

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
     * Get the error generated while performing the previously called action.
     *
     * @return an eventual exception or {@code null} if no exception was thrown
     */
    public Exception getLastError()
    {
        return (Exception) this.execution.getContext().getProperty(WIKITEMPLATEERROR_KEY);
    }

    /**
     * Store a caught exception in the context, so that it can be later retrieved using {@link #getLastError()}.
     *
     * @param e the exception to store, can be {@code null} to clear the previously stored exception
     * @see #getLastError()
     */
    private void setLastError(Exception e)
    {
        this.execution.getContext().setProperty(WIKITEMPLATEERROR_KEY, e);
    }

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
            error("Error while getting all the wiki templates.", e);
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
        XWikiContext context = xcontextProvider.get();
        try {
            // Check if the current script has the programing rights
            authorizationManager.checkAccess(Right.PROGRAM, context.getDoc().getAuthorReference(),
                    context.getDoc().getDocumentReference());
            // Get the descriptor
            WikiDescriptor descriptor = wikiDescriptorManager.getById(wikiId);
            // Get the wiki owner
            String owner = descriptor.getOwnerId();
            // Check right access
            WikiReference wikiReference = new WikiReference(descriptor.getId());
            String currentUser = entityReferenceSerializer.serialize(context.getUserReference());
            if (!currentUser.equals(owner)) {
                authorizationManager.checkAccess(Right.ADMIN, context.getUserReference(), wikiReference);
            }
            // Do the job
            wikiTemplateManager.setTemplate(wikiId, value);
            // Return success
            return true;
        } catch (WikiTemplateManagerException e) {
            error(String.format("Failed to set the template value [%s] for the wiki [%s].", value, wikiId), e);
            return false;
        } catch (AccessDeniedException e) {
            error(String.format("Access denied for [%s] to change the template value of the wiki [%s]. The user has"
                    + " not the right to perform this operation or the script has not the programming right.",
                    context.getUserReference(), wikiId), e);
            return false;
        } catch (WikiManagerException e) {
            error(String.format("Failed to get the descriptor of the wiki [%s].", wikiId), e);
            return false;
        }
    }

    /**
     * @param wikiId The id of the wiki to test
     * @return if the wiki is a template or not (or null if problems occur)
     */
    public Boolean isTemplate(String wikiId)
    {
        try {
            return wikiTemplateManager.isTemplate(wikiId);
        } catch (WikiTemplateManagerException e) {
            error(String.format("Failed to get if the wiki [%s] is a template or not.", wikiId), e);
            return null;
        }
    }

    /**
     * Create a new wiki from the specified template.
     *
     * @param newWikiId ID of the wiki to create
     * @param newWikiAlias Default alias of the wiki to create
     * @param templateId Id of the template to use
     * @param ownerId Id of the wiki owner
     * @param failOnExist fail the creation of the wiki id if not available
     * @return true if it succeed
     * @deprecated since 7.0M2, use
     *             {@code org.xwiki.platform.wiki.creationjob.script.WikiCreationJobScriptServices#createWiki} instead
     */
    @Deprecated
    public boolean createWikiFromTemplate(String newWikiId, String newWikiAlias,
            String templateId, String ownerId, boolean failOnExist)
    {
        try {
            XWikiContext context = xcontextProvider.get();
            // Check if the current script has the programing rights
            authorizationManager.checkAccess(Right.PROGRAM, context.getDoc().getAuthorReference(),
                    context.getDoc().getDocumentReference());
            // Check if the user has the right
            authorizationManager.checkAccess(Right.CREATE_WIKI, context.getUserReference(),
                    new WikiReference(context.getMainXWiki()));

            // Do the job
            wikiTemplateManager.createWikiFromTemplate(newWikiId, newWikiAlias, templateId, ownerId,
                        failOnExist);
            return true;
        } catch (WikiTemplateManagerException e) {
            error("Failed to create the wiki from the template.", e);
        } catch (AccessDeniedException e) {
            error("Error, you or this script does not have the right to create a wiki from a template.", e);
        }
        return false;
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
        // Log exception.
        logger.error(errorMessage, e);

        // Store exception in context.
        setLastError(e);
        // Deprecated but still usable
        this.execution.getContext().setProperty(CONTEXT_LASTEXCEPTION, e);
    }

    /**
     * @return the last exception, or null if there is not.
     * @deprecated since 5.4RC1 use {@link #getLastError()} ()} instead
     */
    @Deprecated
    public Exception getLastException()
    {
        return (Exception) this.execution.getContext().getProperty(CONTEXT_LASTEXCEPTION);
    }

    /**
     * Get the status of the wiki creation job.
     *
     * @param jobId id of the provisioning job.
     * @return the status of the job
     * @deprecated since 7.0M2
     */
    @Deprecated
    public JobStatus getWikiProvisioningJobStatus(List<String> jobId)
    {
        try {
            WikiProvisioningJob wikiProvisioningJob = wikiTemplateManager.getWikiProvisioningJob(jobId);
            if (wikiProvisioningJob == null) {
                return null;
            }
            return wikiProvisioningJob.getStatus();
        } catch (WikiTemplateManagerException e) {
            error("Failed to get tge wiki provisioning job.", e);
            return null;
        }
    }
}
