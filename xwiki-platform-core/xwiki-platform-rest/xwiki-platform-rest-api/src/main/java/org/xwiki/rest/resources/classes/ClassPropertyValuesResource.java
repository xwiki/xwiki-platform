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
