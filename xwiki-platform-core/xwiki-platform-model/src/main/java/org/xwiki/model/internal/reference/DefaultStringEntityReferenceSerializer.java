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
package org.xwiki.model.internal.reference;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * Generate a string representation of an entity reference (eg "wiki:space.page" for a document reference in the "wiki"
 * Wiki, the "space" Space and the "page" Page).
 * 
 * @version $Id$
 * @since 2.2M1
 */
@Component
public class DefaultStringEntityReferenceSerializer implements EntityReferenceSerializer<String>
{
    /**
     * The list of strings to escape for each type of entity.
     */
    private static final Map<EntityType, String[]> ESCAPES = new HashMap<EntityType, String[]>()
    {
        {
            put(EntityType.ATTACHMENT, new String[] {"@", "\\"});
            put(EntityType.DOCUMENT, new String[] {".", "\\"});
            put(EntityType.SPACE, new String[] {":", ".", "\\"});
            put(EntityType.OBJECT, new String[] {"^", "\\"});
            put(EntityType.OBJECT_PROPERTY, new String[] {".", "\\"});
        }
    };

    /**
     * The replacement list corresponding to the list in {@link #ESCAPES} map.
     */
    private static final Map<EntityType, String[]> REPLACEMENTS = new HashMap<EntityType, String[]>()
    {
        {
            put(EntityType.ATTACHMENT, new String[] {"\\@", "\\\\"});
            put(EntityType.DOCUMENT, new String[] {"\\.", "\\\\"});
            put(EntityType.SPACE, new String[] {"\\:", "\\.", "\\\\"});
            put(EntityType.OBJECT, new String[] {"\\^", "\\\\"});
            put(EntityType.OBJECT_PROPERTY, new String[] {"\\.", "\\\\"});
        }
    };

    /**
     * {@inheritDoc}
     * 
     * @see EntityReferenceSerializer#serialize(org.xwiki.model.reference.EntityReference, Object...)
     */
    public String serialize(EntityReference reference, Object... parameters)
    {
        if (reference == null) {
            return null;
        }

        EntityReference currentReference = reference.getRoot();
        StringBuilder representation = new StringBuilder();

        // While we still have children and they're not the children of the reference to serialize
        while (currentReference != null && currentReference != reference.getChild()) {
            serializeEntityReference(currentReference, representation, currentReference == reference, parameters);
            currentReference = currentReference.getChild();
        }

        return representation.toString();
    }

    /**
     * Serialize a reference element.
     * 
     * @param currentReference the reference to serialize
     * @param representation the builder where to happen the serialized member
     * @param isLastReference indicate if it's the last member of the refence
     * @param parameters optional parameters
     */
    protected void serializeEntityReference(EntityReference currentReference, StringBuilder representation,
        boolean isLastReference, Object... parameters)
    {
        String[] currentEscapeChars = ESCAPES.get(currentReference.getType());

        // If we're on the Root reference then we don't need to escape anything
        if (currentEscapeChars != null) {
            representation.append(StringUtils.replaceEach(currentReference.getName(), currentEscapeChars,
                REPLACEMENTS.get(currentReference.getType())));
        } else {
            representation.append(currentReference.getName().replace("\\", "\\\\"));
        }

        // If the reference is the last one in the chain then don't print the separator char
        if (!isLastReference && currentReference.getChild() != null) {
            String separatorChar = ESCAPES.get(currentReference.getChild().getType())[0];
            representation.append(separatorChar);
        }
    }
}
