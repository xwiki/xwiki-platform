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
package org.xwiki.webjars.internal.filter;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.lesscss.compiler.LESSCompiler;
import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.lesscss.resources.LESSResourceReference;
import org.xwiki.resource.ResourceReferenceHandlerException;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Proceed to the filtering of less resources. The less resources is compiled to css, in the context of the current
 * skin.
 *
 * @version $Id$
 * @since 13.3RC1
 */
@Component
@Named("less")
@Singleton
public class LessWebJarsResourceFilter implements WebJarsResourceFilter
{
    private static class WebjarLESSResourceReference implements LESSResourceReference
    {
        private final InputStream resourceStream;

        private final String resourceName;

        WebjarLESSResourceReference(InputStream resourceStream, String resourceName)
        {
            this.resourceStream = resourceStream;
            this.resourceName = resourceName;
        }

        @Override
        public String getContent(String skin) throws LESSCompilerException
        {
            // Load the content of the resource in a string. 
            try {
                return IOUtils.toString(this.resourceStream, UTF_8);
            } catch (IOException e) {
                throw new LESSCompilerException(
                    String.format("Failed to load the webjar resource [%s]", this.serialize()), e);
            }
        }

        @Override
        public String serialize()
        {
            // Return a unique identifier for the webjar resource.
            return this.resourceName;
        }
    }

    @Inject
    private LESSCompiler lessCompiler;

    @Override
    public InputStream filter(InputStream resourceStream, String resourceName)
        throws ResourceReferenceHandlerException
    {
        LESSResourceReference lessResourceReference = new WebjarLESSResourceReference(resourceStream, resourceName);
        try {
            String compile = this.lessCompiler.compile(lessResourceReference, true, false, false);
            return IOUtils.toInputStream(compile, UTF_8);
        } catch (LESSCompilerException e) {
            throw new ResourceReferenceHandlerException(
                String.format("Error when compiling the resource [%s]", resourceName), e);
        }
    }
}
