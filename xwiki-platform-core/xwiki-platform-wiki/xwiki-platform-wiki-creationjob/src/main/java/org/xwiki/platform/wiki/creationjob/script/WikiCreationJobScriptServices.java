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
package org.xwiki.platform.wiki.creationjob.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.distribution.internal.DistributionManager;
import org.xwiki.job.Job;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.platform.wiki.creationjob.WikiCreationRequest;
import org.xwiki.platform.wiki.creationjob.WikiCreator;
import org.xwiki.platform.wiki.creationjob.WikiCreationException;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;

/**
 * Script services for the creation of wikis.
 *
 * @version $Id$
 * @since 7.0M2
 */
@Component
@Singleton
@Named("wiki.creationjob")
public class WikiCreationJobScriptServices implements ScriptService
{
    /**
     * The key under which the last encountered error is stored in the current execution context.
     */
    private static final String ERROR_KEY = "scriptservice.wikicreationjob.error";
            
    @Inject
    private WikiCreator wikiCreator;

    @Inject
    private Execution execution;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;
    
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private DistributionManager distributionManager;
    
    @Inject
    private Logger logger;

    /**
     * Asynchronously create a wiki.
     *
     * @param request creation wiki request containing all information about the wiki to create
     * @return the creationjob that creates the wiki
     */
    public Job createWiki(WikiCreationRequest request)
    {
        try {
            // Verify that the user has the CREATE_WIKI right
            XWikiContext xcontext = xcontextProvider.get();
            WikiReference mainWikiReference = new WikiReference(wikiDescriptorManager.getMainWikiId());
            authorizationManager.checkAccess(Right.CREATE_WIKI, xcontext.getUserReference(), mainWikiReference);
            
            // Verify that if an extension id is provided, this extension is authorized.
            if (request.getExtensionId() != null) {
                if (!isAuthorizedExtension(request.getExtensionId())) {
                    throw new WikiCreationException(String.format("The extension [%s] is not authorized.",
                            request.getExtensionId()));
                }
            }
            return wikiCreator.createWiki(request);
            
        } catch (WikiCreationException e) {
            setLastError(e);
            logger.warn("Failed to create a new wiki.", e);
        } catch (AccessDeniedException e) {
            setLastError(e);
        }

        return null;
    }

    private boolean isAuthorizedExtension(ExtensionId extensionId)
    {
        // For now, only the extension declared in the WAR is authorized
        return getDefaultWikiExtensionId().equals(extensionId);
    }

    /**
     * @return the extension id of the default flavor
     */
    public ExtensionId getDefaultWikiExtensionId()
    {
        return distributionManager.getWikiUIExtensionId();
    }

    /**
     * @param wikiId id of the wiki
     * @return the creationjob status corresponding to the creation of the wiki
     */
    public JobStatus getJobStatus(String wikiId)
    {
        return wikiCreator.getJobStatus(wikiId);
    }

    /**
     * @return a new request for the creation of a new wiki
     */
    public WikiCreationRequest newWikiCreationRequest()
    {
        return new WikiCreationRequest();
    }

    /**
     * Get the error generated while performing the previously called action.
     * @return an eventual exception or {@code null} if no exception was thrown
     * @since 1.1
     */
    public Exception getLastError()
    {
        return (Exception) this.execution.getContext().getProperty(ERROR_KEY);
    }

    /**
     * Store a caught exception in the context, so that it can be later retrieved using {@link #getLastError()}.
     *
     * @param e the exception to store, can be {@code null} to clear the previously stored exception
     * @see #getLastError()
     * @since 1.1
     */
    private void setLastError(Exception e)
    {
        this.execution.getContext().setProperty(ERROR_KEY, e);
    }
}
