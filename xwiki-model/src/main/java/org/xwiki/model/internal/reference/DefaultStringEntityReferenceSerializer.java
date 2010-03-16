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

import org.apache.commons.lang.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generate a string representation of an entity reference (eg "wiki:space.page" for a document reference in the wiki
 * Wiki, the space Space and the page Page).
 * 
 * @version $Id$
 * @since 2.2M1
 */
@Component
public class DefaultStringEntityReferenceSerializer implements EntityReferenceSerializer<String>
{
    private Map<EntityType, List<String>> escapes = new HashMap<EntityType, List<String>>()
    {
        {
            put(EntityType.ATTACHMENT, Arrays.asList("@"));
            put(EntityType.DOCUMENT, Arrays.asList("."));
            put(EntityType.SPACE, Arrays.asList(":", "."));
            put(EntityType.OBJECT, Arrays.asList("^"));
            put(EntityType.OBJECT_PROPERTY, Arrays.asList("."));
        }
    };

    private Map<EntityType, List<String>> replacements = new HashMap<EntityType, List<String>>()
    {
        {
            put(EntityType.ATTACHMENT, Arrays.asList("\\@"));
            put(EntityType.DOCUMENT, Arrays.asList("\\."));
            put(EntityType.SPACE, Arrays.asList("\\:", "\\."));
            put(EntityType.OBJECT, Arrays.asList("\\^"));
            put(EntityType.OBJECT_PROPERTY, Arrays.asList("\\."));
        }
    };

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.model.reference.EntityReferenceSerializer#serialize(org.xwiki.model.reference.EntityReference,
     *      java.lang.Object[])
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

    protected void serializeEntityReference(EntityReference currentReference, StringBuilder representation,
        boolean isLastReference, Object... parameters)
    {
        List<String> currentEscapeChars = this.escapes.get(currentReference.getType());

        // If we're on the Root reference then we don't need to escape anything
        if (currentEscapeChars != null) {
            representation.append(StringUtils.replaceEach(currentReference.getName(), currentEscapeChars
                .toArray(new String[0]), this.replacements.get(currentReference.getType()).toArray(new String[0])));
        } else {
            representation.append(currentReference.getName());
        }

        // If the reference is the last one in the chain then don't print the separator char
        if (!isLastReference && currentReference.getChild() != null) {
            String separatorChar = this.escapes.get(currentReference.getChild().getType()).get(0);
            representation.append(separatorChar);
        }
    }
}
