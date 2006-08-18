package com.xpn.xwiki.test.store.jcr;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.test.XWikiTest;

public class XWikiJcrStoreTest extends XWikiTest {
	public void setUp() throws Exception {
		this.context = new XWikiContext();
		JcrTestConvertor.setUp(getXWikiContext());
		xwiki = context.getWiki();
    }
	
	protected void tearDown() {
		JcrTestConvertor.tearDown(getXWikiContext());
	}
	
	public void testXWikiPrefs() throws XWikiException {
		// super.testXWikiPrefs() is Hibernate-specific
	}
	
	public void testXWikiInit() throws XWikiException {
		// super.testXWikiInit() is Hibernate-specific
		
	}
}
