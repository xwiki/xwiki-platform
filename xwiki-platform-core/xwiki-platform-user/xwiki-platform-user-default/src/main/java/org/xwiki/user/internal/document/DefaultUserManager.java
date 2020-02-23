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
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.user.User;
import org.xwiki.user.UserManager;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWikiContext;

/**
 * Document-based implementation of {@link UserManager}.
 *
 * @version $Id$
 * @since 12.2RC1
 */
@Component
@Singleton
public class DocumentUserManager implements UserManager
{
    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> currentReferenceResolver;

    @Inject
    private DocumentAccessBridge dab;

    @Inject
    private Execution execution;

    @Inject
    private EntityReferenceProvider entityReferenceProvider;

    @Override
    public User getUser(UserReference userReference)
    {
        return new DocumentUser((DocumentUserReference) userReference, this.dab, this.currentReferenceResolver,
            this.entityReferenceProvider);
    }

    @Override
    public User getCurrentUser()
    {
        return getUser(new DocumentUserReference(getXWikiContext().getUserReference()));
    }

    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
    }
}
