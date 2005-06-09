/**
 * ===================================================================
 *
 * Copyright (c) 2003-2005 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.
 */
package com.xpn.xwiki.test;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.PatternPlugin;
import com.xpn.xwiki.plugin.XWikiPluginManager;
import com.xpn.xwiki.render.XWikiRenderingEngine;

public class PluginTest extends HibernateTestCase {

    public void setUp() throws Exception {
        super.setUp();
        getXWiki().setPluginManager(new XWikiPluginManager("com.xpn.xwiki.plugin.PatternPlugin, com.xpn.xwiki.plugin.TablePlugin", getXWikiContext()));
        PatternPlugin pplugin = (PatternPlugin) getXWiki().getPluginManager().getPlugin("com.xpn.xwiki.plugin.PatternPlugin");
        pplugin.addPattern(":)","smile","no desc");
        pplugin.addPattern(":-)","smile","no desc");
        pplugin.addPattern("8-)","cool","no desc");
        pplugin.addPattern("s/[bB]ug\\s+([0-9]+?)/http:\\/\\/bugzilla.xpertnet.biz\\/show_bug.cgi?id=$1/go","bugzilla link","no desc");
    }

    public void testSmilies() throws XWikiException {
        XWikiRenderingEngine wikibase = getXWiki().getRenderingEngine();
        AbstractRenderTest.renderTest(wikibase, "Hello 1\n :)\nHello 2",
                "smile", false, getXWikiContext());
        AbstractRenderTest.renderTest(wikibase, "Hello 1\n :-)\nHello 2",
                "smile", false, getXWikiContext());
        AbstractRenderTest.renderTest(wikibase, "Hello 1\n :) :)\nHello 2",
                "smile smile", false, getXWikiContext());
        AbstractRenderTest.renderTest(wikibase, "Hello 1\n :) 8-)\nHello 2",
                "smile cool", false, getXWikiContext());
    }

    public void testBugzilla() throws XWikiException {
        XWikiRenderingEngine wikibase = getXWiki().getRenderingEngine();
        AbstractRenderTest.renderTest(wikibase, "hello bug 234 end",
                "http://bugzilla.xpertnet.biz/show_bug.cgi?id=234", false, getXWikiContext());
        AbstractRenderTest.renderTest(wikibase, "hello Bug 234 end",
                "http://bugzilla.xpertnet.biz/show_bug.cgi?id=234", false, getXWikiContext());
    }

    public void testPatternsTag() throws XWikiException {
        XWikiRenderingEngine wikibase = getXWiki().getRenderingEngine();
        AbstractRenderTest.renderTest(wikibase, "Hello 1\n%PATTERNS%\nHello 2",
                "smile", false, getXWikiContext());
    }

    public void testTablePlugin() throws XWikiException {
        XWikiRenderingEngine wikibase = getXWiki().getRenderingEngine();
        AbstractRenderTest.renderTest(wikibase, "Hello 1\n| a | b |c |\n| d | e | f |\nHello 2",
                "<table", false, getXWikiContext());
        AbstractRenderTest.renderTest(wikibase, "Hello 1\n| a | b | c |\n| d | e | f |\nHello 2",
                "DDDDDD", false, getXWikiContext());
        AbstractRenderTest.renderTest(wikibase, "Hello 1\n%TABLE{ headerbg=\"#EEEEEE\" }%\n| a | b | c |\n| d | e | f |\nHello 2",
                "EEEEEE", false, getXWikiContext());
    }

}
