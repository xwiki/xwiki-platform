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
package com.xpn.xwiki.wysiwyg.client.plugin.macro;

import com.xpn.xwiki.wysiwyg.client.AbstractWysiwygClientTest;

/**
 * Unit tests for {@link MacroCall}.
 * 
 * @version $Id$
 */
public class MacroCallTest extends AbstractWysiwygClientTest
{
    /**
     * Tests if the start macro comment is parsed correctly when macro content and macro parameter values contain
     * special symbols like {@code "} and {@code \} or the separator {@code |-|}.
     */
    public void testParseStartMacroComment()
    {
        MacroCall call = new MacroCall("startmacro:html|-| a =  \"1\\\"2|-|3=\\\\\\\"4\\\\\" b=\"\"|-|=\"|-|\\");
        assertEquals("html", call.getName());
        assertEquals("=\"|-|\\", call.getContent());
        assertEquals("1\"2|-|3=\\\"4\\", call.getArgument("a"));
        assertEquals("", call.getArgument("b"));
        assertEquals("startmacro:html|-|a=\"1\\\"2|-|3=\\\\\\\"4\\\\\" b=\"\" |-|=\"|-|\\", call.toString());
    }
}
