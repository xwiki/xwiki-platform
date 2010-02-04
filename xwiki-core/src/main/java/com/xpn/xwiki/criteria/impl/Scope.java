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

package com.xpn.xwiki.criteria.impl;

/**
 * Immutable scope for retrieving statistics. A scope is associated with a single document but it can match any number
 * of documents. Here, a document can be a page, a space, a wiki or the entire application as a unit. For instance, a
 * scope associated with a space can match all the pages within that space.
 */
public class Scope
{
    /**
     * Any scope that is associated with a page
     */
    public static final int PAGE_SCOPE = 1;

    /**
     * Any scope that is associated with a space
     */
    public static final int SPACE_SCOPE = 2;

    /**
     * Any scope that is associated with a wiki
     */
    public static final int WIKI_SCOPE = 3;

    /**
     * The scope that is associated with the entire application as a unit
     */
    public static final int GLOBAL_SCOPE = 4;

    /**
     * The type of the scope. It can be {@link #PAGE_SCOPE}, {@link #SPACE_SCOPE}, {@link #WIKI_SCOPE} or
     * {@link #GLOBAL_SCOPE}
     */
    private int type;

    /**
     * Depending on the scope type it can mean:
     * <ul>
     * <li>the name of the page associated with this scope, for {@link #PAGE_SCOPE}</li>
     * <li>the name of the space associated with this scope, for {@link #SPACE_SCOPE}</li>
     * <li>the name of the wiki associated with this scope, for {@link #WIKI_SCOPE}</li>
     * <li>empty string, for {@link #GLOBAL_SCOPE}</li>
     * </ul>
     */
    private String name;

    /**
     * Specifies whether the document given by the {@link #name} field should be considered as a unit or not. When
     * {@link #deep} is <code>false</code> the scope matches only the document with the given {@link #name} (taken as a
     * unit). Otherwise the scope matches all its sub documents (like all pages within a space).
     */
    private boolean deep;

    /**
     * Creates a new Scope instance with the specified field values.
     * 
     * @param type The type of the scope
     * @param name The name of the document associated with this scope
     * @param deep <code>true</code> for matching all sub documents; <code>false</code> for matching the associated
     *            document as a unit
     */
    public Scope(int type, String name, boolean deep)
    {
        this.type = type;
        this.name = name;
        this.deep = deep;
    }

    /**
     * @see #type
     */
    public int getType()
    {
        return this.type;
    }

    /**
     * @see #name
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * @see #deep
     */
    public boolean isDeep()
    {
        return this.deep;
    }

    /**
     * @return The pattern used for matching document names in the database
     */
    public String getPattern()
    {
        switch (this.type) {
            case PAGE_SCOPE:
                return getPagePattern();
            case SPACE_SCOPE:
                return getSpacePattern();
            default:
                return getGlobalPattern();
        }
    }

    /**
     * @return The pattern used in the case of a {@link #PAGE_SCOPE}
     * @see #getPattern()
     */
    private String getPagePattern()
    {
        // ignore deep
        if ("".equals(this.name)) {
            // a pattern to match any page name
            return "%.%";
        }
        return this.name;
    }

    /**
     * @return The pattern used in the case of a {@link #SPACE_SCOPE}
     * @see #getPattern()
     */
    private String getSpacePattern()
    {
        if ("".equals(this.name)) {
            // TODO a pattern to match any space name
            return null;
        } else if (this.deep) {
            return this.name + ".%";
        }
        return this.name;
    }

    /**
     * @return The pattern used in the case of a {@link #GLOBAL_SCOPE}
     * @see #getPattern()
     */
    private String getGlobalPattern()
    {
        return "";
    }
}
