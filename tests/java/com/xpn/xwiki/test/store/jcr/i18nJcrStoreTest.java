package com.xpn.xwiki.test.store.jcr;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.test.i18nTest;

public class i18nJcrStoreTest extends i18nTest {
	public void setUp() throws Exception {
		this.context = new XWikiContext();
		JcrTestConvertor.setUp(getXWikiContext());
		xwiki = context.getWiki();
    }
	
	protected void tearDown() {
		JcrTestConvertor.tearDown(getXWikiContext());
	}
}
