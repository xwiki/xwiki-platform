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
import java.util.HashMap;
import java.util.Map;

import org.jmock.Mock;
import org.xwiki.component.logging.Logger;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.internal.transformation.TestSimpleMacro;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroFactory;
import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.parser.SyntaxType;
import org.xwiki.rendering.scaffolding.AbstractRenderingTestCase;

/**
 * Unit tests for {@link org.xwiki.rendering.internal.macro.DefaultMacroFactory}.
 * 
 * @version $Id$
 * @since 1.5M2
 */
public class DefaultMacroFactoryTest extends AbstractRenderingTestCase
{
    private Mock mockLogger;

    private Mock mockComponentManager;

    private DefaultMacroFactory factory;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.test.AbstractXWikiComponentTestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        this.mockLogger = mock(Logger.class);
        this.mockComponentManager = mock(ComponentManager.class);

        this.factory = (DefaultMacroFactory) getComponentManager().lookup(MacroFactory.ROLE);
        this.factory.enableLogging((Logger) this.mockLogger.proxy());
    }

    public void testGetExistingMacro() throws Exception
    {
        this.factory.getMacro("testsimplemacro", new Syntax(SyntaxType.XWIKI, "2.0"));
    }

    public void testMacroRegisteredForAGivenSyntaxOnly() throws Exception
    {
        Macro< ? > macro = new TestSimpleMacro();
        this.mockComponentManager.expects(once()).method("lookupMap").will(
            returnValue(Collections.singletonMap("macro/xwiki/2.0", macro)));
        this.factory.compose((ComponentManager) this.mockComponentManager.proxy());
        this.factory.initialize();

        Macro< ? > macroResult = this.factory.getMacro("macro", new Syntax(SyntaxType.XWIKI, "2.0"));
        assertSame(macro, macroResult);
    }

    public void testMacroRegisteredForAGivenSyntaxOverridesMacroRegisteredForAllSyntaxes() throws Exception
    {
        Macro< ? > macro1 = new TestSimpleMacro();
        Macro< ? > macro2 = new TestSimpleMacro();
        Map<String, Macro< ? >> macroDefinitions = new HashMap<String, Macro< ? >>();
        macroDefinitions.put("macro", macro1);
        macroDefinitions.put("macro/xwiki/2.0", macro2);

        this.mockComponentManager.expects(once()).method("lookupMap").will(returnValue(macroDefinitions));
        this.factory.compose((ComponentManager) this.mockComponentManager.proxy());
        this.factory.initialize();

        Macro< ? > macroResult = this.factory.getMacro("macro", new Syntax(SyntaxType.XWIKI, "2.0"));
        assertSame(macro2, macroResult);
    }

    /**
     * Tests what happens when a macro is registered with an invalid hint.
     */
    public void testInvalidMacroHint() throws Exception
    {
        this.mockComponentManager.expects(once()).method("lookupMap").will(
            returnValue(Collections.singletonMap("macro/invalidsyntax", "dummy")));
        this.factory.compose((ComponentManager) this.mockComponentManager.proxy());

        // Verify that when a Macro has an invalid hint it's logged as a warning.
        this.mockLogger.expects(once()).method("warn").with(
            eq("Invalid Macro descriptor format for hint "
                + "[macro/invalidsyntax]. The hint should contain either the macro name only or the macro name "
                + "followed by the syntax for which it is valid. In that case the macro name should be followed by "
                + "a \"/\" followed by the syntax name followed by another \"/\" followed by the syntax version. "
                + "This macro will not be available in the system."));
        this.factory.initialize();
    }

}
