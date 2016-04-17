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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.model.EntityType;

/**
 * Some constants related to {@link org.xwiki.model.reference.EntityReference}.
 *
 * @version $Id$
 * @since 8.1M2
 */
public interface EntityReferenceConstants
{
    /**
     * The hierarchy of Entity Types.
     */
    Map<EntityType, List<EntityType>> PARENT_TYPES = new HashMap<EntityType, List<EntityType>>()
    {
        {
            put(EntityType.ATTACHMENT, Arrays.asList(EntityType.DOCUMENT));
            put(EntityType.DOCUMENT, Arrays.asList(EntityType.SPACE));
            put(EntityType.SPACE, Arrays.asList(EntityType.WIKI, EntityType.SPACE));
            put(EntityType.WIKI, Collections.<EntityType>emptyList());
            put(EntityType.OBJECT, Arrays.asList(EntityType.DOCUMENT));
            put(EntityType.OBJECT_PROPERTY, Arrays.asList(EntityType.OBJECT));
            put(EntityType.CLASS_PROPERTY, Arrays.asList(EntityType.DOCUMENT));
            put(EntityType.BLOCK, Arrays.asList(EntityType.DOCUMENT, EntityType.OBJECT_PROPERTY));
        }
    };
}
