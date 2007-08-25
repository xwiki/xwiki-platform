package com.xpn.xwiki.plugin.test;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

public class testPlugin extends XWikiDefaultPlugin  implements XWikiPluginInterface  {
    public testPlugin(String name, String className, XWikiContext context) {
        super(name, className, context);
    }

    public String commonTagsHandler(String line, XWikiContext context){
        return "It's working";
    }

}
