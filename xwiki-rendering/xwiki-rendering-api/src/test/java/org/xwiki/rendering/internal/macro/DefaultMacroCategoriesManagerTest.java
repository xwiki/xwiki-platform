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

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;
import org.xwiki.component.internal.ReflectionUtils;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.rendering.macro.MacroCategoriesManager;
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

    private Mockery context = new Mockery();

    /**
     * {@inheritDoc}
     */
    public void setUp() throws Exception
    {
        super.setUp();
        this.macroCategoriesManager = getComponentManager().lookup(MacroCategoriesManager.class);
    }

    @Test
    public void testGetMacroCategories() throws Exception
    {
        final ConfigurationSource configurationSource = this.context.mock(ConfigurationSource.class);
        ReflectionUtils.setFieldValue(macroCategoriesManager, "configurationSource", configurationSource);

        this.context.checking(new Expectations()
        {
            {
                String key = "org.xwiki.rendering.macro.testcontentmacro.category";
                allowing(configurationSource).getProperty(key, String.class);
                will(returnValue("Content"));

                key = "org.xwiki.rendering.macro.testfailingmacro.category";
                allowing(configurationSource).getProperty(key, String.class);
                will(returnValue(null));

                key = "org.xwiki.rendering.macro.testformatmacro.category";
                allowing(configurationSource).getProperty(key, String.class);
                will(returnValue(null));

                key = "org.xwiki.rendering.macro.testnestedmacro.category";
                allowing(configurationSource).getProperty(key, String.class);
                will(returnValue(null));

                key = "org.xwiki.rendering.macro.testprioritymacro.category";
                allowing(configurationSource).getProperty(key, String.class);
                will(returnValue(null));

                key = "org.xwiki.rendering.macro.testrecursivemacro.category";
                allowing(configurationSource).getProperty(key, String.class);
                will(returnValue(null));

                key = "org.xwiki.rendering.macro.testsimpleinlinemacro.category";
                allowing(configurationSource).getProperty(key, String.class);
                will(returnValue(null));

                key = "org.xwiki.rendering.macro.testsimplemacro.category";
                allowing(configurationSource).getProperty(key, String.class);
                will(returnValue("Simple"));
            }
        });

        Set<String> macroCategories = macroCategoriesManager.getMacroCategories();

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
        Set<String> testCategoryMacros = macroCategoriesManager.getMacroNames("Test");
        // There should be exactly 4 macros belonging to "Test" category.
        Assert.assertEquals(4, testCategoryMacros.size());
    }
}
