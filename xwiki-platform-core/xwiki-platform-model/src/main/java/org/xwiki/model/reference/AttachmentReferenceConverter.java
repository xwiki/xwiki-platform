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
package org.xwiki.model.reference;

import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.properties.converter.AbstractConverter;

/**
 * Converter that converts a value into a {@link AttachmentReference} object. Relative references are resolved using
 * "current" {@link DocumentReferenceResolver}.
 * 
 * @version $Id$
 * @since 11.6RC1
 * @since 10.11.9
 * @since 11.3.2
 */
@Component
@Singleton
public class AttachmentReferenceConverter extends AbstractConverter<AttachmentReference>
{
    @Inject
    @Named("current")
    private AttachmentReferenceResolver<String> stringResolver;

    @Inject
    @Named("current")
    private AttachmentReferenceResolver<EntityReference> referenceResolver;

    @Inject
    @Named("compact")
    private EntityReferenceSerializer<String> serializer;

    @Override
    protected AttachmentReference convertToType(Type type, Object value)
    {
        if (value == null) {
            return null;
        }

        AttachmentReference reference;

        if (value instanceof EntityReference) {
            reference = this.referenceResolver.resolve((EntityReference) value);
        } else {
            reference = this.stringResolver.resolve(value.toString());
        }

        return reference;
    }

    @Override
    protected String convertToString(AttachmentReference value)
    {
        if (value == null) {
            return null;
        }

        return this.serializer.serialize(value);
    }
}
