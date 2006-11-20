package com.xpn.xwiki.test.store.jcr;

import java.net.URL;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.test.BackLinksHibernateTest;
import com.xpn.xwiki.web.XWikiServletURLFactory;

public class BackLinksJcrStoreTest extends BackLinksHibernateTest {
	public void setUp() throws Exception {
		this.context = new XWikiContext();
		JcrTestConvertor.setUp(getXWikiContext());
		xwiki = context.getWiki();
		getXWikiContext().setURLFactory(new XWikiServletURLFactory(new URL("http://www.xwiki.org/"), "xwiki/" , "bin/"));
    }
	
	protected void tearDown() {
		JcrTestConvertor.tearDown(getXWikiContext());
	}
	
	public void testBackLinksHibernateDelete() throws XWikiException {}
	public void testBackLinkHibernateWrite() throws XWikiException {}
}
