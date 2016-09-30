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
package org.xwiki.gwt.wysiwyg;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.xwiki.gwt.wysiwyg.client.ToolBarControllerTest;
import org.xwiki.gwt.wysiwyg.client.plugin.color.ColorConverterTest;
import org.xwiki.gwt.wysiwyg.client.plugin.format.exec.FormatBlockExecutableTest;
import org.xwiki.gwt.wysiwyg.client.plugin.format.exec.RemoveFormatExecutableTest;
import org.xwiki.gwt.wysiwyg.client.plugin.history.HistoryTest;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfigDOMWriterTest;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfigJSONSerializerTest;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageMetaDataExtractorTest;
import org.xwiki.gwt.wysiwyg.client.plugin.image.exec.InsertImageExecutableTest;
import org.xwiki.gwt.wysiwyg.client.plugin.indent.exec.IndentExecutableTest;
import org.xwiki.gwt.wysiwyg.client.plugin.indent.exec.OutdentExecutableTest;
import org.xwiki.gwt.wysiwyg.client.plugin.line.LinePluginTest;
import org.xwiki.gwt.wysiwyg.client.plugin.link.EmptyLinkFilterTest;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkMetaDataExtractorTest;
import org.xwiki.gwt.wysiwyg.client.plugin.list.ListBehaviorAdjusterTest;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.MacroCallTest;
import org.xwiki.gwt.wysiwyg.client.plugin.separator.exec.InsertHRExecutableTest;
import org.xwiki.gwt.wysiwyg.client.plugin.style.StyleDescriptorJSONParserTest;
import org.xwiki.gwt.wysiwyg.client.plugin.style.exec.BlockStyleNameExecutableTest;
import org.xwiki.gwt.wysiwyg.client.plugin.style.exec.InlineStyleNameExecutableTest;
import org.xwiki.gwt.wysiwyg.client.syntax.internal.DefaultSyntaxValidatorManagerTest;
import org.xwiki.gwt.wysiwyg.client.syntax.internal.DefaultSyntaxValidatorTest;
import org.xwiki.gwt.wysiwyg.client.syntax.internal.DefaultValidationRuleTest;
import org.xwiki.gwt.wysiwyg.client.syntax.internal.DisablingRuleTest;

import com.google.gwt.junit.tools.GWTTestSuite;

/**
 * All the client tests to be run. We're using a test suite because it decreases the overall testing time. GWTTestCase
 * derived tests are slow. This is because the JUnitShell has to load the module for each test (create the shell, hook
 * into it, etc). GWTTestSuite mitigates this by grouping all the tests that are for the same module (those that return
 * the same value for getModuleName) together and running them via the same shell instance.
 * <p>
 * As described in GWT issue 2486, GWTTestSuite is not a translatable class and in order to prevent reports of harmless
 * errors we moved our test suite class out of module's source path.
 * <p>
 * See http://code.google.com/p/google-web-toolkit/issues/detail?id=2486.
 * 
 * @version $Id$
 */
public class WysiwygTestSuite extends GWTTestSuite
{
    /**
     * @return The suite of all the client tests to be run.
     */
    public static Test suite()
    {
        TestSuite suite = new TestSuite("WYSIWYG Client Unit Tests");

        addPluginTests(suite);

        suite.addTestSuite(ToolBarControllerTest.class);

        // syntax
        suite.addTestSuite(DefaultSyntaxValidatorManagerTest.class);
        suite.addTestSuite(DefaultValidationRuleTest.class);
        suite.addTestSuite(DisablingRuleTest.class);
        suite.addTestSuite(DefaultSyntaxValidatorTest.class);

        return suite;
    }

    /**
     * Adds unit tests for the plug-ins to the given test suite.
     * 
     * @param suite the test suite
     */
    private static void addPluginTests(TestSuite suite)
    {
        suite.addTestSuite(HistoryTest.class);
        suite.addTestSuite(FormatBlockExecutableTest.class);
        suite.addTestSuite(RemoveFormatExecutableTest.class);
        suite.addTestSuite(InsertHRExecutableTest.class);
        suite.addTestSuite(ImageConfigJSONSerializerTest.class);
        suite.addTestSuite(ImageConfigDOMWriterTest.class);
        suite.addTestSuite(ImageMetaDataExtractorTest.class);
        suite.addTestSuite(InsertImageExecutableTest.class);
        suite.addTestSuite(LinkMetaDataExtractorTest.class);
        suite.addTestSuite(EmptyLinkFilterTest.class);
        suite.addTestSuite(MacroCallTest.class);
        suite.addTestSuite(IndentExecutableTest.class);
        suite.addTestSuite(OutdentExecutableTest.class);
        suite.addTestSuite(ListBehaviorAdjusterTest.class);
        suite.addTestSuite(ColorConverterTest.class);
        suite.addTestSuite(LinePluginTest.class);
        suite.addTestSuite(StyleDescriptorJSONParserTest.class);
        suite.addTestSuite(BlockStyleNameExecutableTest.class);
        suite.addTestSuite(InlineStyleNameExecutableTest.class);
    }
}
