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
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.Response;
import org.xwiki.rendering.async.internal.AsyncRendererExecutor;
import org.xwiki.rendering.async.internal.AsyncRendererResult;
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

        // Get the asynchronous renderer result
        AsyncRendererResult result;
        try {
            result = this.executor.getResult(reference.getId(), true);
        } catch (InterruptedException e) {
            throw new ResourceReferenceHandlerException("Failed to get content", e);
        }

        // Check of a result was actually found for this id
        if (result == null) {
            throw new ResourceReferenceHandlerException("Cannot find any result for id [" + reference.getId() + "]");
        }

        // Send the result back
        Response response = this.container.getResponse();
        response.setContentType("text/html");

        try (OutputStream stream = response.getOutputStream()) {
            IOUtils.write(result.getResult(), stream, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ResourceReferenceHandlerException("Failed to send content", e);
        }

        // Be a good citizen, continue the chain, in case some lower-priority Handler has something to do for this
        // Resource Reference.
        chain.handleNext(reference);
    }
}
