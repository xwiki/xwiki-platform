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

import java.io.InputStream;

import org.xwiki.component.annotation.Role;
import org.xwiki.resource.ResourceReferenceHandlerException;

/**
 * Provides the operations to filter webjar resources. This can be used to apply transformation on the request resource,
 * such as on the fly compilation.
 *
 * @version $Id$
 * @since 13.3RC1
 */
@Role
public interface WebJarsResourceFilter
{
    /**
     * Handle the filtering of the request resource, returning a new content instead of the one initially requested.
     *
     * @param resourceStream the input stream of the webjar resource initially requested
     * @param resourceName the name of the webjar resource initially requested
     * @return an input stream of the filtered resource
     * @throws ResourceReferenceHandlerException in case of error during the filtering
     */
    InputStream filter(InputStream resourceStream, String resourceName) throws ResourceReferenceHandlerException;
}
