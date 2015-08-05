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
package org.xwiki.search.solr.internal;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.reference.AbstractEntityReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceResolver;

/**
 * Resolves a <strong>local</strong> {@link EntityReference} that has been serialized with
 * {@link SolrFieldStringEntityReferenceSerializer}.
 * 
 * @version $Id$
 * @since 5.3RC1
 */
@Component
@Named("solr")
@Singleton
public class SolrFieldStringEntityReferenceResolver extends AbstractEntityReferenceResolver implements
    EntityReferenceResolver<String>
{
    /**
     * The character used to separate the entities in the string serialization.
     */
    static final char SEPARATOR = '.';

    /**
     * Map defining ordered entity types of a proper reference chain for a given entity type.
     */
    @SuppressWarnings("serial")
    private static final Map<EntityType, EntityType[]> ENTITY_TYPES = new HashMap<EntityType, EntityType[]>()
    {
        {
            put(EntityType.DOCUMENT, new EntityType[]{EntityType.DOCUMENT, EntityType.SPACE, EntityType.WIKI});
            put(EntityType.ATTACHMENT, new EntityType[]{EntityType.ATTACHMENT, EntityType.DOCUMENT, EntityType.SPACE,
                EntityType.WIKI});
            put(EntityType.SPACE, new EntityType[]{EntityType.SPACE, EntityType.WIKI});
            put(EntityType.OBJECT, new EntityType[]{EntityType.OBJECT, EntityType.DOCUMENT, EntityType.SPACE,
                EntityType.WIKI});
            put(EntityType.OBJECT_PROPERTY, new EntityType[]{EntityType.OBJECT_PROPERTY, EntityType.OBJECT,
                EntityType.DOCUMENT, EntityType.SPACE, EntityType.WIKI});
            put(EntityType.CLASS_PROPERTY, new EntityType[]{EntityType.CLASS_PROPERTY, EntityType.DOCUMENT,
                EntityType.SPACE, EntityType.WIKI});
        }
    };

    @Inject
    @Named("current")
    private EntityReferenceProvider currentEntityReferenceProvider;

    @Override
    public EntityReference resolve(String entityReferenceRepresentation, EntityType type, Object... parameters)
    {
        EntityType[] entityTypesForType = ENTITY_TYPES.get(type);
        if (entityTypesForType == null) {
            throw new RuntimeException("No parsing definition found for Entity Type [" + type + "]");
        }

        EntityReference entityReference = null;
        int entityTypeOffset = 0;
        int offset = entityReferenceRepresentation.length() - 1;
        while (offset >= 0) {
            // Small hack in order to be able to resolve nested spaces. Basically we never read the WIKI component from
            // the passed entity reference representation (we assume it is a space). We fill the WIKI entity reference
            // after this while loop.
            entityTypeOffset = Math.min(entityTypeOffset, entityTypesForType.length - 2);

            StringBuilder entityName = new StringBuilder();
            offset = readEntityName(entityReferenceRepresentation, entityName, offset);
            EntityReference parent = getNewEntityReference(entityName.reverse().toString(),
                entityTypesForType[entityTypeOffset++], parameters);
            entityReference = entityReference == null ? parent : entityReference.appendParent(parent);
        }

        // Handle the case when the passed string representation is relative. Use the provided parameters in this case.
        for (int i = entityTypeOffset; i < entityTypesForType.length; i++) {
            EntityReference parent = resolveDefaultReference(entityTypesForType[i], parameters);
            if (parent != null) {
                entityReference = entityReference == null ? parent : entityReference.appendParent(parent);
            } else {
                // We cannot skip reference components.
                break;
            }
        }

        return entityReference;
    }

    /**
     * Tries to read a non-separator character from the input and to write it to the output and returns the new input
     * offset. If the read character is a separator then the input offset it not modified. If the read character is an
     * escaped separator then the separator is written to the output and the offset is updated to skip the escaping.
     * 
     * @param input the input string
     * @param output the output
     * @param offset the input offset
     * @return the updated offset, unless the current character is a separator
     */
    private int readCharacter(String input, StringBuilder output, int offset)
    {
        if (offset >= 0) {
            char c = input.charAt(offset);
            if (c == SEPARATOR) {
                if (offset == 0 || (offset > 0 && input.charAt(offset - 1) != SEPARATOR)) {
                    return offset;
                } else {
                    output.append(c);
                    return offset - 2;
                }
            } else {
                output.append(c);
                return offset - 1;
            }
        }
        return offset;
    }

    /**
     * Reads an escaped entity name from the given string.
     * 
     * @param input the input string to read the entity name from
     * @param output where to write the entity name
     * @param inputOffset the input offset
     * @return the updated offset
     */
    private int readEntityName(String input, StringBuilder output, int inputOffset)
    {
        int oldOffset;
        int offset = inputOffset;
        do {
            oldOffset = offset;
            offset = readCharacter(input, output, offset);
        } while (offset != oldOffset);
        return offset - 1;
    }

    private EntityReference getNewEntityReference(String name, EntityType type, Object... parameters)
    {
        if ("".equals(name)) {
            return resolveDefaultReference(type, parameters);
        } else {
            return new EntityReference(name, type);
        }
    }

    @Override
    protected EntityReference getDefaultReference(EntityType type, Object... parameters)
    {
        return currentEntityReferenceProvider.getDefaultReference(type);
    }
}
