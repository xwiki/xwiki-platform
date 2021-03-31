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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.xwiki.classloader.ClassLoaderManager;
import org.xwiki.component.annotation.Component;
import org.xwiki.lesscss.compiler.LESSCompiler;
import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.lesscss.resources.LESSResourceReference;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.servlet.AbstractServletResourceReferenceHandler;
import org.xwiki.velocity.VelocityManager;

import static java.nio.charset.StandardCharsets.UTF_8;

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
     * Used to evaluate the Velocity code from the WebJar resources.
     */
    @Inject
    private VelocityManager velocityManager;

    @Inject
    private ClassLoaderManager classLoaderManager;

    @Inject
    private LESSCompiler lessCompiler;

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
        return !Boolean.parseBoolean(resourceReference.getParameterValue("evaluate"));
    }

    @Override
    protected InputStream filterResource(WebJarsResourceReference resourceReference, InputStream resourceStream,
        HttpServletResponse response)
        throws ResourceReferenceHandlerException
    {
        String resourceName = getResourceName(resourceReference);
        InputStream stream;
        if (!isResourceCacheable(resourceReference)) {
            if (resourceName.endsWith(".less")) {
                stream = lessResourceHandler(resourceReference, resourceStream, response);
            } else {
                stream = defaultResourceHandler(resourceStream, resourceName);
            }
        } else {
            stream = super.filterResource(resourceReference, resourceStream, response);
        }
        return stream;
    }

    private InputStream lessResourceHandler(WebJarsResourceReference resourceReference,
        InputStream resourceStream, HttpServletResponse response)
        throws ResourceReferenceHandlerException
    {
        if (response != null) {
            response.setHeader(HttpHeaders.CONTENT_TYPE, "text/css");
        }
        
        LESSResourceReference lessResourceReference = new LESSResourceReference()
        {
            @Override
            public String getContent(String skin) throws LESSCompilerException
            {
                // Load the content of the resource in a string 
                try {
                    return IOUtils.toString(resourceStream, UTF_8);
                } catch (IOException e) {
                    throw new LESSCompilerException(
                        String.format("Failed to load the webjer resource [%s]", this.serialize()), e);
                }
            }

            @Override
            public String serialize()
            {
                // Return a unique identifier for the webjar resource.
                return resourceReference.getResourceName();
            }
        };
        try {
            String compile = this.lessCompiler.compile(lessResourceReference, true, false, false);
            return IOUtils.toInputStream(compile, UTF_8);
        } catch (LESSCompilerException e) {
            throw new ResourceReferenceHandlerException("Error when compiling the resource", e);
        }
    }

    private ByteArrayInputStream defaultResourceHandler(InputStream resourceStream, String resourceName)
        throws ResourceReferenceHandlerException
    {
        try {
            // Evaluates the given resource using Velocity.
            StringWriter writer = new StringWriter();
            this.velocityManager.getVelocityEngine().evaluate(this.velocityManager.getVelocityContext(), writer,
                resourceName, new InputStreamReader(resourceStream, UTF_8));
            return new ByteArrayInputStream(writer.toString().getBytes(UTF_8));
        } catch (Exception e) {
            throw new ResourceReferenceHandlerException(
                String.format("Failed to evaluate the Velocity code from WebJar resource [%s]", resourceName),
                e);
        }
    }

    /**
     * @return the Class Loader from which to look for WebJars resources
     */
    protected ClassLoader getClassLoader(String namespace)
    {
        return this.classLoaderManager.getURLClassLoader(namespace, true);
    }
}
