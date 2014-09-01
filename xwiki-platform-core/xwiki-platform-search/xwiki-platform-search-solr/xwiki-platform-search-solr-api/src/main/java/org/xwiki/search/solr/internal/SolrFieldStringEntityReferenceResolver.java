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
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceValueProvider;

/**
 * Resolves an {@link EntityReference} that has been serialized with {@link SolrFieldStringEntityReferenceSerializer}.
 * 
 * @version $Id$
 * @since 5.3RC1
 */
@Component
@Named("solr")
@Singleton
public class SolrFieldStringEntityReferenceResolver implements EntityReferenceResolver<String>
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
    private EntityReferenceValueProvider currentEntityReferenceValueProvider;

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
        while (offset >= 0 && entityTypeOffset < entityTypesForType.length) {
            StringBuilder entityName = new StringBuilder();
            offset = readEntityName(entityReferenceRepresentation, entityName, offset);
            EntityReference parent =
                new EntityReference(entityName.reverse().toString(), entityTypesForType[entityTypeOffset++]);
            entityReference = entityReference == null ? parent : entityReference.appendParent(parent);
        }

        // Handle the case when the passed string representation is relative. Use the provided parameters in this case.
        for (int i = entityTypeOffset; i < entityTypesForType.length; i++) {
            String entityName = resolveDefaultValue(entityTypesForType[i], parameters);
            if (entityName != null) {
                EntityReference parent = new EntityReference(entityName, entityTypesForType[i]);
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

    /**
     * Resolve the default name for a given reference type.
     * 
     * @param type the type for which a default name is requested
     * @param parameters optional parameters, if the first parameter is an entity reference which is of the given type
     *            or contains the given types in its parent chain, use the name of the reference having the requested
     *            type in place of the default value
     * @return a name for the given type
     */
    private String resolveDefaultValue(EntityType type, Object... parameters)
    {
        if (parameters.length > 0 && parameters[0] instanceof EntityReference) {
            // Try to extract the type from the passed parameter.
            EntityReference referenceParameter = (EntityReference) parameters[0];
            EntityReference extractedReference = referenceParameter.extractReference(type);
            if (extractedReference != null) {
                return extractedReference.getName();
            }
        }

        return getDefaultValue(type, parameters);
    }

    /**
     * @param type the entity type for which to return the default value to use (since the user has not specified it)
     * @param parameters optional parameters. Their meaning depends on the resolver implementation
     * @return the default value to use
     */
    private String getDefaultValue(EntityType type, Object... parameters)
    {
        return currentEntityReferenceValueProvider.getDefaultValue(type);
    }
}
