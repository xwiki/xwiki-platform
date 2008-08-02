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
package org.xwiki.rendering.macro;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.xwiki.rendering.scaffolding.AbstractRenderingTestCase;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;

/**
 * Unit tests for {@link org.xwiki.rendering.macro.XHTMLMacro}.
 * 
 * @version $Id$
 * @since 1.5M2
 */
public class XHTMLMacroTest extends AbstractRenderingTestCase
{
    public void testMacro() throws Exception
    {
        String html = "<table border=\"1\">\n"
            + "<tr>\n"
            + "<td>\n"
            + "* listitem\n"
            + "</td>\n"
            + "</tr>\n"
            + "</table>";

        String expected = "beginDocument\n"
        	+ "beginXMLElement: [table] [border=1]\n"
            + "beginXMLElement: [tr] []\n"
            + "beginXMLElement: [td] []\n"
            + "beginList: [BULLETED]\n"
            + "beginListItem\n"
            + "onWord: [listitem]\n"
            + "endListItem\n"
            + "endList: [BULLETED]\n"
            + "endXMLElement: [td] []\n"
            + "endXMLElement: [tr] []\n"
            + "endXMLElement: [table] [border=1]\n"
            + "endDocument\n";
        
        Macro macro = (Macro) getComponentManager().lookup(XHTMLMacro.ROLE, "xhtml/xwiki");
        List<Block> blocks = macro.execute(Collections.EMPTY_MAP, html, XDOM.EMPTY);

        assertBlocks(expected, blocks);
    }

    public void testMacroWithEntities() throws Exception
    {
        String html = "&nbsp;";

        String expected = "beginDocument\n"
            + "onWord: [Ê]\n"
            + "endDocument\n";
        
        Macro macro = (Macro) getComponentManager().lookup(XHTMLMacro.ROLE, "xhtml/xwiki");
        List<Block> blocks = macro.execute(Collections.EMPTY_MAP, html, XDOM.EMPTY);

        assertBlocks(expected, blocks);
    }

    public void testMacroEscapeWikiSyntax() throws Exception
    {
        String html = "**some escaped text**";

        String expected = "beginDocument\n"
            + "onWord: [**some escaped text**]\n"
            + "endDocument\n";

        Macro macro = (Macro) getComponentManager().lookup(XHTMLMacro.ROLE, "xhtml/xwiki");
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("escapeWikiSyntax", "true");
        List<Block> blocks = macro.execute(parameters, html, XDOM.EMPTY);

        assertBlocks(expected, blocks);
    }

    /**
     * Verify that if there's a space before an XML element it's correctly preserved.
     * @throws Exception
     */
    public void testMacroWhenWhiteSpaces() throws Exception
    {
        String html = "<p>Some <span>text</span></p>";

        String expected = "beginDocument\n"
            + "beginXMLElement: [p] []\n"
            + "onWord: [Some]\n"
            + "onSpace\n"
            + "beginXMLElement: [span] []\n"
            + "onWord: [text]\n"
            + "endXMLElement: [span] []\n"
            + "endXMLElement: [p] []\n"
            + "endDocument\n";

        Macro macro = (Macro) getComponentManager().lookup(XHTMLMacro.ROLE, "xhtml/xwiki");
        List<Block> blocks = macro.execute(Collections.EMPTY_MAP, html, XDOM.EMPTY);

        assertBlocks(expected, blocks);
    }
}
