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
 * Helper factory class for creating Scope objects in velocity
 */
public class ScopeFactory
{
    /**
     * A scope that matches all pages within any space and any wiki
     */
    public static final Scope ALL_PAGES = createScope(Scope.PAGE_SCOPE, "", false);

    /**
     * A scope that matches all spaces within any wiki
     */
    public static final Scope ALL_SPACES = createScope(Scope.SPACE_SCOPE, "", false);

    /**
     * A scope that matches all wikis within the application
     */
    public static final Scope ALL_WIKIS = createScope(Scope.WIKI_SCOPE, "", false);

    /**
     * A scope that matches the entire application as a unit
     */
    public static final Scope ALL = createScope(Scope.GLOBAL_SCOPE, "", false);

    public ScopeFactory()
    {
    }

    /**
     * @see Scope#Scope(int, String, boolean)
     */
    public static Scope createScope(int type, String name, boolean deep)
    {
        return new Scope(type, name, deep);
    }

    /**
     * Creates a new scope associated with the specified page.
     * 
     * @param pageName A page name
     * @return A new Scope instance
     */
    public static Scope createPageScope(String pageName)
    {
        return createScope(Scope.PAGE_SCOPE, pageName, false);
    }

    /**
     * Creates a new scope associated with the specified space and matching all its pages.
     * 
     * @param spaceName A space name
     * @return A new Scope instance
     */
    public static Scope createSpaceScope(String spaceName)
    {
        return createSpaceScope(spaceName, true);
    }

    /**
     * Creates a new scope associated with the specified space.
     * 
     * @param spaceName A space name
     * @param deep <code>true</code> for matching all its pages; <code>false</code> for matching this space as a unit
     * @return A new Scope instance
     */
    public static Scope createSpaceScope(String spaceName, boolean deep)
    {
        return createScope(Scope.SPACE_SCOPE, spaceName, deep);
    }

    /**
     * Creates a new scope associated with the specified wiki and matching all its spaces.
     * 
     * @param wikiName A wiki name
     * @return A new Scope instance
     */
    public static Scope createWikiScope(String wikiName)
    {
        return createWikiScope(wikiName, true);
    }

    /**
     * Creates a new scope associated with the specified wiki.
     * 
     * @param wikiName A wiki name
     * @param deep <code>true</code> for matching all its spaces; <code>false</code> for matching this wiki as a unit
     * @return A new Scope instance
     */
    public static Scope createWikiScope(String wikiName, boolean deep)
    {
        return createScope(Scope.WIKI_SCOPE, wikiName, deep);
    }

    /**
     * Helper method for accessing {@link #ALL_PAGES} static field in velocity
     */
    public static Scope getALL_PAGES()
    {
        return ALL_PAGES;
    }

    /**
     * Helper method for accessing {@link #ALL_SPACES} static field in velocity
     */
    public static Scope getALL_SPACES()
    {
        return ALL_SPACES;
    }

    /**
     * Helper method for accessing {@link #ALL_WIKIS} static field in velocity
     */
    public static Scope getALL_WIKIS()
    {
        return ALL_WIKIS;
    }

    /**
     * Helper method for accessing {@link #ALL} static field in velocity
     */
    public static Scope getALL()
    {
        return ALL;
    }
}
