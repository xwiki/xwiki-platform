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
package org.xwiki.rendering.renderer;

import org.xwiki.rendering.renderer.xhtml.XHTMLRenderer;
import org.xwiki.rendering.scaffolding.ParserListenerTestSuite;
import org.xwiki.rendering.scaffolding.ParserListenerTester;
import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.parser.SyntaxType;
import org.xwiki.rendering.DocumentManager;
import junit.framework.Test;
import junit.framework.TestCase;

/**
 * @version $Id$
 * @since 1.5M2
 */
public class XHTMLRendererTestSuite extends TestCase
{
    public static Test suite() throws Exception
    {
        Syntax syntax = new Syntax(SyntaxType.XWIKI, "2.0");

        ParserListenerTestSuite suite = new ParserListenerTestSuite("Test the XHTML Renderer");
        DocumentManager dm = new DocumentManager() {
            public String getDocumentContent(String documentName) throws Exception
            {
                return "Some content";
            }

            public boolean exists(String documentName) throws Exception
            {
                return documentName.equals("Space.ExistingPage");
            }

            public String getURL(String documentName, String action, String queryString, String anchor) throws Exception
            {
                String result = "/xwiki/bin/view/" + documentName.replace(".", "/");
                if (anchor != null) {
                    result = result + "#" + anchor;
                }
                if (queryString != null) {
                    result = result + "?" + queryString;
                }
                return result;
            }
        };
        suite.addTestSuite(syntax, XHTMLRenderer.class, dm);

        // Add tests specific to the XWiki Parser
        suite.addTest(new ParserListenerTester("macroxhtml", syntax, XHTMLRenderer.class, true, dm));

        return suite;
    }
}
