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
package org.xwiki.url.internal.standard;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.resource.CreateResourceTypeException;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.ResourceTypeResolver;
import org.xwiki.resource.entity.EntityResourceReference;

/**
 * Converts the passed type which has been extracted from the URL as a String into a {@link ResourceType} proper.
 * This allows Resource Types to be common to all URL Schemes and as a consequence it allows Resource Reference
 * Resolvers to be registered for all URL Schemes (for those who wish to do this, for example the WebJars one is doing
 * this), since the Resolver is looked up based on the Resource Type id in general.
 *
 * @version $Id$
 * @since 7.1M1
 */
@Component
@Named("standard")
@Singleton
public class StandardStringResourceTypeResolver implements ResourceTypeResolver<String>
{
    @Inject
    private StandardURLConfiguration configuration;

    @Override
    public ResourceType resolve(String type, Map<String, Object> parameters)
        throws CreateResourceTypeException
    {
        if (type == null) {
            throw new CreateResourceTypeException(String.format("Invalid standard scheme URL type. The URL is "
                + "missing a path segment and should be of the format [/<type>/something/...]"));
        }

        if (type.equals(this.configuration.getEntityPathPrefix())) {
            return EntityResourceReference.TYPE;
        } else {
            return new ResourceType(type);
        }
    }
}
