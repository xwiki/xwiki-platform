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
package org.xwiki.security.authorization.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.async.AsyncContext;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.DefaultAuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Extends {@link DefaultAuthorizationManager} to manipulated APIs not accessible by xwiki-platform-security-api.
 * 
 * @version $Id$
 * @since 11.8RC1
 */
@Component
@Singleton
// Put all that in DefaultAuthorizationManager if it ever move in some non xwiki-platform-security-api module.
public class BridgeAuthorizationManager extends DefaultAuthorizationManager
{
    @Inject
    private AsyncContext asyncContext;

    @Override
    public void checkAccess(Right right, DocumentReference userReference, EntityReference entityReference)
        throws AccessDeniedException
    {
        boolean allowed = false;
        try {
            super.checkAccess(right, userReference, entityReference);

            allowed = true;
        } finally {
            // Associated the currently executing content with the passed right check
            this.asyncContext.useRight(right, userReference, entityReference, allowed);
        }
    }

    @Override
    public boolean hasAccess(Right right, DocumentReference userReference, EntityReference entityReference)
    {
        boolean allowed = false;
        try {
            allowed = super.hasAccess(right, userReference, entityReference);
        } finally {
            // Associated the currently executing content with the passed right check
            this.asyncContext.useRight(right, userReference, entityReference, allowed);
        }

        return allowed;
    }
}
