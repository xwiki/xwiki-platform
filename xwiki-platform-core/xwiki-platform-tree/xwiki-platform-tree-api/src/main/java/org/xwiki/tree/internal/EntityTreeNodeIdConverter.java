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
package org.xwiki.tree.internal;

import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.properties.converter.AbstractConverter;

/**
 * Supports conversion between string entity tree node id and {@link EntityReference}.
 * 
 * @version $Id$
 * @since 11.10
 */
@Component
@Singleton
@Named("entityTreeNodeId")
public class EntityTreeNodeIdConverter extends AbstractConverter<EntityReference>
{
    @Inject
    protected EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    @Inject
    @Named("current")
    private EntityReferenceResolver<String> currentEntityReferenceResolver;

    @Override
    protected String convertToString(EntityReference entityReference)
    {
        if (entityReference == null) {
            return null;
        }

        return underscoreToCamelCase(entityReference.getType().name().toLowerCase()) + ':'
            + this.defaultEntityReferenceSerializer.serialize(entityReference);
    }

    private String underscoreToCamelCase(String entityType)
    {
        StringBuilder result = new StringBuilder();
        for (String part : StringUtils.split(entityType, '_')) {
            result.append(StringUtils.capitalize(part));
        }
        return StringUtils.uncapitalize(result.toString());
    }

    @SuppressWarnings("unchecked")
    @Override
    protected EntityReference convertToType(Type targetType, Object value)
    {
        String[] parts = StringUtils.split(String.valueOf(value), ":", 2);
        if (parts == null || parts.length != 2) {
            return null;
        }

        try {
            EntityType entityType = EntityType.valueOf(camelCaseToUnderscore(parts[0]).toUpperCase());
            return this.currentEntityReferenceResolver.resolve(parts[1], entityType);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String camelCaseToUnderscore(String nodeType)
    {
        return StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(nodeType), '_');
    }
}
