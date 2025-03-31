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

import java.util.Date;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.platform.comment.Comment;
import org.xwiki.platform.comment.CommentId;
import org.xwiki.user.UserReference;

public class DefaultComment implements Comment
{
    private final CommentId id;
    private UserReference author;
    private Date created;
    private String content;
    private CommentId replyId;

    public DefaultComment(int id)
    {
        this.id = new DefaultCommentId(id);
    }

    @Override
    public CommentId getId()
    {
        return this.id;
    }

    @Override
    public UserReference getAuthor()
    {
        return this.author;
    }

    @Override
    public String getContent()
    {
        return this.content;
    }

    @Override
    public Date getCreationDate()
    {
        return this.created;
    }

    @Override
    public CommentId getReplyTo()
    {
        return this.replyId;
    }

    public DefaultComment setAuthor(UserReference author)
    {
        this.author = author;
        return this;
    }

    public DefaultComment setCreated(Date created)
    {
        this.created = created;
        return this;
    }

    public DefaultComment setContent(String content)
    {
        this.content = content;
        return this;
    }

    public DefaultComment setReplyId(int replyId)
    {
        this.replyId = new DefaultCommentId(replyId);
        return this;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultComment that = (DefaultComment) o;

        return new EqualsBuilder().append(id, that.id).append(author, that.author)
            .append(created, that.created).append(content, that.content).append(replyId, that.replyId).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 35).append(id).append(author).append(created).append(content).append(replyId)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
            .append("id", id)
            .append("author", author)
            .append("created", created)
            .append("content", content)
            .append("replyId", replyId)
            .toString();
    }
}
