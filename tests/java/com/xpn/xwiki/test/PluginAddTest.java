package com.xpn.xwiki.test;

import com.xpn.xwiki.plugin.XWikiPluginManager;

/**
 * Created by IntelliJ IDEA.
 * User: jeremi
 * Date: Sep 1, 2006
 * Time: 12:30:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class PluginAddTest extends HibernateTestCase {

    public void setUp() throws Exception {
        super.setUp();
        getXWiki().setPluginManager(new XWikiPluginManager("com.xpn.xwiki.plugin.test.testPlugin", getXWikiContext()));
    }

    public void testGetPlugin(){
        assertNull(getXWiki().getPluginManager().getPlugins("invalidFunctionName"));
        assertEquals("There should be one plugin with the function commonTagsHandler implemented", 1, getXWiki().getPluginManager().getPlugins("commonTagsHandler").size());
        assertEquals(0, getXWiki().getPluginManager().getPlugins("flushCache").size());        
    }


}
