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
package org.xwiki.rendering.macro.wikibridge;

import java.util.HashMap;
import java.util.Map;

/**
 * The visibility of a Wiki Macro, ie whether it's visible to the current user only, to the current wiki only
 * or globally.
 *
 * @version $Id$
 * @since 2.2M1
 */
public enum WikiMacroVisibility
{
    /**
     * Macro visible only by the current user.
     */
    USER,

    /**
     * Macro visible only for the current wiki.
     */
    WIKI,

    /**
     * Macro visible for all wikis in a farm. 
     */
    GLOBAL;

    /**
     * Mapping between String definition of visibility and enums. The strings defined are coming from the
     * Wiki Macro Class field definition for the Visibility property.
     */
    private static final Map<String, WikiMacroVisibility> MAPPINGS = new HashMap<String, WikiMacroVisibility>()
    {
        {
            put("Current User", USER);
            put("Current Wiki", WIKI);
            put("Global", GLOBAL);
        }
    };

    /**
     * Convert between a string representation of a Macro visibility and its matching enum. If no matching enum is
     * found then defaults to Wiki level visibility.
     *
     * @param visibilityAsString the visibility as a string
     * @return the enum matching the visibility defined as a string
     */
    public static WikiMacroVisibility fromString(String visibilityAsString)
    {
        WikiMacroVisibility visibility = WIKI;
        if (MAPPINGS.containsKey(visibilityAsString)) {
            visibility = MAPPINGS.get(visibilityAsString);
        }
        return visibility;
    }
}
