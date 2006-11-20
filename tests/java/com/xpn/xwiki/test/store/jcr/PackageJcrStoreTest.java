package com.xpn.xwiki.test.store.jcr;

import org.hibernate.HibernateException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.jcr.XWikiJcrStore;
import com.xpn.xwiki.test.PackageTest;

public class PackageJcrStoreTest extends PackageTest {
	public void setUp() throws Exception {
		this.context = new XWikiContext();
		JcrTestConvertor.setUp(getXWikiContext());
		xwiki = context.getWiki();
		prepareData();
    }
	
	protected void tearDown() {
		JcrTestConvertor.tearDown(getXWikiContext());
	}
	
	public void cleanUpStore() throws HibernateException, XWikiException {
       	((XWikiJcrStore)getXWiki().getNotCacheStore()).cleanupWiki(getXWiki().getDatabase());
	}
}
