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

package org.xwiki.extension.repository.xwiki.internal;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.repository.xwiki.model.jaxb.Extension;
import org.xwiki.query.QueryException;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 * @since 3.1M2
 */
@Component("org.xwiki.extension.repository.xwiki.internal.ExtensionFileRESTResource")
@Path("/extension/{extensionId}/{extensionVersion}/file")
public class ExtensionFileRESTResource extends AbstractExtensionRESTResource
{
    @GET
    public Response downloadExtension(@PathParam("extensionId") String extensionId,
        @PathParam("extensionVersion") String extensionVersion) throws XWikiException, QueryException
    {
        Document extensionDocument = getExtensionDocument(extensionId);

        if (extensionDocument.isNew()) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        Extension extension = createExtension(extensionDocument, extensionVersion);

        com.xpn.xwiki.api.Attachment xwikiAttachment =
            extensionDocument.getAttachment(extensionId + "-" + extensionVersion + "." + extension.getType());
        if (xwikiAttachment == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        ResponseBuilder response = Response.ok();

        response.type(xwikiAttachment.getMimeType());
        response.entity(xwikiAttachment.getContent());
        response.header("Content-Disposition", "attachment; filename=\"" + xwikiAttachment.getFilename() + "\"");

        return response.build();
    }
}
