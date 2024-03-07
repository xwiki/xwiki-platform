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
package org.xwiki.extension.script;

import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.context.Execution;
import org.xwiki.extension.internal.validator.AbstractExtensionValidator;
import org.xwiki.job.AbstractRequest;
import org.xwiki.job.JobExecutor;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.job.script.JobScriptService;
import org.xwiki.script.safe.ScriptSafeProvider;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.ContextualAuthorizationManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Base class for all extension related script services.
 * 
 * @version $Id$
 * @since 5.3M1
 */
public abstract class AbstractExtensionScriptService implements ScriptService
{
    /**
     * The key under which the last encountered error is stored in the current execution context.
     */
    public static final String EXTENSIONERROR_KEY = "scriptservice.extension.error";

    /**
     * Extension request property that specifies from which wiki the job was started.
     */
    protected static final String PROPERTY_CONTEXT_WIKI = "context.wiki";

    /**
     * Extension request property that specifies from which document action the job was started.
     */
    protected static final String PROPERTY_CONTEXT_ACTION = "context.action";

    protected static final String PROPERTY_USERREFERENCE = AbstractExtensionValidator.PROPERTY_USERREFERENCE;

    protected static final String PROPERTY_CALLERREFERENCE = AbstractExtensionValidator.PROPERTY_CALLERREFERENCE;

    protected static final String PROPERTY_CHECKRIGHTS = AbstractExtensionValidator.PROPERTY_CHECKRIGHTS;

    /**
     * The prefix used for wiki namespace id.
     */
    protected static final String WIKI_NAMESPACE_PREFIX = "wiki:";

    @Inject
    @SuppressWarnings("rawtypes")
    protected ScriptSafeProvider scriptProvider;

    /**
     * Provides access to the current context.
     */
    @Inject
    protected Execution execution;

    /**
     * Needed for getting the current user reference.
     */
    @Inject
    protected DocumentAccessBridge documentAccessBridge;

    @Inject
    protected Provider<XWikiContext> xcontextProvider;

    @Inject
    protected JobExecutor jobExecutor;

    @Inject
    protected ContextualAuthorizationManager authorization;

    @Inject
    @Named("job")
    private ScriptService jobScriptService;

    /**
     * @param <S> the type of the safe object version
     * @param unsafe the unsafe object
     * @return the safe version of the passed object
     */
    @SuppressWarnings("unchecked")
    protected <S> S safe(Object unsafe)
    {
        return (S) this.scriptProvider.get(unsafe);
    }

    protected <T extends AbstractRequest> void setRightsProperties(T extensionRequest)
    {
        extensionRequest.setProperty(AbstractExtensionValidator.PROPERTY_CHECKRIGHTS, true);
        extensionRequest.setProperty(AbstractExtensionValidator.PROPERTY_CHECKRIGHTS_USER, true);
        extensionRequest.setProperty(AbstractExtensionValidator.PROPERTY_USERREFERENCE,
            this.documentAccessBridge.getCurrentUserReference());
        XWikiDocument callerDocument = getCallerDocument();
        if (callerDocument != null) {
            extensionRequest.setProperty(AbstractExtensionValidator.PROPERTY_CHECKRIGHTS_CALLER, true);
            extensionRequest.setProperty(AbstractExtensionValidator.PROPERTY_CALLERREFERENCE,
                callerDocument.getContentAuthorReference());
        }
    }

    protected XWikiDocument getCallerDocument()
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiDocument sdoc = (XWikiDocument) xcontext.get("sdoc");
        if (sdoc == null) {
            sdoc = xcontext.getDoc();
        }

        return sdoc;
    }

    protected JobStatus getJobStatus(List<String> jobId)
    {
        return ((JobScriptService) jobScriptService).getJobStatus(jobId);
    }

    /**
     * @param namespace the namespace
     * @return the wiki identifier
     * @since 8.1M1
     */
    protected String toWikiId(String namespace)
    {
        if (namespace != null && namespace.startsWith(WIKI_NAMESPACE_PREFIX)) {
            return namespace.substring(WIKI_NAMESPACE_PREFIX.length());
        }

        return null;
    }

    /**
     * @param wiki the wiki identifier
     * @return the namespace
     * @since 8.1M1
     */
    protected String fromWikitoNamespace(String wiki)
    {
        return WIKI_NAMESPACE_PREFIX + wiki;
    }

    /**
     * Add to the job request various information about the current context.
     * 
     * @param request the job request
     * @since 9.5
     */
    public void contextualize(AbstractRequest request)
    {
        // Provide informations on what started the job
        request.setProperty(PROPERTY_CONTEXT_WIKI, this.xcontextProvider.get().getWikiId());
        request.setProperty(PROPERTY_CONTEXT_ACTION, this.xcontextProvider.get().getAction());
    }

    // Error management

    /**
     * Get the error generated while performing the previously called action.
     * 
     * @return an eventual exception or {@code null} if no exception was thrown
     */
    public Exception getLastError()
    {
        return (Exception) this.execution.getContext().getProperty(EXTENSIONERROR_KEY);
    }

    /**
     * Store a caught exception in the context, so that it can be later retrieved using {@link #getLastError()}.
     * 
     * @param e the exception to store, can be {@code null} to clear the previously stored exception
     * @see #getLastError()
     */
    protected void setError(Exception e)
    {
        this.execution.getContext().setProperty(EXTENSIONERROR_KEY, e);
    }

    /**
     * Call the passed callable but try/catch and return null in case of exception (and update the last error).
     * 
     * @param <R> the result type of method {@code call}
     * @param callable a task that returns a result and may throw an exception
     * @return the computed result
     * @since 11.10
     */
    protected <R> R wrapError(Callable<R> callable)
    {
        setError(null);

        try {
            return callable.call();
        } catch (Exception e) {
            setError(e);
        }

        return null;
    }

    /**
     * Call the passed callable but try/catch and return null in case of exception (and update the last error). A safe
     * verison of the result is returned.
     * 
     * @param <R> the result type of method {@code call}
     * @param callable a task that returns a result and may throw an exception
     * @return the safe version of the computed result
     * @since 11.10
     */
    protected <R> R safeWrapError(Callable<R> callable)
    {
        return wrapError(() -> safe(callable.call()));
    }
}
