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
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.namespace.Namespace;
import org.xwiki.component.namespace.NamespaceUtils;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.properties.ConverterManager;
import org.xwiki.properties.converter.AbstractConverter;
import org.xwiki.properties.converter.ConversionException;
import org.xwiki.properties.converter.Converter;

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
    private ConverterManager converterManager;

    @Inject
    private Logger logger;

    @Inject
    @Named("withtype")
    private EntityReferenceSerializer<String> serializer;

    @Override
    protected EntityReference convertToType(Type type, Object value)
    {
        EntityReference result;
        if (value == null) {
            result = null;
        } else {
            Converter<Object> converter = this.converterManager.getConverter(value.getClass());

            if (converter != null) {
                try {
                    result = converter.convert(EntityReference.class, value);
                } catch (ConversionException e) {
                    logger.warn("The type [{}] cannot be converted natively to EntityReference, "
                        + "falling back on using toString to convert it.", value.getClass().getName());
                    result = convertToType(type, value.toString());
                }
            } else {
                result = convertToType(type, value.toString());
            }
        }

        return result;
    }

    private EntityReference convertToType(Type type, String value)
    {
        Namespace namespace = NamespaceUtils.toNamespace(value);

        EntityType entityType = namespace.getType() != null
            ? EnumUtils.getEnum(EntityType.class, namespace.getType().toUpperCase()) : null;
        String entityReference = namespace.getValue();
        if (entityType == null) {
            entityType = EntityType.DOCUMENT;
            entityReference = value;
        }

        return this.stringResolver.resolve(entityReference, entityType);
    }

    @Override
    protected String convertToString(EntityReference value)
    {
        if (value == null) {
            return null;
        }

        return this.serializer.serialize(value);
    }
}
