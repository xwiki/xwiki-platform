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
package org.xwiki.annotation.reference.internal;

import org.xwiki.annotation.reference.TypedStringEntityReferenceResolver;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;

/**
 * Resolves references serialized as string that gets its type from the string serialization, in the form {@code
 * type://} and, if no such type specification is found, it uses the default passed type. <br>
 * For example, something like: {@code DOCUMENT://XWiki.TagClass[0]#tags} will result in parsing {@code
 * XWiki.TagClass[0]#tags} as a document reference, while {OBJECT_PROPERTY://XWiki.TagClass[0]#tags} will result in
 * parsing {@code XWiki.TagClass[0]#tags} as an object property reference.
 *
 * @version $Id$
 * @since 2.3M1
 */
public abstract class AbstractTypedStringEntityReferenceResolver implements TypedStringEntityReferenceResolver
{
    /**
     * {@inheritDoc}
     * <p>
     * Override to allow detecting the type of the reference from the prefix specified in the beginning of the
     * serialized reference (e.g. DOCUMENT://Page). In case no type is specified, the second part parameter will be used
     * as the default type.
     * </p>
     *
     * @see TypedStringEntityReferenceResolver#resolve(String, org.xwiki.model.EntityType)
     */
    @Override
    public EntityReference resolve(String entityReferenceRepresentation, EntityType defaultType)
    {
        String refToParse = entityReferenceRepresentation;
        EntityType type = defaultType;
        // get the type specified at the beginning
        EntityType specifiedType = getSerializedType(refToParse);
        // if such type is specified
        if (specifiedType != null) {
            // this will be used to parse...
            type = specifiedType;
            // ...what's left of the reference after the protocol part
            refToParse = refToParse.substring(type.toString().length() + 3);
        }

        // use the super resolver to resolve the stripped reference and the resolved type
        return getResolver().resolve(refToParse, type);
    }

    /**
     * Helper function to get the type of the serialized entity from its serialization as a protocol at the beginning of
     * the serialization (e.g. DOCUMENT://Page).
     *
     * @param entityReferenceRepresentation the string representation of the entity to resolve
     * @return the entity type specified as a prefix or null if no entity such exists
     */
    private EntityType getSerializedType(String entityReferenceRepresentation)
    {
        if (entityReferenceRepresentation != null) {
            for (EntityType type : EntityType.values()) {
                if (entityReferenceRepresentation.startsWith(type + "://")) {
                    return type;
                }
            }
        }
        return null;
    }

    /**
     * @return the resolver to be used by this typed resolver to delegate resolving the actual reference, for the
     *         extracted type. Override to apply a specific strategy for the missing fields, for example.
     */
    protected abstract EntityReferenceResolver<String> getResolver();
}
