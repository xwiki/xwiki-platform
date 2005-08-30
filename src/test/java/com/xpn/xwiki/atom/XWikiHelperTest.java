package com.xpn.xwiki.atom;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionMapping;
import org.jmock.Mock;

import com.xpn.xwiki.XWikiException;

public class XWikiHelperTest extends org.jmock.cglib.MockObjectTestCase {

  protected void setUp() throws Exception {
    super.setUp();
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testInitXWikiContextNotNull() throws XWikiException {
    // Create mocks
    Mock mockActionMapping = mock(ActionMapping.class, "mockActionMapping");
    Mock mockServletRequest = mock(HttpServletRequest.class, "mockServletRequest");
    Mock mockServletResponse = mock(HttpServletResponse.class, "mockServletResponse");
    Mock mockServletContext = mock(ServletContext.class, "mockServletContext");
    
    // Set expectations
    mockServletRequest.expects(once()).method("getRequestURL").will(returnValue(new StringBuffer("http://127.0.0.1:9080/xwiki/bin/lifeblog")));
    mockServletRequest.expects(once()).method("getQueryString").will(returnValue(""));
    mockServletRequest.expects(once()).method("getServletPath").will(returnValue("/bin/lifeblog"));
    
    XWikiHelper xwikiHelper = new XWikiHelper();
    xwikiHelper.initXWikiContext(
        "lifeblog",
        (HttpServletRequest)mockServletRequest.proxy(),
        (HttpServletResponse)mockServletResponse.proxy(),
        (ServletContext)mockServletContext.proxy());
    
    assertNotNull("XWikiContext should not be null", xwikiHelper.getXwikiContext());
  }

}
