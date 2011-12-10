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
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.repository.xwiki.Resources;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionVersions;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;

/**
 * @version $Id$
 * @since 3.2M3
 */
@Component("org.xwiki.extension.repository.xwiki.internal.resources.ExtensionVersionsRESTResource")
@Path(Resources.EXTENSION_VERSIONS)
public class ExtensionVersionsRESTResource extends AbstractExtensionRESTResource
{
    @GET
    public ExtensionVersions getExtensionVersions(@PathParam("extensionId") String extensionId,
        @QueryParam(Resources.QPARAM_LIST_START) @DefaultValue("-1") int offset,
        @QueryParam(Resources.QPARAM_LIST_NUMBER) @DefaultValue("-1") int number) throws QueryException
    {
        Query query = createExtensionsSummariesQuery(null, "extension.id = :extensionId", offset, number, true);

        query.bindValue("extensionId", extensionId);

        ExtensionVersions extensions = this.objectFactory.createExtensionVersions();

        getExtensionSummaries(extensions.getExtensionVersionSummaries(), query);

        return extensions;
    }
}
