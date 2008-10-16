package com.xpn.xwiki.plugin.webdav;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.plugin.webdav.utils.XWikiDavUtils;

public class XWikiDavApi extends Api {
	
	private XWikiDavPlugin plugin;
	
	public XWikiDavApi(XWikiDavPlugin plugin, XWikiContext context) {
		super(context);		
		this.plugin = plugin;
	}
	
	public String getDavURL(Document doc, Attachment attachment) {
		return XWikiDavUtils.getDavURL(doc, attachment);
	}
}
