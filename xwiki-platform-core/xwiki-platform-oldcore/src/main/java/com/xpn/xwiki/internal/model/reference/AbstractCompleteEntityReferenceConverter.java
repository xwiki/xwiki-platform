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
package com.xpn.xwiki.internal.model.reference;

import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.properties.converter.AbstractConverter;

/**
 * Base class to convert a specific EntityReferences type.
 * 
 * @param <R> the type in which the provided value has to be converted
 * @version $Id$
 * @since 12.5RC1
 */
public abstract class AbstractCompleteEntityReferenceConverter<R extends EntityReference> extends AbstractConverter<R>
{
    @Inject
    @Named("current")
    private EntityReferenceResolver<String> stringResolver;

    @Inject
    @Named("current")
    private EntityReferenceResolver<EntityReference> referenceResolver;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    private final EntityType type;

    /**
     * @param type the type of reference
     */
    public AbstractCompleteEntityReferenceConverter(EntityType type)
    {
        this.type = type;
    }

    @Override
    protected R convertToType(Type type, Object value)
    {
        if (value == null) {
            return null;
        }

        EntityReference result;

        if (value instanceof EntityReference) {
            result = this.referenceResolver.resolve((EntityReference) value, this.type);
        } else {
            result = this.stringResolver.resolve(value.toString(), this.type);
        }

        return toType(result);
    }

    /**
     * @param result the entity reference
     * @return the typed entity reference
     */
    protected abstract R toType(EntityReference result);

    @Override
    protected String convertToString(R value)
    {
        if (value == null) {
            return null;
        }

        return this.serializer.serialize(value);
    }
}
