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
package org.xwiki.user.internal.converter;

import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.properties.converter.AbstractConverter;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;

/**
 * Converts a String to a {@link UserReference}. Useful from Velocity scripts for example when resolving a user.
 *
 * @param <U> the type associated with the converter
 * @version $Id$
 * @since 12.2
 */
public abstract class AbstractUserReferenceConverter<U extends UserReference> extends AbstractConverter<U>
{
    @Inject
    @Named("current")
    private UserReferenceResolver<String> userReferenceResolver;

    @Inject
    private UserReferenceSerializer<String> userReferenceSerializer;

    @Inject
    @Named("document")
    private UserReferenceSerializer<DocumentReference> userReferenceDocumentReferenceSerializer;

    @Override
    protected UserReference convertToType(Type targetType, Object value)
    {
        if (value == null) {
            return null;
        }

        return this.userReferenceResolver.resolve(value.toString());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <G> G convert(Type targetType, Object sourceValue)
    {
        G result;

        // Special handling for target type DocumentReference to support converting UserReference to DocumentReference.
        if (targetType == DocumentReference.class && sourceValue instanceof UserReference) {
            result = (G) this.userReferenceDocumentReferenceSerializer.serialize((UserReference) sourceValue);
        } else {
            result = super.convert(targetType, sourceValue);
        }

        return result;
    }

    @Override
    protected String convertToString(U value)
    {
        return this.userReferenceSerializer.serialize(value);
    }
}
