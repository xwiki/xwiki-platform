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

import java.util.List;
import java.util.Map;

import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.url.ExtendedURL;

/**
 * Helper for implementers of {@link ResourceReferenceResolver}.
 *
 * @version $Id$
 * @since 7.1M1
 */
public abstract class AbstractResourceReferenceResolver implements ResourceReferenceResolver<ExtendedURL>
{
    /**
     * Copies query string parameters from the passed {@link org.xwiki.url.ExtendedURL} to the passed
     * {@link ResourceReference}.
     *
     * @param source the source URL from where to get the query string parameters
     * @param target the {@link ResourceReference} into which to copy the query string parameters
     */
    protected void copyParameters(ExtendedURL source, ResourceReference target)
    {
        for (Map.Entry<String, List<String>> entry : source.getParameters().entrySet()) {
            if (entry.getValue().isEmpty()) {
                target.addParameter(entry.getKey(), null);
            } else {
                target.addParameter(entry.getKey(), entry.getValue());
            }
        }
    }
}
