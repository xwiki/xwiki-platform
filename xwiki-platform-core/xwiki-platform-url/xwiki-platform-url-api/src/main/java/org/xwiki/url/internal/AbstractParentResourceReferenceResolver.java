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
package org.xwiki.url.internal;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.xwiki.resource.CreateResourceReferenceException;
import org.xwiki.resource.ResourceReferenceHandler;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.url.ExtendedURL;

/**
 * Base class for a reference resolver of a {@link ResourceReferenceHandler} leading to other
 * sub-{@link ResourceReferenceHandler}s.
 * 
 * @version $Id$
 * @since 10.2
 */
public abstract class AbstractParentResourceReferenceResolver extends AbstractResourceReferenceResolver
{
    @Override
    public ParentResourceReference resolve(ExtendedURL extendedURL, ResourceType resourceType,
        Map<String, Object> parameters) throws CreateResourceReferenceException, UnsupportedResourceReferenceException
    {
        String path = "";
        String child = "";
        List<String> pathSegments = extendedURL.getSegments();
        if (!pathSegments.isEmpty()) {
            StringBuilder pathBuilder = new StringBuilder();
            try {
                for (String pathSegment : extendedURL.getSegments()) {
                    if (pathBuilder.length() > 0) {
                        pathBuilder.append('/');
                    }
                    pathBuilder.append(URLEncoder.encode(pathSegment, "UTF8"));
                }
            } catch (UnsupportedEncodingException e) {
                // Should never happen
            }
            path = pathBuilder.toString();
            child = pathSegments.get(0);

            if (pathSegments.size() > 1) {
                pathSegments = pathSegments.subList(1, pathSegments.size());
            } else {
                pathSegments = Collections.emptyList();
            }
        }

        ParentResourceReference reference = new ParentResourceReference(resourceType, path, child, pathSegments);

        copyParameters(extendedURL, reference);

        return reference;
    }
}
