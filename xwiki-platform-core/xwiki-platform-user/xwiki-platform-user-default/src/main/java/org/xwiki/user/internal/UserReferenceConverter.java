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
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.properties.converter.AbstractConverter;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

/**
 * Converts a String to {@link UserReference}. Useful from Velocity scripts for example when resolving a user.
 *
 * @version $Id$
 * @since 12.2RC1
 */
@Component
@Singleton
public class UserReferenceConverter extends AbstractConverter<UserReference>
{
    @Inject
    private UserReferenceResolver<String> userReferenceResolver;

    @Override
    protected UserReference convertToType(Type targetType, Object value)
    {
        if (value == null) {
            return null;
        }

        return this.userReferenceResolver.resolve(value.toString());
    }
}
