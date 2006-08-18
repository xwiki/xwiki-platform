package com.xpn.xwiki.test.store.jcr;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.test.AbstractStoreObjectTest;

public class ObjectJcrStoreTest extends AbstractStoreObjectTest {
	private XWikiContext context = new XWikiContext();
	
	protected void setUp() throws Exception {
		JcrTestConvertor.setUp(context);
		super.setUp();
	}
	protected void tearDown() throws Exception {
		JcrTestConvertor.tearDown(context);
		super.tearDown();
	}
	protected XWikiContext getXWikiContext() {
		return context;
	}
}
