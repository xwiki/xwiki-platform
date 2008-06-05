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
import org.xwiki.rendering.listener.SpecialSymbol;

public class XWikiSyntaxRenderer implements Renderer
{
    private PrintWriter writer;
    
    private StringBuffer listStyle = new StringBuffer();
    
    private boolean needsLineBreakForList = false;

    /**
     * Record if we should start elements with a linebreak or not.
     */
    private boolean shouldAddLinebreak = false;
    
    public XWikiSyntaxRenderer(Writer writer)
    {
        this.writer = new PrintWriter(writer);
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

    public void onLink(String text)
    {
        // TODO Auto-generated method stub
    }

    public void onMacro(String name, Map<String, String> parameters, String content)
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append("{" + name);
        if (!parameters.isEmpty()) {
            buffer.append(':');
            for (Iterator<String> paramsIt = parameters.keySet().iterator(); paramsIt.hasNext();) {
                String paramName = paramsIt.next();
                buffer.append(paramName + "=" + parameters.get(paramName));
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
            buffer.append("{/" + name + "}");
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

    public void onSpecialSymbol(SpecialSymbol symbol)
    {
        write(symbol.toString());
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
        // TODO
    }

    public void endMacroMarker(String name, Map<String, String> parameters, String content)
    {
        // TODO
    }

    private void write(String text)
    {
        this.writer.write(text);
        // The first text written shouldn't have a linebreak added.
        this.needsLineBreakForList = true;
    }
    
    private void addLineBreak()
    {
        if (this.needsLineBreakForList) {
            write("\n");
        }
    }
}
