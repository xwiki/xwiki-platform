package com.xpn.xwiki.atom.lifeblog;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.jmock.Mock;


public class LifeblogActionTest extends org.jmock.cglib.MockObjectTestCase {

  protected void setUp() throws Exception {
    super.setUp();
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }

  /*
   * Class under test for ActionForward execute(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse)
   */
  public void testUnathorized() throws Exception {
    // Create mocks
    Mock mockActionMapping = mock(ActionMapping.class, "mockActionMapping");
    Mock mockActionForm = mock(ActionForm.class, "mockActionForm");
    Mock mockServletRequest = mock(HttpServletRequest.class, "mockServletRequest");
    Mock mockServletResponse = mock(HttpServletResponse.class, "mockServletResponse");
    
    // Set expectations
    mockServletRequest.expects(once()).method("getHeader").with(eq("X-WSSE")).will(returnValue(null));
    mockServletResponse.expects(once()).method("setHeader").with(eq("WWW-Authenticate"),eq("WSSE realm=\"foo\", profile=\"UsernameToken\""));
    mockServletResponse.expects(once()).method("sendError").with(eq(401),eq("Unauthorized"));
    
    LifeblogAction action = new LifeblogAction();

    action.execute(
        (ActionMapping)mockActionMapping.proxy(),
        (ActionForm)mockActionForm.proxy(),
        (HttpServletRequest)mockServletRequest.proxy(),
        (HttpServletResponse)mockServletResponse.proxy());
  }

}
