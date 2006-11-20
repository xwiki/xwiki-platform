package com.xpn.xwiki.test.store.jcr;

import java.net.URL;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.jcr.XWikiJcrStore;
import com.xpn.xwiki.web.XWikiServletURLFactory;

/** Class for convert hibernate tests to jcr tests */
public class JcrTestConvertor {
	public static void setUp(XWikiContext context) throws Exception {
		context.setDatabase("xwikitest");
		XWikiConfig config = new XWikiConfig();
		makeConfig(config);
		
		XWiki xwiki = new XWiki(config, context);
		xwiki.setDatabase("xwikitest");
		context.setDatabase(xwiki.getDatabase());
		context.setWiki(xwiki);
		context.setURLFactory(new XWikiServletURLFactory(new URL("http://www.xwiki.org/"), "xwiki/" , "bin/"));
		XWikiJcrStore store = (XWikiJcrStore) xwiki.getNotCacheStore();
		store.createWiki(context.getDatabase(), context);
		store.cleanupWiki(context.getDatabase());
		xwiki.flushCache();
		System.gc();
    }
	public static void tearDown(XWikiContext context) {
		XWikiJcrStore store = (XWikiJcrStore) context.getWiki().getNotCacheStore();		
		store.shutdown(context);
		context.clear();
		System.gc();
	}
	protected static void makeConfig(XWikiConfig config) {
		config.put("xwiki.store.class",					"com.xpn.xwiki.store.jcr.XWikiJcrStore");
		config.put("xwiki.store.attachment.class",		"com.xpn.xwiki.store.jcr.XWikiJcrAttachmentStore");
		config.put("xwiki.store.versioning.class",		"com.xpn.xwiki.store.jcr.XWikiJcrPropertyVersioningStore");
		config.put("xwiki.backlinks",					"1");
		config.put("xwiki.store.cache",					"1");
		config.put("xwiki.store.jcr.provider",			"com.xpn.xwiki.store.jcr.JackRabbitJCRProvider");
		config.put("xwiki.store.jcr.mapping",			"jcrmapping.xml");
		config.put("xwiki.store.jcr.jackrabbit.repository.config",	"jackrabbit/repository.xml");
		config.put("xwiki.store.jcr.jackrabbit.repository.path",	"jackrabbitrepo");
		config.put("xwiki.store.jcr.jackrabbit.nodetypes.config",	"jackrabbit/nodetypes.cnd");		
    }
}
