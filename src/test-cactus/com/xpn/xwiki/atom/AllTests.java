package com.xpn.xwiki.atom;

import org.apache.cactus.extension.jetty.JettyTestSetup;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

  public static Test suite () {
    
    System.setProperty("cactus.contextURL", "http://localhost:8080/xwiki");
    
    TestSuite suite= new TestSuite("Test for com.xpn.xwiki.atom");
    //$JUnit-BEGIN$
    suite.addTestSuite(XWikiHelperIntegrationTest.class);
    //$JUnit-END$
    
    return new JettyTestSetup(suite);
  }

}
