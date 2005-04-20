package com.xpn.xwiki.atom;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWikiIntegrationTest;

public class XWikiHelperIntegrationTest extends XWikiIntegrationTest {

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

}
