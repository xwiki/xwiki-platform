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
package org.xwiki.model.internal.reference.converter;

import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.EnumUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.properties.converter.AbstractConverter;

/**
 * Converter that converts a value into an {@link EntityReference} object. Reference are not resolved to created
 * complete reference but kept exactly as indicated in the value.
 * 
 * @version $Id$
 * @since 5.2RC1
 */
@Component
@Singleton
public class EntityReferenceConverter extends AbstractConverter<EntityReference>
{
    @Inject
    @Named("relative")
    private EntityReferenceResolver<String> stringResolver;

    @Inject
    private EntityReferenceSerializer<String> serialier;

    @Override
    protected EntityReference convertToType(Type type, Object value)
    {
        if (value == null) {
            return null;
        }

        return convertToType(type, value.toString());
    }

    private EntityReference convertToType(Type type, String value)
    {
        EntityType entityType = EntityType.DOCUMENT;
        String entityReference = value;

        int index = value.indexOf(':');
        if (index > 0) {
            String entityTypeString = value.substring(0, index);
            EntityType foundEntityType = EnumUtils.getEnum(EntityType.class, entityTypeString.toUpperCase());
            if (foundEntityType != null) {
                entityType = foundEntityType;
                ++index;
                entityReference = index < value.length() ? value.substring(index) : "";
            }
        }

        return this.stringResolver.resolve(entityReference, entityType);
    }

    @Override
    protected String convertToString(EntityReference value)
    {
        if (value == null) {
            return null;
        }

        return value.getType().getLowerCase() + ':' + this.serialier.serialize(value);
    }
}
