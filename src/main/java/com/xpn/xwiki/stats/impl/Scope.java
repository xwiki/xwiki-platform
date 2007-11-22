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
 *
 */

package com.xpn.xwiki.stats.impl;

/**
 * Immutable scope for retrieving statistics
 */
public class Scope
{
    public static final int PAGE_SCOPE = 1;

    public static final int SPACE_SCOPE = 2;

    public static final int WIKI_SCOPE = 3;

    public static final int GLOBAL_SCOPE = 4;

    private int type;

    private String name;

    private boolean deep;

    public Scope(int type, String name, boolean deep)
    {
        this.type = type;
        this.name = name;
        this.deep = deep;
    }

    public int getType()
    {
        return type;
    }

    public String getName()
    {
        return name;
    }

    public boolean isDeep()
    {
        return deep;
    }

    public String getPattern()
    {
        switch (type) {
            case PAGE_SCOPE:
                return getPagePattern();
            case SPACE_SCOPE:
                return getSpacePattern();
            default:
                return getGlobalPattern();
        }
    }

    private String getPagePattern()
    {
        // ignore deep
        if ("".equals(name)) {
            // a pattern to match any page name
            return "%.%";
        }
        return name;
    }

    private String getSpacePattern()
    {
        if ("".equals(name)) {
            // TODO a pattern to match any space name
            return null;
        } else if (deep) {
            return name + ".%";
        }
        return name;
    }

    private String getGlobalPattern()
    {
        return "";
    }
}
