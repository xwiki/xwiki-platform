
package com.xpn.xwiki.test;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.PatternPlugin;
import com.xpn.xwiki.plugin.XWikiPluginManager;
import com.xpn.xwiki.render.XWikiRenderingEngine;
import junit.framework.TestCase;

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
        PatternPlugin pplugin = (PatternPlugin) xwiki.getPluginManager().getPlugin("com.xpn.xwiki.plugin.PatternPlugin");
        pplugin.addPattern(":)","smile","no desc");
        pplugin.addPattern(":-)","smile","no desc");
        pplugin.addPattern("8-)","cool","no desc");
        pplugin.addPattern("s/[bB]ug\\s+([0-9]+?)/http:\\/\\/bugzilla.xpertnet.biz\\/show_bug.cgi?id=$1/go","bugzilla link","no desc");
    }


    public void testSmilies() throws XWikiException {
        XWikiRenderingEngine wikibase = xwiki.getRenderingEngine();
        RenderTest.renderTest(wikibase, "Hello 1\n :)\nHello 2",
                "smile", false, context);
        RenderTest.renderTest(wikibase, "Hello 1\n :-)\nHello 2",
                "smile", false, context);
        RenderTest.renderTest(wikibase, "Hello 1\n :) :)\nHello 2",
                "smile smile", false, context);
        RenderTest.renderTest(wikibase, "Hello 1\n :) 8-)\nHello 2",
                "smile cool", false, context);
    }

    public void testBugzilla() throws XWikiException {
        XWikiRenderingEngine wikibase = xwiki.getRenderingEngine();
        RenderTest.renderTest(wikibase, "hello bug 234 end",
                "http://bugzilla.xpertnet.biz/show_bug.cgi?id=234", false, context);
        RenderTest.renderTest(wikibase, "hello Bug 234 end",
                "http://bugzilla.xpertnet.biz/show_bug.cgi?id=234", false, context);
        RenderTest.renderTest(wikibase, "hello [bug 234] end",
                "http://bugzilla.xpertnet.biz/show_bug.cgi?id=234", false, context);
    }

    public void testPatternsTag() throws XWikiException {
        XWikiRenderingEngine wikibase = xwiki.getRenderingEngine();
        RenderTest.renderTest(wikibase, "Hello 1\n%PATTERNS%\nHello 2",
                "smile", false, context);
    }

}
