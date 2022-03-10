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

import org.apache.tika.mime.MediaType;
import org.xwiki.classloader.ClassLoaderManager;
import org.xwiki.component.annotation.Component;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.servlet.AbstractServletResourceReferenceHandler;
import org.xwiki.tika.internal.TikaUtils;
import org.xwiki.webjars.internal.filter.WebJarsResourceFilter;

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

    private static final String LESS_FILE_EXTENSION = ".less";

    @Inject
    private ClassLoaderManager classLoaderManager;

    @Inject
    @Named("less")
    private WebJarsResourceFilter lessFilter;

    @Inject
    @Named("velocity")
    private WebJarsResourceFilter velocityFilter;

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
    protected InputStream filterResource(WebJarsResourceReference resourceReference, InputStream resourceStream)
        throws ResourceReferenceHandlerException
    {
        InputStream stream;
        if (!isResourceCacheable(resourceReference)) {
            String resourceName = getResourceName(resourceReference);
            if (resourceName.endsWith(LESS_FILE_EXTENSION)) {
                stream = this.lessFilter.filter(resourceStream, resourceName);
            } else {
                stream = this.velocityFilter.filter(resourceStream, resourceName);
            }
        } else {
            stream = super.filterResource(resourceReference, resourceStream);
        }
        return stream;
    }

    /**
     * @return the Class Loader from which to look for WebJars resources
     */
    protected ClassLoader getClassLoader(String namespace)
    {
        return this.classLoaderManager.getURLClassLoader(namespace, true);
    }

    @Override
    protected String getContentType(InputStream resourceStream,
        WebJarsResourceReference resourceReference) throws IOException
    {
        String mimeType;
        // When the resource is not cacheable and the request resource is a less file, we compile it to css.
        // In this case, the content type must be explicitly set to text/css instead of the default text/x-less
        // that would otherwise be computed from the resource filename.
        if (!isResourceCacheable(resourceReference) && getResourceName(resourceReference)
            .endsWith(LESS_FILE_EXTENSION))
        {
            mimeType = "text/css";
        } else {
            // Tika is doing some content analysis even for text files and will tend to give priority to that over the
            // file extension. But content based detection is much less accurate than file name based detection for most
            // cases of text files.
            mimeType = TikaUtils.detect(getResourceName(resourceReference));

            // If file name did not help try the content
            if (mimeType.equals(MediaType.OCTET_STREAM.toString())) {
                mimeType = TikaUtils.detect(resourceStream);
            }
        }
        return mimeType;
    }
}
