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

package org.xwiki.rendering.macro.wikibridge;

import java.util.ArrayList;

import org.xwiki.rendering.internal.macro.wikibridge.DefaultWikiMacro;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.test.AbstractXWikiComponentTestCase;

/**
 * Test case for verifying correct macro registration.
 * 
 * @version $id$
 * @since 2.0M2
 */
public class DefaultWikiMacroManagerTest extends AbstractXWikiComponentTestCase
{
    /**
     * The {@link WikiMacroManager} component.
     */
    private WikiMacroManager wikiMacroManager;

    /**
     * The {@link MacroManager} component.
     */
    private MacroManager macroManager;

    /**
     * A {@link DefaultWikiMacro} instance.
     */
    private DefaultWikiMacro wikiMacro;

    /**
     * {@inheritDoc}
     */
    protected void setUp() throws Exception
    {
        super.setUp();

        macroManager = getComponentManager().lookup(MacroManager.class);
        wikiMacroManager = getComponentManager().lookup(WikiMacroManager.class);

        // Create a wiki macro with no parameters.
        WikiMacroDescriptor descriptor =
            new WikiMacroDescriptor("Test Wiki Macro", "Description", "Test", new DefaultContentDescriptor(),
                new ArrayList<WikiMacroParameterDescriptor>());
        wikiMacro =
            new DefaultWikiMacro("xwiki:Main.TestWikiMacro", "testwikimacro", true, descriptor, "== Test ==",
                "xwiki/2.0", getComponentManager());
    }

    /**
     * Tests wiki macro registration.
     */
    public void testWikiMacroRegistrationAndUnregistration() throws Exception
    {
        String docName = "xwiki:Main.TestMacro";
        assertTrue(!wikiMacroManager.hasWikiMacro(docName));

        wikiMacroManager.registerWikiMacro(docName, wikiMacro);
        assertTrue(wikiMacroManager.hasWikiMacro(docName));

        Macro< ? > registeredMacro = macroManager.getMacro(new MacroId("testwikimacro"));
        assertEquals(0, registeredMacro.compareTo(wikiMacro));

        wikiMacroManager.unregisterWikiMacro(docName);
        assertTrue(!wikiMacroManager.hasWikiMacro(docName));

        assertTrue(!macroManager.exists(new MacroId("testwikimacro")));
    }
}
