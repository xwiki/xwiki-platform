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
package org.xwiki.rest.internal.user;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rest.UserReferenceModelSerializer;
import org.xwiki.user.UserConfiguration;

import jakarta.inject.Provider;

/**
 * Provider for instances of {@link UserReferenceModelSerializer}, based on the current user store.
 *
 * @since 18.2.0RC1
 * @version $Id$
 */
@Component
@Singleton
public class UserReferenceModelSerializerProvider implements Provider<UserReferenceModelSerializer>
{
    @Inject
    private UserConfiguration userConfiguration;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Override
    public UserReferenceModelSerializer get()
    {
        try {
            return this.componentManager.getInstance(UserReferenceModelSerializer.class,
                this.userConfiguration.getStoreHint());
        } catch (ComponentLookupException e) {
            return null;
        }
    }
}
