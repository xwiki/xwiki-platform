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
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.math.NumberUtils;
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

        // Decode again the path segments which have been double escaped to avoid problems with the ridiculous default
        // behavior of Tomcat regarding / and \
        List<String> id = new ArrayList<>(pathSegments.size());
        for (String pathSegment : representation.getSegments()) {
            try {
                id.add(URLDecoder.decode(pathSegment, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new CreateResourceReferenceException("Failed to decode the path segment", e);
            }
        }

        String clientId = getParameter(representation, "clientId");

        long timeout = Long.MAX_VALUE;
        String timeoutString = getParameter(representation, "timeout");
        if (timeoutString != null) {
            timeout = NumberUtils.toLong(timeoutString, timeout);
        }

        String wiki = getParameter(representation, "wiki");

        return new AsyncRendererResourceReference(resourceType, id, clientId, timeout, wiki);
    }

    private String getParameter(ExtendedURL representation, String key)
    {
        List<String> timeouts = representation.getParameters().get(key);
        if (CollectionUtils.isNotEmpty(timeouts)) {
            return timeouts.get(0);
        }

        return null;
    }
}
