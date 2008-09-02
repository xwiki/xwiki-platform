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
package org.xwiki.rendering.renderer;

import java.util.Map;

import org.xwiki.rendering.internal.renderer.XWikiMacroPrinter;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.SectionLevel;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.Format;

/**
 * Generates XWiki Syntax from {@link org.xwiki.rendering.block.XDOM}. This is useful for example to convert other wiki
 * syntaxes to the XWiki syntax. It's also useful in our tests to verify that round tripping from XWiki Syntax to the
 * DOM and back to XWiki Syntax generates the same content as the initial syntax.
 * 
 * @version $Id$
 * @since 1.5M2
 */
public class XWikiSyntaxRenderer extends AbstractPrintRenderer
{
    /**
     * Top level elements.
     */
    private enum Element {
        DOCUMENT, PARAGRAPH, HORIZONTALLINE, LIST, MACRO, SECTION
    }

    private StringBuffer listStyle = new StringBuffer();

    private boolean needsNewLine = false;

    private Element currentElement;
    
    private boolean isInsideMacroMarker = false;

    private boolean isBeginListItemFound = false;

    private boolean isEndListItemFound = false;

    private int listDepth = 0;

    private XWikiMacroPrinter macroPrinter;

    public XWikiSyntaxRenderer(WikiPrinter printer)
    {
        super(printer);
        this.macroPrinter = new XWikiMacroPrinter();
    }

    /**
     * {@inheritDoc}
     * @see PrintRenderer#beginDocument()
     */
    public void beginDocument()
    {
        this.currentElement = Element.DOCUMENT;
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.renderer.PrintRenderer#endDocument() 
     */
    public void endDocument()
    {
        // Don't do anything
    }

    /**
     * {@inheritDoc}
     * @see PrintRenderer#onLink(org.xwiki.rendering.listener.Link)
     */
    public void onLink(Link link)
    {
        print("[[");
        if (link.getLabel() != null) {
            print(link.getLabel());
            print(">");
        }
        print(link.getReference());
        if (link.getAnchor() != null) {
            print("#");
            print(link.getAnchor());
        }
        if (link.getQueryString() != null) {
            print("?");
            print(link.getQueryString());
        }
        if (link.getInterWikiAlias() != null) {
            print("@");
            print(link.getInterWikiAlias());
        }
        if (link.getTarget() != null) {
            print(">");
            print(link.getTarget());
        }
        print("]]");
    }

    /**
     * {@inheritDoc}
     *
     * @see Renderer#beginFormat(org.xwiki.rendering.listener.Format)
     */
    public void beginFormat(Format format)
    {
        switch(format)
        {
            case BOLD:
                print("**");
                break;
            case ITALIC:
                print("~~");
                break;
            case STRIKEDOUT:
                print("--");
                break;
            case UNDERLINED:
                print("__");
                break;
            case SUPERSCRIPT:
                print("^^");
                break;
            case SUBSCRIPT:
                print(",,");
                break;
            case MONOSPACE:
                print("##");
                break;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see Renderer#endFormat(org.xwiki.rendering.listener.Format)
     */
    public void endFormat(Format format)
    {
        switch(format)
        {
            case BOLD:
                print("**");
                break;
            case ITALIC:
                print("~~");
                break;
            case STRIKEDOUT:
                print("--");
                break;
            case UNDERLINED:
                print("__");
                break;
            case SUPERSCRIPT:
                print("^^");
                break;
            case SUBSCRIPT:
                print(",,");
                break;
            case MONOSPACE:
                print("##");
                break;
        }
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.renderer.PrintRenderer#beginParagraph()
     */
    public void beginParagraph()
    {
        if ((this.currentElement != Element.DOCUMENT) && (this.currentElement != Element.HORIZONTALLINE)
            && (this.currentElement != Element.MACRO) && (this.currentElement != Element.SECTION))
        {
            print("\n");
        }
        this.currentElement = Element.PARAGRAPH;
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.renderer.PrintRenderer#endParagraph()
     */
    public void endParagraph()
    {
        this.needsNewLine = true;
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.renderer.PrintRenderer#onLineBreak()
     */
    public void onLineBreak()
    {
        print("\n");
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.renderer.PrintRenderer#onNewLine()
     */
    public void onNewLine()
    {
        print("\\");
    }

    /**
     * {@inheritDoc}
     * @see PrintRenderer#onInlineMacro(String, java.util.Map, String)
     */
    public void onInlineMacro(String name, Map<String, String> parameters, String content)
    {
        print(this.macroPrinter.print(name, parameters, content));
    }

    /**
     * {@inheritDoc}
     * @see PrintRenderer#onStandaloneMacro(String, java.util.Map, String)
     */
    public void onStandaloneMacro(String name, Map<String, String> parameters, String content)
    {
        if ((this.currentElement != Element.DOCUMENT)
            // Don't print a new line since the paragraph already adds a new line by default after itself
            && (this.currentElement != Element.PARAGRAPH))
        {
            print("\n");
        }
        print(this.macroPrinter.print(name, parameters, content));
        this.currentElement = Element.MACRO;
    }

    /**
     * {@inheritDoc}
     * @see PrintRenderer#beginSection(org.xwiki.rendering.listener.SectionLevel)
     */
    public void beginSection(SectionLevel level)
    {
        String prefix;

        switch (level) {
            case LEVEL1:
                prefix = "1";
                break;
            case LEVEL2:
                prefix = "1.1";
                break;
            case LEVEL3:
                prefix = "1.1.1";
                break;
            case LEVEL4:
                prefix = "1.1.1.1";
                break;
            case LEVEL5:
                prefix = "1.1.1.1.1";
                break;
            default:
                prefix = "1.1.1.1.1.1";
                break;
        }
        print(prefix + " ");
        this.currentElement = Element.SECTION;
    }

    /**
     * {@inheritDoc}
     * @see PrintRenderer#endSection(org.xwiki.rendering.listener.SectionLevel)
     */
    public void endSection(SectionLevel level)
    {
        this.needsNewLine = true;
    }

    /**
     * {@inheritDoc}
     * @see PrintRenderer#onWord(String)
     */
    public void onWord(String word)
    {
        print(word);
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.renderer.PrintRenderer#onSpace()
     */
    public void onSpace()
    {
        print(" ");
    }

    /**
     * {@inheritDoc}
     * @see PrintRenderer#onSpecialSymbol(String)
     */
    public void onSpecialSymbol(String symbol)
    {
        print(symbol);
    }

    /**
     * {@inheritDoc}
     * @see PrintRenderer#onEscape(String)
     */
    public void onEscape(String escapedString)
    {
        for (int i = 0; i < escapedString.length(); i++) {
            print("\\" + escapedString.charAt(i));
        }
    }

    /**
     * {@inheritDoc}
     * @see PrintRenderer#beginList(org.xwiki.rendering.listener.ListType)
     */
    public void beginList(ListType listType)
    {
        if (this.isBeginListItemFound && !this.isEndListItemFound) {
            print("\n");
            this.isBeginListItemFound = false;
        }

        if (listType == ListType.BULLETED) {
            this.listStyle.append("*");
        } else {
            this.listStyle.append("1");
        }
        this.listDepth++;
        this.currentElement = Element.LIST;
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.renderer.PrintRenderer#beginListItem()
     */
    public void beginListItem()
    {
        if (this.isEndListItemFound) {
            print("\n");
            this.isEndListItemFound = false;
            this.isBeginListItemFound = false;
        }
        this.isBeginListItemFound = true;

        print(this.listStyle.toString());
        if (this.listStyle.charAt(0) == '1') {
            print(".");
        }
        print(" ");
    }

    /**
     * {@inheritDoc}
     * @see PrintRenderer#endList(org.xwiki.rendering.listener.ListType)
     */
    public void endList(ListType listType)
    {
        this.listStyle.setLength(this.listStyle.length() - 1);
        this.listDepth--;
        if (this.listDepth == 0) {
            this.isBeginListItemFound = false;
            this.isEndListItemFound = false;
            this.needsNewLine = true;
        }
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.renderer.PrintRenderer#endListItem()
     */
    public void endListItem()
    {
        this.isEndListItemFound = true;
    }

    /**
     * {@inheritDoc}
     * @see PrintRenderer#beginXMLElement(String, java.util.Map)
     */
    public void beginXMLElement(String name, Map<String, String> attributes)
    {
        // There's no xwiki wiki syntax for writing HTML (we have to use Macros for that). Hence discard
        // any XML element events.
    }

    /**
     * {@inheritDoc}
     * @see PrintRenderer#endXMLElement(String, java.util.Map)
     */
    public void endXMLElement(String name, Map<String, String> attributes)
    {
        // There's no xwiki wiki syntax for writing HTML (we have to use Macros for that). Hence discard
        // any XML element events.
    }

    /**
     * {@inheritDoc}
     * @see PrintRenderer#beginMacroMarker(String, java.util.Map, String)
     */
    public void beginMacroMarker(String name, Map<String, String> parameters, String content)
    {
        // When we encounter a macro marker we ignore all other blocks inside since we're going to use the macro
        // definition wrapped by the macro marker to construct the xwiki syntax.
        this.isInsideMacroMarker = true;
    }

    /**
     * {@inheritDoc}
     * @see PrintRenderer#endMacroMarker(String, java.util.Map, String) 
     */
    public void endMacroMarker(String name, Map<String, String> parameters, String content)
    {
        this.isInsideMacroMarker = false;
        print(this.macroPrinter.print(name, parameters, content));
    }

    /**
     * {@inheritDoc}
     * @see PrintRenderer#onId(String)
     */
    public void onId(String name)
    {
        print("{{id name=\"" + name + "\"}}");
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.renderer.Renderer#onHorizontalLine() 
     */
    public void onHorizontalLine()
    {
        print("----");
        this.needsNewLine = true;
        this.currentElement = Element.HORIZONTALLINE;
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.renderer.Renderer#onInlineVerbatim(String)
     */
    public void onInlineVerbatim(String protectedString)
    {
        print("{{{" + protectedString + "}}}");
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.renderer.Renderer#onStandaloneVerbatim(String)
     */
    public void onStandaloneVerbatim(String protectedString)
    {
        onInlineVerbatim(protectedString);
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.renderer.Renderer#onEmptyLines(int)
     */
    public void onEmptyLines(int count)
    {
        if (count > 1) {
            for (int i = 0; i < count; i++) {
                print("\n");
            }
        }
    }

    protected void print(String text)
    {
        if (!this.isInsideMacroMarker) {
            if (this.needsNewLine) {
                super.print("\n");
            }
            this.needsNewLine = false;
            super.print(text);
        }
    }
}
