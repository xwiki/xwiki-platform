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
package org.xwiki.rendering.parser;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @version $Id$
 * @since 1.7M1
 */
public class SyntaxType
{
    public static final SyntaxType XWIKI = getSyntaxType("XWiki");

    public static final SyntaxType CONFLUENCE = getSyntaxType("Confluence");

    public static final SyntaxType MEDIAWIKI = getSyntaxType("MediaWiki");

    public static final SyntaxType CREOLE = getSyntaxType("Creole");

    public static final SyntaxType JSPWIKI = getSyntaxType("JSPWiki");

    public static final SyntaxType TWIKI = getSyntaxType("TWiki");

    public static final SyntaxType XHTML = getSyntaxType("XHTML");

    public static final SyntaxType HTML = getSyntaxType("HTML");

    private static Map<String, SyntaxType> syntaxTypeMap;

    private String id;

    public static SyntaxType getSyntaxType(String id)
    {
        String lowerId = id.toLowerCase();

        if (syntaxTypeMap == null) {
            syntaxTypeMap = new ConcurrentHashMap<String, SyntaxType>();
        }

        SyntaxType syntaxType = syntaxTypeMap.get(lowerId);

        if (syntaxType == null) {
            syntaxType = new SyntaxType(id);
            syntaxTypeMap.put(lowerId, syntaxType);
        }

        return syntaxType;
    }

    private SyntaxType(String id)
    {
        this.id = id;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return this.id;
    }

    public String toIdString()
    {
        return this.id.toLowerCase();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        // no need to call id's equals method as SyntaxType for an id should be unique
        return this == obj;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        // use the default implementation based on internal address of the object as SyntaxType for an id should be
        // unique
        return super.hashCode();
    }
}
