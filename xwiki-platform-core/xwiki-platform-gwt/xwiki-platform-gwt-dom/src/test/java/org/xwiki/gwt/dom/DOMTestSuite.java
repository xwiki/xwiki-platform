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
package org.xwiki.gwt.dom;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.xwiki.gwt.dom.client.DOMUtilsTest;
import org.xwiki.gwt.dom.client.DepthFirstPreOrderIteratorTest;
import org.xwiki.gwt.dom.client.DocumentFragmentTest;
import org.xwiki.gwt.dom.client.DocumentTest;
import org.xwiki.gwt.dom.client.ElementTest;
import org.xwiki.gwt.dom.client.JavaScriptObjectTest;
import org.xwiki.gwt.dom.client.RangeTest;
import org.xwiki.gwt.dom.client.SelectionTest;
import org.xwiki.gwt.dom.client.StyleTest;
import org.xwiki.gwt.dom.client.TextTest;
import org.xwiki.gwt.dom.client.internal.ie.IEOldRangeTest;

import com.google.gwt.junit.tools.GWTTestSuite;

/**
 * All the DOM tests to be run. We're using a test suite because it decreases the overall testing time. GWTTestCase
 * derived tests are slow. This is because the JUnitShell has to load the module for each test (create the shell, hook
 * into it, etc). GWTTestSuite mitigates this by grouping all the tests that are for the same module (those that return
 * the same value for getModuleName) together and running them via the same shell instance.
 * <p>
 * As described in GWT issue 2486, GWTTestSuite is not a translatable class and in order to prevent reports of harmless
 * errors we moved our test suite class out of module's source path.
 * 
 * @version $Id$
 * @see <a href="http://code.google.com/p/google-web-toolkit/issues/detail?id=2486">Issue 2486</a>
 */
public class DOMTestSuite extends GWTTestSuite
{
    /**
     * @return The suite of all the DOM tests to be run.
     */
    public static Test suite()
    {
        TestSuite suite = new TestSuite("XWiki GWT-DOM Unit Tests");

        suite.addTestSuite(JavaScriptObjectTest.class);
        suite.addTestSuite(DOMUtilsTest.class);
        suite.addTestSuite(DocumentTest.class);
        suite.addTestSuite(DocumentFragmentTest.class);
        suite.addTestSuite(ElementTest.class);
        suite.addTestSuite(TextTest.class);
        suite.addTestSuite(RangeTest.class);
        suite.addTestSuite(IEOldRangeTest.class);
        suite.addTestSuite(SelectionTest.class);
        suite.addTestSuite(StyleTest.class);
        suite.addTestSuite(DepthFirstPreOrderIteratorTest.class);

        return suite;
    }
}
