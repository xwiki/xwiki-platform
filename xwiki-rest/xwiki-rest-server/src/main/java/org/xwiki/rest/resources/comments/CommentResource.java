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
package org.xwiki.rest.resources.comments;

import java.util.Vector;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.jaxb.Comment;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 */
@Component("org.xwiki.rest.resources.comments.CommentResource")
@Path("/wikis/{wikiName}/spaces/{spaceName}/pages/{pageName}/comments/{id}")
public class CommentResource extends XWikiResource
{
    @GET
    public Comment getComment(@PathParam("wikiName") String wikiName, @PathParam("spaceName") String spaceName,
        @PathParam("pageName") String pageName, @PathParam("id") Integer id,
        @QueryParam("start") @DefaultValue("0") Integer start, @QueryParam("number") @DefaultValue("-1") Integer number)
        throws XWikiException
    {
        DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, false);

        Document doc = documentInfo.getDocument();

        Vector<com.xpn.xwiki.api.Object> xwikiComments = doc.getComments();

        for (com.xpn.xwiki.api.Object xwikiComment : xwikiComments) {
            if (id.equals(xwikiComment.getNumber())) {
                return DomainObjectFactory.createComment(objectFactory, uriInfo.getBaseUri(), doc, xwikiComment);
            }
        }

        throw new WebApplicationException(Status.NOT_FOUND);
    }

}
