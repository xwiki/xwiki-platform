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

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.xwiki.model.EntityType;

/**
 * Some constants related to {@link org.xwiki.model.reference.EntityReference}.
 *
 * @version $Id$
 * @since 8.1M2
 */
// Old interface not describing a type, hard to remove for backward-compatibility reasons.
@SuppressWarnings("checkstyle:InterfaceIsType")
public interface EntityReferenceConstants
{
    /**
     * The hierarchy of Entity Types.
     *
     * @deprecated use {@link EntityType#getAllowedParents()} instead
     */
    @Deprecated(since = "10.6RC1")
    Map<EntityType, List<EntityType>> PARENT_TYPES = createParentTypes();

    /**
     * Creates the parent types map.
     *
     * @return the parent types map
     */
    private static Map<EntityType, List<EntityType>> createParentTypes()
    {
        Map<EntityType, List<EntityType>> map = new EnumMap<>(EntityType.class);
        for (EntityType type : EntityType.values()) {
            map.put(type, type.getAllowedParents());
        }
        return map;
    }
}
