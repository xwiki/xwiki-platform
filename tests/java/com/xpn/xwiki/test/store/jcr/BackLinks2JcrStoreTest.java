package com.xpn.xwiki.test.store.jcr;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.test.BackLinksTest;

public class BackLinks2JcrStoreTest extends BackLinksTest {
	public void setUp() throws Exception {
		this.context = new XWikiContext();
		JcrTestConvertor.setUp(getXWikiContext());
		xwiki = context.getWiki();
    }
	
	protected void tearDown() {
		JcrTestConvertor.tearDown(getXWikiContext());
	}
}
