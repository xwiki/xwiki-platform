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
 * Represents a wiki syntax that the user can use to enter wiki content. A syntax is made of two parts: a type
 * (eg XWiki, Confluence, MediaWiki, etc) and a version (1.0, 2.0, etc). 
 * For example the XWiki 1.0 syntax, the XWiki 2.0 syntax, the Confluence 1.0 syntax, etc.
 * 
 * @version $Id$
 * @since 2.0RC1
 */
public class Syntax
{
    public static final Syntax XHTML_1_0 = new Syntax(SyntaxType.XHTML, "1.0");
    public static final Syntax HTML_4_01 = new Syntax(SyntaxType.HTML, "4.01");
    public static final Syntax XWIKI_1_0 = new Syntax(SyntaxType.XWIKI, "1.0");
    public static final Syntax XWIKI_2_0 = new Syntax(SyntaxType.XWIKI, "2.0");
    public static final Syntax XWIKI_2_1 = new Syntax(SyntaxType.XWIKI, "2.1", "experimental");
    public static final Syntax PLAIN_1_0 = new Syntax(SyntaxType.PLAIN, "1.0");
    public static final Syntax EVENT_1_0 = new Syntax(SyntaxType.EVENT, "1.0");
    public static final Syntax TEX_1_0 = new Syntax(SyntaxType.TEX, "1.0");
    public static final Syntax CREOLE_1_0 = new Syntax(SyntaxType.CREOLE, "1.0");
    public static final Syntax JSPWIKI_1_0 = new Syntax(SyntaxType.JSPWIKI, "1.0");
    public static final Syntax MEDIAWIKI_1_0 = new Syntax(SyntaxType.MEDIAWIKI, "1.0");
    public static final Syntax CONFLUENCE_1_0 = new Syntax(SyntaxType.CONFLUENCE, "1.0");
    public static final Syntax TWIKI_1_0 = new Syntax(SyntaxType.TWIKI, "1.0");

    /**
     * This is HTML with annotations (comments) in order to allow round tripping between for example the WYSIWYG editor
     * and wiki syntax.
     */
    public static final Syntax ANNOTATED_XHTML_1_0 = new Syntax(SyntaxType.ANNOTATED_XHTML, "1.0");

    private SyntaxType type;

    private String version;

    /**
     * Optional free form text that qualifies the version, eg "experimental". 
     */
    private String qualifier;

    public Syntax(SyntaxType type, String version)
    {
        this.type = type;
        this.version = version;
    }

    public Syntax(SyntaxType type, String version, String qualifier)
    {
        this(type, version);
        this.qualifier = qualifier;
    }

    public SyntaxType getType()
    {
        return this.type;
    }

    public String getVersion()
    {
        return this.version;
    }

    public String getQualifier()
    {
        return this.qualifier;
    }

    public String toIdString()
    {
        return getType().getId() + "/" + getVersion().toLowerCase();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Object#toString()
     */
    @Override
    public String toString()
    {
        return getType().toString() + " " + getVersion() + (getQualifier() != null ? " (" + getQualifier() + ")" : "");
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
        int hash = 7;
        hash = 31 * hash + (null == getType() ? 0 : getType().hashCode());
        hash = 31 * hash + (null == getVersion() ? 0 : getVersion().hashCode());
        hash = 31 * hash + (null == getQualifier() ? 0 : getQualifier().hashCode());
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
                // object must be Syntax at this point
                Syntax syntax = (Syntax) object;
                result =
                    (getType() == syntax.getType() || (getType() != null && getType().equals(syntax.getType())))
                        && (getVersion() == syntax.getVersion() || (getVersion() != null && getVersion().equals(
                            syntax.getVersion())))
                        && (getQualifier() == syntax.getQualifier() || (getQualifier() != null && getQualifier().equals(
                            syntax.getQualifier())));
            }
        }
        return result;
    }
}
