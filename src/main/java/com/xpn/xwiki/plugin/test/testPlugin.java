package com.xpn.xwiki.plugin.test;

import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.XWikiContext;

/**
 * Created by IntelliJ IDEA.
 * User: jeremi
 * Date: Sep 1, 2006
 * Time: 12:33:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class testPlugin extends XWikiDefaultPlugin  implements XWikiPluginInterface  {
    public testPlugin(String name, String className, XWikiContext context) {
        super(name, className, context);
    }

    public String commonTagsHandler(String line, XWikiContext context){
        return "It's working";
    }

}
