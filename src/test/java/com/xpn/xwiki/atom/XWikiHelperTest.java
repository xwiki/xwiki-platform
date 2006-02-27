/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * @author ludovic
 * @author vmassol
 */
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
