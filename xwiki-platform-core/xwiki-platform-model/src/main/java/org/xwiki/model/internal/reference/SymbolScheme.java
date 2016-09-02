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

import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.EntityType;
import org.xwiki.stability.Unstable;

/**
 * The character symbols used to represent an {@link org.xwiki.model.reference.EntityReference} as a String.
 *
 * @version $Id$
 * @since 8.1M2
 */
@Unstable
@Role
public interface SymbolScheme
{
    /**
     * @return the character used for escaping characters in Entity Reference names
     */
    Character getEscapeSymbol();

    /**
     * @return the map containing all the symbols to separate Entity Types. The map's key is an Entity Type and
     *         the value is another map who's key is the second Entity Type and its value is the character separating
     *         the 2 Entity Types. For example you could have the {@code .} character separating an
     *         {@link EntityType#DOCUMENT} and a {@link EntityType#SPACE}. Note that a given Entity Type can have
     *         several separator characters if it can have several different parent types (e.g. a Space reference can
     *         have either a Space Entity Type or a Wiki Entity Type)
     */
    Map<EntityType, Map<EntityType, Character>> getSeparatorSymbols();

    /**
     * @param type the Entity Type for which to get the list of strings to escape. For example for a SPACE Entity type
     *             you could want to escape {@code .}, {@code :} and {@code \}.
     * @return the various strings that require escaping for the passed Entity type
     */
    String[] getSymbolsRequiringEscapes(EntityType type);

    /**
     * @param type the Entity Type for which to get the list of strings to use to replace each escape returned by
     *             {@link #getSymbolsRequiringEscapes(EntityType)}. For example for a SPACE Entity type you could
     *             want to replace {@code .} with {@code \.}, {@code :} with  {@code \:} and {@code \} with {@code \\}
     * @return the various replacement strings to replace string that require escaping
     */
    String[] getReplacementSymbols(EntityType type);
}
