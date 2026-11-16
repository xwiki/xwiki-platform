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
import java.util.List;
import java.util.Vector;

import javax.inject.Named;
import javax.ws.rs.core.Response;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.DomainObjectFactory;
import org.xwiki.rest.internal.RangeIterable;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Comment;
import org.xwiki.rest.model.jaxb.Comments;
import org.xwiki.rest.resources.comments.CommentResource;
import org.xwiki.rest.resources.comments.CommentsResource;
import org.xwiki.store.DocumentRevisionConflictException;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.comments.CommentsResourceImpl")
public class CommentsResourceImpl extends XWikiResource implements CommentsResource
{
    private static final String COMMENTS_CLASS_NAME = "XWiki.XWikiComments";

    /**
     * The number of times a comment is added to the latest version of the document when the document keeps being
     * saved concurrently.
     */
    private static final int SAVE_ATTEMPTS = 5;

    @Override
    public Comments getComments(String wikiName, String spaceName, String pageName, Integer start, Integer number,
            Boolean withPrettyNames) throws XWikiRestException
    {
        try {
            DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, false);

            Document doc = documentInfo.getDocument();

            Comments comments = objectFactory.createComments();

            Vector<com.xpn.xwiki.api.Object> xwikiComments = doc.getComments();

            RangeIterable<com.xpn.xwiki.api.Object> ri = new RangeIterable<>(xwikiComments, start, number);

            for (com.xpn.xwiki.api.Object xwikiComment : ri) {
                comments.getComments().add(
                        DomainObjectFactory.createComment(objectFactory, uriInfo.getBaseUri(), doc, xwikiComment,
                                Utils.getXWikiApi(componentManager), withPrettyNames));
            }

            return comments;
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        }
    }

    @Override
    public Response postComment(String wikiName, String spaceName, String pageName, Comment comment)
            throws XWikiRestException
    {
        try {
            List<String> spaces = parseSpaceSegments(spaceName);

            for (int attempt = 1;; attempt++) {
                // Load the document at each attempt so that the comment is added to the latest version.
                DocumentInfo documentInfo = getDocumentInfo(wikiName, spaces, pageName, null, null, true, true);
                Document doc = documentInfo.getDocument();

                // Without a highlight or a text, there's no comment to add.
                if (comment.getHighlight() == null && comment.getText() == null) {
                    return null;
                }

                com.xpn.xwiki.api.Object commentObject = addComment(doc, comment);

                try {
                    doc.save();
                } catch (DocumentRevisionConflictException e) {
                    // The document was saved by another request since it was loaded: adding a comment doesn't depend
                    // on the other changes, so it can be added again to the newly saved version.
                    if (attempt >= SAVE_ATTEMPTS) {
                        throw e;
                    }
                    continue;
                }

                Comment createdComment = DomainObjectFactory.createComment(objectFactory, uriInfo.getBaseUri(), doc,
                    commentObject, Utils.getXWikiApi(componentManager), false);

                return Response.created(Utils.createURI(uriInfo.getBaseUri(), CommentResource.class, wikiName,
                    spaces, pageName, commentObject.getNumber())).entity(createdComment).build();
            }
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        }
    }

    private com.xpn.xwiki.api.Object addComment(Document doc, Comment comment) throws XWikiException
    {
        int id = doc.createNewObject(COMMENTS_CLASS_NAME);
        com.xpn.xwiki.api.Object commentObject = doc.getObject(COMMENTS_CLASS_NAME, id);
        commentObject.set("author", Utils.getXWikiUser(componentManager));
        commentObject.set("date", new Date());

        if (comment.getHighlight() != null) {
            commentObject.set("highlight", comment.getHighlight());
        }

        if (comment.getText() != null) {
            commentObject.set("comment", comment.getText());
        }

        if (comment.getReplyTo() != null) {
            commentObject.set("replyto", comment.getReplyTo());
        }

        return commentObject;
    }
}
