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
package org.xwiki.rest.resources.wikis;

import java.io.InputStream;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.Wiki;

/**
 * Resource for interacting with a specific wiki.
 *
 * @version $Id$
 */
@Path("/wikis/{wikiName}")
public interface WikiResource
{
    /**
     * Get information about a wiki.
     *
     * @param wikiName the wiki name.
     * @return information about the wiki.
     * @throws XWikiRestException if something goes wrong.
     */
    @GET Wiki get(
            @PathParam("wikiName") String wikiName
    ) throws XWikiRestException;

    /**
     * Import a XAR into a given wiki.
     *
     * @param wikiName the wiki name.
     * @param backup whether this is a backup pack.
     * @param history how to manage page version when importing pages.
     * @param is the input stream containing XAR data (POSTed by the client)
     * @return the information about the wiki.
     * @throws XWikiRestException if there was an error during import.
     */
    @POST Wiki importXAR(
            @PathParam("wikiName") String wikiName,
            @QueryParam("backup") @DefaultValue("false") Boolean backup,
            @QueryParam("history") @DefaultValue("add") String history,
            InputStream is
    ) throws XWikiRestException;
}
