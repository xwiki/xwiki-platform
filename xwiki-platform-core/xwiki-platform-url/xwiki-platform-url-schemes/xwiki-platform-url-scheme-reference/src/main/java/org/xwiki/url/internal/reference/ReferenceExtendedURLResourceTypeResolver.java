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
package org.xwiki.url.internal.reference;

import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.resource.CreateResourceTypeException;
import org.xwiki.resource.ResourceType;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.internal.AbstractExtendedURLResourceTypeResolver;

/**
 * Extracts the {@link ResourceType} from a passed {@link ExtendedURL}, using the {@code reference} URL scheme format.
 * In that format the Resource Type is the path segment in the URL just after the Context Path one (e.g.
 * {@code entity} in {@code http://localhost:8080/entity/view/page/wiki:space.page}.
 *
 * @version $Id$
 * @since 7.1M1
 */
@Component
@Named("reference")
@Singleton
public class ReferenceExtendedURLResourceTypeResolver extends AbstractExtendedURLResourceTypeResolver
{
    @Override
    public ResourceType resolve(ExtendedURL extendedURL, Map<String, Object> parameters)
        throws CreateResourceTypeException
    {
        return resolve("reference", extendedURL, parameters);
    }
}
