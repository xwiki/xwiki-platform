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

package org.xwiki.extension.repository.xwiki.internal.resources;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.repository.xwiki.Resources;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionsSearchResult;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;

/**
 * @version $Id$
 * @since 3.2M3
 */
@Component("org.xwiki.extension.repository.xwiki.internal.resources.SearchRESTResource")
@Path(Resources.SEARCH)
public class SearchRESTResource extends AbstractExtensionRESTResource
{
    /**
     * @since 3.3M2
     */
    @GET
    public ExtensionsSearchResult search(@QueryParam(Resources.QPARAM_SEARCH_QUERY) @DefaultValue("") String pattern,
        @QueryParam(Resources.QPARAM_LIST_START) @DefaultValue("0") int offset,
        @QueryParam(Resources.QPARAM_LIST_NUMBER) @DefaultValue("-1") int number) throws QueryException
    {
        String where =
            "extension.id like :pattern or extension.name like :pattern or extension.description like :pattern";

        Query query = createExtensionsQuery(null, where, offset, number);

        query.bindValue("pattern", '%' + pattern + '%');

        ExtensionsSearchResult result = this.objectFactory.createExtensionsSearchResult();

        getExtensions(result.getExtensions(), query);

        return result;
    }
}
