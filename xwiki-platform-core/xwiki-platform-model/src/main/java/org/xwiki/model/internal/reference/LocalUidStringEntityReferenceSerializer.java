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

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import static org.xwiki.model.internal.reference.StringReferenceSeparators.WIKISEP;

/**
 * Serialize a reference into a unique identifier string within a wiki. Its similar to the
 * {@link UidStringEntityReferenceSerializer}, but is made appropriate for a wiki independent storage.
 *
 * ie: The string created looks like 5:space3:doc for the wiki:space.doc document.
 * and 5:space3:doc15:xspace.class[0] for the wiki:space.doc^wiki:xspace.class[0] object.
 *
 * @version $Id$
 * @since 4.0M1
 * @see UidStringEntityReferenceSerializer
 */
@Component
@Named("local/uid")
@Singleton
public class LocalUidStringEntityReferenceSerializer implements EntityReferenceSerializer<String>
{
    @Override
    public String serialize(EntityReference reference, Object... parameters)
    {
        if (reference == null) {
            return null;
        }

        StringBuilder representation = new StringBuilder();
        List<EntityReference> references = reference.getReversedReferenceChain();
        EntityReference wikiReference = references.get(0);
        if (wikiReference.getType() == EntityType.WIKI) {
            references.remove(0);
        } else {
            wikiReference = null;
        }

        for (EntityReference currentReference : references) {
            serializeEntityReference(currentReference, representation, wikiReference, parameters);
        }

        return representation.toString();
    }

    /**
     * Serialize a single reference element into the representation string builder.
     *
     * @param currentReference the reference to serialize
     * @param representation the builder where to happen the serialized member
     * @param wikiReference the wiki reference of this entity reference
     * @param parameters optional parameters
     */
    protected void serializeEntityReference(EntityReference currentReference, StringBuilder representation,
        EntityReference wikiReference, Object... parameters)
    {
        String name = currentReference.getName();

        // FIXME: Not really nice to parse here the serialized XClass reference to remove its wiki name when local.
        // This also why this is not a simple derived class of UidStringEntityReferenceSerializer.
        if (wikiReference != null && currentReference.getType() == EntityType.OBJECT) {
            if (name.startsWith(wikiReference.getName() + WIKISEP)) {
                name = name.substring(wikiReference.getName().length() + 1);
            }
        }
        representation.append(name.length()).append(':').append(name);
    }
}
