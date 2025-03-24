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

import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.resource.CreateResourceTypeException;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.ResourceTypeResolver;

/**
 * A basic Resource Type Resolver which creates a Resource Type based on the passed String representation without
 * performing any transformation to the passed String.
 *
 * @version $Id$
 * @since 7.1M1
 */
@Component
@Named("generic")
@Singleton
public class GenericStringResourceTypeResolver implements ResourceTypeResolver<String>
{
    @Override
    public ResourceType resolve(String type, Map<String, Object> parameters)
        throws CreateResourceTypeException
    {
        if (type == null) {
            throw new CreateResourceTypeException(String.format("Invalid scheme URL type. The URL is "
                + "missing a path segment and should be of the format [/<type>/something/...]"));
        }

        return new ResourceType(type);
    }
}
