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
package com.xpn.xwiki.wysiwyg.client;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.google.gwt.junit.tools.GWTTestSuite;
import com.xpn.xwiki.wysiwyg.client.plugin.color.ColorPickerTest;
import com.xpn.xwiki.wysiwyg.client.plugin.image.ImageMetaDataExtractorTest;
import com.xpn.xwiki.wysiwyg.client.plugin.indent.IndentExecutableTest;
import com.xpn.xwiki.wysiwyg.client.plugin.indent.OutdentExecutableTest;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.DefaultPluginFactoryManagerTest;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.DefaultPluginManagerTest;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.FocusWidgetUIExtensionTest;
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkMetaDataExtractorTest;
import com.xpn.xwiki.wysiwyg.client.plugin.macro.MacroCallTest;
import com.xpn.xwiki.wysiwyg.client.syntax.internal.DefaultSyntaxValidatorManagerTest;
import com.xpn.xwiki.wysiwyg.client.syntax.internal.DefaultValidationRuleTest;
import com.xpn.xwiki.wysiwyg.client.syntax.internal.DisablingRuleTest;
import com.xpn.xwiki.wysiwyg.client.syntax.internal.XWikiSyntaxValidatorTest;
import com.xpn.xwiki.wysiwyg.client.util.TimerTest;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextAreaTest;
import com.xpn.xwiki.wysiwyg.client.widget.rta.SelectionPreserverTest;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.internal.FormatBlockExecutableTest;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.internal.InsertHRExecutableTest;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.internal.InsertHTMLExecutableTest;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.internal.StyleExecutableTest;
import com.xpn.xwiki.wysiwyg.client.widget.rta.history.HistoryTest;

/**
 * All the client tests to be run. We're using a test suite because it decreases the overall testing time. GWTTestCase
 * derived tests are slow. This is because the JUnitShell has to load the module for each test (create the shell, hook
 * into it, etc). GWTTestSuite mitigates this by grouping all the tests that are for the same module (those that return
 * the same value for getModuleName) together and running them via the same shell instance.
 * 
 * @version $Id$
 */
public class WysiwygClientTestSuite extends GWTTestSuite
{
    /**
     * @return The suite of all the client tests to be run.
     */
    public static Test suite()
    {
        TestSuite suite = new TestSuite("WYSIWYG Client Unit Tests");

        addPluginTests(suite);

        // syntax
        suite.addTestSuite(DefaultSyntaxValidatorManagerTest.class);
        suite.addTestSuite(DefaultValidationRuleTest.class);
        suite.addTestSuite(DisablingRuleTest.class);
        suite.addTestSuite(XWikiSyntaxValidatorTest.class);

        // util
        suite.addTestSuite(TimerTest.class);

        addWidgetTests(suite);

        return suite;
    }

    /**
     * Adds unit tests for the plug-ins to the given test suite.
     * 
     * @param suite the test suite
     */
    private static void addPluginTests(TestSuite suite)
    {
        suite.addTestSuite(DefaultPluginFactoryManagerTest.class);
        suite.addTestSuite(DefaultPluginManagerTest.class);
        suite.addTestSuite(FocusWidgetUIExtensionTest.class);
        suite.addTestSuite(ImageMetaDataExtractorTest.class);
        suite.addTestSuite(LinkMetaDataExtractorTest.class);
        suite.addTestSuite(MacroCallTest.class);
        suite.addTestSuite(IndentExecutableTest.class);
        suite.addTestSuite(OutdentExecutableTest.class);
        suite.addTestSuite(ColorPickerTest.class);
    }

    /**
     * Adds unit tests for the widgets to the given test suite.
     * 
     * @param suite the test suite
     */
    private static void addWidgetTests(TestSuite suite)
    {
        suite.addTestSuite(RichTextAreaTest.class);
        suite.addTestSuite(HistoryTest.class);
        suite.addTestSuite(InsertHTMLExecutableTest.class);
        suite.addTestSuite(InsertHRExecutableTest.class);
        suite.addTestSuite(FormatBlockExecutableTest.class);
        suite.addTestSuite(StyleExecutableTest.class);
        suite.addTestSuite(SelectionPreserverTest.class);
    }
}
