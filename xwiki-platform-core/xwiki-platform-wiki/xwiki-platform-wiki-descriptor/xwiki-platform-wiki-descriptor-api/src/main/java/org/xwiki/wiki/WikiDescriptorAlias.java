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
package org.xwiki.wiki;

import org.xwiki.stability.Unstable;

/**
 * Defines an alias to a wiki (ie the name by which a wiki can be called in a URL). A wiki can have 1 or more aliases.
 *
 * @version $Id$
 * @since 5.3M1
 */
@Unstable
public class WikiDescriptorAlias
{
    /**
     * @see #getWikiAlias()
     */
    private String wikiAlias;

    /**
     * @param wikiAlias see {@link #getWikiAlias()}
     */
    public WikiDescriptorAlias(String wikiAlias)
    {
        this.wikiAlias = wikiAlias;
    }

    /**
     * @return another name under which the wiki is known and can be addressed in URLs
     */
    public String getWikiAlias()
    {
        return this.wikiAlias;
    }
}
