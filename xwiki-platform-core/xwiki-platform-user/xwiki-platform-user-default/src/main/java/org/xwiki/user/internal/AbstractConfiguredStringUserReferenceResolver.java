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

import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

/**
 * Common code to find the string User Reference Resolver based on the configured User store hint.
 *
 * @version $Id$
 * @since 12.8RC1
 */
public abstract class AbstractConfiguredStringUserReferenceResolver implements UserReferenceResolver<String>
{
    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Override
    public UserReference resolve(String userName, Object... parameters)
    {
        Type type = new DefaultParameterizedType(null, UserReferenceResolver.class, String.class);
        try {
            UserReferenceResolver<String> resolver =
                this.componentManager.getInstance(type, getUserReferenceResolverHint());
            return resolver.resolve(userName, parameters);
        } catch (ComponentLookupException e) {
            // If the configured user store hint is invalid (i.e. there's no resolver for it, then the XWiki instance
            // cannot work and thus we need to fail hard and fast. Hence the runtime exception.
            throw new RuntimeException(String.format(
                "Failed to find user reference resolver for role [%s] and hint [%s]", type,
                getUserReferenceResolverHint()), e);
        }
    }

    /**
     * @return the hint to use for retrieving the {@code UserReferenceResolver<String>} component instance
     */
    protected abstract String getUserReferenceResolverHint();
}
