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

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
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
public class DefaultUserManager implements UserManager
{
    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private Execution execution;

    @Override
    public User getUser(UserReference userReference)
    {
        User user;
        if (userReference == null) {
            user = getUser(new DocumentUserReference(getXWikiContext().getUserReference()));
        } else {
            user = resolveUserManager(userReference).getUser(userReference);
        }
        return user;
    }

    @Override
    public boolean exists(UserReference userReference)
    {
        return resolveUserManager(userReference).exists(userReference);
    }

    private UserManager resolveUserManager(UserReference userReference)
    {
        try {
            return this.componentManager.getInstance(UserManager.class, userReference.getClass().getName());
        } catch (ComponentLookupException e) {
            throw new RuntimeException(String.format(
                "Failed to find component implementation for role [%s] and hint [%s]", UserManager.class.getName(),
                userReference.getClass().getName()));
        }
    }

    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
    }
}
