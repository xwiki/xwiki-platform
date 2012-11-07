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
package org.xwiki.component.wiki;

import java.util.HashMap;
import java.util.Map;

/**
 * The scope of a {@link WikiComponent}, i.e. whether it's registered for the current user only, for the current wiki
 * only, or for all the wikis.
 *
 * @version $Id$
 * @since 4.3M2
 */
public enum WikiComponentScope
{
    /**
     * Component registered for the current user only.
     */
    USER,

    /**
     * Component registered for the current wiki only.
     */
    WIKI,

    /**
     * Component registered for all wikis in a farm.
     */
    GLOBAL;

    /**
     * Mapping between String definition of scope and enums. The strings defined are coming from the scope property of
     * the Wiki Component Class.
     */
    private static final Map<String, WikiComponentScope> MAPPINGS = new HashMap<String, WikiComponentScope>()
    {
        {
            put("wiki", WIKI);
            put("user", USER);
            put("global", GLOBAL);
        }
    };

    /**
     * Convert between a string representation of a Macro scope and its matching enum. If no matching enum is found then
     * defaults to Wiki level visibility.
     *
     * @param scopeAsString the scope as a string
     * @return the enum matching the scope defined as a string
     */
    public static WikiComponentScope fromString(String scopeAsString)
    {
        WikiComponentScope scope = WIKI;
        if (MAPPINGS.containsKey(scopeAsString)) {
            scope = MAPPINGS.get(scopeAsString);
        }
        return scope;
    }
}
