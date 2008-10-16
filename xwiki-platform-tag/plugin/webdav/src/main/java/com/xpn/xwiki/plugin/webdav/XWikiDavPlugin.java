package com.xpn.xwiki.plugin.webdav;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

public class XWikiDavPlugin extends XWikiDefaultPlugin {

	public XWikiDavPlugin(String name, String className, XWikiContext context) {
		super(name, className, context);
		init(context);
	}

	public String getName() {
		return "webdav";
	}

	public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context) {
		return new XWikiDavApi((XWikiDavPlugin) plugin, context);
	}

}
