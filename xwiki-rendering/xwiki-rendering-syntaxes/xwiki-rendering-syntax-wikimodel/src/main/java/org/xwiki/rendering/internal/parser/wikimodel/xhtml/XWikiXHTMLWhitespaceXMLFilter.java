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
package org.xwiki.rendering.internal.parser.wikimodel.xhtml;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.wikimodel.wem.xhtml.filter.XHTMLWhitespaceXMLFilter;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

/**
 * Extension to the WikiModel {@link XHTMLWhitespaceXMLFilter} to support both the ability to not remove spaces inside
 * XHTML elements containing wiki syntax (we only trim leading and trailing spaces) and to handle XWiki special XHTML
 * comment placeholders (for recognizing links, images, etc).
 * 
 * @version $Id$
 * @since 2.1RC1
 */
public class XWikiXHTMLWhitespaceXMLFilter extends XHTMLWhitespaceXMLFilter
{
    /**
     * The SAX property controlling whether XHTML elements can contain wiki syntax or not. This controls the whitespace
     * stripping behavior. 
     */
    public static final String SAX_CONTAINS_WIKI_SYNTAX_PROPERTY = 
        "http://xwiki.org/sax/properties/contains-wiki-syntax";

    /**
     * The leading and trailing white spaces matching pattern.
     */
    private static final Pattern HTML_WHITESPACE_BOUNDARIES_PATTERN = Pattern.compile("^\\s+|\\s+$");

    /**
     * Indicate if the element can contain wiki syntax.
     */
    private boolean containsWikiSyntax;

    /**
     * {@inheritDoc}
     * 
     * @see XHTMLWhitespaceXMLFilter#XHTMLWhitespaceXMLFilter(XMLReader)
     */
    public XWikiXHTMLWhitespaceXMLFilter(XMLReader reader)
    {
        super(reader);
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see XHTMLWhitespaceXMLFilter#setProperty(String, Object)
     */
    @Override
    public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException
    {
        if (SAX_CONTAINS_WIKI_SYNTAX_PROPERTY.equalsIgnoreCase(name)) {
            this.containsWikiSyntax = (Boolean) value;
        } else {
            super.setProperty(name, value);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see XHTMLWhitespaceXMLFilter#endCDATA()
     */
    @Override
    public void endCDATA() throws SAXException
    {
        if (getContent().length() > 0) {
            if (this.containsWikiSyntax) {
                // Make sure we clean head/trail white spaces
                trimLeadingWhiteSpaces();
                trimTrailingWhiteSpaces();
            }
        }
        super.endCDATA();
    }

    /**
     * {@inheritDoc}
     * 
     * @see XHTMLWhitespaceXMLFilter#shouldRemoveWhiteSpaces()
     */
    @Override
    protected boolean shouldRemoveWhiteSpaces()
    {
        // Always remove leading/trailing white spaces if we're in wiki mode even if we're inside CDATA and PRE
        // elements.
        return this.containsWikiSyntax || super.shouldRemoveWhiteSpaces();
    }

    /**
     * {@inheritDoc}
     * 
     * @see XHTMLWhitespaceXMLFilter#cleanContentExtraWhiteSpaces()
     */
    @Override
    protected void cleanContentExtraWhiteSpaces()
    {
        // If the element texts can contain wiki syntax only clean whitespaces at beginning and end of texts.
        if (this.containsWikiSyntax) {
            if (getContent().length() > 0) {
                Matcher matcher = HTML_WHITESPACE_BOUNDARIES_PATTERN.matcher(getContent());
                String result = matcher.replaceAll(" ");
                getContent().setLength(0);
                getContent().append(result);
            }
        } else {
            super.cleanContentExtraWhiteSpaces();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see XHTMLWhitespaceXMLFilter#isSemanticComment(String)
     */
    @Override
    protected boolean isSemanticComment(String comment)
    {
        if (super.isSemanticComment(comment)) {
            return true;
        }

        return comment.startsWith("startwikilink:") || comment.startsWith("stopwikilink")
            || comment.startsWith("startimage:") || comment.startsWith("stopimage");
    }
}
