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
package org.xwiki.resource.servlet;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.container.Response;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.resource.AbstractResourceReferenceHandler;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandler;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.ResourceType;
import org.xwiki.stability.Unstable;

/**
 * Base class for {@link ResourceReferenceHandler}s that can handle servlet resource requests.
 * 
 * @param <R> the resource type
 * @version $Id$
 * @since 7.4.6
 * @since 8.2.2
 * @since 8.3
 */
@Unstable
public abstract class AbstractServletResourceReferenceHandler<R extends ResourceReference>
    extends AbstractResourceReferenceHandler<ResourceType>
{
    /**
     * One year duration can be considered as permanent caching.
     */
    private static final long CACHE_DURATION = 365 * 24 * 3600 * 1000L;

    @Inject
    private Logger logger;

    @Inject
    private Container container;

    /**
     * Used to determine the Content Type of the requested resource files.
     */
    private Tika tika = new Tika();

    @Override
    public void handle(ResourceReference resourceReference, ResourceReferenceHandlerChain chain)
        throws ResourceReferenceHandlerException
    {
        @SuppressWarnings("unchecked")
        R typedResourceReference = (R) resourceReference;

        if (!isResourceAccessible(typedResourceReference)) {
            sendError(HttpStatus.SC_FORBIDDEN, "You are not allowed to view [%s].",
                getResourceName(typedResourceReference));
        } else if (!shouldBrowserUseCachedContent(typedResourceReference)) {
            // If we get here then either the resource is not cached by the browser or the resource is dynamic.
            InputStream resourceStream = getResourceStream(typedResourceReference);
            if (resourceStream != null) {
                try {
                    serveResource(typedResourceReference, filterResource(typedResourceReference, resourceStream));
                } catch (ResourceReferenceHandlerException e) {
                    this.logger.error(e.getMessage(), e);
                    sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                }
            } else {
                sendError(HttpStatus.SC_NOT_FOUND, "Resource not found [%s].", getResourceName(typedResourceReference));
            }
        }

        // Be a good citizen, continue the chain, in case some lower-priority handler has something to do for this
        // resource reference.
        chain.handleNext(resourceReference);
    }

    /**
     * @param resourceReference the reference of the requested resource
     * @return {@code true} if the specified resource is accessible, {@code false} otherwise
     */
    protected boolean isResourceAccessible(R resourceReference)
    {
        return true;
    }

    /**
     * @param resourceReference the reference of the requested resource
     * @return {@code true} if the requested resource is static and is cached by the browser, {@code false} if the
     *         browser should discard the cached version and use the new version from this response
     */
    private boolean shouldBrowserUseCachedContent(R resourceReference)
    {
        // If the request contains an "If-Modified-Since" header and the requested resource has not been modified then
        // return a 304 Not Modified to tell the browser to use its cached version.
        Request request = this.container.getRequest();
        if (request instanceof ServletRequest
            && ((ServletRequest) request).getHttpServletRequest().getHeader("If-Modified-Since") != null
            && isResourceCacheable(resourceReference)) {
            // The user probably used F5 to reload the page and the browser checks if there are changes.
            Response response = this.container.getResponse();
            if (response instanceof ServletResponse) {
                // Return the 304 Not Modified.
                ((ServletResponse) response).getHttpServletResponse().setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                return true;
            }
        }
        return false;
    }

    /**
     * @param resourceReference a resource reference
     * @return {@code true} if the specified resource can be cached, {@code false} otherwise
     */
    protected boolean isResourceCacheable(R resourceReference)
    {
        return true;
    }

    /**
     * @param resourceReference the reference of the requested resource
     * @return the stream that can be used to read the resource
     */
    protected abstract InputStream getResourceStream(R resourceReference);

    /**
     * @param resourceReference the reference of the requested resource
     * @return the name of the specified resource
     */
    protected abstract String getResourceName(R resourceReference);

    /**
     * Sends back the specified resource.
     *
     * @param resourceReference the reference of the requested resource
     * @param rawResourceStream the resource stream used to read the resource
     * @throws ResourceReferenceHandlerException if it fails to read the resource
     */
    private void serveResource(R resourceReference, InputStream rawResourceStream)
        throws ResourceReferenceHandlerException
    {
        InputStream resourceStream = rawResourceStream;
        String resourceName = getResourceName(resourceReference);

        // Make sure the resource stream supports mark & reset which is needed in order be able to detect the
        // content type without affecting the stream (Tika may need to read a few bytes from the start of the
        // stream, in which case it will mark & reset the stream).
        if (!resourceStream.markSupported()) {
            resourceStream = new BufferedInputStream(resourceStream);
        }

        try {
            Response response = this.container.getResponse();
            setResponseHeaders(response, resourceReference);
            response.setContentType(this.tika.detect(resourceStream, resourceName));
            IOUtils.copy(resourceStream, response.getOutputStream());
        } catch (Exception e) {
            throw new ResourceReferenceHandlerException(String.format("Failed to read resource [%s]", resourceName), e);
        } finally {
            IOUtils.closeQuietly(resourceStream);
        }
    }

    /**
     * Filter the resource before sending it to the client.
     * 
     * @param resourceReference the resource to filter
     * @param resourceStream the resource content
     * @return the filtered resource content
     */
    protected InputStream filterResource(R resourceReference, InputStream resourceStream)
        throws ResourceReferenceHandlerException
    {
        return resourceStream;
    }

    /**
     * Sets the response headers needed to cache the resource permanently, if the resource can be cached.
     * 
     * @param response the response
     * @param resourceReference the resource that is being served
     */
    private void setResponseHeaders(Response response, R resourceReference)
    {
        // Cache the resource if possible.
        if (response instanceof ServletResponse && isResourceCacheable(resourceReference)) {
            HttpServletResponse httpResponse = ((ServletResponse) response).getHttpServletResponse();
            httpResponse.setHeader(HttpHeaders.CACHE_CONTROL, "public");
            httpResponse.setDateHeader(HttpHeaders.EXPIRES, new Date().getTime() + CACHE_DURATION);
            // Even if the resource is cached permanently, most browsers are still sending a request if the user reloads
            // the page using F5. We send back the "Last-Modified" header in the response so that the browser will send
            // us an "If-Modified-Since" request for any subsequent call for this static resource. When this happens we
            // return a 304 to tell the browser to use its cached version.
            httpResponse.setDateHeader(HttpHeaders.LAST_MODIFIED, new Date().getTime());
        }
    }

    /**
     * Sends back the specified status code with the given message in order for the browser to know the resource
     * couldn't be served. This is especially important as we don't want to cache an empty response.
     * 
     * @param statusCode the response status code to send
     * @param message the error message
     * @param parameters the message parameters
     * @throws ResourceReferenceHandlerException if setting the response status code fails
     */
    private void sendError(int statusCode, String message, Object... parameters)
        throws ResourceReferenceHandlerException
    {
        Response response = this.container.getResponse();
        if (response instanceof ServletResponse) {
            HttpServletResponse httpResponse = ((ServletResponse) response).getHttpServletResponse();
            try {
                httpResponse.sendError(statusCode, String.format(message, parameters));
            } catch (IOException e) {
                throw new ResourceReferenceHandlerException(
                    String.format("Failed to return status code [%s].", statusCode), e);
            }
        }
    }
}
