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
package org.xwiki.webjars.internal;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.tika.Tika;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.container.Response;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.resource.AbstractResourceReferenceHandler;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.velocity.VelocityManager;

/**
 * Handles {@code webjars} Resource References.
 * <p>
 * At the moment we're mapping calls to the "webjars" URL as an EntityResourceReference.
 * In the future it would be cleaner to register some new URL factory instead of reusing the format for Entity
 * Resources and have some URL of the type {@code http://server/context/webjars?resource=(resourceName)}.
 * Since we don't have this now and we're using the Entity Resource Reference URL format we're using the following URL
 * format:
 * <code>
 *   http://server/context/bin/webjars/resource/path?value=(resource name)
 * </code>
 * (for example: http://localhost:8080/xwiki/bin/webjars/resource/path?value=angularjs/1.1.5/angular.js)
 * So this means that the resource name will be parsed as a query string "value" parameter (with a fixed space of
 * "resource" and a fixed page name of "path").
 * </p>
 *
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Named("webjars")
@Singleton
public class WebJarsResourceReferenceHandler extends AbstractResourceReferenceHandler<EntityResourceAction>
{
    /**
     * The WebJars Action.
     */
    public static final EntityResourceAction ACTION = new EntityResourceAction("webjars");

    /**
     * Prefix for locating resource files (JavaScript, CSS) in the classloader.
     */
    private static final String WEBJARS_RESOURCE_PREFIX = "META-INF/resources/webjars/";

    /**
     * The encoding used when evaluating WebJar (text) resources.
     */
    private static final String UTF8 = "UTF-8";

    @Inject
    private Container container;

    /**
     * Used to evaluate the Velocity code from the WebJar resources.
     */
    @Inject
    private VelocityManager velocityManager;

    /**
     * Used to determine the Content Type of the requested resource files.
     */
    private Tika tika = new Tika();

    @Override
    public List<EntityResourceAction> getSupportedResourceReferences()
    {
        return Arrays.asList(ACTION);
    }

    @Override
    public void handle(ResourceReference reference, ResourceReferenceHandlerChain chain)
        throws ResourceReferenceHandlerException
    {
        if (!shouldBrowserUseCachedContent(reference)) {
            String resourceName = reference.getParameterValue("value");
            String resourcePath = String.format("%s%s", WEBJARS_RESOURCE_PREFIX, resourceName);

            InputStream resourceStream = getClassLoader().getResourceAsStream(resourcePath);

            if (resourceStream != null) {
                if (shouldEvaluateResource(reference)) {
                    resourceStream = evaluate(resourceName, resourceStream);
                }
                
                // Make sure the resource stream supports mark & reset which is needed in order be able to detect the
                // content type without affecting the stream (Tika may need to read a few bytes from the start of the
                // stream, in which case it will mark & reset the stream).
                if (!resourceStream.markSupported()) {
                    resourceStream = new BufferedInputStream(resourceStream);
                }

                try {
                    Response response = this.container.getResponse();
                    setResponseHeaders(response, reference);
                    response.setContentType(tika.detect(resourceStream, resourceName));
                    IOUtils.copy(resourceStream, this.container.getResponse().getOutputStream());
                } catch (Exception e) {
                    throw new ResourceReferenceHandlerException(
                        String.format("Failed to read resource [%s]", resourceName), e);
                } finally {
                    IOUtils.closeQuietly(resourceStream);
                }
            }
        }

        // Be a good citizen, continue the chain, in case some lower-priority Handler has something to do for this
        // Resource Reference.
        chain.handleNext(reference);
    }

    private void setResponseHeaders(Response response, ResourceReference reference)
    {
        // If the resource contains Velocity code then this code must be evaluated on each request and so the resource
        // must not be cached. Otherwise, if the resource is static we need to send back the "Last-Modified" header in
        // the response so that the browser will send us an "If-Modified-Since" request for any subsequent call for this
        // static resource. When this happens we return a 304 to tell the browser to use its cached version.
        if (response instanceof ServletResponse && !shouldEvaluateResource(reference)) {
            HttpServletResponse httpResponse = ((ServletResponse) response).getHttpServletResponse();
            httpResponse.setDateHeader("Last-Modified", new Date().getTime() / 1000 * 1000);
        }
    }

    private boolean shouldBrowserUseCachedContent(ResourceReference reference)
    {
        // If the request contains a "If-Modified-Since" and the referenced resource is not supposed to be evaluated
        // (i.e. no Velocity code) then return a 304 so to tell the browser to use its cached version.
        Request request = this.container.getRequest();
        if (request instanceof ServletRequest && !shouldEvaluateResource(reference)) {
            HttpServletRequest httpRequest = ((ServletRequest) request).getHttpServletRequest();
            if (httpRequest.getHeader("If-Modified-Since") != null) {
                // Return the 304
                Response response = this.container.getResponse();
                if (response instanceof ServletResponse) {
                    HttpServletResponse httpResponse = ((ServletResponse) response).getHttpServletResponse();
                    httpResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * @param reference a resource reference
     * @return {@code true} if the resource should be evaluated (e.g. if the resource has Velocity code), {@code false}
     *         otherwise
     */
    private boolean shouldEvaluateResource(ResourceReference reference)
    {
        return Boolean.valueOf(reference.getParameterValue("evaluate"));
    }

    private InputStream evaluate(String resourceName, InputStream resourceStream)
        throws ResourceReferenceHandlerException
    {
        try {
            StringWriter writer = new StringWriter();
            this.velocityManager.getVelocityEngine().evaluate(this.velocityManager.getVelocityContext(), writer,
                resourceName, new InputStreamReader(resourceStream, UTF8));
            return new ByteArrayInputStream(writer.toString().getBytes(UTF8));
        } catch (Exception e) {
            throw new ResourceReferenceHandlerException("Faild to evaluate the Velocity code from WebJar resource ["
                + resourceName + "]", e);
        }
    }

    /**
     * @return the Class Loader from which to look for WebJars resources
     */
    protected ClassLoader getClassLoader()
    {
        // Load the resource from the context class loader in order to support webjars located in XWiki Extensions
        // loaded by the Extension Manager.
        return Thread.currentThread().getContextClassLoader();
    }
}
