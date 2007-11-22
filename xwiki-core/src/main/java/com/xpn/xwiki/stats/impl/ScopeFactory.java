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
 * Helper factory class for creating Scope objects in velocity
 */
public class ScopeFactory
{
    private static final ScopeFactory instance = new ScopeFactory();

    public static final Scope ALL_PAGES = createScope(Scope.PAGE_SCOPE, "", false);

    public static final Scope ALL_SPACES = createScope(Scope.SPACE_SCOPE, "", false);

    public static final Scope ALL_WIKIS = createScope(Scope.WIKI_SCOPE, "", false);

    public static final Scope ALL = createScope(Scope.GLOBAL_SCOPE, "", false);

    private ScopeFactory()
    {
    }

    public static ScopeFactory getInstance()
    {
        return instance;
    }

    public static Scope createScope(int type, String name, boolean deep)
    {
        return new Scope(type, name, deep);
    }

    public static Scope createPageScope(String pageName)
    {
        return createScope(Scope.PAGE_SCOPE, pageName, false);
    }

    public static Scope createSpaceScope(String spaceName)
    {
        return createSpaceScope(spaceName, true);
    }

    public static Scope createSpaceScope(String spaceName, boolean deep)
    {
        return createScope(Scope.SPACE_SCOPE, spaceName, deep);
    }

    public static Scope createWikiScope(String wikiName)
    {
        return createWikiScope(wikiName, true);
    }

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
