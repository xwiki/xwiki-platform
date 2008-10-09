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

import org.xwiki.rendering.scaffolding.RenderingTestSuite;
import com.xpn.xwiki.test.PlexusTestSetup;

/**
 * All Rendering integration tests defined in text files using a special format.
 * 
 * @version $Id: $
 * @since 1.6M1
 */
public class RenderingTests extends TestCase
{
    public static Test suite() throws Exception
    {
        RenderingTestSuite suite = new RenderingTestSuite("Test all Parsers/Renderers");

        
        // Text formatting
        suite.addTestsFromResource("bold/bold1", false);
        suite.addTestsFromResource("bold/bold2", false);
        suite.addTestsFromResource("bold/bold3", false);
        suite.addTestsFromResource("bold/bold4", false);
        suite.addTestsFromResource("bold/bold5", false);
        suite.addTestsFromResource("bold/bold6", false);
        suite.addTestsFromResource("bold/bold7", false);
        suite.addTestsFromResource("bold/bold8", false);
        suite.addTestsFromResource("italic/italic1", false);
        suite.addTestsFromResource("italic/italic2", false);
        suite.addTestsFromResource("italic/italic3", false);
        suite.addTestsFromResource("italic/italic4", false);
        suite.addTestsFromResource("italic/italic5", false);
        suite.addTestsFromResource("italic/italic6", false);
        suite.addTestsFromResource("italic/italic7", false);
        suite.addTestsFromResource("underline/underline1", false);
        suite.addTestsFromResource("underline/underline2", false);
        suite.addTestsFromResource("strikedout/strikedout1", false);
        suite.addTestsFromResource("strikedout/strikedout2", false);
        suite.addTestsFromResource("strikedout/strikedout3", false);
        suite.addTestsFromResource("superscript/superscript1", false);
        suite.addTestsFromResource("subscript/subscript1", false);
        suite.addTestsFromResource("monospace/monospace1", false);
        suite.addTestsFromResource("paragraph/paragraph1", false);
        suite.addTestsFromResource("paragraph/paragraph2", false);
        suite.addTestsFromResource("paragraph/paragraph3", false);
        suite.addTestsFromResource("paragraph/paragraph4", false);
        suite.addTestsFromResource("paragraph/paragraph5", false);
        suite.addTestsFromResource("paragraph/paragraph6", false);
        suite.addTestsFromResource("paragraph/paragraph7", false);
        suite.addTestsFromResource("paragraph/paragraph8", false);
        suite.addTestsFromResource("verbatim/verbatim1", false);
        suite.addTestsFromResource("verbatim/verbatim2", false);
        suite.addTestsFromResource("verbatim/verbatim3", false);
        suite.addTestsFromResource("verbatim/verbatim4", false);
        suite.addTestsFromResource("verbatim/verbatim5", false);

        // Table formatting
        suite.addTestsFromResource("table/table1", false);
        suite.addTestsFromResource("table/table2", false);
        suite.addTestsFromResource("table/table3", false);
        suite.addTestsFromResource("table/style/style1", false);

        // Macros
        suite.addTestsFromResource("macros/macro1", false);
        suite.addTestsFromResource("macros/macro2", false);
        suite.addTestsFromResource("macros/macro3", false);
        suite.addTestsFromResource("macros/macro4", false);
        suite.addTestsFromResource("macros/macro5", false);
        suite.addTestsFromResource("macros/macrohtml", true);
        suite.addTestsFromResource("macros/velocity/macrovelocity1", true);
        suite.addTestsFromResource("macros/velocity/macrovelocity2", true);
        suite.addTestsFromResource("macros/velocity/macrovelocity3", true);
        suite.addTestsFromResource("macros/xhtml/macroxhtml1", true);
        suite.addTestsFromResource("macros/xhtml/macroxhtml2", true);
        suite.addTestsFromResource("macros/xhtml/macroxhtml3", true);
        suite.addTestsFromResource("macros/xhtml/macroxhtml4", true);
        suite.addTestsFromResource("macros/xhtml/macroxhtml5", true);
        suite.addTestsFromResource("macros/macroid", true);
        suite.addTestsFromResource("macros/toc/macrotoc1", true);
        suite.addTestsFromResource("macros/toc/macrotoc2", true);
        suite.addTestsFromResource("macros/toc/macrotoc3", true);

        // Other
        suite.addTestsFromResource("escape/escape1", false);
        suite.addTestsFromResource("escape/escape2", false);
        suite.addTestsFromResource("escape/escape3", false);
        suite.addTestsFromResource("horizontalline/horizontalline1", false);
        suite.addTestsFromResource("horizontalline/horizontalline2", false);
        suite.addTestsFromResource("horizontalline/horizontalline3", false);
        suite.addTestsFromResource("horizontalline/horizontalline4", false);
        suite.addTestsFromResource("horizontalline/horizontalline5", false);
        suite.addTestsFromResource("link/links1", false);
        suite.addTestsFromResource("link/links2", false);
        suite.addTestsFromResource("link/links3", false);
        suite.addTestsFromResource("list/list1", false);
        suite.addTestsFromResource("list/list2", false);
        suite.addTestsFromResource("list/list3", false);
        suite.addTestsFromResource("list/list4", false);
        suite.addTestsFromResource("list/list5", false);
        suite.addTestsFromResource("list/list6", false);
        suite.addTestsFromResource("list/list7", false);
        suite.addTestsFromResource("list/list8", false);
        suite.addTestsFromResource("list/list9", false);
        suite.addTestsFromResource("list/definitionlist1", false);
        suite.addTestsFromResource("list/definitionlist2", false);
        suite.addTestsFromResource("list/definitionlist3", false);
        suite.addTestsFromResource("list/definitionlist4", false);
        suite.addTestsFromResource("list/definitionlist5", false);
        suite.addTestsFromResource("list/definitionlist6", false);
        suite.addTestsFromResource("section/section1", false);
        suite.addTestsFromResource("section/section2", false);
        suite.addTestsFromResource("section/section3", true);
        suite.addTestsFromResource("section/section4", false);
        suite.addTestsFromResource("section/section5", false);
        suite.addTestsFromResource("section/section6", false);
        suite.addTestsFromResource("quote/quote1", false);
        suite.addTestsFromResource("quote/quote2", false);
        suite.addTestsFromResource("quote/quote3", false);
        suite.addTestsFromResource("misc/misc1", false);
        suite.addTestsFromResource("misc/misc2", false);
        suite.addTestsFromResource("misc/misc3", false);
        suite.addTestsFromResource("misc/misc4", false);
        suite.addTestsFromResource("misc/misc5", false);
        suite.addTestsFromResource("encoding/encoding1", false);
        suite.addTestsFromResource("encoding/encoding2", false);
        
        //suite.addTestsFromResource("macros/code/macrocode1", true);
        
        return new PlexusTestSetup(suite);
    }
}
