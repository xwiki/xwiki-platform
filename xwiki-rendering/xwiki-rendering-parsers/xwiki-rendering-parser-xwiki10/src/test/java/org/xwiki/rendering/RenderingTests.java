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

import junit.framework.Test;
import junit.framework.TestCase;

import org.xwiki.rendering.scaffolding.RenderingTestCase;
import org.xwiki.rendering.scaffolding.RenderingTestSuite;
import org.xwiki.test.ComponentManagerTestSetup;

/**
 * All Rendering integration tests defined in text files using a special format.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class RenderingTests extends TestCase
{
    public static Test suite() throws Exception
    {
        RenderingTestSuite suite = new RenderingTestSuite("Test XWiki1.0 -> XWiki2.0 conversion");

        // Velocity
        suite.addTestsFromResource("velocity/velocity1", false);
        suite.addTestsFromResource("velocity/velocity2", false);
        suite.addTestsFromResource("velocity/velocity3", false);
        suite.addTestsFromResource("velocity/velocity4", false);
        suite.addTestsFromResource("velocity/velocity5", false);
        suite.addTestsFromResource("velocity/velocity6", false);
        suite.addTestsFromResource("velocity/velocity7", false);
        suite.addTestsFromResource("velocity/velocity8", false);
        suite.addTestsFromResource("velocity/velocity9", false);
        suite.addTestsFromResource("velocity/velocity10", false);
        suite.addTestsFromResource("velocity/velocity11", false);
        suite.addTestsFromResource("velocity/velocity12", false);
        suite.addTestsFromResource("velocity/velocity13", false);
        suite.addTestsFromResource("velocity/velocity14", false);
        suite.addTestsFromResource("velocity/velocity15", false);
        suite.addTestsFromResource("velocity/velocity16", false);
        suite.addTestsFromResource("velocity/velocity17", false);
        suite.addTestsFromResource("velocity/velocity18", false);
        suite.addTestsFromResource("velocity/velocity19", false);
        suite.addTestsFromResource("velocity/velocity20", false);
        suite.addTestsFromResource("velocity/velocity21", false);
        suite.addTestsFromResource("velocity/velocity22", false);

        // Groovy
        suite.addTestsFromResource("groovy/groovy1", false);

        // Velocity Macros
        suite.addTestsFromResource("macros/velocity/include1", false);
        suite.addTestsFromResource("macros/velocity/toc1", false);
        suite.addTestsFromResource("macros/velocity/info1", false);
        suite.addTestsFromResource("macros/velocity/warning1", false);
        suite.addTestsFromResource("macros/velocity/error1", false);

        // Radeox Macros
        suite.addTestsFromResource("macros/radeox/attach1", false);
        suite.addTestsFromResource("macros/radeox/image1", false);
        suite.addTestsFromResource("macros/radeox/code1", false);
        suite.addTestsFromResource("macros/radeox/table1", false);
        suite.addTestsFromResource("macros/radeox/quote1", false);
        suite.addTestsFromResource("macros/radeox/style1", false);

        suite.addTestsFromResource("pre/pre1", false);
        suite.addTestsFromResource("pre/pre2", false);

        // Wiki syntaxes
        suite.addTestsFromResource("italic/italic1", false);
        suite.addTestsFromResource("bold/bold1", false);
        suite.addTestsFromResource("underline/underline1", false);
        suite.addTestsFromResource("strikeout/strikeout1", false);
        suite.addTestsFromResource("link/link1", false);
        suite.addTestsFromResource("link/link2", false);
        suite.addTestsFromResource("list/list1", false);
        suite.addTestsFromResource("list/list2", false);
        suite.addTestsFromResource("list/list3", false);
        suite.addTestsFromResource("list/list4", false);
        suite.addTestsFromResource("section/section1", false);
        suite.addTestsFromResource("section/section2", false);
        suite.addTestsFromResource("hr/hr1", false);

        // HTML
        suite.addTestsFromResource("html/html1", false);
        suite.addTestsFromResource("html/html2", false);
        suite.addTestsFromResource("html/html3", false);
        suite.addTestsFromResource("html/html4", false);
        suite.addTestsFromResource("html/html5", false);
        suite.addTestsFromResource("html/html6", false);
        suite.addTestsFromResource("html/html7", false);
        suite.addTestsFromResource("html/html8", false);
        suite.addTestsFromResource("html/html9", false);
        suite.addTestsFromResource("html/html10", false);
        suite.addTestsFromResource("html/html11", false);
        suite.addTestsFromResource("html/html12", false);
        suite.addTestsFromResource("html/html13", false);
        suite.addTestsFromResource("html/html14", false);
        suite.addTestsFromResource("html/html15", false);
        suite.addTestsFromResource("html/html16", false);

        // HTML elements
        suite.addTestsFromResource("macros/html/br1", false);
        suite.addTestsFromResource("macros/html/div1", false);
        suite.addTestsFromResource("macros/html/span1", false);

        // Escape
        suite.addTestsFromResource("escape/escape1", false);
        suite.addTestsFromResource("escape/escape2", false);
        suite.addTestsFromResource("escape/escape3", false);

        // Cleaning
        suite.addTestsFromResource("cleaning/cleaning1", false);

        // Misc
        suite.addTestsFromResource("misc/misc1", false);
        suite.addTestsFromResource("misc/misc2", false);
        
        return new ComponentManagerTestSetup(suite, RenderingTestCase.DEFAULT_MOCK_DESCRIPTORS);
    }
}
