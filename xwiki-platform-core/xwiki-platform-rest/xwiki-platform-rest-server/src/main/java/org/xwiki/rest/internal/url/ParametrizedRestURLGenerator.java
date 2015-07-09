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
package org.xwiki.rest.internal.url;

import java.net.URL;

import org.xwiki.component.annotation.Role;
import org.xwiki.rest.XWikiRestException;

/**
 * A parametrized REST URL generator. Enable to create one component per supported resource.
 *
 * @param <T> the type of the resource for which the URL are created for. Must inherit from 
 * {@link org.xwiki.model.reference.EntityReference}. 
 * @version $Id$
 * @since 7.2M1
 */
@Role
public interface ParametrizedRestURLGenerator<T>
{
    /**
     * @param reference an entity reference
     * @return the REST URL to access the given entity
     * @throws XWikiRestException if problems occur
     */
    URL getURL(T reference) throws XWikiRestException;
}
