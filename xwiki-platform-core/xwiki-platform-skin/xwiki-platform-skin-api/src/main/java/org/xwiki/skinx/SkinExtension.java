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
package org.xwiki.skinx;

import java.util.Map;

import org.xwiki.component.annotation.Role;

/**
 * Allows a component to use a skin file (js or css), in the current page being rendered.
 * 
 * @version $Id$
 * @since 3.0M1
 */
@Role
public interface SkinExtension
{
    /**
     * Mark a resource as used in the current result. A resource is registered only once per request, further calls will
     * not result in additional links, even if it is pulled with different parameters.
     * 
     * @param resource The name of the resource to pull.
     */
    void use(String resource);

    /**
     * Mark a skin extension document as used in the current result, together with some parameters. How the parameters
     * are used, depends on the type of resource being pulled. For example, JS and CSS extensions use the parameters in
     * the resulting URL, while Link extensions use the parameters as attributes of the link tag. A resource is
     * registered only once per request, further calls will not result in additional links, even if it is pulled with
     * different parameters. If more than one calls per request are made, the parameters used are the ones from the last
     * call (or none, if the last call did not specify any parameters). <br />
     * TODO: document here the parameters that can be used and their meaning.
     * 
     * @param resource The name of the resource to pull.
     * @param parameters The parameters for this resource.
     */
    void use(String resource, Map<String, Object> parameters);
}
