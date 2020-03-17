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
package org.xwiki.user.internal;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.SuperAdminUserReference;
import org.xwiki.user.UserProperties;
import org.xwiki.user.UserPropertiesResolver;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

/**
 * Generic implementation of {@link UserPropertiesResolver} which proxies to the specific implementation based on the
 * type of the passed {@link UserReference}.
 *
 * @version $Id$
 * @since 12.2RC1
 */
public abstract class AbstractUserPropertiesResolver implements UserPropertiesResolver
{
    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    @Named("superadminuser")
    private ConfigurationSource superAdminConfigurationSource;

    @Inject
    @Named("guestuser")
    private ConfigurationSource guestConfigurationSource;

    @Inject
    private UserReferenceResolver<CurrentUserReference> currentUserReferenceUserReferenceResolver;

    @Override
    public UserProperties resolve(UserReference userReference, Object... parameters)
    {
        UserProperties userProperties;

        // Handle special cases
        UserReference normalizedUserReference = userReference;
        if (normalizedUserReference == null || CurrentUserReference.INSTANCE == normalizedUserReference) {
            normalizedUserReference = this.currentUserReferenceUserReferenceResolver.resolve(null, parameters);
        }
        if (SuperAdminUserReference.INSTANCE == normalizedUserReference) {
            userProperties = new DefaultUserProperties(this.superAdminConfigurationSource);
        } else if (GuestUserReference.INSTANCE == normalizedUserReference) {
            userProperties = new DefaultUserProperties(this.guestConfigurationSource);
        } else {
            userProperties =
                findUserPropertiesResolver(normalizedUserReference).resolve(normalizedUserReference, parameters);
        }
        return userProperties;
    }

    private UserPropertiesResolver findUserPropertiesResolver(UserReference userReference)
    {
        String hint =
            String.format("%s/%s", getUnderlyingConfigurationSourceHint(), userReference.getClass().getName());
        try {
            return this.componentManager.getInstance(UserPropertiesResolver.class, hint);
        } catch (ComponentLookupException e) {
            // If there's no resolver for the passed UserReference type, then the XWiki instance cannot work and thus
            // we need to fail hard and fast. Hence the runtime exception.
            throw new RuntimeException(String.format(
                "Failed to find user properties resolver for role [%s] and hint [%s]",
                UserPropertiesResolver.class.getName(), hint), e);
        }
    }

    protected abstract String getUnderlyingConfigurationSourceHint();
}
