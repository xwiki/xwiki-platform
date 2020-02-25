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
package org.xwiki.user.internal.document;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.user.User;
import org.xwiki.user.UserResolver;

import com.xpn.xwiki.XWikiContext;

/**
 * Helps implement Document-based User Resolvers.
 *
 * @param <T> the type of the user representation
 * @version $Id$
 * @since 12.2RC1
 */
public abstract class AbstractDocumentUserResolver<T> implements UserResolver<T>
{
    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> currentReferenceResolver;

    @Inject
    private DocumentAccessBridge dab;

    @Inject
    private EntityReferenceProvider entityReferenceProvider;

    @Inject
    private Execution execution;

    /**
     * @param userReference the reference to the user to create. If null then resolves the current user reference
     * @return the User object
     */
    protected User resolveUser(DocumentUserReference userReference)
    {
        User user;
        if (userReference == null) {
            user = resolveUserInternal(new DocumentUserReference(getXWikiContext().getUserReference()));
        } else {
            user = resolveUserInternal(userReference);
        }
        return user;
    }

    private User resolveUserInternal(DocumentUserReference userReference)
    {
        return new DocumentUser(userReference, this.dab, this.currentReferenceResolver, this.entityReferenceProvider);
    }

    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
    }
}
