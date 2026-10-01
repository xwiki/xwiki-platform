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
package com.xpn.xwiki.web;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Action used to remove a comment from a page. It requires the comment right instead of the edit right, and only the
 * author of the comment or an administrator can remove it.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
@Component
@Named("commentremove")
@Singleton
public class CommentDeleteAction extends AbstractObjectRemoveAction
{
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private AuthorizationManager authorizationManager;

    /**
     * Sets up the translation keys used by this action.
     */
    public CommentDeleteAction()
    {
        super("platform.core.action.commentRemove.noCommentSpecified",
            "platform.core.action.commentRemove.invalidComment", "core.comment.deleteComment");
    }

    @Override
    protected DocumentReference getClassReference(XWikiDocument doc, String className)
    {
        DocumentReference commentClassReference =
            new DocumentReference(doc.getDocumentReference().getWikiReference().getName(), XWiki.SYSTEM_SPACE,
                XWikiDocument.COMMENTSCLASS_REFERENCE.getName());
        // Reject any other class so that this action cannot remove other objects with only the comment right.
        if (StringUtils.isNotBlank(className) && !commentClassReference.equals(doc.resolveClassReference(className))) {
            setErrorMessage("platform.core.action.commentRemove.invalidClass");
            return null;
        }
        return commentClassReference;
    }

    @Override
    protected boolean canRemoveObject(BaseObject obj, XWikiContext context)
    {
        // Only the author of the comment or an administrator can remove it.
        DocumentReference authorReference = this.documentReferenceResolver.resolve(obj.getStringValue("author"));
        DocumentReference userReference = context.getUserReference();
        boolean allowed = authorReference.equals(userReference)
            || this.authorizationManager.hasAccess(Right.ADMIN, userReference, context.getDoc().getDocumentReference());
        if (!allowed) {
            setErrorMessage("platform.core.action.commentRemove.notAllowed");
        }
        return allowed;
    }
}
