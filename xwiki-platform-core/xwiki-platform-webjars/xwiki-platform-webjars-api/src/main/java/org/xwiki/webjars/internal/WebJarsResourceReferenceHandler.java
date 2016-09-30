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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.classloader.ClassLoaderManager;
import org.xwiki.component.annotation.Component;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.servlet.AbstractServletResourceReferenceHandler;
import org.xwiki.velocity.VelocityManager;

/**
 * Handles {@code webjars} Resource References.
 *
 * @version $Id$
 * @since 6.1M2
 * @see WebJarsResourceReferenceResolver for the URL format handled
 */
@Component
@Named("webjars")
@Singleton
public class WebJarsResourceReferenceHandler extends AbstractServletResourceReferenceHandler<WebJarsResourceReference>
{
    /**
     * Prefix for locating resource files (JavaScript, CSS) in the classloader.
     */
    private static final String WEBJARS_RESOURCE_PREFIX = "META-INF/resources/webjars/";

    /**
     * The encoding used when evaluating WebJar (text) resources.
     */
    private static final String UTF8 = "UTF-8";

    /**
     * Used to evaluate the Velocity code from the WebJar resources.
     */
    @Inject
    private VelocityManager velocityManager;

    @Inject
    private ClassLoaderManager classLoaderManager;

    @Override
    public List<ResourceType> getSupportedResourceReferences()
    {
        return Arrays.asList(WebJarsResourceReference.TYPE);
    }

    @Override
    protected InputStream getResourceStream(WebJarsResourceReference resourceReference)
    {
        String resourcePath = String.format("%s%s", WEBJARS_RESOURCE_PREFIX, getResourceName(resourceReference));
        return getClassLoader(resourceReference.getNamespace()).getResourceAsStream(resourcePath);
    }

    @Override
    protected String getResourceName(WebJarsResourceReference resourceReference)
    {
        return resourceReference.getResourceName();
    }

    @Override
    protected boolean isResourceCacheable(WebJarsResourceReference resourceReference)
    {
        return !Boolean.valueOf(resourceReference.getParameterValue("evaluate"));
    }

    @Override
    protected InputStream filterResource(WebJarsResourceReference resourceReference, InputStream resourceStream)
        throws ResourceReferenceHandlerException
    {
        if (!isResourceCacheable(resourceReference)) {
            String resourceName = getResourceName(resourceReference);
            try {
                // Evaluates the given resource using Velocity.
                StringWriter writer = new StringWriter();
                this.velocityManager.getVelocityEngine().evaluate(this.velocityManager.getVelocityContext(), writer,
                    resourceName, new InputStreamReader(resourceStream, UTF8));
                return new ByteArrayInputStream(writer.toString().getBytes(UTF8));
            } catch (Exception e) {
                throw new ResourceReferenceHandlerException(
                    String.format("Failed to evaluate the Velocity code from WebJar resource [%s]", resourceName), e);
            }
        }
        return super.filterResource(resourceReference, resourceStream);
    }

    /**
     * @return the Class Loader from which to look for WebJars resources
     */
    protected ClassLoader getClassLoader(String namespace)
    {
        return this.classLoaderManager.getURLClassLoader(namespace, true);
    }
}
