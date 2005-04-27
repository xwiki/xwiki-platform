package com.xpn.xwiki.atom.lifeblog;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.cactus.extension.jetty.JettyTestSetup;

import com.xpn.xwiki.test.XWikiIntegrationTest;


public class LifeblogActionIntegrationTest extends XWikiIntegrationTest {

  public static Test suite () {
    boolean inContainer = System.getProperty("cactus.contextURL") != null;

    if (!inContainer) {
      // Will use Jetty
      System.setProperty("cactus.contextURL", "http://localhost:8080/xwiki");
    }
     
    TestSuite suite= new TestSuite("Test for com.xpn.xwiki.atom.lifeblog.LifeblogActionIntegrationTest");
    //$JUnit-BEGIN$
    suite.addTestSuite(LifeblogActionIntegrationTest.class);
    //$JUnit-END$
    
    return inContainer ? suite : new JettyTestSetup(suite);
  }
  
  /*
   * Class under test for ActionForward execute(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse)
   */
  public void testUnathorized() throws Exception {
    throw new RuntimeException("not implemented");
  }

}
