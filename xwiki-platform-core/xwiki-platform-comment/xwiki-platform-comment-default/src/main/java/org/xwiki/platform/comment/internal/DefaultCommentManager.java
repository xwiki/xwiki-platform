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
package org.xwiki.platform.comment.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.platform.comment.Comment;
import org.xwiki.platform.comment.CommentException;
import org.xwiki.platform.comment.CommentManager;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

@Component
@Singleton
public class DefaultCommentManager implements CommentManager
{
    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private UserReferenceResolver<String> userReferenceResolver;

    @Override
    public List<Comment> getComments(EntityReference entityReference, boolean ascendindOrder)
        throws CommentException
    {
        if (entityReference.getType() != EntityType.DOCUMENT) {
            throw new CommentException("Current implementation only support document entities");
        }
        XWikiContext context = this.contextProvider.get();
        try {
            XWikiDocument document = context.getWiki().getDocument(entityReference, context);
            List<BaseObject> comments = document.getComments();
            List<Comment> result = new ArrayList<>();
            for (BaseObject comment : comments) {
                result.add(this.buildComment(comment));
            }
            Comparator<Comment> comparator = Comparator.comparing(Comment::getCreationDate);
            if (!ascendindOrder) {
                comparator = comparator.reversed();
            }
            result.sort(comparator);
            return result;
        } catch (XWikiException e) {
            throw new CommentException(
                String.format("Error when accessing comments for document [%s].", entityReference), e);
        }
    }

    private DefaultComment buildComment(BaseObject commentObject) throws XWikiException
    {
        DefaultComment result = new DefaultComment(commentObject.getNumber());
        UserReference author = this.userReferenceResolver.resolve(commentObject.getStringValue("author"));
        Date date = commentObject.getDateValue("date");
        String content = commentObject.getStringValue("comment");
        int replyTo = commentObject.getIntValue("replyto");
        return result.setAuthor(author).setCreated(date).setContent(content).setReplyId(replyTo);
    }
}
