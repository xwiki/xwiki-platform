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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.resource.CreateResourceReferenceException;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.internal.AbstractResourceReferenceResolver;

/**
 * Transform Async renderer URL into a typed Resource Reference. The URL format handled is
 * {@code http://server/context/asyncrenderer/}.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
@Component
@Named(AsyncRendererResourceReferenceHandler.HINT)
@Singleton
public class AsyncRendererResourceReferenceResolver extends AbstractResourceReferenceResolver
{
    @Override
    public ResourceReference resolve(ExtendedURL representation, ResourceType resourceType,
        Map<String, Object> parameters) throws CreateResourceReferenceException, UnsupportedResourceReferenceException
    {
        List<String> pathSegments = representation.getSegments();
        if (!pathSegments.isEmpty()) {
            StringBuilder pathBuilder = new StringBuilder();
            try {
                for (String pathSegment : representation.getSegments()) {
                    if (pathBuilder.length() > 0) {
                        pathBuilder.append('/');
                    }
                    pathBuilder.append(URLEncoder.encode(pathSegment, "UTF8"));
                }
            } catch (UnsupportedEncodingException e) {
                // Should never happen
            }
        }

        return new AsyncRendererResourceReference(resourceType, pathSegments);
    }
}
