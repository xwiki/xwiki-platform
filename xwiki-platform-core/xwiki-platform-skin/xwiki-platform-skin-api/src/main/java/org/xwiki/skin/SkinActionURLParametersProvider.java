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
package org.xwiki.skin;

import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Provides query parameters to add to the URL of the skin resources that are evaluated server-side by the skin action
 * (e.g. JavaScript or CSS files containing Velocity code).
 * <p>
 * The browser caches these resources based on their URL, which by default only changes when the resource file is
 * modified. When the result of the evaluation depends on some configuration (e.g. the current icon theme), the
 * resource needs a different URL for each value of this configuration, otherwise the browser keeps using the result
 * evaluated with the previous configuration. Implementations return the parameters identifying the configuration they
 * represent, and the skin adds them to the URL of each such resource.
 * <p>
 * The parameters are computed for every evaluated resource URL generated in a request, so implementations should be
 * fast.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Role
@Unstable
public interface SkinActionURLParametersProvider
{
    /**
     * @return the query parameters to add to the URL of the skin resources evaluated by the skin action, or an empty
     *         map if there's nothing to add (e.g. when the configuration has its default value)
     */
    Map<String, Object> getParameters();
}
