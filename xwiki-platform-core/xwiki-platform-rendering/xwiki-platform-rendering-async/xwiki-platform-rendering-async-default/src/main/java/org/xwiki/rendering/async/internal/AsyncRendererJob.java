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
package org.xwiki.rendering.async.internal;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.GroupedJob;
import org.xwiki.job.JobGroupPath;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.async.AsyncContext;
import org.xwiki.rendering.async.internal.DefaultAsyncContext.ContextUse;
import org.xwiki.template.TemplateManager;

import com.xpn.xwiki.internal.context.XWikiContextContextStore;

/**
 * Default implementation of {@link AsyncRendererJob}.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
@Component
@Named(AsyncRendererJobStatus.JOBTYPE)
public class AsyncRendererJob extends AbstractJob<AsyncRendererJobRequest, AsyncRendererJobStatus> implements GroupedJob
{
    @Inject
    private AsyncRendererCache cache;

    @Inject
    private AsyncContext asyncContext;

    @Inject
    private TemplateManager templateManager;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Override
    protected AsyncRendererJobStatus createNewStatus(AsyncRendererJobRequest request)
    {
        return new AsyncRendererJobStatus(request, this.observationManager, this.loggerManager);
    }

    @Override
    public String getType()
    {
        return AsyncRendererJobStatus.JOBTYPE;
    }

    @Override
    protected void runInternal() throws Exception
    {
        AsyncRenderer renderer = getRequest().getRenderer();

        // Enable async execution only if cache is disabled as otherwise we could end up with place holders not
        // associated to any job since it was not really executed the following times
        this.asyncContext.setEnabled(!renderer.isCacheAllowed());

        // Prepare to catch stuff to invalidate the cache
        if (this.asyncContext instanceof DefaultAsyncContext) {
            ((DefaultAsyncContext) this.asyncContext).pushContextUse();
        }

        // Many UI elements expect xwikivars.vm result to be in the context so we execute it
        // FIXME: not very happy with that but can't find a better place yet
        // (other than executing it at the beginning of every single element which might be executed asynchronously...)
        this.templateManager.execute("xwikivars.vm");

        // Mark the context document as used if it was explicitly set in the context, unless the context document is 
        // null.
        if (this.request.getContext() != null
            && this.request.getContext().containsKey(XWikiContextContextStore.PROP_DOCUMENT_REFERENCE))
        {
            DocumentReference currentDocumentReference = this.documentAccessBridge.getCurrentDocumentReference();
            if (currentDocumentReference != null) {
                this.asyncContext.useEntity(currentDocumentReference);
            }
        }

        AsyncRendererResult result = renderer.render(true, renderer.isCacheAllowed());

        getStatus().setResult(result);

        if (this.asyncContext instanceof DefaultAsyncContext) {
            // Remember various elements used during the execution (to invalidate the cache or restore them when needed)
            ContextUse contextUse = ((DefaultAsyncContext) this.asyncContext).popContextUse();
            getStatus().setReferences(contextUse.getReferences());
            getStatus().setRoles(contextUse.getRoles());
            getStatus().setRoleTypes(contextUse.getRoleTypes());
            getStatus().setUses(contextUse.getUses());
        }
    }

    @Override
    protected void jobFinished(Throwable error)
    {
        super.jobFinished(error);

        // Cache the result
        this.cache.put(getStatus());
    }

    @Override
    public JobGroupPath getGroupPath()
    {
        return getRequest().getJobGroupPath();
    }
}
