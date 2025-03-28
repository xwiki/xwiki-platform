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
package org.xwiki.platform.comment.script;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.platform.comment.Comment;
import org.xwiki.platform.comment.CommentException;
import org.xwiki.platform.comment.CommentManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

/**
 * Script service dedicated to manipulate comments.
 *
 * @version $Id$
 * @since 17.3.0RC1
 * @since 16.10.6
 * @since 16.4.7
 */
@Component
@Named("comment")
@Singleton
@Unstable
public class CommentScriptService implements ScriptService
{
    @Inject
    private CommentManager commentManager;

    /**
     * Retrieve comments sorted by dates.
     *
     * @param entityReference the reference for which to retrieve comments.
     * @param ascending {@code true} to retrieve comments in date ascending order, {@code false} for date descending
     * order
     * @return the list of comments associated to the given reference ordered by date
     * @throws CommentException in case of problem to retrieve the comments
     */
    public List<Comment> getComments(EntityReference entityReference, boolean ascending) throws CommentException
    {
        return this.commentManager.getComments(entityReference, ascending);
    }
}
