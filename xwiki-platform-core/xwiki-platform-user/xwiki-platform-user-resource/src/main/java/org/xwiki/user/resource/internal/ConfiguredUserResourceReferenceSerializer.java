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
package org.xwiki.user.resource.internal;

import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.url.ExtendedURL;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.SuperAdminUserReference;
import org.xwiki.user.UserConfiguration;
import org.xwiki.user.UserReferenceResolver;

/**
 * Converts a {@link UserResourceReference} into a relative {@link ExtendedURL} (with the Context Path added).
 *
 * @version $Id$
 * @since 12.2RC1
 */
@Component
@Singleton
public class ConfiguredUserResourceReferenceSerializer
    implements ResourceReferenceSerializer<UserResourceReference, ExtendedURL>
{
    @Inject
    private UserConfiguration userConfiguration;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private UserReferenceResolver<CurrentUserReference> currentUserReferenceResolver;

    @Override
    public ExtendedURL serialize(UserResourceReference resourceReference)
        throws SerializeResourceReferenceException, UnsupportedResourceReferenceException
    {
        UserResourceReference normalizedReference = resourceReference;
        // Try to resolve the current user if possible.
        if (normalizedReference.getUserReference() == null
            || CurrentUserReference.INSTANCE == normalizedReference.getUserReference())
        {
            normalizedReference = new UserResourceReference(this.currentUserReferenceResolver.resolve(null));
        }
        // SuperAdmin and Guest do not have profile URLs!
        if (SuperAdminUserReference.INSTANCE == normalizedReference.getUserReference()
            || GuestUserReference.INSTANCE == normalizedReference.getUserReference())
        {
            // Guest and SuperAdmin are virtual users and currently don't have a profile URL
            throw new IllegalArgumentException(
                "Guest and SuperAdmin are virtual users and currently don't have a profile URL");
        }
        return findResourceReferenceSerializer().serialize(normalizedReference);
    }

    private ResourceReferenceSerializer<UserResourceReference, ExtendedURL> findResourceReferenceSerializer()
    {
        Type type = new DefaultParameterizedType(null, ResourceReferenceSerializer.class, UserResourceReference.class,
            ExtendedURL.class);
        try {
            return this.componentManager.getInstance(type, this.userConfiguration.getStoreHint());
        } catch (ComponentLookupException e) {
            // If the configured user store hint is invalid (i.e. there's no serializer for it, then the XWiki instance
            // cannot work and thus we need to fail hard and fast. Hence the runtime exception.
            throw new RuntimeException(String.format(
                "Failed to find user resource reference serializer for role [%s] and hint [%s]", type,
                this.userConfiguration.getStoreHint()), e);
        }
    }
}
