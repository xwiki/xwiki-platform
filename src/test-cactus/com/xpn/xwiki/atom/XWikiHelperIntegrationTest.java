package com.xpn.xwiki.atom;

import java.net.MalformedURLException;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.cactus.extension.jetty.JettyTestSetup;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.atom.lifeblog.LifeblogActionIntegrationTest;
import com.xpn.xwiki.atom.lifeblog.UserBlog;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.XWikiIntegrationTest;

public class XWikiHelperIntegrationTest extends XWikiIntegrationTest {

  public static Test suite () {
    boolean inContainer = System.getProperty("cactus.contextURL") != null;

    if (!inContainer) {
      // Will use Jetty
      System.setProperty("cactus.contextURL", "http://localhost:8080/xwiki");
    }
      
    TestSuite suite= new TestSuite("Test for com.xpn.xwiki.atom.XWikiHelperIntegrationTest");
    //$JUnit-BEGIN$
    suite.addTestSuite(XWikiHelperIntegrationTest.class);
    //$JUnit-END$
   
    return inContainer ? suite : new JettyTestSetup(suite);
  }

  public void testGetAtomAuthenticationTokenUnknownUser() throws XWikiException {
    XWikiHelper xwikiHelper = new XWikiHelper(context);
    
    String authenticationToken = xwikiHelper.getAtomAuthenticationToken("XWiki.UnknownUser");
    assertNull("Authentication Token for Unknown User should be null", authenticationToken);
 }

  public void testGetAtomAuthenticationToken() throws XWikiException {
    XWikiHelper xwikiHelper = new XWikiHelper(context);
    
    String authenticationToken = xwikiHelper.getAtomAuthenticationToken("XWiki.Admin");
    assertNotNull("Authentication for known user should not be null", authenticationToken);
    System.out.println("Authentication Token : " + authenticationToken);
  }
  
  public void testListUserBlogs() throws XWikiException, MalformedURLException {
    XWiki xwiki = context.getWiki();
    context.setAction("lifeblog");
 
    // Make sure there's at least one BlogPreferences object at Blog.WebHome
    XWikiDocument doc = null;
    try {
      doc = xwiki.getDocument("Blog.WebHome", context);
    } catch (Exception e) {
    }
    if (doc != null) {
      BaseObject bobject = doc.getObject("XWiki.BlogPreferences");
      if (bobject==null) {
        bobject = new BaseObject();
        bobject.setName("Blog.WebHome");
        bobject.setClassName("XWiki.BlogPreferences");
        bobject.setStringValue("blogname", "XWiki Blog");
        bobject.setIntValue("pingenabled", 1);
        doc.addObject("XWiki.BlogPreferences", bobject);
        xwiki.saveDocument(doc, context);
      } 
    }
 
    XWikiHelper xwikiHelper = new XWikiHelper(context);

    List userBlogs = xwikiHelper.listUserBlogs("XWiki.Admin");
    
    assertNotNull("There is at least one user blog", userBlogs);
    assertEquals(userBlogs.size(), 1);
    
    UserBlog userBlog = new UserBlog();
    userBlog.setPostHref("http://localhost:8080/xwiki/bin/lifeblog/Blog/WebHome");
    userBlog.setFeedHref("http://localhost:8080/xwiki/bin/view/Blog/WebHome?xpage=rdf");
    userBlog.setAlternateHref("http://localhost:8080/xwiki/bin/view/Blog/WebHome");
    
    assertEquals(userBlog, (UserBlog)userBlogs.get(0));
  }

}
