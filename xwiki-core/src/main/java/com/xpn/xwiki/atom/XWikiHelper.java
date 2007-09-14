/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
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
 */

package com.xpn.xwiki.atom;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.atom.lifeblog.UserBlog;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.*;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class XWikiHelper {

  private XWikiContext xwikiContext;
  private static final int MAX_BLOGS = 10;

  /**
   * @return Returns the xwikiContext.
   */
  public XWikiContext getXwikiContext() {
    return xwikiContext;
  }

  /**
   * 
   */
  public XWikiHelper() {
    super();
  }

  /**
   * This constructor has package visibility because it was initially
   * written for unit testing purposes.
   * 
   * @param xwikiContext
   */
  public XWikiHelper(XWikiContext xwikiContext) {
    this.xwikiContext = xwikiContext;
  }

  /**
   * The "Atom Authentication Token" is simply the password *as it is stored in the database".
   * This means that if passwords are encrypted, the token is the encrypted password.  Otherwise
   * the token corresponds to the clear text password.
   * 
   * @param mapping
   * @param request
   * @param response
   * @param wsseHeader
   * @return
   * @throws XWikiException
   */
  public String getAtomAuthenticationToken(String userName) throws XWikiException {
    XWiki xwiki = xwikiContext.getWiki();
    XWikiDocument doc = xwiki.getDocument(userName, xwikiContext);
    String passwd = null;
    if (doc.getObject("XWiki.XWikiUsers")!= null) {
       passwd = doc.getStringValue("XWiki.XWikiUsers", "password");
    }
    return passwd;
  }

  public List listUserBlogs(String userName) throws XWikiException, MalformedURLException {
    List userBlogs = new ArrayList();
    XWiki xwiki = xwikiContext.getWiki();
    List searchResults = xwiki.search("select doc from XWikiDocument as doc, BaseObject as obj where obj.className='XWiki.BlogPreferences' and obj.name <> 'XWiki.BlogPreferences' and obj.name = doc.fullName order by obj.name", MAX_BLOGS, 0, xwikiContext);
    Iterator it = searchResults.iterator();
    while (it.hasNext()) {
      XWikiDocument doc = (XWikiDocument)it.next();
      if (xwiki.getRightService().hasAccessLevel("edit", userName, doc.getFullName(), xwikiContext)) {
        UserBlog userBlog = new UserBlog();
        userBlog.setPostHref(doc.getExternalURL("lifeblog", "", xwikiContext));
        userBlog.setFeedHref(doc.getExternalURL("view", "xpage=rdf", xwikiContext));
        userBlog.setAlternateHref(doc.getExternalURL("view", "", xwikiContext));
        userBlogs.add(userBlog);
      }
    }
    return userBlogs;
  }
  
  /**
   * Initializes the XWiki context from a Struts action.
   * 
   * @param context
   * @return
   * @throws XWikiException 
   */
  public void initXWikiContext(
      String action, 
      HttpServletRequest request, 
      HttpServletResponse response,
      ServletContext servletContext) throws XWikiException {
    XWikiRequest xwikiRequest = new XWikiServletRequest(request);
    XWikiResponse xwikiResponse = new XWikiServletResponse(response);
    XWikiContext xwikiContext = Utils.prepareContext(
      action, 
      xwikiRequest, 
      xwikiResponse, 
      new XWikiServletContext(servletContext));
    this.xwikiContext = xwikiContext;
  }

  public URL getURL() {
    return xwikiContext.getURL();
  }

  public String getBlogTitle(String web, String doc) throws XWikiException {
    XWiki xwiki = xwikiContext.getWiki();
    XWikiDocument blogDoc = xwiki.getDocument(web + "." + doc, xwikiContext);
    BaseObject blogPrefs = blogDoc.getObject("XWiki.BlogPreferences");
    return blogPrefs.getStringValue("title");
  }

  public UserBlog [] getUserBlogs() throws XWikiException {
    XWiki xwiki = xwikiContext.getWiki();
    List list = xwiki.search("from ", xwikiContext);
    UserBlog [] blogs = new UserBlog[list.size()];
    Iterator it = list.iterator();
    while (it.hasNext()) {
      XWikiDocument doc = (XWikiDocument)it.next();
      // TODO: Fill blogs array
    }
    return blogs;
  }

  public HttpSession getSession() {
    return xwikiContext.getRequest().getSession();
  }

  public HttpServletRequest getRequest() {
    // TODO Auto-generated method stub
    return xwikiContext.getRequest();
  }

  public HttpServletResponse getResponse() {
    // TODO Auto-generated method stub
    return xwikiContext.getResponse();
  }

  public String getWSSEHeader() {
	return getRequest().getHeader("X-WSSE");
  }
}
