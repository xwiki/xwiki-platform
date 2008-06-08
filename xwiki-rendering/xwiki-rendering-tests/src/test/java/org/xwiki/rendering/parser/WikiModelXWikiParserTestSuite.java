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
package org.xwiki.rendering.parser;

import org.xwiki.rendering.scaffolding.TestEventsListener;
import org.xwiki.rendering.scaffolding.ParserListenerTestSuite;
import org.xwiki.rendering.scaffolding.AbstractRenderingTestCase;
import org.xwiki.rendering.scaffolding.ParserListenerTester;
import junit.framework.Test;

public class WikiModelXWikiParserTestSuite extends AbstractRenderingTestCase
{
    public static Test suite() throws Exception
    {
        Syntax syntax = new Syntax(SyntaxType.XWIKI, "2.0");

        ParserListenerTestSuite suite = 
            new ParserListenerTestSuite("Test the WikiModel Parser for XWiki");
        suite.addTestSuite(syntax, TestEventsListener.class);

        // Add tests specific to the XWiki Parser

        // TODO: Move this test to ParserListenerTestSuite once it passes with the XHTL and
        // XWiki Syntax renderers.
        suite.addTest(new ParserListenerTester("links", syntax, TestEventsListener.class, false));

        return suite;
    }
}
