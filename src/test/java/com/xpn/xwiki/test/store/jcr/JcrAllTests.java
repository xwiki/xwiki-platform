package com.xpn.xwiki.test.store.jcr;

import junit.framework.Test;
import junit.framework.TestSuite;

public class JcrAllTests {
	public static Test suite() {
		TestSuite suite = new TestSuite("JcrStore tests");
		//$JUnit-BEGIN$
		suite.addTestSuite(JcrTest.class);
		suite.addTestSuite(BaseJcrStoreTest.class);
		suite.addTestSuite(i18nJcrStoreTest.class);
		suite.addTestSuite(ObjectJcrStoreTest.class);
		suite.addTestSuite(LocksJcrStoreTest.class);
		suite.addTestSuite(BackLinksSimpleJcrStoreTest.class);
		suite.addTestSuite(BackLinks2JcrStoreTest.class);
		suite.addTestSuite(BackLinksJcrStoreTest.class);
		suite.addTestSuite(BackLinksIntegrationJcrStoreTest.class);
		suite.addTestSuite(DocumentInfoJcrStoreTest.class);
		suite.addTestSuite(ClassesJcrStoreTest.class);
		suite.addTestSuite(CustomClassJcrStoreTest.class);
		suite.addTestSuite(PackageJcrStoreTest.class);
		suite.addTestSuite(UserJcrStoreTest.class);
		suite.addTestSuite(XWikiJcrStoreTest.class);
		//$JUnit-END$
		return suite;
	}
}
