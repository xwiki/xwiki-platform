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
package org.xwiki.rendering.async.internal.service;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.container.Container;
import org.xwiki.container.Response;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.rendering.async.AsyncContextHandler;
import org.xwiki.rendering.async.internal.AsyncRendererExecutor;
import org.xwiki.rendering.async.internal.AsyncRendererJobStatus;
import org.xwiki.resource.AbstractResourceReferenceHandler;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.annotations.Authenticate;

/**
 * Async renderer resource handler.
 *
 * @version $Id$
 * @since 10.10RC1
 */
@Component
@Named(AsyncRendererResourceReferenceHandler.HINT)
@Singleton
@Authenticate
public class AsyncRendererResourceReferenceHandler extends AbstractResourceReferenceHandler<ResourceType>
{
    /**
     * The role hint to use for job related resource handler.
     */
    public static final String HINT = "asyncrenderer";

    /**
     * Represents a Async renderer Resource Type.
     */
    public static final ResourceType TYPE = new ResourceType(HINT);

    @Inject
    private AsyncRendererExecutor executor;

    @Inject
    private Container container;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    @Override
    public List<ResourceType> getSupportedResourceReferences()
    {
        return Arrays.asList(TYPE);
    }

    @Override
    public void handle(ResourceReference resourceReference, ResourceReferenceHandlerChain chain)
        throws ResourceReferenceHandlerException
    {
        AsyncRendererResourceReference reference = (AsyncRendererResourceReference) resourceReference;

        String clientIdString = reference.getClientId();

        if (clientIdString == null) {
            throw new ResourceReferenceHandlerException("Client id is mandatory");
        }

        long clientId = Long.parseLong(clientIdString);

        // Get the asynchronous renderer status
        AsyncRendererJobStatus status;
        try {
            // TODO: don't wait forever and return the job progress if not finished
            status = this.executor.getAsyncStatus(reference.getId(), clientId, Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            throw new ResourceReferenceHandlerException("Failed to get content", e);
        }

        // Check of a result was actually found for this id
        if (status == null) {
            throw new ResourceReferenceHandlerException("Cannot find any status for id [" + reference.getId() + "]");
        }

        // Send the result back
        sendReponse(status);

        // Be a good citizen, continue the chain, in case some lower-priority Handler has something to do for this
        // Resource Reference.
        chain.handleNext(reference);
    }

    private void sendReponse(AsyncRendererJobStatus status) throws ResourceReferenceHandlerException
    {
        // Send the result back
        Response response = this.container.getResponse();
        response.setContentType("text/html");

        // Create the asynchronous HTML meta
        StringBuilder head = new StringBuilder();
        Map<String, Collection<Object>> uses = status.getUses();
        for (Map.Entry<String, Collection<Object>> entry : uses.entrySet()) {
            AsyncContextHandler handler;
            try {
                handler = this.componentManager.getInstance(AsyncContextHandler.class, entry.getKey());
            } catch (ComponentLookupException e) {
                this.logger.error("Failed to get AsyncContextHandler with type [{}]", entry.getKey(), e);

                continue;
            }

            handler.addHTMLHead(head, entry.getValue());
        }
        if (head.length() > 0) {
            if (response instanceof ServletResponse) {
                ((ServletResponse) response).getHttpServletResponse().addHeader("X-XWIKI-HTML-HEAD", head.toString());
            }
        }

        try (OutputStream stream = response.getOutputStream()) {
            IOUtils.write(status.getResult().getResult(), stream, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ResourceReferenceHandlerException("Failed to send content", e);
        }
    }
}
