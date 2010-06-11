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
package org.xwiki.rendering.syntax;

/**
 * @version $Id$
 * @since 2.0RC1
 */
public class SyntaxType
{
    public static final SyntaxType XWIKI = new SyntaxType("xwiki", "XWiki");

    public static final SyntaxType CONFLUENCE = new SyntaxType("confluence", "Confluence");

    public static final SyntaxType MEDIAWIKI = new SyntaxType("mediawiki", "MediaWiki");

    public static final SyntaxType CREOLE = new SyntaxType("creole", "Creole");

    public static final SyntaxType JSPWIKI = new SyntaxType("jspwiki", "JSPWiki");

    public static final SyntaxType TWIKI = new SyntaxType("twiki", "TWiki");

    public static final SyntaxType XHTML = new SyntaxType("xhtml", "XHTML");

    public static final SyntaxType ANNOTATED_XHTML = new SyntaxType("annotatedxhtml", "Annotated XHTML");

    public static final SyntaxType HTML = new SyntaxType("html", "HTML");

    public static final SyntaxType PLAIN = new SyntaxType("plain", "Plain");

    public static final SyntaxType EVENT = new SyntaxType("event", "Event");

    public static final SyntaxType TEX = new SyntaxType("tex", "TeX");

    /**
     * @see #getName()
     */
    private String name;

    /**
     * @see #getId()
     */
    private String id;

    /**
     * @param id the technical id of the Syntax type (ex "annotatedxhtml")
     * @param name the human readable name of the Syntax type (ex "Annotated XHTML")
     * @since 2.0M3
     */
    public SyntaxType(String id, String name)
    {
        this.name = name;
        this.id = id;
    }

    /**
     * @return the technical id of the Syntax type (ex "annotatedxhtml")
     * @since 2.0M3
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * @return the human readable name of the Syntax type (ex "Annotated XHTML")
     * @since 2.0M3
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Display a human readable name of the Syntax type.
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return this.name;
    }

    /**
     * @return the technical id fo the Syntax type
     * @deprecated starting with 2.0M3 use {@link #getId()} instead
     */
    public String toIdString()
    {
        return this.id;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        // Random number. See http://www.technofundo.com/tech/java/equalhash.html for the detail of this
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

        // See http://www.technofundo.com/tech/java/equalhash.html for the detail of this algorithm.
        if (this == object) {
            result = true;
        } else {
            if ((object == null) || (object.getClass() != this.getClass())) {
                result = false;
            } else {
                // Object must be Syntax at this point.
                SyntaxType syntaxType = (SyntaxType) object;
                // Note that the name isn't part of the hashCode computation since it's not part of the Syntax type's
                // identity.
                result = (getId() == syntaxType.getId() || (getId() != null && getId().equals(syntaxType.getId())));
            }
        }

        return result;
    }
}
