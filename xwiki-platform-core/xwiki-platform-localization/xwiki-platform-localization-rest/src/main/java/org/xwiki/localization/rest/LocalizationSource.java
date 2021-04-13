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
package org.xwiki.localization.rest;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.xwiki.stability.Unstable;

/**
 * Exposes the wiki translations through REST.
 *
 * @version $Id$
 * @since 13.3RC1
 */
@Path("/wikis/{wikiName}/localization/translations")
@Unstable
public interface LocalizationSource
{
    /**
     * Returns the raw translation values of the request keys. In other words, the translations values without the
     * parameters resolved.
     * <p>
     * The payload of a successful response is an object where the keys are the requested translation keys (with the
     * prefix concatenated), and the values are the resolved translation raw values. When the value of a key is not
     * found, it is returned with a @{code null} value.
     *
     * @param wikiName the name of the wiki holding the translation keys
     * @param locale the locale of the translation values
     * @param prefix a common prefix, concatenated to each translation key before resolving their values
     * @param keys the translation keys to resolve
     * @return the response, an {@link Response.Status#INTERNAL_SERVER_ERROR} in case of component resolution issue, or
     *     a {@link Response.Status#OK} response with an object holding the translation values otherwise
     */
    @GET
    Response translations(@PathParam("wikiName") String wikiName, @QueryParam("locale") String locale,
        @QueryParam("prefix") String prefix, @QueryParam("key") List<String> keys);
}
