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
package org.xwiki.rendering;

import org.junit.Assert;
import org.junit.Before;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.internal.macro.html.HTMLMacro;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.html.HTMLMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Unit tests for {@link HTMLMacro} that cannot be performed using the Rendering Test framework.
 * 
 * @version $Id$
 * @since 1.8.3
 */
public class HTMLMacroTest extends AbstractComponentTestCase
{
    /**
     * {@inheritDoc}
     * 
     * @see AbstractComponentTestCase#setUp()
     */
    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        registerMockComponent(DocumentAccessBridge.class);
        registerMockComponent(EntityReferenceSerializer.class);
    }

    /**
     * Verify that inline HTML macros with non inline content generate an exception.
     */
    @org.junit.Test(expected = MacroExecutionException.class)
    public void executeMacroWhenNonInlineContentInInlineContext() throws Exception
    {
        HTMLMacro macro = (HTMLMacro) getComponentManager().lookup(Macro.class, "html");
        HTMLMacroParameters parameters = new HTMLMacroParameters();
        MacroTransformationContext context = new MacroTransformationContext();
        context.setInline(true);
        macro.execute(parameters, "<ul><li>item</li></ul>", context);
    }

    @org.junit.Test
    public void macroDescriptor() throws Exception
    {
        HTMLMacro macro = (HTMLMacro) getComponentManager().lookup(Macro.class, "html");

        Assert.assertEquals("Indicate if the HTML should be transformed into valid XHTML or not.",
            macro.getDescriptor().getParameterDescriptorMap().get("clean").getDescription());
    }
}
