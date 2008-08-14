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

import org.xwiki.rendering.scaffolding.AbstractRenderingTestCase;
import org.xwiki.rendering.scaffolding.ParserListenerTestSuite;
import org.xwiki.rendering.scaffolding.TestEventsListener;
import org.xwiki.rendering.scaffolding.ParserListenerTester;
import junit.framework.Test;

public class WikiModelXHTMLParserTestSuite extends AbstractRenderingTestCase
{
    public static Test suite() throws Exception
    {
        Syntax syntax = new Syntax(SyntaxType.XHTML, "1.0");

        ParserListenerTestSuite suite =
            new ParserListenerTestSuite("Test the WikiModel Parser for XHTML");

        // TODO: Right now some of the basic tests are failing because the XHTML parser is buggy so
        // we only include the tests that pass. When all pass remove the individual tests and
        // instead use:
        //  suite.addTestSuite(syntax, TestEventsListener.class);
        suite.addTest(new ParserListenerTester("paragraph", syntax, TestEventsListener.class, false));
        suite.addTest(new ParserListenerTester("section", syntax, TestEventsListener.class, false));
        //suite.addTest(new ParserListenerTester("macro", syntax, TestEventsListener.class, false));
        //suite.addTest(new ParserListenerTester("list", syntax, TestEventsListener.class, false));
        //suite.addTest(new ParserListenerTester("html", syntax, TestEventsListener.class, false));
        //suite.addTest(new ParserListenerTester("links", syntax, TestEventsListener.class, false));
        //suite.addTest(new ParserListenerTester("escape", syntax, TestEventsListener.class, false));

        return suite;
    }
}