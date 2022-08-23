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

import java.io.Serializable;
import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.EntityType;

/**
 * The character symbols used to represent an {@link org.xwiki.model.reference.EntityReference} as a String.
 *
 * @version $Id$
 * @since 8.1M2
 */
@Role
public interface SymbolScheme
{
    /**
     * @return the character used for escaping characters in Entity Reference names
     */
    Character getEscapeSymbol();

    /**
     * @return the separator used to separate the entity type from the reference
     * @since 14.8RC1
     */
    default Character getEntityTypeSeparator()
    {
        return ':';
    }

    /**
     * @return the map containing all the symbols to separate Entity Types. The map's key is an Entity Type and the
     *         value is another map who's key is the second Entity Type and its value is the character separating the 2
     *         Entity Types. For example you could have the {@code .} character separating an
     *         {@link EntityType#DOCUMENT} and a {@link EntityType#SPACE}. Note that a given Entity Type can have
     *         several separator characters if it can have several different parent types (e.g. a Space reference can
     *         have either a Space Entity Type or a Wiki Entity Type)
     */
    Map<EntityType, Map<EntityType, Character>> getSeparatorSymbols();

    /**
     * @param type the Entity Type for which to get the list of strings to escape. For example for a SPACE Entity type
     *            you could want to escape {@code .}, {@code :} and {@code \}.
     * @return the various strings that require escaping for the passed Entity type
     */
    String[] getSymbolsRequiringEscapes(EntityType type);

    /**
     * @param type the Entity Type for which to get the list of strings to use to replace each escape returned by
     *            {@link #getSymbolsRequiringEscapes(EntityType)}. For example for a SPACE Entity type you could want to
     *            replace {@code .} with {@code \.}, {@code :} with {@code \:} and {@code \} with {@code \\}
     * @return the various replacement strings to replace string that require escaping
     */
    String[] getReplacementSymbols(EntityType type);

    /**
     * @param type the Entity Type for which to get the parameters separator
     * @return the {@link Character} used to mark where parameters or an entity reference element starts or null if the
     *         passed {@link EntityType} does not support parameters
     * @since 10.6RC1
     */
    Character getParameterSeparator(EntityType type);

    /**
     * @param type the Entity Type for which to get the default parameter
     * @return the name of the default parameter which will be optional when serializing/unserializing parameters for
     *         this entity type
     * @since 10.6RC1
     */
    String getDefaultParameter(EntityType type);

    /**
     * @param type the Entity Type for which to get the list of strings to escape. For example for a SPACE Entity type
     *            you could want to escape {@code .}, {@code :} and {@code \}.
     * @return the various strings that require escaping for the passed Entity type
     */
    String[] getParameterSymbolsRequiringEscapes(EntityType type);

    /**
     * @param type the Entity Type for which to get the list of strings to use to replace each escape returned by
     *            {@link #getSymbolsRequiringEscapes(EntityType)}. For example for a SPACE Entity type you could want to
     *            replace {@code .} with {@code \.}, {@code :} with {@code \:} and {@code \} with {@code \\}
     * @return the various replacement strings to replace string that require escaping
     */
    String[] getParameterReplacementSymbols(EntityType type);

    /**
     * Resolve a parameter into its expected Java type.
     * <p>
     * {@link org.xwiki.model.reference.EntityReference} parameters can have various types and some are expected to have
     * very specific types so we need to convert them when they are parsed from {@link String} when resolving a
     * reference. A very common example is the parameter "locale" in DocumentReference and PageReference which is
     * expected to be of type Locale.
     * 
     * @param parameter the name of the parameter
     * @param value the String value of the parameter
     * @return the parameter converted in the expected type
     * @since 14.8RC1
     */
    default Serializable resolveParameter(String parameter, String value)
    {
        return value;
    }

    /**
     * @param type the Entity Type for which to get the current reference keyword or null if not supported
     * @return the {@link String} used to indicate current reference (for example ".")
     */
    String getCurrentReferenceKeyword(EntityType type);

    /**
     * @param type the Entity Type for which to get the parent reference keyword or null if not supported
     * @return the {@link String} used to indicate parent reference (for example "..")
     */
    String getParentReferenceKeyword(EntityType type);
}
