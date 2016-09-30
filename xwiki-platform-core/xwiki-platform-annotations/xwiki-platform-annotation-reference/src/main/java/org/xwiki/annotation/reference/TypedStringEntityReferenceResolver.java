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
package org.xwiki.annotation.reference;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

/**
 * Typed version of the entity reference resolver from string representations which gets its type from the string
 * serialization, in the form {@code type://} and, if no such type specification is found, it uses the default passed
 * type. <br>
 * For example, something like: {@code DOCUMENT://XWiki.TagClass[0]#tags} will result in parsing {@code
 * XWiki.TagClass[0]#tags} as a document reference, while {OBJECT_PROPERTY://XWiki.TagClass[0]#tags} will result in
 * parsing {@code XWiki.TagClass[0]#tags} as an object property reference.<br>
 * Note that, although it roughly does the same thing, this is a different hierarchy than EntityReferenceResolver
 * because it's a different strategy, different interpretation of the type parameter and resolvers and serializers of
 * this type should be used together.
 * 
 * @version $Id$
 * @since 2.3M1
 */
@Role
public interface TypedStringEntityReferenceResolver
{
    /**
     * @param entityReferenceRepresentation representation of the entity, with or without a type (e.g. {@code
     *            DOCUMENT://wiki:Space.Page} or {@code wiki:Space.WebHome})
     * @param type the default type to be used if none is specified in the serialization
     * @return the resolved entity reference
     */
    EntityReference resolve(String entityReferenceRepresentation, EntityType type);
}
