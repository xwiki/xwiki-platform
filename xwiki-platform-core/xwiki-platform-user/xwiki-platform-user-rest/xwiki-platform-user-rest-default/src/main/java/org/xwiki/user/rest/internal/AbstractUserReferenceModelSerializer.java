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
package org.xwiki.user.rest.internal;

import javax.inject.Provider;

import org.xwiki.user.GuestUserReference;
import org.xwiki.user.UserProperties;
import org.xwiki.user.UserPropertiesResolver;
import org.xwiki.user.rest.UserReferenceModelSerializer;
import org.xwiki.user.rest.model.jaxb.ObjectFactory;
import org.xwiki.user.rest.model.jaxb.User;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.user.api.XWikiRightService;

import jakarta.inject.Inject;

/**
 * Abstract implementation of {@link UserReferenceModelSerializer} to handle pseudo-users, like Guest.
 *
 * @since 18.2.0RC1
 * @version $Id$
 */
public abstract class AbstractUserReferenceModelSerializer implements UserReferenceModelSerializer
{
    protected final ObjectFactory userObjectFactory = new ObjectFactory();
    protected final org.xwiki.rest.model.jaxb.ObjectFactory xwikiObjectFactory =
        new org.xwiki.rest.model.jaxb.ObjectFactory();

    @Inject
    protected UserPropertiesResolver userPropertiesResolver;

    @Inject
    protected Provider<XWikiContext> xcontextProvider;

    protected User guestToRestUser()
    {
        User user = this.userObjectFactory.createUser();

        UserProperties userProperties = this.userPropertiesResolver.resolve(GuestUserReference.INSTANCE);

        user.setId(XWikiRightService.GUEST_USER_FULLNAME);
        user.setGlobal(GuestUserReference.INSTANCE.isGlobal());
        user.setFirstName(userProperties.getFirstName());
        user.setLastName(userProperties.getLastName());
        user.setDisplayName("Guest");

        XWikiContext xcontext = this.xcontextProvider.get();

        String defaultAvatarUrl = xcontext.getWiki().getSkinFile("icons/xwiki/noavatar.png", xcontext);
        user.setAvatarUrl(defaultAvatarUrl);

        return user;
    }
}
