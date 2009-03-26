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
package org.xwiki.rendering.internal.macro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmock.cglib.MockObjectTestCase;
import org.xwiki.rendering.internal.macro.DefaultMacroManager;
import org.xwiki.rendering.macro.AbstractMacroSource;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroSource;

/**
 * Tests for {@link DefaultMacroManager}
 * 
 * @version $Id$
 */
public class DefaultMacroManagerTest extends MockObjectTestCase
{

    private DefaultMacroManager macroManager = new DefaultMacroManager();;

    private List<MacroSource> sources;

    private class DummyMacroSource extends AbstractMacroSource
    {
        public DummyMacroSource(Map<String, Macro< ? >> macros, int priority)
        {
            this.priority = priority;
            for (String name : macros.keySet()) {
                this.registerMacroForAllSyntaxes(name, macros.get(name));
            }
        }
    }

    @Override
    protected void setUp() throws Exception
    {
        sources = new ArrayList<MacroSource>();

        macroManager.setMacroSources(sources);
    }
    
    public void testMacroSourcePriority() throws Exception
    {
        Map<String, Macro< ? >> macros = new HashMap<String, Macro< ? >>();

        Macro< ? > macro1 = (Macro< ? >) mock(Macro.class).proxy();
        macros.put("macro", macro1);

        MacroSource source1 = new DummyMacroSource(macros, 1);

        Macro< ? > macro2 = (Macro< ? >) mock(Macro.class).proxy();
        macros.put("macro", macro2);

        MacroSource source2 = new DummyMacroSource(macros, 10);

        sources.add(source1);
        sources.add(source2);

        macroManager.initialize();

        assertEquals(macro1, macroManager.getMacro("macro"));
    }
    
}
