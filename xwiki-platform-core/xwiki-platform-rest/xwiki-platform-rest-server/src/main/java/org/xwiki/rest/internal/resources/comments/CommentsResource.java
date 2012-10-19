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
package org.xwiki.rest.internal.resources.comments;

import java.util.Date;
import java.util.Vector;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.internal.DomainObjectFactory;
import org.xwiki.rest.internal.RangeIterable;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Comment;
import org.xwiki.rest.model.jaxb.Comments;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 */
@Component("org.xwiki.rest.internal.resources.comments.CommentsResource")
@Path("/wikis/{wikiName}/spaces/{spaceName}/pages/{pageName}/comments")
public class CommentsResource extends XWikiResource
{
    @GET
    public Comments getComments(@PathParam("wikiName") String wikiName, @PathParam("spaceName") String spaceName,
        @PathParam("pageName") String pageName, @QueryParam("start") @DefaultValue("0") Integer start,
        @QueryParam("number") @DefaultValue("-1") Integer number,
        @QueryParam("prettynames") @DefaultValue("0") Boolean withPrettyNames) throws XWikiException
    {
        DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, false);

        Document doc = documentInfo.getDocument();

        Comments comments = objectFactory.createComments();

        Vector<com.xpn.xwiki.api.Object> xwikiComments = doc.getComments();

        RangeIterable<com.xpn.xwiki.api.Object> ri =
            new RangeIterable<com.xpn.xwiki.api.Object>(xwikiComments, start, number);

        for (com.xpn.xwiki.api.Object xwikiComment : ri) {
            comments.getComments().add(
                DomainObjectFactory.createComment(objectFactory, uriInfo.getBaseUri(), doc, xwikiComment, Utils.getXWikiApi(componentManager), withPrettyNames));
        }

        return comments;
    }

    @POST
    public Response postComment(@PathParam("wikiName") String wikiName, @PathParam("spaceName") String spaceName,
        @PathParam("pageName") String pageName, Comment comment) throws XWikiException
    {
        DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, true);

        Document doc = documentInfo.getDocument();

        int id = doc.createNewObject("XWiki.XWikiComments");
        com.xpn.xwiki.api.Object commentObject = doc.getObject("XWiki.XWikiComments", id);
        commentObject.set("author", Utils.getXWikiUser(componentManager));
        commentObject.set("date", new Date());

        boolean save = false;

        if (comment.getHighlight() != null) {
            commentObject.set("highlight", comment.getHighlight());
            save = true;
        }

        if (comment.getText() != null) {
            commentObject.set("comment", comment.getText());
            save = true;
        }
        
        if (comment.getReplyTo() != null) {
            commentObject.set("replyto", comment.getReplyTo());
        }

        if (save) {
            doc.save();

            Comment createdComment =
                DomainObjectFactory.createComment(objectFactory, uriInfo.getBaseUri(), doc, commentObject, Utils.getXWikiApi(componentManager), false);

            return Response.created(
                UriBuilder.fromUri(uriInfo.getBaseUri()).path(CommentResource.class).build(wikiName, spaceName,
                    pageName, id)).entity(createdComment).build();
        }

        return null;
    }
}
