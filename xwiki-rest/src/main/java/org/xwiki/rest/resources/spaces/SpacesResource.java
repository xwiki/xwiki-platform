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
package org.xwiki.rest.resources.spaces;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.RangeIterable;
import org.xwiki.rest.Utils;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.jaxb.Spaces;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 */
@Path("/wikis/{wikiName}/spaces")
public class SpacesResource extends XWikiResource
{
    public SpacesResource(@Context UriInfo uriInfo)
    {
        super(uriInfo);
    }

    @GET
    public Spaces getSpaces(@PathParam("wikiName") String wikiName,
        @QueryParam("start") @DefaultValue("0") Integer start, @QueryParam("number") @DefaultValue("-1") Integer number)
        throws XWikiException
    {
        String database = xwikiContext.getDatabase();

        Spaces spaces = objectFactory.createSpaces();

        /* This try is just needed for executing the finally clause. */
        try {
            xwikiContext.setDatabase(wikiName);

            List<String> spaceNames = xwikiApi.getSpaces();
            Collections.sort(spaceNames);

            RangeIterable<String> ri = new RangeIterable<String>(spaceNames, start, number);

            for (String spaceName : ri) {
                List<String> docNames = xwikiApi.getSpaceDocsName(spaceName);

                String homeId = Utils.getPageId(wikiName, spaceName, "WebHome");
                Document home = null;
                
                if (xwikiApi.exists(homeId)) {
                    home = xwikiApi.getDocument(homeId);
                }

                spaces.getSpaces().add(DomainObjectFactory.createSpace(objectFactory, uriInfo.getBaseUri(), wikiName, spaceName, home,
                    docNames.size()));
            }
        } finally {
            xwikiContext.setDatabase(database);
        }

        return spaces;
    }
}
