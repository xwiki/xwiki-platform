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

/**
 * Represents various Wikis supported by Wiki Importer.
 * 
 * @version $Id$
 */
public class WikiType
{
    public static final WikiType MEDIAWIKI = new WikiType("mediawiki", "MediaWiki");

    public static final WikiType CONFLUENCE = new WikiType("confluence", "Confluence");
    
    public static final WikiType XWIKI=new WikiType("xwiki","XWiki");

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

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return this.name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        // Random number. See http://www.geocities.com/technofundo/tech/java/equalhash.html for the detail of this
        // algorithm.
        // Note that the name isn't part of the hashCode computation since it's not part of the Syntax type's identity
        int hash = 7;
        hash = 31 * hash + (null == getId() ? 0 : getId().hashCode());
        return hash;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object object)
    {
        boolean result;

        // See http://www.geocities.com/technofundo/tech/java/equalhash.html for the detail of this algorithm.
        if (this == object) {
            result = true;
        } else {
            if ((object == null) || (object.getClass() != this.getClass())) {
                result = false;
            } else {
                // Object must be Syntax at this point.
                WikiType syntaxType = (WikiType) object;
                // Note that the name isn't part of the hashCode computation since it's not part of the Syntax type's
                // identity.
                result = (getId() == syntaxType.getId() || (getId() != null && getId().equals(syntaxType.getId())));
            }
        }

        return result;
    }

}
