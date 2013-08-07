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
package org.xwiki.wikistream.type;

import org.apache.commons.lang3.ObjectUtils;
import org.xwiki.stability.Unstable;

/**
 * Represents various Wikis supported by Wiki Importer.
 * 
 * @version $Id$
 */
@Unstable
public class WikiType
{
    public static final WikiType MEDIAWIKI = new WikiType("mediawiki", "MediaWiki");

    public static final WikiType CONFLUENCE = new WikiType("confluence", "Confluence");

    public static final WikiType XWIKI = new WikiType("xwiki", "XWiki");

    /**
     * Generic WIKI.
     */
    public static final WikiType WIKI = new WikiType("wiki", "Wiki");

    /**
     * Id of a Wiki
     */
    private String id;

    /**
     * Name of a Wiki
     */
    private String name;

    /**
     * @param id of a wiki
     * @param name of a wiki
     */
    public WikiType(String id, String name)
    {
        this.id = id;
        this.name = name;
    }

    /**
     * @return id of the wiki
     */
    public String getId()
    {
        return id;
    }

    /**
     * @return name of the wiki
     */
    public String getName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return this.name;
    }

    @Override
    public int hashCode()
    {
        return getId().hashCode();
    }

    @Override
    public boolean equals(Object object)
    {
        boolean result;

        if (this == object) {
            result = true;
        } else {
            if (object instanceof WikiType) {
                result = ObjectUtils.equals(getId(), ((WikiType) object).getId());
            } else {
                result = false;
            }
        }

        return result;
    }
}
