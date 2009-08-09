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

import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.Before;
import org.xwiki.rendering.macro.MacroCategoriesManager;
import org.xwiki.rendering.configuration.RenderingConfiguration;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfiguration;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Unit tests for {@link MacroCategoriesManager}.
 * 
 * @version $Id$
 * @since 2.0M3
 */
public class DefaultMacroCategoriesManagerTest extends AbstractComponentTestCase
{
    private MacroCategoriesManager macroCategoriesManager;

    @Before
    public void setUp() throws Exception
    {
        super.setUp();
        this.macroCategoriesManager = getComponentManager().lookup(MacroCategoriesManager.class);
    }

    @Test
    public void testGetMacroCategories() throws Exception
    {
        // TODO: This test needs to be improved. Right now it's based on the Test Macro located in the transformation
        // package and for 4 of them a "Test" category has been set...
        DefaultRenderingConfiguration configuration =
            (DefaultRenderingConfiguration) getComponentManager().lookup(RenderingConfiguration.class);
        configuration.addMacroCategory("testcontentmacro", "Content");
        configuration.addMacroCategory("testsimplemacro", "Simple");

        Set<String> macroCategories = this.macroCategoriesManager.getMacroCategories();

        // Check for a default category.
        Assert.assertTrue(macroCategories.contains("Test"));

        // Check for null category.
        Assert.assertTrue(macroCategories.contains(null));

        // Check for overwritten categories.
        Assert.assertTrue(macroCategories.contains("Content"));
        Assert.assertTrue(macroCategories.contains("Simple"));
    }

    @Test
    public void testGetMacroNamesForCategory() throws Exception
    {
        Set<String> testCategoryMacros = this.macroCategoriesManager.getMacroNames("Test");
        // There should be exactly 4 macros belonging to "Test" category.
        Assert.assertEquals(4, testCategoryMacros.size());
    }
}
