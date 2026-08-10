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
package org.xwiki.rest.resources.classes;

import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.PropertyValues;

/**
 * Returns the class property values.
 * 
 * @version $Id$
 * @since 9.8RC1
 */
@Path("/wikis/{wikiName}/classes/{className}/properties/{propertyName}/values")
public interface ClassPropertyValuesResource
{
    /**
     * Returns the allowed values for a given class property (for example the entries of a "Database List" or "List of
     * Users" property).
     *
     * @param wikiName the identifier of the wiki containing the class, for example {@code xwiki} for the main wiki
     * @param className the reference of the XClass the property belongs to, for example {@code XWiki.XWikiUsers}
     * @param propertyName the name of the property whose values are returned, for example {@code email}
     * @param limit the maximum number of values to return; defaults to {@code 100}; only applied when
     *  {@code exactMatch} is {@code false}; when {@code null} the wiki's configured REST query limit is used, and a
     *  value that is negative or larger than that configured limit is rejected with a {@code 400} response
     * @param filterParameters the {@code fp} query parameters used to filter the returned values (for example the text
     *  typed by the user in a suggest widget); when empty and {@code exactMatch} is {@code false}, the most relevant
     *  values are returned unfiltered
     * @param isExactMatch when {@code true}, returns only the value exactly matching each of the given filter
     *  parameters (so an empty {@code fp} yields no value and {@code limit} is ignored); when {@code false} (the
     *  default), returns the values matched by the property's value provider, capped at {@code limit}
     * @return the property values matching the request, together with a link to the property
     * @throws XWikiRestException if the property values cannot be retrieved; a {@code 404} response is returned when
     *  the property does not exist, and a {@code 401} response when the current user is not allowed to view it
     */
    @GET
    PropertyValues getClassPropertyValues(
        @PathParam("wikiName") String wikiName,
        @PathParam("className") String className,
        @PathParam("propertyName") String propertyName,
        @QueryParam("limit") @DefaultValue("100") Integer limit,
        @QueryParam("fp") List<String> filterParameters,
        @QueryParam("exactMatch") @DefaultValue("false") Boolean isExactMatch
    ) throws XWikiRestException;
}
