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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.resource.AbstractResourceReferenceHandler;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.entity.EntityResourceAction;

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
     * Prefix for locating JS resources in the classloader.
     */
    private static final String WEBJARS_RESOURCE_PREFIX = "META-INF/resources/webjars/";

    @Inject
    private Container container;

    @Override
    public List<EntityResourceAction> getSupportedResourceReferences()
    {
        return Arrays.asList(ACTION);
    }

    @Override
    public void handle(ResourceReference reference, ResourceReferenceHandlerChain chain)
        throws ResourceReferenceHandlerException
    {
        String resourceName = reference.getParameterValue("value");
        String resourcePath = String.format("%s%s", WEBJARS_RESOURCE_PREFIX, resourceName);

        InputStream resourceStream = getClassLoader().getResourceAsStream(resourcePath);

        if (resourceStream != null) {
            try {
                IOUtils.copy(resourceStream, this.container.getResponse().getOutputStream());
            } catch (IOException e) {
                throw new ResourceReferenceHandlerException(
                    String.format("Failed to read resource [%s]", resourceName), e);
            } finally {
                IOUtils.closeQuietly(resourceStream);
            }
        }

        // Be a good citizen, continue the chain, in case some lower-priority Handler has something to do for this
        // Resource Reference.
        chain.handleNext(reference);
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
