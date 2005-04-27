package com.xpn.xwiki.atom.lifeblog;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.Calendar;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.cactus.extension.jetty.JettyTestSetup;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.atom.WSSEHttpHeader;
import com.xpn.xwiki.atom.XWikiHelper;
import com.xpn.xwiki.test.XWikiIntegrationTest;

public class LifeblogServicesIntegrationTest extends XWikiIntegrationTest {

  private static final String TEST_ATOM_USER_BLOG_LIST = 
    "<?xml version=\"1.0\"?>\r\n"
    + "<feed xmlns=\"http://purl.org/atom/ns#\">\r\n"
    + "<link type=\"application/atom+xml\" rel=\"service.post\" href=\"http://localhost:8080/xwiki/bin/lifeblog/Blog/WebHome\" title=\"Blog.WebHome\"/>\r\n"
    + "<link type=\"application/atom+xml\" rel=\"service.feed\" href=\"http://localhost:8080/xwiki/bin/view/Blog/WebHome?xpage=rdf\" title=\"Blog.WebHome\"/>\r\n"
    + "<link type=\"application/atom+xml\" rel=\"service.alternate\" href=\"http://localhost:8080/xwiki/bin/lifeblog/Blog/WebHome\" title=\"Blog.WebHome\"/>\r\n"
    + "</feed>";
  
  public static Test suite () {
    boolean inContainer = System.getProperty("cactus.contextURL") != null;

    if (!inContainer) {
      // Will use Jetty
      System.setProperty("cactus.contextURL", "http://localhost:8080/xwiki");
    }
    
    TestSuite suite= new TestSuite("Test for com.xpn.xwiki.atom.lifeblog.LifeblogServicesIntegrationTest");
    //$JUnit-BEGIN$
    suite.addTestSuite(LifeblogServicesIntegrationTest.class);
    //$JUnit-END$
    
    if (inContainer) {
      return suite;
    } else {
      return new JettyTestSetup(suite);
    }
  }

  public void testIsAuthenticatedNullHeader() throws LifeblogServiceException, XWikiException, ParseException {
    String header = null;
    
    XWikiHelper xwikiHelper = new XWikiHelper(context);
    LifeblogContext lifeblogContext = new LifeblogContext(xwikiHelper);
    
    LifeblogServices lifeblogServices = new LifeblogServices(lifeblogContext);

    boolean authenticated = lifeblogServices.isAuthenticated(header);
    
    assertFalse("Not authenticated if no WSSE header", authenticated);
  }

  public void testIsAuthenticated() throws LifeblogServiceException, XWikiException, ParseException {
    String nonce = "d36e316282959a9ed4c89851497a717f";
    String created = WSSEHttpHeader.CalendarToW3CDSTFormat(Calendar.getInstance());
    String login = "Admin";
    String password = "admin";
    String passwordDigest = new String(Base64.encodeBase64(DigestUtils.sha(nonce + created + password)));

    String header = "UsernameToken Username=\"" + login +"\", PasswordDigest=\"" + passwordDigest + "\", Nonce=\""+ nonce + "\", Created=\""+ created + "\"";
    
    XWikiHelper xwikiHelper = new XWikiHelper(context);
    LifeblogContext lifeblogContext = new LifeblogContext(xwikiHelper);
    
    LifeblogServices lifeblogServices = new LifeblogServices(lifeblogContext);

    boolean authenticated = lifeblogServices.isAuthenticated(header);
    
    assertTrue(authenticated);
  }

  public void testIsAuthenticatedSameNonceTwice() throws LifeblogServiceException, XWikiException, ParseException {
    String nonce = "d36e316282959a9ed4c89851497a717f";
    String created = WSSEHttpHeader.CalendarToW3CDSTFormat(Calendar.getInstance());
    String login = "Admin";
    String password = "admin";
    String passwordDigest = new String(Base64.encodeBase64(DigestUtils.sha(nonce + created + password)));

    String header = "UsernameToken Username=\"" + login +"\", PasswordDigest=\"" + passwordDigest + "\", Nonce=\""+ nonce + "\", Created=\""+ created + "\"";
    
    XWikiHelper xwikiHelper = new XWikiHelper(context);
    LifeblogContext lifeblogContext = new LifeblogContext(xwikiHelper);
    
    LifeblogServices lifeblogServices = new LifeblogServices(lifeblogContext);

    boolean authenticated = lifeblogServices.isAuthenticated(header);    
    assertTrue(authenticated);
    
    authenticated = lifeblogServices.isAuthenticated(header);   
    assertFalse("Same Nonce twice", authenticated);
  }
}
