
import junit.framework.TestCase;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.test.RenderTest;
import com.xpn.xwiki.render.XWikiRenderer;
import com.xpn.xwiki.render.XWikiWikiBaseRenderer;
import com.xpn.xwiki.plugin.XWikiPluginManager;

/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at 
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 21 janv. 2004
 * Time: 13:54:52
 */

public class PluginTest extends TestCase {

    private static XWiki xwiki;
    private static XWikiContext context;

    public PluginTest() throws XWikiException {
        context = new XWikiContext();
        xwiki = new XWiki("./xwiki.cfg", context);
        context.setWiki(xwiki);
        xwiki.setPluginManager(new XWikiPluginManager("com.xpn.xwiki.plugin.PatternPlugin", context));
    }

        public void testSmilies() throws XWikiException {
        XWikiRenderer wikibase = new XWikiWikiBaseRenderer();
        RenderTest.renderTest(wikibase, "Hello 1\n:)\nHello 2",
                "Hello 1\nI am happy\nHello 2", true, context);
        RenderTest.renderTest(wikibase, "Hello 1\n:(\nHello 2",
                    "Hello 1\nI am sad\nHello 2", true, context);
        RenderTest.renderTest(wikibase, "Hello 1\n:) :)\nHello 2",
                    "Hello 1\nI am happy I am happy\nHello 2", true, context);
    }

    public void testPatternsTag() throws XWikiException {
    XWikiRenderer wikibase = new XWikiWikiBaseRenderer();
    RenderTest.renderTest(wikibase, "Hello 1\n%PATTERNS%\nHello 2",
            "I am happy", false, context);
    }

}
