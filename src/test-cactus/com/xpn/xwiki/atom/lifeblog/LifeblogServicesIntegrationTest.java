package com.xpn.xwiki.atom.lifeblog;

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
  
  public static Test suite () {
    
    System.setProperty("cactus.contextURL", "http://localhost:8080/xwiki");
    
    TestSuite suite= new TestSuite("Test for com.xpn.xwiki.atom.lifeblog.LifeblogServicesIntegrationTest");
    //$JUnit-BEGIN$
    suite.addTestSuite(LifeblogServicesIntegrationTest.class);
    //$JUnit-END$
    
    return new JettyTestSetup(suite);
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
  
  public void testListUserBlogs() {
    XWikiHelper xwikiHelper = new XWikiHelper(context);
    LifeblogContext lifeblogContext = new LifeblogContext(xwikiHelper);
    
    LifeblogServices lifeblogServices = new LifeblogServices(lifeblogContext);
    lifeblogContext.setUserName("UnknownUser");
    
    
  }
}
