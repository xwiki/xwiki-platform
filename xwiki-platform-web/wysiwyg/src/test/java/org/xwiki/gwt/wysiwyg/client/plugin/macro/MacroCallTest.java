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
package org.xwiki.gwt.wysiwyg.client.plugin.macro;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.wysiwyg.client.WysiwygTestCase;

import com.google.gwt.dom.client.Document;

/**
 * Unit tests for {@link MacroCall}.
 * 
 * @version $Id$
 */
public class MacroCallTest extends WysiwygTestCase
{
    /**
     * The empty list of macro call arguments.
     */
    public static final Map<String, String> NO_ARGUMENTS = Collections.emptyMap();

    /**
     * Tests if the start macro comment is parsed correctly when macro content and macro parameter values contain
     * special symbols like {@code "} and {@code \} or the separator {@code |-|}.
     */
    public void testParseStartMacroComment()
    {
        Map<String, String> args = new HashMap<String, String>();
        args.put("a", "1\"2|-|3=\\\"4\\");
        args.put("b", "");
        testSerializeAndDeserialize("html", "=\"|-|\\", args);
    }

    /**
     * Tests if the start macro comment is parsed correctly when it contains the {@code --} sequence.
     */
    public void testParseStartMacroCommentWithDashes()
    {
        testSerializeAndDeserialize("a--b\\c\\\\d-");
        testSerializeAndDeserialize("a--b-c\\\\d\\");
    }

    /**
     * Tests if the case used in parameter names is kept.
     */
    public void testKeepParameterNameCase()
    {
        MacroCall call = new MacroCall();
        call.setName("box");
        call.setContent("");

        String sTaRt = "sTaRt";
        String stArt = "stArt";

        call.setArgument(sTaRt, "1");
        assertEquals("1", call.getArgument(stArt));

        call.setArgument(stArt, "2");
        assertEquals("2", call.getArgument(sTaRt));

        assertEquals("startmacro:box|-|sTaRt=\"2\" |-|", call.toString());
    }

    /**
     * @see XWIKI-3735: Differentiate macros with empty content from macros without content.
     */
    public void testDifferentiateMacrosWithEmptyContentFromMacrosWithoutContent()
    {
        // No content and no arguments.
        testSerializeAndDeserialize("x", null, NO_ARGUMENTS);
        // Empty content and no arguments.
        testSerializeAndDeserialize("y", "", NO_ARGUMENTS);
        // No content but with arguments.
        Map<String, String> args = new HashMap<String, String>();
        args.put("c", "1|-|2");
        testSerializeAndDeserialize("z", null, args);
        // Empty content with arguments.
        testSerializeAndDeserialize("w", "", args);
    }

    /**
     * Tests if a {@link MacroCall} instance keeps its fields after serialization and deserialization. The given text is
     * used to fill all the macro call fields.
     * 
     * @param text the text used to fill all the macro call fields
     */
    private void testSerializeAndDeserialize(String text)
    {
        Map<String, String> args = new HashMap<String, String>();
        args.put(text, text);
        testSerializeAndDeserialize(text, text, args);
    }

    /**
     * Tests if a {@link MacroCall} instance keeps its fields after serialization and deserialization. The
     * {@link MacroCall} instance is created based on the specified fields.
     * 
     * @param name the macro name
     * @param content the macro content
     * @param args the map of macro cal arguments
     */
    private void testSerializeAndDeserialize(String name, String content, Map<String, String> args)
    {
        MacroCall call = new MacroCall();
        call.setName(name);
        call.setContent(content);
        for (Map.Entry<String, String> entry : args.entrySet()) {
            call.setArgument(entry.getKey(), entry.getValue());
        }

        Element container = Document.get().createSpanElement().cast();
        container.setInnerHTML("before<!--" + call.toString() + "-->after");
        assertEquals(3, container.getChildNodes().getLength());

        call = new MacroCall(container.getChildNodes().getItem(1).getNodeValue());
        assertEquals(name, call.getName());
        assertEquals(content, call.getContent());
        for (Map.Entry<String, String> entry : args.entrySet()) {
            assertEquals(entry.getValue(), call.getArgument(entry.getKey()));
        }
    }
}
