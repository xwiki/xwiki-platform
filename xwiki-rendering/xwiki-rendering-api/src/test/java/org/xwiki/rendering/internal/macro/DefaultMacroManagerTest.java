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

import java.util.Collections;

import org.jmock.Mock;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.internal.ReflectionUtils;
import org.xwiki.component.logging.Logger;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.internal.transformation.TestSimpleMacro;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroLookupException;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxFactory;
import org.xwiki.rendering.syntax.SyntaxType;
import org.xwiki.test.AbstractXWikiComponentTestCase;

/**
 * Unit tests for {@link org.xwiki.rendering.internal.macro.DefaultMacroManager}.
 * 
 * @version $Id$
 * @since 1.9M1
 */
public class DefaultMacroManagerTest extends AbstractXWikiComponentTestCase
{
    private Mock mockLogger;

    private Mock mockComponentManager;

    private DefaultMacroManager macroManager;
    
    private SyntaxFactory syntaxFactory;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.scaffolding.AbstractXWikiComponentTestCase#setUp() 
     */
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        this.mockLogger = mock(Logger.class);
        this.mockComponentManager = mock(ComponentManager.class);

        this.macroManager = (DefaultMacroManager) getComponentManager().lookup(MacroManager.class);
        this.syntaxFactory = getComponentManager().lookup(SyntaxFactory.class);
        this.macroManager.enableLogging((Logger) this.mockLogger.proxy());
    }

    public void testMacroExists()
    {
        assertTrue(this.macroManager.exists(new MacroId("testsimplemacro")));
    }

    public void testGetExistingMacro() throws Exception
    {
        assertNotNull(this.macroManager.getMacro(new MacroId("testsimplemacro")));
    }

    public void testGetNotExistingMacro()
    {
        try {
            this.macroManager.getMacro(new MacroId("notregisteredmacro"));
            fail("Expected a macro lookup exception when looking for not registered macro");
        } catch (MacroLookupException expected) {
            assertEquals("No macro [notregisteredmacro] could be found.", expected.getMessage());
        }
    }

    public void testSyntaxSpecificMacroExistsWhenMacroIsRegisteredForAllSyntaxes()
    {
        assertFalse(this.macroManager.exists(new MacroId("testsimplemacro", new Syntax(SyntaxType.XWIKI, "2.0"))));
    }

    public void testGetExistingMacroForASpecificSyntaxWhenMacroIsRegisteredForAllSyntaxes() throws Exception
    {
        assertNotNull(this.macroManager.getMacro(new MacroId("testsimplemacro", new Syntax(SyntaxType.XWIKI, "2.0"))));
    }

    public void testMacroRegisteredForAGivenSyntaxOnly() throws Exception
    {
        Macro< ? > macro = new TestSimpleMacro();
        DefaultComponentDescriptor<Macro> descriptor = new DefaultComponentDescriptor<Macro>();
        descriptor.setRole(Macro.class);
        descriptor.setRoleHint("macro/xwiki/2.0");
        getComponentManager().registerComponent(descriptor, macro);

        assertFalse(this.macroManager.exists(new MacroId("macro")));
        assertTrue(this.macroManager.exists(new MacroId("macro", new Syntax(SyntaxType.XWIKI, "2.0"))));

        Macro< ? > macroResult = this.macroManager.getMacro(new MacroId("macro", new Syntax(SyntaxType.XWIKI, "2.0")));
        assertSame(macro, macroResult);
    }

    public void testMacroRegisteredForAGivenSyntaxOverridesMacroRegisteredForAllSyntaxes() throws Exception
    {
        Macro< ? > macro1 = new TestSimpleMacro();
        Macro< ? > macro2 = new TestSimpleMacro();

        DefaultComponentDescriptor<Macro> descriptor = new DefaultComponentDescriptor<Macro>();
        descriptor.setRole(Macro.class);
        descriptor.setRoleHint("macro");
        getComponentManager().registerComponent(descriptor, macro1);

        descriptor = new DefaultComponentDescriptor<Macro>();
        descriptor.setRole(Macro.class);
        descriptor.setRoleHint("macro/xwiki/2.0");
        getComponentManager().registerComponent(descriptor, macro2);

        assertTrue(this.macroManager.exists(new MacroId("macro")));
        assertTrue(this.macroManager.exists(new MacroId("macro", new Syntax(SyntaxType.XWIKI, "2.0"))));

        Macro< ? > macroResult1 = this.macroManager.getMacro(new MacroId("macro", new Syntax(SyntaxType.XWIKI, "2.0")));
        assertSame(macro2, macroResult1);

        Macro< ? > macroResult2 = this.macroManager.getMacro(new MacroId("macro"));
        assertSame(macro1, macroResult2);
    }

    /**
     * Tests what happens when a macro is registered with an invalid hint.
     */
    public void testInvalidMacroHint() throws Exception
    {
        this.mockComponentManager.expects(once()).method("lookupMap").will(
            returnValue(Collections.singletonMap("macro/invalidsyntax", "dummy")));
        ReflectionUtils.setFieldValue(this.macroManager, "componentManager", this.mockComponentManager.proxy());

        // Verify that when a Macro has an invalid hint it's logged as a warning.
        this.mockLogger.expects(once()).method("warn").with(
            eq("Invalid Macro descriptor format for hint "
                + "[macro/invalidsyntax]. The hint should contain either the macro name only or the macro name "
                + "followed by the syntax for which it is valid. In that case the macro name should be followed by "
                + "a \"/\" followed by the syntax name followed by another \"/\" followed by the syntax version. "
                + "For example \"html/xwiki/2.0\". This macro will not be available in the system."));
        this.macroManager.getMacroIds(syntaxFactory.createSyntaxFromIdString("macro/xwiki/2.0"));
    }
}
