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
package org.xwiki.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Represents a type of entity (ie a Model Object such as a Wiki, a Space, a Document, an Attachment, etc).
 * 
 * @version $Id$
 * @since 2.2M1
 */
public enum EntityType
{
    // Note that order below is important since it creates an order.
    // For example: EntityType.WIKI.ordinal() < EntityType.SPACE.ordinal()

    /**
     * Represents a Wiki Entity.
     */
    WIKI,

    // Documents

    /**
     * Represents a Space Entity.
     */
    SPACE(WIKI, null),

    /**
     * Represents a Document Entity.
     */
    DOCUMENT(SPACE),

    /**
     * Represents an Attachment Entity.
     */
    ATTACHMENT(DOCUMENT),

    /**
     * Represents an Object Entity.
     */
    OBJECT(DOCUMENT),

    /**
     * Represents an Object Property Entity.
     */
    OBJECT_PROPERTY(OBJECT),

    /**
     * Represents a class property entity.
     * 
     * @since 3.2M1
     */
    CLASS_PROPERTY(DOCUMENT),

    /**
     * Represents a structured part of the content of a document or an object property.
     * 
     * @since 6.0M1
     */
    BLOCK(DOCUMENT, OBJECT_PROPERTY),

    // Pages

    /**
     * Represents a Page Entity.
     * 
     * @since 10.6RC1
     */
    PAGE(WIKI, (EntityType) null),

    /**
     * Represents an Attachment Entity in a page.
     * 
     * @since 10.6RC1
     */
    PAGE_ATTACHMENT(PAGE),

    /**
     * Represents an Object Entity in a page.
     * 
     * @since 10.6RC1
     */
    PAGE_OBJECT(PAGE),

    /**
     * Represents an Object Property Entity in a page.
     * 
     * @since 10.6RC1
     */
    PAGE_OBJECT_PROPERTY(PAGE_OBJECT),

    /**
     * Represents a class property entity in a page.
     * 
     * @since 10.6RC1
     */
    PAGE_CLASS_PROPERTY(PAGE);

    // TODO: should probably introduce a PAGE_BLOCK when we decide how we want it (we might want to move a two types or
    // decide to have BLOCK being parent of BLOCK, etc.)

    /**
     * The lower case String version of the enum.
     */
    private final String lowerCase;

    private final List<EntityType> allowedParents;

    EntityType(EntityType... allowedParents)
    {
        this.lowerCase = name().toLowerCase(Locale.US);

        List<EntityType> list = new ArrayList<>(allowedParents.length);
        for (EntityType parent : allowedParents) {
            list.add(parent != null ? parent : this);
        }
        this.allowedParents = Collections.unmodifiableList(list);
    }

    /**
     * @return the lower case String version of the enum
     * @since 6.2.1
     */
    public String getLowerCase()
    {
        return this.lowerCase;
    }

    /**
     * @return the list of allowed parent for this entity type
     * @since 10.6RC1
     */
    public List<EntityType> getAllowedParents()
    {
        return this.allowedParents;
    }
}
