package com.xpn.xwiki.test.store.jcr;

import org.hibernate.HibernateException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.test.HibernateClassesTest;

public class ClassesJcrStoreTest extends HibernateClassesTest {
	public void setUp() throws Exception {
		this.context = new XWikiContext();
		JcrTestConvertor.setUp(getXWikiContext());
		xwiki = context.getWiki();
    }
	
	protected void tearDown() {
		JcrTestConvertor.tearDown(getXWikiContext());
	}
	
	public void testDBListDisplayers() throws XWikiException, HibernateException {
		// TODO rewrite to jcr
		
	}
}
