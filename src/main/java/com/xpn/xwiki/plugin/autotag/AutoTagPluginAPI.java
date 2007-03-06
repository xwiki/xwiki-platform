package com.xpn.xwiki.plugin.autotag;

import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.XWikiContext;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 22 janv. 2007
 * Time: 10:09:26
 * To change this template use File | Settings | File Templates.
 */
public class AutoTagPluginAPI extends Api {

    private AutoTagPlugin plugin;

    public AutoTagPluginAPI(XWikiContext context) {
        super(context);
    }

    public AutoTagPluginAPI(AutoTagPlugin autoTagPlugin, XWikiContext context) {
        super(context);
        this.plugin = autoTagPlugin;
    }

    public TagCloud generateTagCloud(String text, int lang) {
        return plugin.generateTagCloud(text, lang);
    }
}
