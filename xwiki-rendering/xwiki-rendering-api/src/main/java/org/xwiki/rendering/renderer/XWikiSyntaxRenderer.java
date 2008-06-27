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

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;

import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.SectionLevel;
import org.xwiki.rendering.listener.Link;

/**
 * Generates XWiki Syntax from {@link org.xwiki.rendering.block.XDOM}. This is useful for example
 * to convert other wiki syntaxes to the XWiki syntax. It's also useful in our tests to verify
 * that round tripping from XWiki Syntax to the DOM and back to XWiki Syntax generates the same
 * content as the initial syntax.
 *
 * @version $Id$
 * @since 1.5M2
 */
public class XWikiSyntaxRenderer implements Renderer
{
    private PrintWriter writer;
    
    private StringBuffer listStyle = new StringBuffer();
    
    private boolean needsLineBreakForList = false;

    private boolean isInsideMacroMarker = false;
    
    public XWikiSyntaxRenderer(Writer writer)
    {
        this.writer = new PrintWriter(writer);
    }

    public void onLink(Link link)
    {
        write("[");
        if (link.getLabel() != null) {
            write(link.getLabel());
            write(">");
        }
        write(link.getReference());
        if (link.getAnchor() != null) {
            write("#");
            write(link.getAnchor());
        }
        if (link.getQueryString() != null) {
            write("?");
            write(link.getQueryString());
        }
        if (link.getInterWikiAlias() != null) {
            write("@");
            write(link.getInterWikiAlias());
        }
        if (link.getTarget() != null) {
            write(">");
            write(link.getTarget());
        }
        write("]");
    }

    public void beginBold()
    {
        write("*");
    }

    public void beginItalic()
    {
        write("~~");
    }

    public void beginParagraph()
    {
        addLineBreak();
        addLineBreak();
    }

    public void endBold()
    {
        write("*");
    }

    public void endItalic()
    {
        write("~~");
    }

    public void endParagraph()
    {
        // Nothing to do
    }

    public void onLineBreak()
    {
        write("\n");
    }

    public void onNewLine()
    {
        write("\\");
    }

    public void onMacro(String name, Map<String, String> parameters, String content)
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append("{").append(name);
        if (!parameters.isEmpty()) {
            buffer.append(':');
            for (Iterator<String> paramsIt = parameters.keySet().iterator(); paramsIt.hasNext();) {
                String paramName = paramsIt.next();
                buffer.append(paramName).append("=").append(parameters.get(paramName));
                if (paramsIt.hasNext()) {
                    buffer.append("|");
                }
            }
        }
        if (content == null) {
            buffer.append("/}");
        } else {
            buffer.append('}');
            buffer.append(content);
            buffer.append("{/").append(name).append("}");
        }
        write(buffer.toString());
    }

    public void beginSection(SectionLevel level)
    {
        String prefix;

        addLineBreak();
        switch (level) {
            case LEVEL1: prefix = "1"; break; 
            case LEVEL2: prefix = "1.1"; break; 
            case LEVEL3: prefix = "1.1.1"; break; 
            case LEVEL4: prefix = "1.1.1.1"; break; 
            case LEVEL5: prefix = "1.1.1.1.1"; break; 
            default: prefix = "1.1.1.1.1.1"; break; 
        }
        write(prefix + " ");
    }

    public void endSection(SectionLevel level)
    {
        // Nothing to do
    }

    public void onWord(String word)
    {
        write(word);
    }

    public void onSpace()
    {
        write(" ");
    }

    public void onSpecialSymbol(String symbol)
    {
        write(symbol);
    }

    public void onEscape(String escapedString)
    {
        write("\\" + escapedString);
    }

    public void beginList(ListType listType)
    {
        if (this.needsLineBreakForList) {
            write("\n");
            this.needsLineBreakForList = false;
        }
        
        if (listType == ListType.BULLETED) {
            this.listStyle.append("*"); 
        } else {
            this.listStyle.append("1");
        }
    }

    public void beginListItem()
    {
        this.needsLineBreakForList = true;
        
        write(this.listStyle.toString());
        if (this.listStyle.charAt(0) == '1') {
            write(".");
        }
        write(" ");
    }

    public void endList(ListType listType)
    {
        if (this.needsLineBreakForList) {
            write("\n");
            this.needsLineBreakForList = false;
        }

        this.listStyle.setLength(this.listStyle.length() - 1);
    }

    public void endListItem()
    {
        if (this.needsLineBreakForList) {
            write("\n");
            this.needsLineBreakForList = false;
        }
    }

    public void beginXMLElement(String name, Map<String, String> attributes)
    {
        // There's no xwiki wiki syntax for writing HTML (we have to use Macros for that). Hence discard
        // any XML element events.
    }

    public void endXMLElement(String name, Map<String, String> attributes)
    {
        // There's no xwiki wiki syntax for writing HTML (we have to use Macros for that). Hence discard
        // any XML element events.
    }

    public void beginMacroMarker(String name, Map<String, String> parameters, String content)
    {
        // When we encounter a macro marker we ignore all other blocks inside since we're going to use the macro
        // definition wrapped by the macro marker to construct the xwiki syntax.
        this.isInsideMacroMarker = true;
    }

    public void endMacroMarker(String name, Map<String, String> parameters, String content)
    {
        this.isInsideMacroMarker = false;
        onMacro(name, parameters, content);
    }

    private void write(String text)
    {
        if (!this.isInsideMacroMarker) {
            this.writer.write(text);
            // The first text written shouldn't have a linebreak added.
            this.needsLineBreakForList = true;
        }
    }
    
    private void addLineBreak()
    {
        if (this.needsLineBreakForList) {
            write("\n");
        }
    }
}
