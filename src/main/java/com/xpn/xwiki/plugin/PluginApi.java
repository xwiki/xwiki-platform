package com.xpn.xwiki.plugin;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;

/**
 * Created by IntelliJ IDEA.
 * User: ludovic
 * Date: 22 août 2005
 * Time: 09:42:03
 * To change this template use File | Settings | File Templates.
 */
public abstract class PluginApi extends Api {
    private XWikiPluginInterface plugin;


    public PluginApi(XWikiPluginInterface plugin, XWikiContext context) {
            super(context);
            setPlugin(plugin);
        }

    public XWikiPluginInterface getPlugin() {
        return plugin;
    }

    public void setPlugin(XWikiPluginInterface plugin) {
        this.plugin = plugin;
    }
}
